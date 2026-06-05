package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.DrawContext;

/** Переиспользуемый вертикальный скроллбар для экранов мода. */
public final class GuiScrollbar {
    private GuiScrollbar() {}

    public static void draw(DrawContext context, int x, int top, int bottom, int scrollOffset, int totalContentHeight) {
        int trackHeight = bottom - top;
        context.fill(x, top, x + 6, bottom, ModUiTheme.SCROLL_TRACK);

        if (totalContentHeight <= trackHeight) {
            return;
        }

        int thumbHeight = Math.max(16, (trackHeight * trackHeight) / totalContentHeight);
        int maxThumbY = bottom - thumbHeight;
        int thumbY = top + (scrollOffset * (trackHeight - thumbHeight)) / (totalContentHeight - trackHeight);
        thumbY = Math.max(top, Math.min(maxThumbY, thumbY));
        context.fill(x, thumbY, x + 6, thumbY + thumbHeight, ModUiTheme.SCROLL_THUMB);
    }

    public static int clampScroll(int scrollOffset, int totalContentHeight, int viewportHeight) {
        return Math.max(0, Math.min(Math.max(0, totalContentHeight - viewportHeight), scrollOffset));
    }
}
