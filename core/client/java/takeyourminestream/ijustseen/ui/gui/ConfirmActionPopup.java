package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/** Модальное подтверждение поверх текущего экрана (Да / Отмена). */
public final class ConfirmActionPopup {
    public enum Entry { CONFIRM, CANCEL }

    private static final int MIN_WIDTH = 220;
    private static final int MAX_WIDTH = 320;
    private static final int PADDING = 12;
    private static final int TITLE_GAP = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_WIDTH = 88;

    private Text title = Text.empty();
    private Text confirmLabel = Text.empty();
    private Text cancelLabel = Text.empty();
    private Runnable onConfirm = () -> {};
    private int x;
    private int y;
    private int width;
    private int height;
    private int titleBottom;
    private boolean open;
    private Entry hoveredEntry;

    public boolean isOpen() {
        return open;
    }

    public void open(
        int screenWidth,
        int screenHeight,
        TextRenderer textRenderer,
        Text title,
        Text confirmLabel,
        Text cancelLabel,
        Runnable onConfirm
    ) {
        this.title = title;
        this.confirmLabel = confirmLabel;
        this.cancelLabel = cancelLabel;
        this.onConfirm = onConfirm != null ? onConfirm : () -> {};

        int contentWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, textRenderer.getWidth(title) + PADDING * 2));
        List<OrderedText> lines = textRenderer.wrapLines(title, contentWidth - PADDING * 2);
        int titleHeight = Math.max(textRenderer.fontHeight, lines.size() * textRenderer.fontHeight);
        width = contentWidth;
        height = PADDING + titleHeight + TITLE_GAP + BUTTON_HEIGHT + PADDING;
        x = Math.max(8, (screenWidth - width) / 2);
        y = Math.max(8, (screenHeight - height) / 2);
        titleBottom = y + PADDING + titleHeight;
        open = true;
        hoveredEntry = null;
    }

    public void dismiss() {
        open = false;
        hoveredEntry = null;
        onConfirm = () -> {};
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!open) {
            return;
        }

        PopupGuiRenderer.fill(context, 0, 0, screenWidth, screenHeight, ModUiTheme.OVERLAY_DIM);
        drawPanel(context, x, y, width, height);

        List<OrderedText> lines = textRenderer.wrapLines(title, width - PADDING * 2);
        int lineY = y + PADDING;
        for (OrderedText line : lines) {
            int lineWidth = textRenderer.getWidth(line);
            context.drawTextWithShadow(
                textRenderer,
                line,
                x + (width - lineWidth) / 2,
                lineY,
                ModUiTheme.TEXT_PRIMARY
            );
            lineY += textRenderer.fontHeight;
        }

        hoveredEntry = hitTest(mouseX, mouseY);
        renderButton(context, textRenderer, confirmRect(), confirmLabel, hoveredEntry == Entry.CONFIRM, true);
        renderButton(context, textRenderer, cancelRect(), cancelLabel, hoveredEntry == Entry.CANCEL, false);
    }

    public Entry hitTest(int mouseX, int mouseY) {
        if (!open) {
            return null;
        }
        if (contains(confirmRect(), mouseX, mouseY)) {
            return Entry.CONFIRM;
        }
        if (contains(cancelRect(), mouseX, mouseY)) {
            return Entry.CANCEL;
        }
        return null;
    }

    public boolean contains(int mouseX, int mouseY) {
        return open && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean handleClick(int mouseX, int mouseY) {
        Entry entry = hitTest(mouseX, mouseY);
        if (entry == Entry.CONFIRM) {
            Runnable action = onConfirm;
            dismiss();
            action.run();
            return true;
        }
        if (entry == Entry.CANCEL || (open && !contains(mouseX, mouseY))) {
            dismiss();
            return true;
        }
        return open;
    }

    private int[] confirmRect() {
        int pairWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int left = x + (width - pairWidth) / 2;
        int top = titleBottom + TITLE_GAP;
        return new int[] { left, top, BUTTON_WIDTH, BUTTON_HEIGHT };
    }

    private int[] cancelRect() {
        int[] confirm = confirmRect();
        return new int[] { confirm[0] + BUTTON_WIDTH + BUTTON_GAP, confirm[1], BUTTON_WIDTH, BUTTON_HEIGHT };
    }

    private static boolean contains(int[] rect, int mouseX, int mouseY) {
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2]
            && mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }

    private static void renderButton(
        DrawContext context,
        TextRenderer textRenderer,
        int[] rect,
        Text label,
        boolean hovered,
        boolean danger
    ) {
        if (danger) {
            int bg = hovered ? ModUiTheme.DANGER_BG_HOVER : ModUiTheme.DANGER_BG;
            PopupGuiRenderer.fill(context, rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3], bg);
            String fitted = ModUiTheme.fit(textRenderer, label.getString(), Math.max(4, rect[2] - 8));
            int labelWidth = textRenderer.getWidth(fitted);
            context.drawTextWithShadow(
                textRenderer,
                fitted,
                rect[0] + Math.max(4, (rect[2] - labelWidth) / 2),
                rect[1] + (rect[3] - textRenderer.fontHeight) / 2,
                ModUiTheme.TEXT_PRIMARY
            );
            return;
        }
        ModUiTheme.drawPopupButton(
            context,
            textRenderer,
            rect[0],
            rect[1],
            rect[2],
            rect[3],
            label,
            hovered,
            true,
            false
        );
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        PopupGuiRenderer.fill(context, x, y, x + width, y + height, ModUiTheme.POPUP_PANEL_BG);
        PopupGuiRenderer.fill(context, x, y, x + width, y + 1, ModUiTheme.PANEL_BORDER);
        PopupGuiRenderer.fill(context, x, y + height - 1, x + width, y + height, ModUiTheme.PANEL_BORDER);
        PopupGuiRenderer.fill(context, x, y, x + 1, y + height, ModUiTheme.PANEL_BORDER);
        PopupGuiRenderer.fill(context, x + width - 1, y, x + width, y + height, ModUiTheme.PANEL_BORDER);
    }
}
