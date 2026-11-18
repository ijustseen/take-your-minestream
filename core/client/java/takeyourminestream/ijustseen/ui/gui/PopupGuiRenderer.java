package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;

/** Popup fills on the default GUI layer (1.21.8+ uses createNewRootLayer in GuiLayerFlush). */
public final class PopupGuiRenderer {
    private PopupGuiRenderer() {
    }

    public static void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }
}
