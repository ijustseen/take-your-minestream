package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;

/** Popup fills on RenderLayer.getGuiOverlay() so card text stays underneath (Minecraft 1.21.1). */
public final class PopupGuiRenderer {
    private PopupGuiRenderer() {
    }

    public static void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(RenderLayer.getGuiOverlay(), x1, y1, x2, y2, color);
    }
}
