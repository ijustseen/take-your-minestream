package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Микро-меню действий над сообщением в истории. */
public final class HistoryMessageActionPopup {
    public enum Entry { PIN, UNPIN, BLOCK, REPLAY }

    private static final int WIDTH = 148;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 4;
    private static final int GAP = 2;

    private Entry[] entries = new Entry[0];
    private int x;
    private int y;
    private int height;
    private boolean open;
    private Entry hoveredEntry;

    public boolean isOpen() {
        return open;
    }

    public void open(int anchorX, int anchorY, int screenWidth, int screenHeight, boolean pinned, boolean hasUsername) {
        int count = 1 + (hasUsername ? 1 : 0) + 1;
        entries = new Entry[count];
        int idx = 0;
        entries[idx++] = pinned ? Entry.UNPIN : Entry.PIN;
        if (hasUsername) {
            entries[idx++] = Entry.BLOCK;
        }
        entries[idx] = Entry.REPLAY;

        height = PADDING * 2 + count * BUTTON_HEIGHT + (count - 1) * GAP;
        x = Math.min(anchorX, screenWidth - WIDTH - 8);
        y = Math.min(anchorY, screenHeight - height - 8);
        x = Math.max(8, x);
        y = Math.max(8, y);
        open = true;
        hoveredEntry = null;
    }

    public void dismiss() {
        open = false;
        entries = new Entry[0];
        hoveredEntry = null;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (!open) {
            return;
        }

        hoveredEntry = hitTest(mouseX, mouseY);
        drawPopupPanel(context, x, y, WIDTH, height);

        int buttonY = y + PADDING;
        for (Entry entry : entries) {
            int left = x + PADDING;
            int right = x + WIDTH - PADDING;
            boolean hovered = entry == hoveredEntry;
            ModUiTheme.drawPopupButton(
                context,
                textRenderer,
                left,
                buttonY,
                right - left,
                BUTTON_HEIGHT,
                labelFor(entry),
                hovered,
                true,
                false
            );
            buttonY += BUTTON_HEIGHT + GAP;
        }
    }

    public Entry hitTest(int mouseX, int mouseY) {
        if (!open) {
            return null;
        }

        int buttonY = y + PADDING;
        for (Entry entry : entries) {
            if (mouseX >= x + PADDING && mouseX <= x + WIDTH - PADDING
                && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
                return entry;
            }
            buttonY += BUTTON_HEIGHT + GAP;
        }
        return null;
    }

    public boolean contains(int mouseX, int mouseY) {
        return open && mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + height;
    }

    private static Text labelFor(Entry entry) {
        return switch (entry) {
            case PIN -> Text.translatable("takeyourstreamchat.history.action.pin");
            case UNPIN -> Text.translatable("takeyourstreamchat.history.action.unpin");
            case BLOCK -> Text.translatable("takeyourstreamchat.history.action.block");
            case REPLAY -> Text.translatable("takeyourstreamchat.history.action.replay");
        };
    }

    private static void drawPopupPanel(DrawContext context, int x, int y, int width, int height) {
        PopupGuiRenderer.fill(context, x, y, x + width, y + height, ModUiTheme.POPUP_PANEL_BG);
        PopupGuiRenderer.fill(context, x, y, x + width, y + 1, ModUiTheme.PANEL_BORDER);
        PopupGuiRenderer.fill(context, x, y + height - 1, x + width, y + height, ModUiTheme.PANEL_BORDER);
        PopupGuiRenderer.fill(context, x, y, x + 1, y + height, ModUiTheme.PANEL_BORDER);
        PopupGuiRenderer.fill(context, x + width - 1, y, x + width, y + height, ModUiTheme.PANEL_BORDER);
    }
}
