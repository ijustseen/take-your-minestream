package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;

/** Renders popup overlays above batched card text (GuiRenderState root layer). */
public final class GuiLayerFlush {
    private GuiLayerFlush() {
    }

    public static void flushPending(DrawContext context) {
    }

    public static void renderOverlay(DrawContext context, Runnable overlay, int mouseX, int mouseY, float delta) {
        context.createNewRootLayer();
        overlay.run();
    }
}
