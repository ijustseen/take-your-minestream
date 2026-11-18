package takeyourminestream.ijustseen.integration.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.utils.PlayerMessageCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Вывод локализованных причин ошибок подключения в чат игрока, без спама при ретраях. */
public final class ChatErrorReporter {
    private static final Map<ChatPlatform, String> lastReported = new ConcurrentHashMap<>();

    private ChatErrorReporter() {}

    /** Сообщает игроку причину ошибки; повторно одна и та же причина не выводится. */
    public static void report(ChatPlatform platform, Exception error) {
        if (error instanceof ChatConnectException cce && isOfflineStatus(cce)) {
            ChatStatusNotifier.setOfflineWarning(platform, cce.getTranslationKey(), cce.getArgs());
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            Text reason = error instanceof ChatConnectException cce
                ? Text.translatable(cce.getTranslationKey(), cce.getArgs())
                : Text.translatable(
                    "takeyourstreamchat.chat.error.generic",
                    platform.getDisplayName(),
                    String.valueOf(error.getMessage())
                );
            String message = "§c" + reason.getString();
            String previous = lastReported.put(platform, message);
            if (message.equals(previous)) {
                return;
            }
            PlayerMessageCompat.send(client, message);
        });
    }

    /** Сбрасывает дедупликацию (после успешного подключения или отключения платформы). */
    public static void clear(ChatPlatform platform) {
        lastReported.remove(platform);
        ChatStatusNotifier.clearOfflineWarning(platform);
    }

    private static boolean isOfflineStatus(ChatConnectException error) {
        String key = error.getTranslationKey();
        return "takeyourstreamchat.chat.error.no_live_room".equals(key)
            || "takeyourstreamchat.chat.error.not_live".equals(key);
    }
}
