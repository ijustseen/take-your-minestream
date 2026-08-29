package takeyourminestream.ijustseen.messages;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Пиксельные бейджи ролей автора. Как и иконка платформы, едут вместе с сообщением
 * в виде {@link MessageEmote} с провайдером {@link #PROVIDER} и индексами -1.
 */
public final class RoleBadges {
    /** Провайдер эмоута-маркера: assets/take-your-stream-chat/textures/badge/&lt;key&gt;.png */
    public static final String PROVIDER = "badge";
    public static final int ICON_SIZE = 8;
    /** Отступ между бейджами в 3D-панели (как у эмоутов). */
    public static final int WORLD_SPACING = 1;
    /** Отступ между бейджами в GUI (как у {@code drawGuiIcon}). */
    public static final int GUI_SPACING = 2;

    private RoleBadges() {}

    /** Ключи бейджей от старшей роли к младшей; пустой список, если ролей нет. */
    public static List<String> keysFor(boolean subscriber, boolean vip, boolean moderator, boolean broadcaster) {
        List<String> keys = new ArrayList<>(4);
        if (broadcaster) {
            keys.add("host");
        }
        if (moderator) {
            keys.add("mod");
        }
        if (vip) {
            keys.add("vip");
        }
        if (subscriber) {
            keys.add("sub");
        }
        return keys;
    }

    public static Identifier iconTexture(String key) {
        return Identifier.of("take-your-stream-chat", "textures/badge/" + key + ".png");
    }

    /** Ширина ряда бейджей в 3D-панели вместе с отступом после последнего. */
    public static int worldRowWidth(int badgeCount) {
        return badgeCount <= 0 ? 0 : badgeCount * (ICON_SIZE + WORLD_SPACING);
    }

    /** Ширина ряда бейджей в GUI вместе с отступом после последнего. */
    public static int guiRowWidth(int badgeCount) {
        return badgeCount <= 0 ? 0 : badgeCount * (ICON_SIZE + GUI_SPACING);
    }
}
