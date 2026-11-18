package takeyourminestream.ijustseen.core.render;

import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.storage.StoragePaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Заливка и бортик панели. Файлы в jar не перезаписываются:
 * рисунок пользователя лежит в {@code take-your-stream-chat/textures/}.
 */
public final class MessagePanelTextures {
    private static final Identifier DYNAMIC_PANEL = Identifier.of("take-your-stream-chat", "dynamic/message_panel");
    private static final Identifier DYNAMIC_BORDER = Identifier.of("take-your-stream-chat", "dynamic/message_panel_border");
    private static final String BUNDLED_PANEL = "assets/take-your-stream-chat/textures/gui/message_panel.png";
    private static final String BUNDLED_BORDER = "assets/take-your-stream-chat/textures/gui/message_panel_border.png";

    private static boolean loaded;
    private static int[] panelPixels = blank(MessagePanelConstants.TEX_SIZE);
    private static int[] borderPixels = blank(MessagePanelConstants.TEX_SIZE);
    private static int panelSize = MessagePanelConstants.TEX_SIZE;
    private static int borderSize = MessagePanelConstants.TEX_SIZE;
    private static boolean panelCustom;
    private static boolean borderCustom;
    private static boolean panelDirty;
    private static boolean borderDirty;
    private static int revision;
    private static int shapeStamp;

    private MessagePanelTextures() {}

    public static Identifier panel() {
        ensureLoaded();
        flush();
        return panelCustom ? DYNAMIC_PANEL : MessagePanelConstants.PANEL_TEXTURE;
    }

    public static Identifier border() {
        ensureLoaded();
        flush();
        return borderCustom ? DYNAMIC_BORDER : MessagePanelConstants.PANEL_BORDER_TEXTURE;
    }

    public static int sizeOf(Identifier texture) {
        ensureLoaded();
        if (DYNAMIC_PANEL.equals(texture)) {
            return panelSize;
        }
        if (DYNAMIC_BORDER.equals(texture)) {
            return borderSize;
        }
        return MessagePanelConstants.TEX_SIZE;
    }

    /** Меняется, когда слой переключается между файлом мода и рисунком игрока. */
    public static int revision() {
        return revision;
    }

    /** Меняется, когда у заливки или бортика меняется прозрачность или размер. */
    public static int shapeStamp() {
        ensureLoaded();
        return shapeStamp;
    }

    public static int size(boolean border) {
        ensureLoaded();
        return border ? borderSize : panelSize;
    }

    public static int argb(boolean border, int x, int y) {
        ensureLoaded();
        int size = size(border);
        if (x < 0 || y < 0 || x >= size || y >= size) {
            return 0;
        }
        int[] pixels = border ? borderPixels : panelPixels;
        return pixels[y * size + x];
    }

    public static boolean paint(boolean borderLayer, int x, int y, int argb) {
        ensureLoaded();
        int size = size(borderLayer);
        if (x < 0 || y < 0 || x >= size || y >= size) {
            return false;
        }
        int[] pixels = borderLayer ? borderPixels : panelPixels;
        int index = y * size + x;
        if (pixels[index] == argb) {
            return false;
        }
        if ((pixels[index] >>> 24) != (argb >>> 24)) {
            shapeStamp++;
        }
        pixels[index] = argb;
        markCustom(borderLayer);
        return true;
    }

    public static int[] copyPixels(boolean borderLayer) {
        ensureLoaded();
        return Arrays.copyOf(pixels(borderLayer), pixels(borderLayer).length);
    }

    /** Подменяет весь слой. Копия остаётся у вызывающего снимка. */
    public static void replace(boolean borderLayer, int[] source) {
        ensureLoaded();
        int size = (int) Math.round(Math.sqrt(source.length));
        if (size < 1 || size * size != source.length) {
            return;
        }
        int[] copy = Arrays.copyOf(source, source.length);
        if (borderLayer) {
            borderPixels = copy;
            borderSize = size;
        } else {
            panelPixels = copy;
            panelSize = size;
        }
        markCustom(borderLayer);
        shapeStamp++;
        save(borderLayer);
    }

    public static int nextSize(int size) {
        if (size < 64) {
            return 64;
        }
        if (size < 128) {
            return 128;
        }
        return 32;
    }

    public static void resize(boolean borderLayer, int requested) {
        ensureLoaded();
        int target = snap(requested);
        int current = size(borderLayer);
        if (target == current) {
            return;
        }
        int[] scaled = scale(pixels(borderLayer), current, target);
        if (borderLayer) {
            borderPixels = scaled;
            borderSize = target;
        } else {
            panelPixels = scaled;
            panelSize = target;
        }
        markCustom(borderLayer);
        shapeStamp++;
        save(borderLayer);
    }

    /** Возвращает слой к текстуре из мода и удаляет файл игрока. */
    public static void restore(boolean borderLayer) {
        ensureLoaded();
        boolean wasCustom = borderLayer ? borderCustom : panelCustom;
        int[] bundled = readBundled(borderLayer);
        if (borderLayer) {
            borderPixels = bundled;
            borderSize = side(bundled);
            borderCustom = false;
            borderDirty = false;
        } else {
            panelPixels = bundled;
            panelSize = side(bundled);
            panelCustom = false;
            panelDirty = false;
        }
        deleteFile(borderLayer);
        PanelTextureUpload.free(borderLayer ? DYNAMIC_BORDER : DYNAMIC_PANEL);
        shapeStamp++;
        if (wasCustom) {
            revision++;
        }
    }

    public static void save(boolean borderLayer) {
        ensureLoaded();
        if (!(borderLayer ? borderCustom : panelCustom)) {
            return;
        }
        Path path = file(borderLayer);
        try {
            Files.createDirectories(path.getParent());
            int size = size(borderLayer);
            int[] pixels = pixels(borderLayer);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    image.setRGB(x, y, pixels[y * size + x]);
                }
            }
            ImageIO.write(image, "png", path.toFile());
        } catch (IOException e) {
            TakeYourMineStreamClient.LOGGER.warn("Failed to save message panel texture", e);
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        loadLayer(false);
        loadLayer(true);
    }

    private static void loadLayer(boolean borderLayer) {
        int[] fromFile = readFile(file(borderLayer));
        if (fromFile != null) {
            if (borderLayer) {
                borderPixels = fromFile;
                borderSize = side(fromFile);
                borderCustom = true;
                borderDirty = true;
            } else {
                panelPixels = fromFile;
                panelSize = side(fromFile);
                panelCustom = true;
                panelDirty = true;
            }
            return;
        }
        int[] bundled = readBundled(borderLayer);
        if (borderLayer) {
            borderPixels = bundled;
            borderSize = side(bundled);
        } else {
            panelPixels = bundled;
            panelSize = side(bundled);
        }
    }

    private static void flush() {
        if (panelDirty && panelCustom) {
            PanelTextureUpload.upload(DYNAMIC_PANEL, panelPixels, panelSize);
            panelDirty = false;
        }
        if (borderDirty && borderCustom) {
            PanelTextureUpload.upload(DYNAMIC_BORDER, borderPixels, borderSize);
            borderDirty = false;
        }
    }

    private static void markCustom(boolean borderLayer) {
        boolean was = borderLayer ? borderCustom : panelCustom;
        if (borderLayer) {
            borderCustom = true;
            borderDirty = true;
        } else {
            panelCustom = true;
            panelDirty = true;
        }
        if (!was) {
            revision++;
        }
    }

    private static int[] pixels(boolean borderLayer) {
        return borderLayer ? borderPixels : panelPixels;
    }

    private static Path file(boolean borderLayer) {
        String name = borderLayer ? "message_panel_border.png" : "message_panel.png";
        return StoragePaths.getModRootDir().resolve("textures").resolve(name);
    }

    private static void deleteFile(boolean borderLayer) {
        try {
            Files.deleteIfExists(file(borderLayer));
        } catch (IOException e) {
            TakeYourMineStreamClient.LOGGER.warn("Failed to delete message panel texture", e);
        }
    }

    private static int[] readBundled(boolean borderLayer) {
        String path = borderLayer ? BUNDLED_BORDER : BUNDLED_PANEL;
        try (InputStream in = MessagePanelTextures.class.getClassLoader().getResourceAsStream(path)) {
            int[] pixels = readStream(in);
            if (pixels != null) {
                return pixels;
            }
        } catch (IOException ignored) {
        }
        return blank(MessagePanelConstants.TEX_SIZE);
    }

    private static int[] readFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return readStream(in);
        } catch (IOException e) {
            TakeYourMineStreamClient.LOGGER.warn("Failed to read message panel texture", e);
            return null;
        }
    }

    private static int[] readStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
            return null;
        }
        int size = snap(Math.max(image.getWidth(), image.getHeight()));
        int[] raw = new int[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                raw[y * image.getWidth() + x] = image.getRGB(x, y);
            }
        }
        if (image.getWidth() == size && image.getHeight() == size) {
            return raw;
        }
        return scale(raw, image.getWidth(), image.getHeight(), size);
    }

    private static int[] scale(int[] src, int srcSize, int dstSize) {
        return scale(src, srcSize, srcSize, dstSize);
    }

    private static int[] scale(int[] src, int srcW, int srcH, int dstSize) {
        int[] dst = new int[dstSize * dstSize];
        for (int y = 0; y < dstSize; y++) {
            int sy = Math.min(srcH - 1, y * srcH / dstSize);
            for (int x = 0; x < dstSize; x++) {
                int sx = Math.min(srcW - 1, x * srcW / dstSize);
                dst[y * dstSize + x] = src[sy * srcW + sx];
            }
        }
        return dst;
    }

    private static int[] blank(int size) {
        int[] pixels = new int[size * size];
        Arrays.fill(pixels, 0xFFFFFFFF);
        return pixels;
    }

    private static int side(int[] pixels) {
        int size = (int) Math.round(Math.sqrt(pixels.length));
        return snap(size);
    }

    public static int snap(int size) {
        if (size <= 48) {
            return 32;
        }
        if (size <= 96) {
            return 64;
        }
        return 128;
    }
}
