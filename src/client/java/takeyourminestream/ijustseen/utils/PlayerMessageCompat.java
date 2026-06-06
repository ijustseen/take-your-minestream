package takeyourminestream.ijustseen.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Отправка короткого сообщения в чат игроку (совместимость 1.21 / 26.1). */
public final class PlayerMessageCompat {
    private PlayerMessageCompat() {}

    public static void send(MinecraftClient client, String message) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.of(message), false);
        }
    }
}
