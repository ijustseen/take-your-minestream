package takeyourminestream.ijustseen.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Открытие экранов (Minecraft 26.1). */
public final class ScreenNavigationCompat {
    private ScreenNavigationCompat() {}

    public static void open(Minecraft client, Screen screen) {
        if (client != null) {
            client.setScreen(screen);
        }
    }

    public static Screen current(Minecraft client) {
        return client != null ? client.screen : null;
    }
}
