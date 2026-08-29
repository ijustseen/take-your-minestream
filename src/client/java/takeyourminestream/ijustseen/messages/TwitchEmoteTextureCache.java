package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.core.storage.StoragePaths;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TwitchEmoteTextureCache {
    private static final Map<String, Identifier> LOADED_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedTextureSet> ANIMATED_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, NativeImageBackedTexture> TEXTURE_REFS = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> IMAGE_BYTES_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_OR_DONE = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<String> REUPLOADING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<PendingUpload> UPLOAD_QUEUE = new ConcurrentLinkedQueue<>();
    private static final int MAX_TEXTURES_PER_TICK = 3;
    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "TYMS-EmoteLoader");
        thread.setDaemon(true);
        return thread;
    });

    private static final Path CACHE_DIR;

    static {
        Path modRoot = StoragePaths.getModRootDir();
        Path legacyRoot = StoragePaths.getLegacyModRootDir();
        StoragePaths.migrateDirectoryIfNeeded(legacyRoot.resolve("emote-cache"), modRoot.resolve("emote-cache"));
        CACHE_DIR = modRoot.resolve("emote-cache");
    }

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

    private static final class PendingUpload {
        private final String key;
        private final String provider;
        private final String emoteId;
        private final EmoteImageCodec.DecodedEmote decoded;
        private final boolean replace;

        private PendingUpload(String key, String provider, String emoteId, EmoteImageCodec.DecodedEmote decoded, boolean replace) {
            this.key = key;
            this.provider = provider;
            this.emoteId = emoteId;
            this.decoded = decoded;
            this.replace = replace;
        }

        private int textureCount() {
            return decoded.textureCount();
        }

        private void close() {
            decoded.close();
        }
    }

    private static final String BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    public static void registerClientTick() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> drainUploads(client));
        DOWNLOAD_EXECUTOR.execute(EmojiTextureCache::warmup);
    }

    private static String getEmoteUrl(String provider, String emoteId) {
        if (emoteId != null && (emoteId.startsWith("http://") || emoteId.startsWith("https://"))) {
            return emoteId;
        }
        return switch (provider) {
            case "7tv" -> String.format(SEVENTV_URL_PNG, emoteId);
            default -> String.format(TWITCH_URL, emoteId);
        };
    }

    private static String getSafeTexturePathPart(String emoteId) {
        String lowered = emoteId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        if (lowered.length() > 48) {
            lowered = lowered.substring(0, 48);
        }
        String hash = Integer.toHexString(emoteId.hashCode());
        return lowered + "_" + hash;
    }

    private static byte[] downloadImageBytes(String provider, String emoteId) {
        if ("emoji".equals(provider)) {
            String sequence = EmojiTextureCache.sequenceFor(emoteId);
            if (sequence == null || sequence.isEmpty()) {
                return null;
            }
            return EmojiTextureCache.rasterizeToPng(sequence);
        }
        if (!"7tv".equals(provider)) {
            return downloadFromUrl(getEmoteUrl(provider, emoteId), provider, emoteId);
        }

        byte[] pngBytes = downloadFromUrl(String.format(SEVENTV_URL_PNG, emoteId), provider, emoteId);
        if (pngBytes != null) return pngBytes;

        return downloadFromUrl(String.format(SEVENTV_URL_GIF, emoteId), provider, emoteId);
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
            connection.setRequestProperty("User-Agent", BROWSER_USER_AGENT);
            connection.setRequestProperty("Accept", "image/png,image/jpeg,image/webp,image/*;q=0.8");
            if (url.contains("ggpht.com") || url.contains("googleusercontent.com")
                || url.contains("youtube.com") || url.contains("ytimg.com")) {
                connection.setRequestProperty("Referer", "https://www.youtube.com/");
            } else if (url.contains("files.kick.com") || url.contains("kick.com")) {
                connection.setRequestProperty("Referer", "https://kick.com/");
            } else if (url.contains("tiktok") || url.contains("ibyteimg.com") || url.contains("byteimg.com")) {
                connection.setRequestProperty("Referer", "https://www.tiktok.com/");
            }

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

    private static String cacheKey(String provider, String emoteId) {
        return provider + ":" + emoteId;
    }

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

        if ("platform".equals(provider)) {
            return Identifier.of("take-your-stream-chat", "textures/platform/" + emoteId + ".png");
        }

        if ("emoji".equals(provider)) {
            String sequence = EmojiTextureCache.sequenceFor(emoteId);
            if (sequence != null && !sequence.isEmpty()) {
                EmojiTextureCache.remember(emoteId, sequence);
            }
        }

        String key = cacheKey(provider, emoteId);
        AnimatedTextureSet animated = ANIMATED_TEXTURES.get(key);
        if (animated != null) {
            return animated.currentFrameId();
        }

        Identifier existing = LOADED_TEXTURES.get(key);
        if (existing != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                net.minecraft.client.texture.AbstractTexture tex = client.getTextureManager().getTexture(existing);
                NativeImageBackedTexture ourTex = TEXTURE_REFS.get(key);
                if (ourTex != null && tex != ourTex) {
                    reRegisterFromCache(key, provider, emoteId);
                    return existing;
                }
            }
            return existing;
        }

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

    public static boolean isFailed(String provider, String emoteId) {
        return FAILED.contains(cacheKey(provider, emoteId));
    }

    /** Локальные иконки сразу готовы; удалённые — когда загружены или окончательно не открылись. */
    public static boolean isResolved(String provider, String emoteId) {
        if (provider == null || emoteId == null || emoteId.isBlank()) {
            return true;
        }
        if ("platform".equals(provider) || RoleBadges.PROVIDER.equals(provider)) {
            return true;
        }
        return isLoaded(provider, emoteId) || isFailed(provider, emoteId);
    }

    public static boolean areEmotesReady(java.util.Collection<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return true;
        }
        for (MessageEmote emote : emotes) {
            if (!isResolved(emote.getProvider(), emote.getEmoteId())) {
                return false;
            }
        }
        return true;
    }

    /** Кладёт готовые байты в фоновую очередь декодирования. */
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
        if (PENDING_OR_DONE.add(key)) {
            DOWNLOAD_EXECUTOR.execute(() -> decodeAndEnqueue(key, provider, emoteId, imageBytes, false));
        }
    }

    private static void reRegisterFromCache(String key, String provider, String emoteId) {
        if (!REUPLOADING.add(key)) {
            return;
        }
        byte[] bytes = IMAGE_BYTES_CACHE.get(key);
        if (bytes == null) {
            bytes = loadFromDiskCache(provider, emoteId);
            if (bytes == null) {
                REUPLOADING.remove(key);
                return;
            }
            IMAGE_BYTES_CACHE.put(key, bytes);
        }
        final byte[] imageBytes = bytes;
        DOWNLOAD_EXECUTOR.execute(() -> {
            try {
                decodeAndEnqueue(key, provider, emoteId, imageBytes, true);
            } finally {
                REUPLOADING.remove(key);
            }
        });
    }

    private static Path diskCachePath(String provider, String emoteId) {
        String fileName = emoteId.matches("[a-zA-Z0-9._-]+")
            ? emoteId + ".png"
            : getSafeTexturePathPart(emoteId) + ".png";
        return CACHE_DIR.resolve(provider).resolve(fileName);
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

    private static void downloadAndRegister(String provider, String emoteId) {
        String key = cacheKey(provider, emoteId);

        byte[] imageBytes = loadFromDiskCache(provider, emoteId);
        if (imageBytes != null && imageBytes.length > 0) {
            IMAGE_BYTES_CACHE.put(key, imageBytes);
            decodeAndEnqueue(key, provider, emoteId, imageBytes, false);
            return;
        }

        try {
            imageBytes = downloadImageBytes(provider, emoteId);
            if (imageBytes == null || imageBytes.length == 0) {
                markFailed(key);
                return;
            }

            saveToDiskCache(provider, emoteId, imageBytes);
            IMAGE_BYTES_CACHE.put(key, imageBytes);
            decodeAndEnqueue(key, provider, emoteId, imageBytes, false);
        } catch (Exception e) {
            markFailed(key);
            TakeYourMineStreamClient.LOGGER.warn("Error downloading {} emote {}: {}", provider, emoteId, e.getMessage());
        }
    }

    private static void decodeAndEnqueue(String key, String provider, String emoteId, byte[] imageBytes, boolean replace) {
        if (!replace && (LOADED_TEXTURES.containsKey(key) || ANIMATED_TEXTURES.containsKey(key))) {
            return;
        }
        try {
            EmoteImageCodec.DecodedEmote decoded = EmoteImageCodec.decode(imageBytes, provider);
            if (decoded == null || decoded.textureCount() == 0) {
                markFailed(key);
                TakeYourMineStreamClient.LOGGER.warn("Failed to decode image for {} emote {}", provider, emoteId);
                return;
            }
            UPLOAD_QUEUE.add(new PendingUpload(key, provider, emoteId, decoded, replace));
        } catch (Exception e) {
            markFailed(key);
            TakeYourMineStreamClient.LOGGER.error("Error decoding {} emote {}", provider, emoteId, e);
        }
    }

    private static void markFailed(String key) {
        FAILED.add(key);
    }

    static void drainUploads(MinecraftClient client) {
        if (client == null) {
            return;
        }

        int uploaded = 0;
        while (uploaded < MAX_TEXTURES_PER_TICK) {
            PendingUpload next = UPLOAD_QUEUE.peek();
            if (next == null) {
                break;
            }
            int cost = Math.max(1, next.textureCount());
            if (uploaded > 0 && uploaded + cost > MAX_TEXTURES_PER_TICK) {
                break;
            }
            UPLOAD_QUEUE.poll();
            uploaded += uploadDecoded(next, client);
        }
    }

    private static int uploadDecoded(PendingUpload pending, MinecraftClient client) {
        String key = pending.key;
        if (!pending.replace && (LOADED_TEXTURES.containsKey(key) || ANIMATED_TEXTURES.containsKey(key))) {
            pending.close();
            return 0;
        }

        try {
            EmoteImageCodec.DecodedEmote decoded = pending.decoded;
            if (decoded.animated()) {
                return registerAnimated(key, pending.provider, pending.emoteId, decoded.frames(), client);
            }

            NativeImage image = decoded.image();
            if (image == null) {
                markFailed(key);
                return 0;
            }
            NativeImageBackedTexture texture = createBackedTexture(
                "tyms-emote-" + pending.provider + "-" + pending.emoteId,
                image
            );
            Identifier textureId = Identifier.of(
                "take-your-stream-chat",
                "emotes/" + pending.provider + "/" + getSafeTexturePathPart(pending.emoteId)
            );
            client.getTextureManager().registerTexture(textureId, texture);
            TEXTURE_REFS.put(key, texture);
            LOADED_TEXTURES.put(key, textureId);
            ANIMATED_TEXTURES.remove(key);
            return 1;
        } catch (Exception e) {
            markFailed(key);
            TakeYourMineStreamClient.LOGGER.error("Error creating texture for {} emote {}", pending.provider, pending.emoteId, e);
            return 0;
        }
    }

    private static int registerAnimated(
        String key,
        String provider,
        String emoteId,
        List<EmoteImageCodec.DecodedFrame> frames,
        MinecraftClient client
    ) {
        List<Identifier> frameIds = new ArrayList<>(frames.size());
        List<Integer> frameDurations = new ArrayList<>(frames.size());
        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++) {
            EmoteImageCodec.DecodedFrame frame = frames.get(frameIndex);
            Identifier frameId = Identifier.of(
                "take-your-stream-chat",
                "emotes/" + provider + "/" + getSafeTexturePathPart(emoteId) + "_f" + frameIndex
            );
            NativeImageBackedTexture frameTexture = createBackedTexture(
                "tyms-emote-" + provider + "-" + emoteId + "-f" + frameIndex,
                frame.image()
            );
            client.getTextureManager().registerTexture(frameId, frameTexture);
            frameIds.add(frameId);
            frameDurations.add(frame.delayMs());
        }
        ANIMATED_TEXTURES.put(key, new AnimatedTextureSet(List.copyOf(frameIds), List.copyOf(frameDurations)));
        LOADED_TEXTURES.remove(key);
        return frameIds.size();
    }

    private static NativeImageBackedTexture createBackedTexture(String debugName, NativeImage image) {
        //? if >=1.21.8 {
        return new NativeImageBackedTexture(() -> debugName, image);
        //?} else {
        return new NativeImageBackedTexture(image);
        //?}
    }
}
