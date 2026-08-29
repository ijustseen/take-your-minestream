package takeyourminestream.ijustseen.core;

import takeyourminestream.ijustseen.config.ModConfig;

/** Пользовательские цвета панели сообщения (заливка и бортик). */
public final class MessagePanelColors {
    private MessagePanelColors() {}

    /** Тон заливки {@code message_panel.png}; 0x000000 — чёрная панель по умолчанию. */
    public static int baseRgb() {
        return ModConfig.getPANEL_BASE_COLOR_RGB();
    }

    /** Цвет бортика: фирменный цвет платформы или заданный вручную. */
    public static int borderRgb(int platformRgb) {
        return ModConfig.isPANEL_BORDER_FROM_PLATFORM()
            ? platformRgb & 0xFFFFFF
            : ModConfig.getPANEL_BORDER_COLOR_RGB();
    }
}
