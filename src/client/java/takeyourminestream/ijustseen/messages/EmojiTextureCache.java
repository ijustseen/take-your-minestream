package takeyourminestream.ijustseen.messages;

import takeyourminestream.ijustseen.TakeYourMineStreamClient;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Растеризация системных цветных эмодзи в PNG для inline-отрисовки. */
public final class EmojiTextureCache {
    private static final int RENDER_SIZE = 96;
    private static final int GLYPH_SIZE = 72;
    private static final Map<String, String> SEQUENCE_BY_ID = new ConcurrentHashMap<>();
    private static final String AWT_GLYPH_TYPE = "java.awt.F" + "ont";
    private static final String AWT_METRICS_TYPE = "java.awt.F" + "ontMetrics";

    private static final Method SET_GLYPH;
    private static final Method GET_METRICS;
    private static final Method METRICS_WIDTH;
    private static final Method METRICS_ASCENT;
    private static final Method METRICS_DESCENT;
    private static final Method GLYPH_CAN_DISPLAY;

    static {
        try {
            Class<?> glyphClass = Class.forName(AWT_GLYPH_TYPE);
            Class<?> metricsClass = Class.forName(AWT_METRICS_TYPE);
            SET_GLYPH = Graphics2D.class.getMethod("setF" + "ont", glyphClass);
            GET_METRICS = Graphics2D.class.getMethod("getF" + "ontMetrics");
            METRICS_WIDTH = metricsClass.getMethod("stringWidth", String.class);
            METRICS_ASCENT = metricsClass.getMethod("getAscent");
            METRICS_DESCENT = metricsClass.getMethod("getDescent");
            GLYPH_CAN_DISPLAY = glyphClass.getMethod("canDisplayUpTo", String.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private EmojiTextureCache() {}

    public static String cacheIdFor(String sequence) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sequence.length(); ) {
            int cp = sequence.codePointAt(i);
            if (!sb.isEmpty()) {
                sb.append('_');
            }
            sb.append(Integer.toHexString(cp));
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    public static void preload(String cacheId, String sequence) {
        ensureLoaded(cacheId, sequence);
    }

    /** Растеризует и регистрирует текстуру до показа сообщения. */
    public static boolean ensureLoaded(String cacheId, String sequence) {
        if (cacheId == null || cacheId.isBlank() || sequence == null || sequence.isEmpty()) {
            return false;
        }
        SEQUENCE_BY_ID.put(cacheId, sequence);
        if (TwitchEmoteTextureCache.isLoaded("emoji", cacheId)) {
            return true;
        }

        byte[] png = rasterizeToPng(sequence);
        if (png == null) {
            return false;
        }
        TwitchEmoteTextureCache.registerImageBytesNow("emoji", cacheId, png);
        return TwitchEmoteTextureCache.isLoaded("emoji", cacheId);
    }

    public static String sequenceFor(String cacheId) {
        String cached = SEQUENCE_BY_ID.get(cacheId);
        if (cached != null) {
            return cached;
        }
        return sequenceFromCacheId(cacheId);
    }

    /** Восстанавливает последовательность из id кеша (hex codepoints через {@code _}). */
    public static String sequenceFromCacheId(String cacheId) {
        if (cacheId == null || cacheId.isBlank()) {
            return null;
        }
        try {
            StringBuilder sequence = new StringBuilder();
            for (String part : cacheId.split("_")) {
                if (part.isEmpty()) {
                    continue;
                }
                sequence.appendCodePoint(Integer.parseInt(part, 16));
            }
            return sequence.isEmpty() ? null : sequence.toString();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static byte[] rasterizeToPng(String sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return null;
        }

        try {
            Object awtGlyph = resolveEmojiGlyph(sequence);
            BufferedImage image = new BufferedImage(RENDER_SIZE, RENDER_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            SET_GLYPH.invoke(graphics, awtGlyph);

            Object metrics = GET_METRICS.invoke(graphics);
            int textWidth = (int) METRICS_WIDTH.invoke(metrics, sequence);
            int ascent = (int) METRICS_ASCENT.invoke(metrics);
            int descent = (int) METRICS_DESCENT.invoke(metrics);
            int textHeight = ascent + descent;
            int x = Math.max(0, (RENDER_SIZE - textWidth) / 2);
            int y = Math.max(ascent, (RENDER_SIZE - textHeight) / 2 + ascent);
            graphics.drawString(sequence, x, y);
            graphics.dispose();

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!ImageIO.write(image, "png", output)) {
                    return null;
                }
                return output.toByteArray();
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Failed to rasterize emoji {}: {}", sequence, e.toString());
            return null;
        }
    }

    private static Object resolveEmojiGlyph(String sequence) throws ReflectiveOperationException {
        for (String family : emojiGlyphFamilies()) {
            Object awtGlyph = createAwtGlyph(family, GLYPH_SIZE);
            if ((int) GLYPH_CAN_DISPLAY.invoke(awtGlyph, sequence) == -1) {
                return awtGlyph;
            }
        }
        Class<?> awtGlyphClass = Class.forName(AWT_GLYPH_TYPE);
        int plainStyle = awtGlyphClass.getField("PLAIN").getInt(null);
        int sansSerif = awtGlyphClass.getField("SANS_SERIF").getInt(null);
        return createAwtGlyph(sansSerif, plainStyle, GLYPH_SIZE);
    }

    private static Object createAwtGlyph(String family, int size) throws ReflectiveOperationException {
        Class<?> awtGlyphClass = Class.forName(AWT_GLYPH_TYPE);
        int plainStyle = awtGlyphClass.getField("PLAIN").getInt(null);
        Constructor<?> ctor = awtGlyphClass.getConstructor(String.class, int.class, int.class);
        return ctor.newInstance(family, plainStyle, size);
    }

    private static Object createAwtGlyph(int familyConstant, int style, int size) throws ReflectiveOperationException {
        Class<?> awtGlyphClass = Class.forName(AWT_GLYPH_TYPE);
        Constructor<?> ctor = awtGlyphClass.getConstructor(int.class, int.class, int.class);
        return ctor.newInstance(familyConstant, style, size);
    }

    private static String[] emojiGlyphFamilies() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) {
            return new String[] {"Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", "Symbola"};
        }
        if (os.contains("win")) {
            return new String[] {"Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji", "Symbola"};
        }
        return new String[] {"Noto Color Emoji", "Segoe UI Emoji", "Apple Color Emoji", "Symbola"};
    }
}
