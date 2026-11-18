package takeyourminestream.ijustseen.core.render;

import takeyourminestream.ijustseen.core.MessagePanelConstants;

/** Геометрия 9-slice панели сообщений — общая для GUI и 3D-рендера. */
public final class MessagePanel9Slice {
    public static final float UV0 = 0f;
    public static final float UV1 = 8f / MessagePanelConstants.TEX_SIZE;
    public static final float UV2 = 24f / MessagePanelConstants.TEX_SIZE;
    public static final float UV3 = 1f;

    public record GuiSlice(int x, int y, int width, int height, int texU, int texV, int regionWidth, int regionHeight) {}

    public record WorldSlice(int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1) {}

    private MessagePanel9Slice() {}

    public static int clampSize(int size) {
        return Math.max(size, MessagePanelConstants.MIN);
    }

    /**
     * Углы на экране всегда {@link MessagePanelConstants#CORNER} пикселей.
     * В текстуре разрезы стоят на четверти и трёх четвертях стороны, поэтому 32, 64 и 128
     * режутся одинаково: угловой кусок не растягивается, середина тянется.
     */
    public static GuiSlice[] guiSlices(int x, int y, int width, int height, int texSize) {
        width = clampSize(width);
        height = clampSize(height);
        int tex = Math.max(4, texSize);
        int srcC = tex / 4;
        int srcM = tex - srcC * 2;
        int c = MessagePanelConstants.CORNER;

        int x0 = x;
        int x1 = x + c;
        int x2 = x + width - c;
        int y0 = y;
        int y1 = y + c;
        int y2 = y + height - c;
        int u1 = srcC;
        int u2 = srcC + srcM;
        int v1 = srcC;
        int v2 = srcC + srcM;

        return new GuiSlice[] {
            new GuiSlice(x0, y0, c, c, 0, 0, srcC, srcC),
            new GuiSlice(x2, y0, c, c, u2, 0, srcC, srcC),
            new GuiSlice(x0, y2, c, c, 0, v2, srcC, srcC),
            new GuiSlice(x2, y2, c, c, u2, v2, srcC, srcC),
            new GuiSlice(x1, y0, x2 - x1, c, u1, 0, srcM, srcC),
            new GuiSlice(x1, y2, x2 - x1, c, u1, v2, srcM, srcC),
            new GuiSlice(x0, y1, c, y2 - y1, 0, v1, srcC, srcM),
            new GuiSlice(x2, y1, c, y2 - y1, u2, v1, srcC, srcM),
            new GuiSlice(x1, y1, x2 - x1, y2 - y1, u1, v1, srcM, srcM),
        };
    }

    public static WorldSlice[] worldSlices(int x, int y, int width, int height) {
        width = clampSize(width);
        height = clampSize(height);

        int x0 = x;
        int x1 = x + MessagePanelConstants.CORNER;
        int x2 = x + width - MessagePanelConstants.CORNER;
        int x3 = x + width;
        int y0 = y;
        int y1 = y + MessagePanelConstants.CORNER;
        int y2 = y + height - MessagePanelConstants.CORNER;
        int y3 = y + height;

        return new WorldSlice[] {
            new WorldSlice(x0, y0, x1, y1, UV0, UV0, UV1, UV1),
            new WorldSlice(x2, y0, x3, y1, UV2, UV0, UV3, UV1),
            new WorldSlice(x0, y2, x1, y3, UV0, UV2, UV1, UV3),
            new WorldSlice(x2, y2, x3, y3, UV2, UV2, UV3, UV3),
            new WorldSlice(x1, y0, x2, y1, UV1, UV0, UV2, UV1),
            new WorldSlice(x1, y2, x2, y3, UV1, UV2, UV2, UV3),
            new WorldSlice(x0, y1, x1, y2, UV0, UV1, UV1, UV2),
            new WorldSlice(x2, y1, x3, y2, UV2, UV1, UV3, UV2),
            new WorldSlice(x1, y1, x2, y2, UV1, UV1, UV2, UV2),
        };
    }
}
