package takeyourminestream.ijustseen.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Открытие экранов (совместимость между версиями Minecraft). */
public final class ScreenNavigationCompat {
    private ScreenNavigationCompat() {}

    public static void open(MinecraftClient client, Screen screen) {
        if (client != null) {
            client.setScreen(screen);
        }
    }

    public static Screen current(MinecraftClient client) {
        return client != null ? client.currentScreen : null;
    }
}
