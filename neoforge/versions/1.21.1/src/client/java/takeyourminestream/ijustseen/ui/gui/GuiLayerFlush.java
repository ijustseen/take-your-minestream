package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;

/** Flushes batched GUI geometry on Minecraft 1.21.1 (vertex consumer, no root layers). */
public final class GuiLayerFlush {
    private GuiLayerFlush() {
    }

    public static void flushPending(DrawContext context) {
        context.draw();
    }

    public static void renderOverlay(DrawContext context, Runnable overlay, int mouseX, int mouseY, float delta) {
        context.draw();
        overlay.run();
        context.draw();
    }
}
