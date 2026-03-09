package takeyourminestream.ijustseen.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.utils.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Хранилище закреплённых сообщений на уровне мира/сервера.
 */
public final class PinnedMessageStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STORE_DIR = "take-your-minestream/pinned-messages";
    private static final Type FILE_TYPE = new TypeToken<PinnedMessagesFile>() {}.getType();

    private PinnedMessageStore() {}

    public static synchronized void loadForCurrentWorld(MessageLifecycleManager lifecycleManager) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || lifecycleManager == null) {
            return;
        }

        removeAllPinnedInMemory(lifecycleManager);

        Path file = getStorageFile(client);
        ensureFileExists(file, resolveWorldKey(client));

        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            PinnedMessagesFile data = GSON.fromJson(reader, FILE_TYPE);
            if (data == null || data.messages == null) {
                return;
            }

            for (PinnedMessageEntry entry : data.messages) {
                if (entry == null || entry.text == null || entry.text.isBlank()) {
                    continue;
                }

                Message message = new Message(
                    entry.text,
                    new Vec3d(entry.x, entry.y, entry.z),
                    lifecycleManager.getTickCounter(),
                    entry.yaw,
                    entry.pitch,
                    entry.authorColorRgb
                );
                message.setPinned(true);
                lifecycleManager.addPinnedMessage(message);
            }
            Logger.info("Загружено закреплённых сообщений: " + data.messages.size());
        } catch (Exception e) {
            Logger.error("Не удалось загрузить закреплённые сообщения", e);
        }
    }

    public static synchronized void saveForCurrentWorld(MessageLifecycleManager lifecycleManager) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || lifecycleManager == null) {
            return;
        }

        Path file = getStorageFile(client);
        String worldKey = resolveWorldKey(client);

        List<PinnedMessageEntry> entries = lifecycleManager.getActiveMessages().stream()
            .filter(Message::isPinned)
            .sorted(Comparator.comparing(Message::getText))
            .map(message -> {
                PinnedMessageEntry entry = new PinnedMessageEntry();
                entry.text = message.getText();
                entry.x = message.getPosition().x;
                entry.y = message.getPosition().y;
                entry.z = message.getPosition().z;
                entry.yaw = message.getYaw();
                entry.pitch = message.getPitch();
                entry.authorColorRgb = message.getAuthorColorRgb();
                return entry;
            })
            .toList();

        PinnedMessagesFile data = new PinnedMessagesFile();
        data.schemaVersion = 1;
        data.worldKey = worldKey;
        data.messages = new ArrayList<>(entries);

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, FILE_TYPE, writer);
            }
        } catch (IOException e) {
            Logger.error("Не удалось сохранить закреплённые сообщения", e);
        }
    }

    private static void removeAllPinnedInMemory(MessageLifecycleManager lifecycleManager) {
        List<Message> activeMessages = new ArrayList<>(lifecycleManager.getActiveMessages());
        for (Message message : activeMessages) {
            if (message.isPinned()) {
                lifecycleManager.removeMessageWithoutParticles(message);
            }
        }
    }

    private static void ensureFileExists(Path file, String worldKey) {
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                PinnedMessagesFile data = new PinnedMessagesFile();
                data.schemaVersion = 1;
                data.worldKey = worldKey;
                data.messages = new ArrayList<>();
                try (Writer writer = Files.newBufferedWriter(file)) {
                    GSON.toJson(data, FILE_TYPE, writer);
                }
            }
        } catch (IOException e) {
            Logger.error("Не удалось подготовить файл закреплённых сообщений", e);
        }
    }

    private static Path getStorageFile(MinecraftClient client) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        String worldKey = resolveWorldKey(client);
        String fileName = sanitize(worldKey) + "-" + shortHash(worldKey) + ".json";
        return configDir.resolve(STORE_DIR).resolve(fileName);
    }

    private static String resolveWorldKey(MinecraftClient client) {
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
            return "server:" + serverInfo.address.toLowerCase(Locale.ROOT).trim();
        }

        if (client.getServer() != null) {
            try {
                String levelName = client.getServer().getSaveProperties().getLevelName();
                Path levelPath = client.getServer().getSavePath(WorldSavePath.ROOT);
                String folder = levelPath.getFileName() != null ? levelPath.getFileName().toString() : "singleplayer";
                return "world:" + levelName + ":" + folder;
            } catch (Exception ignored) {
                // fallback ниже
            }
        }

        if (client.world != null) {
            return "world-registry:" + client.world.getRegistryKey().getValue();
        }

        return "unknown-world";
    }

    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String shortHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private static final class PinnedMessagesFile {
        int schemaVersion;
        String worldKey;
        List<PinnedMessageEntry> messages;
    }

    private static final class PinnedMessageEntry {
        String text;
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        Integer authorColorRgb;
    }
}
