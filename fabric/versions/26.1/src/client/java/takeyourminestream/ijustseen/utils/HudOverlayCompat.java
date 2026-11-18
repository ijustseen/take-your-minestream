package takeyourminestream.ijustseen.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Action bar / overlay сообщения (Minecraft 26.x). */
public final class HudOverlayCompat {
    private HudOverlayCompat() {}

    public static void setOverlayMessage(Minecraft client, Component text, boolean tinted) {
        if (client != null && client.gui != null) {
            client.gui.setOverlayMessage(text, tinted);
        }
    }
}
