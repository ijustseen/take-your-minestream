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

        int thumb = thumbHeight(trackHeight, totalContentHeight);
        int thumbY = thumbTop(top, bottom, scrollOffset, totalContentHeight);
        context.fill(x, thumbY, x + 6, thumbY + thumb, ModUiTheme.SCROLL_THUMB);
    }

    public static int thumbHeight(int trackHeight, int totalContentHeight) {
        if (trackHeight <= 0 || totalContentHeight <= trackHeight) {
            return Math.max(0, trackHeight);
        }
        return Math.max(16, (trackHeight * trackHeight) / totalContentHeight);
    }

    public static int thumbTop(int top, int bottom, int scrollOffset, int totalContentHeight) {
        int trackHeight = bottom - top;
        int thumb = thumbHeight(trackHeight, totalContentHeight);
        if (totalContentHeight <= trackHeight || trackHeight <= thumb) {
            return top;
        }
        int travel = trackHeight - thumb;
        int maxScroll = totalContentHeight - trackHeight;
        int clamped = Math.max(0, Math.min(maxScroll, scrollOffset));
        return top + (clamped * travel) / maxScroll;
    }

    /** scrollOffset для бегунка, чей верх стоит в thumbTop. */
    public static int scrollForThumbTop(int top, int bottom, int thumbTop, int totalContentHeight) {
        int trackHeight = bottom - top;
        int thumb = thumbHeight(trackHeight, totalContentHeight);
        int travel = trackHeight - thumb;
        if (travel <= 0 || totalContentHeight <= trackHeight) {
            return 0;
        }
        int maxScroll = totalContentHeight - trackHeight;
        int local = Math.max(0, Math.min(travel, thumbTop - top));
        return (local * maxScroll) / travel;
    }

    public static int clampScroll(int scrollOffset, int totalContentHeight, int viewportHeight) {
        return Math.max(0, Math.min(Math.max(0, totalContentHeight - viewportHeight), scrollOffset));
    }
}
