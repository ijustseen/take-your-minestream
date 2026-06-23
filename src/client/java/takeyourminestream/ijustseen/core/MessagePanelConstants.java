package takeyourminestream.ijustseen.core;

import net.minecraft.util.Identifier;

/** Общие параметры 9-slice панели сообщений (GUI и 3D). */
public final class MessagePanelConstants {
    public static final Identifier PANEL_TEXTURE = Identifier.of("take-your-stream-chat", "textures/gui/message_panel.png");
    /** Фон панели без бортика. */
    public static final Identifier PANEL_BASE_TEXTURE = Identifier.of("take-your-stream-chat", "textures/gui/message_panel_base.png");
    /** Белый бортик панели — тонируется цветом платформы. */
    public static final Identifier PANEL_BORDER_TEXTURE = Identifier.of("take-your-stream-chat", "textures/gui/message_panel_border.png");
    public static final Identifier PIN_TEXTURE = Identifier.of("take-your-stream-chat", "textures/gui/pin.png");

    /** Цвет бортика по умолчанию (исходный фиолетовый из текстуры). */
    public static final int DEFAULT_BORDER_RGB = 0x6B00E3;

    public static final int TEX_SIZE = 32;
    public static final int CORNER = 8;
    public static final int MIN = CORNER * 2;
    public static final int PADDING_X = 6;
    public static final int PADDING_Y = 4;
    /** Ширина переноса текста сообщения (как в 3D-режиме). */
    public static final int MESSAGE_WRAP_WIDTH = 120;
    /** Макс. дистанция клика / перетаскивания 3D-сообщений (блоки). */
    public static final double MESSAGE_INTERACTION_DISTANCE = 16.0;
    /** Фиксированное время падения / затухания (0.5 с @ 20 TPS). */
    public static final int MESSAGE_FALL_TICKS = 10;
    public static final int PIN_ICON_SIZE = 8;
    public static final int PIN_ICON_MARGIN = 1;
    /** Насколько иконка закрепа выступает над/вбок от панели (половина размера + отступ). */
    public static final int PIN_ICON_OVERFLOW = (PIN_ICON_SIZE / 2) + PIN_ICON_MARGIN;

    private MessagePanelConstants() {}
}
