package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

/**
 * Когда показывать HUD-сообщения поверх экрана.
 * Инвентарь и контейнеры — да; ESC-меню, чат, настройки — нет.
 */
public final class MessageHudVisibility {
    private MessageHudVisibility() {}

    public static boolean shouldRender(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return false;
        }
        Screen screen = client.currentScreen;
        if (screen == null) {
            return true;
        }
        return screen instanceof HandledScreen;
    }
}
