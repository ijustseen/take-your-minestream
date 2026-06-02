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
    }

    public void close() {
        open = false;
        entries = new Entry[0];
    }

    public void render(DrawContext context, TextRenderer textRenderer) {
        if (!open) {
            return;
        }

        context.fill(x, y, x + WIDTH, y + height, 0xE0101010);
        context.fill(x, y, x + WIDTH, y + 1, 0xFF555555);
        context.fill(x, y + height - 1, x + WIDTH, y + height, 0xFF555555);
        context.fill(x, y, x + 1, y + height, 0xFF555555);
        context.fill(x + WIDTH - 1, y, x + WIDTH, y + height, 0xFF555555);

        int buttonY = y + PADDING;
        for (Entry entry : entries) {
            context.fill(x + PADDING, buttonY, x + WIDTH - PADDING, buttonY + BUTTON_HEIGHT, 0x90404040);
            Text label = labelFor(entry);
            int textX = x + PADDING + 6;
            int textY = buttonY + (BUTTON_HEIGHT - textRenderer.fontHeight) / 2;
            context.drawText(textRenderer, label, textX, textY, 0xFFFFFFFF, false);
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
            case PIN -> Text.translatable("takeyourminestream.history.action.pin");
            case UNPIN -> Text.translatable("takeyourminestream.history.action.unpin");
            case BLOCK -> Text.translatable("takeyourminestream.history.action.block");
            case REPLAY -> Text.translatable("takeyourminestream.history.action.replay");
        };
    }
}
