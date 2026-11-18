package takeyourminestream.ijustseen.messages;

import takeyourminestream.ijustseen.integration.chat.ChatPlatform;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Сообщения автора мода: приписка [dev] и своя рамка у зрителей с модом. */
public final class ModAuthor {
    public static final String NAME_PREFIX = "§d§l[dev]§r ";
    /** Старший бит: этот цвет рамки не подменяется настройкой зрителя. */
    public static final int FORCE_BORDER_BIT = 0x01000000;

    private static final Map<ChatPlatform, Set<String>> NAMES = Map.of(
        ChatPlatform.TWITCH, Set.of("ijustseen"),
        ChatPlatform.TIKTOK, Set.of("ijustseen_u", "ijustseen_you"),
        ChatPlatform.KICK, Set.of("ijustseen")
    );

    private ModAuthor() {}

    public static boolean matches(ChatPlatform platform, String login, String displayName) {
        Set<String> names = NAMES.get(platform);
        if (names == null) {
            return false;
        }
        return names.contains(normalize(login)) || names.contains(normalize(displayName));
    }

    public static boolean isMarked(Message message) {
        return message != null && message.getText() != null && message.getText().startsWith(NAME_PREFIX);
    }

    /** Цвет бортика: радуга у автора, иначе цвет платформы. */
    public static int borderRgb(Message message) {
        int platformRgb = ChatPlatform.accentColorForIconKey(
            message == null ? null : message.getPlatformIconKey()
        );
        if (!isMarked(message)) {
            return platformRgb;
        }
        return rainbowRgb() | FORCE_BORDER_BIT;
    }

    public static int rainbowRgb() {
        int step = (int) ((System.currentTimeMillis() / 120L) % 24L);
        return java.awt.Color.HSBtoRGB(step / 24f, 0.8f, 1f) & 0xFFFFFF;
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        while (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
