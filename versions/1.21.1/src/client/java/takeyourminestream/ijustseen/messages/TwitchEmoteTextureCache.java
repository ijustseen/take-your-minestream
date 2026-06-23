package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.core.storage.StoragePaths;

import com.mojang.blaze3d.systems.RenderSystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

public final class TwitchEmoteTextureCache {
    private static final Map<String, Identifier> LOADED_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTextureSet> ANIMATED_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, NativeImageBackedTexture> TEXTURE_REFS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> IMAGE_BYTES_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_OR_DONE = ConcurrentHashMap.newKeySet();
    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "TYMS-EmoteLoader");
        thread.setDaemon(true);
        return thread;
    });

    // Папка дискового кеша
    private static final Path CACHE_DIR;

    static {
        Path modRoot = StoragePaths.getModRootDir();
        Path legacyRoot = StoragePaths.getLegacyModRootDir();
        StoragePaths.migrateDirectoryIfNeeded(legacyRoot.resolve("emote-cache"), modRoot.resolve("emote-cache"));
        CACHE_DIR = modRoot.resolve("emote-cache");
    }

    // CDN URL-шаблоны по провайдерам
    private static final String TWITCH_URL = "https://static-cdn.jtvnw.net/emoticons/v2/%s/static/dark/1.0";
    private static final String SEVENTV_URL_PNG = "https://cdn.7tv.app/emote/%s/1x.png";
    private static final String SEVENTV_URL_GIF = "https://cdn.7tv.app/emote/%s/1x.gif";

    private TwitchEmoteTextureCache() {
    }

    private static final class AnimatedTextureSet {
        private final List<Identifier> frameTextureIds;
        private final List<Integer> frameDurationsMs;
        private final long totalDurationMs;
        private final long startTimeMs;

        private AnimatedTextureSet(List<Identifier> frameTextureIds, List<Integer> frameDurationsMs) {
            this.frameTextureIds = frameTextureIds;
            this.frameDurationsMs = frameDurationsMs;
            long total = 0L;
            for (int delay : frameDurationsMs) {
                total += Math.max(20, delay);
            }
            this.totalDurationMs = Math.max(20L, total);
            this.startTimeMs = System.currentTimeMillis();
        }

        private Identifier currentFrameId() {
            if (frameTextureIds.isEmpty()) return null;
            if (frameTextureIds.size() == 1) return frameTextureIds.get(0);

            long elapsed = (System.currentTimeMillis() - startTimeMs) % totalDurationMs;
            long cursor = 0L;
            for (int i = 0; i < frameTextureIds.size(); i++) {
                int delay = Math.max(20, frameDurationsMs.get(i));
                cursor += delay;
                if (elapsed < cursor) {
                    return frameTextureIds.get(i);
                }
            }
            return frameTextureIds.get(0);
        }
    }

    /**
     * Возвращает URL для скачивания эмоута по провайдеру и ID.
     */
    private static String getEmoteUrl(String provider, String emoteId) {
        return switch (provider) {
            case "7tv" -> String.format(SEVENTV_URL_PNG, emoteId);
            default -> String.format(TWITCH_URL, emoteId);
        };
    }

    private static String getSafeTexturePathPart(String emoteId) {
        String lowered = emoteId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        String hash = Integer.toHexString(emoteId.hashCode());
        return lowered + "_" + hash;
    }

    private static byte[] downloadImageBytes(String provider, String emoteId) {
        if ("emoji".equals(provider)) {
            String sequence = EmojiTextureCache.sequenceFor(emoteId);
            if (sequence == null || sequence.isEmpty()) {
                return null;
            }
            EmojiTextureCache.preload(emoteId, sequence);
            return EmojiTextureCache.rasterizeToPng(sequence);
        }
        if (!"7tv".equals(provider)) {
            return downloadFromUrl(getEmoteUrl(provider, emoteId), provider, emoteId);
        }

        byte[] pngBytes = downloadFromUrl(String.format(SEVENTV_URL_PNG, emoteId), provider, emoteId);
        if (pngBytes != null) return pngBytes;

        return downloadFromUrl(String.format(SEVENTV_URL_GIF, emoteId), provider, emoteId);
    }

    private static NativeImage decodeNativeImage(byte[] imageBytes, String provider, String emoteId) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            NativeImage image = NativeImage.read(bais);
            if (image != null) {
                return image;
            }
        } catch (Exception ignored) {
        }

        // Fallback for formats unsupported by NativeImage input reader (e.g. GIF frames)
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            BufferedImage buffered = ImageIO.read(bais);
            if (buffered == null) {
                return null;
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (!ImageIO.write(buffered, "png", baos)) {
                    return null;
                }

                try (ByteArrayInputStream pngInput = new ByteArrayInputStream(baos.toByteArray())) {
                    return NativeImage.read(pngInput);
                }
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Failed fallback decode for {} emote {}: {}", provider, emoteId, e.getMessage());
            return null;
        }
    }

    private static byte[] downloadFromUrl(String url, String provider, String emoteId) {
        HttpURLConnection connection = null;
        try {
            URI emoteUri = URI.create(url);
            connection = (HttpURLConnection) emoteUri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "TakeYourMineStream/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                TakeYourMineStreamClient.LOGGER.warn("HTTP {} for {} emote {} url={}", responseCode, provider, emoteId, url);
                return null;
            }

            try (InputStream stream = connection.getInputStream()) {
                byte[] bytes = stream.readAllBytes();
                if (bytes.length == 0) {
                    return null;
                }
                return bytes;
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Error downloading {} emote {} from {}: {}", provider, emoteId, url, e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Ключ кеша — комбинация провайдера и ID (чтобы не было конфликтов).
     */
    private static String cacheKey(String provider, String emoteId) {
        return provider + ":" + emoteId;
    }

    /**
     * Предзагрузка текстуры — вызывается сразу при парсинге эмоутов.
     */
    public static void preload(String emoteId) {
        preload("twitch", emoteId);
    }

    public static void preload(String provider, String emoteId) {
        if (emoteId == null || emoteId.isBlank()) return;
        String key = cacheKey(provider, emoteId);
        if (LOADED_TEXTURES.containsKey(key) || ANIMATED_TEXTURES.containsKey(key)) return;
        if (PENDING_OR_DONE.add(key)) {
            DOWNLOAD_EXECUTOR.execute(() -> downloadAndRegister(provider, emoteId));
        }
    }

    public static Identifier getTextureIdentifier(String emoteId) {
        return getTextureIdentifier("twitch", emoteId);
    }

    public static Identifier getTextureIdentifier(String provider, String emoteId) {
        if (emoteId == null || emoteId.isBlank()) {
            return null;
        }

        // Встроенные пиксельные иконки платформ (из ресурсов мода, без скачивания)
        if ("platform".equals(provider)) {
            return Identifier.of("take-your-stream-chat", "textures/platform/" + emoteId + ".png");
        }

        if ("emoji".equals(provider)) {
            String sequence = EmojiTextureCache.sequenceFor(emoteId);
            if (sequence != null && !sequence.isEmpty()) {
                EmojiTextureCache.ensureLoaded(emoteId, sequence);
            }
        }

        String key = cacheKey(provider, emoteId);
        AnimatedTextureSet animated = ANIMATED_TEXTURES.get(key);
        if (animated != null) {
            return animated.currentFrameId();
        }

        Identifier existing = LOADED_TEXTURES.get(key);
        if (existing != null) {
            // Проверяем, что текстура всё ещё валидна в TextureManager
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                net.minecraft.client.texture.AbstractTexture tex = client.getTextureManager().getTexture(existing);
                NativeImageBackedTexture ourTex = TEXTURE_REFS.get(key);
                if (ourTex != null && tex != ourTex) {
                    reRegisterFromCache(key, provider, emoteId, client);
                    return existing;
                }
            }
            return existing;
        }

        // Запускаем скачивание
        if (PENDING_OR_DONE.add(key)) {
            DOWNLOAD_EXECUTOR.execute(() -> downloadAndRegister(provider, emoteId));
        }

        return null;
    }

    public static boolean isLoaded(String emoteId) {
        return isLoaded("twitch", emoteId);
    }

    public static boolean isLoaded(String provider, String emoteId) {
        String key = cacheKey(provider, emoteId);
        return LOADED_TEXTURES.containsKey(key) || ANIMATED_TEXTURES.containsKey(key);
    }

    /** Синхронная регистрация уже готовых байтов (локальные emoji PNG). */
    public static void registerImageBytesNow(String provider, String emoteId, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0 || emoteId == null || emoteId.isBlank()) {
            return;
        }

        String key = cacheKey(provider, emoteId);
        if (LOADED_TEXTURES.containsKey(key) || ANIMATED_TEXTURES.containsKey(key)) {
            return;
        }

        IMAGE_BYTES_CACHE.put(key, imageBytes);
        saveToDiskCache(provider, emoteId, imageBytes);
        PENDING_OR_DONE.add(key);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        Runnable task = () -> registerImageBytesImmediate(key, provider, emoteId, imageBytes, client);
        if (RenderSystem.isOnRenderThread()) {
            task.run();
        } else {
            client.execute(task);
        }
    }

    /**
     * Перерегистрирует текстуру из кеша байтов
     */
    private static void reRegisterFromCache(String key, String provider, String emoteId, MinecraftClient client) {
        byte[] bytes = IMAGE_BYTES_CACHE.get(key);
        if (bytes == null) {
            // Попробовать загрузить с диска
            bytes = loadFromDiskCache(provider, emoteId);
            if (bytes == null) return;
            IMAGE_BYTES_CACHE.put(key, bytes);
        }
        registerOnRenderThread(key, provider, emoteId, bytes);
    }

    // ─── Дисковый кеш ───

    private static Path diskCachePath(String provider, String emoteId) {
        return CACHE_DIR.resolve(provider).resolve(emoteId + ".png");
    }

    private static byte[] loadFromDiskCache(String provider, String emoteId) {
        Path path = diskCachePath(provider, emoteId);
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (Exception e) {
                TakeYourMineStreamClient.LOGGER.warn("Error reading emote disk cache for {}:{}: {}", provider, emoteId, e.getMessage());
            }
        }
        return null;
    }

    private static void saveToDiskCache(String provider, String emoteId, byte[] imageBytes) {
        Path path = diskCachePath(provider, emoteId);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, imageBytes);
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Error saving emote disk cache for {}:{}: {}", provider, emoteId, e.getMessage());
        }
    }

    // ─── Скачивание и регистрация ───

    private static void downloadAndRegister(String provider, String emoteId) {
        String key = cacheKey(provider, emoteId);

        // 1. Попробовать дисковый кеш
        byte[] imageBytes = loadFromDiskCache(provider, emoteId);
        if (imageBytes != null && imageBytes.length > 0) {
            IMAGE_BYTES_CACHE.put(key, imageBytes);
            registerOnRenderThread(key, provider, emoteId, imageBytes);
            return;
        }

        // 2. Скачать из CDN
        try {
            imageBytes = downloadImageBytes(provider, emoteId);
            if (imageBytes == null) {
                return;
            }

            if (imageBytes.length == 0) {
                return;
            }

            // Сохранить на диск
            saveToDiskCache(provider, emoteId, imageBytes);
            IMAGE_BYTES_CACHE.put(key, imageBytes);
            registerOnRenderThread(key, provider, emoteId, imageBytes);

        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Error downloading {} emote {}: {}", provider, emoteId, e.getMessage());
        }
    }

    private static void registerOnRenderThread(String key, String provider, String emoteId, byte[] imageBytes) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> registerImageBytesImmediate(key, provider, emoteId, imageBytes, client));
    }

    private static void registerImageBytesImmediate(String key,
                                                    String provider,
                                                    String emoteId,
                                                    byte[] imageBytes,
                                                    MinecraftClient client) {
        try {
            if ("7tv".equals(provider) && isGifData(imageBytes)) {
                if (registerAnimatedGifFrames(key, provider, emoteId, imageBytes, client)) {
                    return;
                }
            }

            NativeImage image = decodeNativeImage(imageBytes, provider, emoteId);
            if (image == null) {
                TakeYourMineStreamClient.LOGGER.warn("Failed to decode image for {} emote {}", provider, emoteId);
                return;
            }

            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            Identifier textureId = Identifier.of("take-your-stream-chat", "emotes/" + provider + "/" + getSafeTexturePathPart(emoteId));
            client.getTextureManager().registerTexture(textureId, texture);
            TEXTURE_REFS.put(key, texture);
            LOADED_TEXTURES.put(key, textureId);
            ANIMATED_TEXTURES.remove(key);
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Error creating texture for {} emote {}", provider, emoteId, e);
        }
    }

    private static boolean isGifData(byte[] bytes) {
        return bytes != null
                && bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';
    }

    private static boolean registerAnimatedGifFrames(String key,
                                                     String provider,
                                                     String emoteId,
                                                     byte[] imageBytes,
                                                     MinecraftClient client) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                return false;
            }

            ImageReader gifReader = readers.next();
            try {
                gifReader.setInput(imageInputStream, false, false);
                int frameCount = gifReader.getNumImages(true);
                if (frameCount <= 0) {
                    return false;
                }

                int[] canvasSize = getGifCanvasSize(gifReader);
                int canvasWidth = Math.max(1, canvasSize[0]);
                int canvasHeight = Math.max(1, canvasSize[1]);
                BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                BufferedImage previousCanvas = null;

                List<Identifier> frameIds = new ArrayList<>(frameCount);
                List<Integer> frameDurations = new ArrayList<>(frameCount);

                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    BufferedImage frame = gifReader.read(frameIndex);
                    if (frame == null) continue;
                    final int stableFrameIndex = frameIndex;

                    GifFrameMeta meta = readGifFrameMeta(gifReader.getImageMetadata(frameIndex));
                    if ("restoreToPrevious".equals(meta.disposalMethod)) {
                        previousCanvas = copyBufferedImage(canvas);
                    }

                    Graphics2D drawGraphics = canvas.createGraphics();
                    drawGraphics.drawImage(frame, meta.left, meta.top, null);
                    drawGraphics.dispose();

                    BufferedImage composedFrame = copyBufferedImage(canvas);

                    NativeImage nativeFrame = bufferedImageToNativeImage(composedFrame);
                    if (nativeFrame == null) continue;

                    Identifier frameId = Identifier.of(
                            "take-your-stream-chat",
                            "emotes/" + provider + "/" + getSafeTexturePathPart(emoteId) + "_f" + frameIndex
                    );
                    NativeImageBackedTexture frameTexture = new NativeImageBackedTexture(nativeFrame);
                    client.getTextureManager().registerTexture(frameId, frameTexture);

                    frameIds.add(frameId);
                    frameDurations.add(meta.delayMs);

                    if ("restoreToBackgroundColor".equals(meta.disposalMethod)) {
                        Graphics2D clearGraphics = canvas.createGraphics();
                        clearGraphics.setComposite(AlphaComposite.Clear);
                        clearGraphics.fillRect(meta.left, meta.top, frame.getWidth(), frame.getHeight());
                        clearGraphics.dispose();
                    } else if ("restoreToPrevious".equals(meta.disposalMethod) && previousCanvas != null) {
                        canvas = copyBufferedImage(previousCanvas);
                        previousCanvas = null;
                    }
                }

                if (frameIds.isEmpty()) {
                    return false;
                }

                ANIMATED_TEXTURES.put(key, new AnimatedTextureSet(List.copyOf(frameIds), List.copyOf(frameDurations)));
                LOADED_TEXTURES.remove(key);
                return true;
            } finally {
                gifReader.dispose();
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Error parsing GIF for {} emote {}: {}", provider, emoteId, e.getMessage());
            return false;
        }
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage bufferedImage) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(bufferedImage, "png", baos)) {
                return null;
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                return NativeImage.read(bais);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage copyBufferedImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static int[] getGifCanvasSize(ImageReader reader) {
        try {
            IIOMetadata streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                String format = streamMeta.getNativeMetadataFormatName();
                if (format != null) {
                    Node root = streamMeta.getAsTree(format);
                    Node child = root.getFirstChild();
                    while (child != null) {
                        if ("LogicalScreenDescriptor".equals(child.getNodeName())) {
                            NamedNodeMap attrs = child.getAttributes();
                            if (attrs != null) {
                                Node w = attrs.getNamedItem("logicalScreenWidth");
                                Node h = attrs.getNamedItem("logicalScreenHeight");
                                if (w != null && h != null) {
                                    return new int[] { Integer.parseInt(w.getNodeValue()), Integer.parseInt(h.getNodeValue()) };
                                }
                            }
                            break;
                        }
                        child = child.getNextSibling();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new int[] { 28, 28 };
    }

    private static final class GifFrameMeta {
        final int left;
        final int top;
        final int delayMs;
        final String disposalMethod;

        GifFrameMeta(int left, int top, int delayMs, String disposalMethod) {
            this.left = left;
            this.top = top;
            this.delayMs = delayMs;
            this.disposalMethod = disposalMethod;
        }
    }

    private static GifFrameMeta readGifFrameMeta(IIOMetadata metadata) {
        int left = 0;
        int top = 0;
        int delayMs = 100;
        String disposalMethod = "none";

        if (metadata == null) {
            return new GifFrameMeta(left, top, delayMs, disposalMethod);
        }

        try {
            String formatName = metadata.getNativeMetadataFormatName();
            if (formatName == null) {
                return new GifFrameMeta(left, top, delayMs, disposalMethod);
            }

            Node root = metadata.getAsTree(formatName);
            Node child = root.getFirstChild();
            while (child != null) {
                if ("GraphicControlExtension".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    if (attrs != null) {
                        Node delayNode = attrs.getNamedItem("delayTime");
                        if (delayNode != null) {
                            int centiseconds = Integer.parseInt(delayNode.getNodeValue());
                            delayMs = Math.max(20, centiseconds * 10);
                        }
                        Node disposalNode = attrs.getNamedItem("disposalMethod");
                        if (disposalNode != null) {
                            disposalMethod = disposalNode.getNodeValue();
                        }
                    }
                } else if ("ImageDescriptor".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    if (attrs != null) {
                        Node leftNode = attrs.getNamedItem("imageLeftPosition");
                        Node topNode = attrs.getNamedItem("imageTopPosition");
                        if (leftNode != null) {
                            left = Integer.parseInt(leftNode.getNodeValue());
                        }
                        if (topNode != null) {
                            top = Integer.parseInt(topNode.getNodeValue());
                        }
                    }
                }
                child = child.getNextSibling();
            }
        } catch (Exception ignored) {
        }

        return new GifFrameMeta(left, top, delayMs, disposalMethod);
    }

    private static int extractGifFrameDelayMs(IIOMetadata metadata) {
        if (metadata == null) return 100;
        try {
            String formatName = metadata.getNativeMetadataFormatName();
            if (formatName == null) return 100;
            Node root = metadata.getAsTree(formatName);
            Node child = root.getFirstChild();
            while (child != null) {
                if ("GraphicControlExtension".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    if (attrs != null) {
                        Node delayNode = attrs.getNamedItem("delayTime");
                        if (delayNode != null) {
                            int centiseconds = Integer.parseInt(delayNode.getNodeValue());
                            return Math.max(20, centiseconds * 10);
                        }
                    }
                    break;
                }
                child = child.getNextSibling();
            }
        } catch (Exception ignored) {
        }
        return 100;
    }
}
