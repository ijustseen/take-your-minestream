package takeyourminestream.ijustseen.core;

import net.minecraft.util.Identifier;

/** Общие параметры 9-slice панели сообщений (GUI и 3D). */
public final class MessagePanelConstants {
    public static final Identifier PANEL_TEXTURE = Identifier.of("take-your-minestream", "textures/gui/message_panel.png");
    public static final Identifier PIN_TEXTURE = Identifier.of("take-your-minestream", "textures/gui/pin.png");

    public static final int TEX_SIZE = 32;
    public static final int CORNER = 8;
    public static final int MIN = CORNER * 2;
    public static final int PADDING_X = 6;
    public static final int PADDING_Y = 4;
    public static final int PIN_ICON_SIZE = 8;
    public static final int PIN_ICON_MARGIN = 1;

    private MessagePanelConstants() {}
}
