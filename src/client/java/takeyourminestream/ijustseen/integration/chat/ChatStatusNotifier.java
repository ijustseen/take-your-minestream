package takeyourminestream.ijustseen.integration.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Статус подключения на экране (action bar), без засорения чата. */
public final class ChatStatusNotifier {
    private static final Map<String, Long> LAST_SHOWN_MS = new ConcurrentHashMap<>();
    private static final long DEDUPE_MS = 8_000L;

    /** Активные «не в эфире» по платформам — компактная строка на action bar. */
    private static final Map<ChatPlatform, OfflineEntry> OFFLINE_WARNINGS = new ConcurrentHashMap<>();

    private ChatStatusNotifier() {}

    private record OfflineEntry(ChatPlatform platform, String channel) {}

    public static void showInfo(Text text) {
        show(text, false);
    }

    public static void showWarning(Text text) {
        show(Text.literal(text.getString()).formatted(Formatting.YELLOW), false);
    }

    public static void showSuccess(Text text) {
        show(Text.literal(text.getString()).formatted(Formatting.GREEN), false);
    }

    public static void showTranslatable(String key, Object... args) {
        showInfo(Text.translatable(key, args));
    }

    public static void showTranslatableWarning(String key, Object... args) {
        showWarning(Text.translatable(key, args));
    }

    public static void showTranslatableSuccess(String key, Object... args) {
        showSuccess(Text.translatable(key, args));
    }

    /** Добавляет/обновляет предупреждение «не в эфире» для платформы; на экране видны все сразу. */
    public static void setOfflineWarning(ChatPlatform platform, String key, Object... args) {
        String channel = extractChannel(args);
        OfflineEntry entry = new OfflineEntry(platform, channel);
        OfflineEntry previous = OFFLINE_WARNINGS.get(platform);
        if (entry.equals(previous)) {
            return;
        }
        OFFLINE_WARNINGS.put(platform, entry);
        refreshOfflineOverlay();
    }

    public static void clearOfflineWarning(ChatPlatform platform) {
        if (OFFLINE_WARNINGS.remove(platform) != null) {
            refreshOfflineOverlay();
        }
    }

    private static String extractChannel(Object... args) {
        if (args != null && args.length >= 2 && args[1] != null) {
            return String.valueOf(args[1]);
        }
        return "";
    }

    private static void refreshOfflineOverlay() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.inGameHud == null) {
                return;
            }
            if (OFFLINE_WARNINGS.isEmpty()) {
                client.inGameHud.setOverlayMessage(Text.empty(), false);
                return;
            }

            List<OfflineEntry> entries = new ArrayList<>();
            for (ChatPlatform platform : ChatPlatform.values()) {
                OfflineEntry entry = OFFLINE_WARNINGS.get(platform);
                if (entry != null) {
                    entries.add(entry);
                }
            }

            Text overlay = buildCompactOfflineOverlay(client, entries);
            client.inGameHud.setOverlayMessage(overlay, false);
        });
    }

    private static Text buildCompactOfflineOverlay(MinecraftClient client, List<OfflineEntry> entries) {
        TextRenderer textRenderer = client.textRenderer;
        int maxWidth = Math.max(120, client.getWindow().getScaledWidth() - 24);

        if (entries.size() == 1) {
            return fitSingleOfflineOverlay(textRenderer, maxWidth, entries.getFirst());
        }

        String platformList = joinPlatformNames(entries);
        Text many = Text.translatable(
            "takeyourstreamchat.status.offline_many",
            platformList
        ).formatted(Formatting.YELLOW);
        if (textRenderer.getWidth(many) <= maxWidth) {
            return many;
        }

        return Text.translatable(
            "takeyourstreamchat.status.offline_count",
            entries.size()
        ).formatted(Formatting.YELLOW);
    }

    private static String joinPlatformNames(List<OfflineEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries.get(i).platform.getDisplayName());
        }
        return sb.toString();
    }

    private static Text fitSingleOfflineOverlay(TextRenderer textRenderer, int maxWidth, OfflineEntry entry) {
        String platformName = entry.platform.getDisplayName();
        String channel = entry.channel.isBlank() ? "" : entry.channel;

        if (channel.isBlank()) {
            return Text.translatable(
                "takeyourstreamchat.status.offline_platform",
                platformName
            ).formatted(Formatting.YELLOW);
        }

        for (int len = channel.length(); len >= 1; len--) {
            String part = len == channel.length()
                ? channel
                : channel.substring(0, len) + "…";
            Text candidate = Text.translatable(
                "takeyourstreamchat.status.offline_one",
                platformName,
                part
            ).formatted(Formatting.YELLOW);
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                return candidate;
            }
        }

        return Text.translatable(
            "takeyourstreamchat.status.offline_platform",
            platformName
        ).formatted(Formatting.YELLOW);
    }

    private static void show(Text text, boolean tinted) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.inGameHud == null) {
                return;
            }
            String dedupeKey = text.getString();
            long now = System.currentTimeMillis();
            Long previous = LAST_SHOWN_MS.get(dedupeKey);
            if (previous != null && now - previous < DEDUPE_MS) {
                return;
            }
            LAST_SHOWN_MS.put(dedupeKey, now);
            client.inGameHud.setOverlayMessage(text, tinted);
        });
    }

    public static void clearDedupe() {
        LAST_SHOWN_MS.clear();
        OFFLINE_WARNINGS.clear();
    }
}
