package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;

/** 1.20.1 рисует GUI сразу, отдельного flush у DrawContext нет. */
public final class GuiLayerFlush {
    private GuiLayerFlush() {
    }

    public static void flushPending(DrawContext context) {
    }

    public static void renderOverlay(DrawContext context, Runnable overlay, int mouseX, int mouseY, float delta) {
        overlay.run();
    }
}
