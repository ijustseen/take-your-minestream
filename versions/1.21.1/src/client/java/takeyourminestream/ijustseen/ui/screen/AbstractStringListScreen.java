package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.ui.gui.GuiScrollbar;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import net.minecraft.client.gui.widget.ButtonWidget;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;

import java.util.ArrayList;
import java.util.List;

/** Базовый экран со скроллируемым списком строк (1.21.8 input API). */
public abstract class AbstractStringListScreen extends Screen {
    protected static final int LINE_HEIGHT = 14;
    protected static final int PADDING = 10;
    protected static final int REMOVE_BTN_SIZE = 12;
    protected static final int LIST_HEADER = 40;
    protected static final int LIST_FOOTER_RESERVE = 60;

    protected final @Nullable Screen parent;
    protected TextFieldWidget inputField;
    protected int scrollOffset = 0;
    protected final List<String> entries = new ArrayList<>();

    protected AbstractStringListScreen(Text title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        reloadEntries();

        int bottomY = this.height - 30;
        int buttonW = 100;
        int buttonH = 20;
        int spacing = 10;

        inputField = new TextFieldWidget(this.textRenderer, PADDING, bottomY - buttonH - spacing,
            this.width - PADDING * 2 - buttonW - spacing, buttonH, inputPlaceholder());
        this.addDrawableChild(inputField);

        this.addDrawableChild(ButtonWidget.builder(addButtonLabel(), btn -> {
            String value = inputField.getText();
            if (value != null && !value.trim().isEmpty()) {
                onAdd(normalizeEntry(value));
                inputField.setText("");
                reloadEntries();
                scrollToEntry(normalizeEntry(value));
            }
        }).dimensions(this.width - PADDING - buttonW, bottomY - buttonH - spacing, buttonW, buttonH).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), btn -> close())
            .dimensions(this.width / 2 - buttonW / 2, bottomY, buttonW, buttonH).build());
    }

    protected abstract Text inputPlaceholder();

    protected abstract Text addButtonLabel();

    protected abstract void reloadEntries();

    protected abstract void onAdd(String entry);

    protected abstract void onRemove(String entry);

    protected String normalizeEntry(String entry) {
        return entry.trim().toLowerCase();
    }

    protected void scrollToEntry(String entry) {
        int idx = entries.indexOf(entry);
        if (idx < 0) {
            return;
        }
        int visible = listBottom() - listTop() - PADDING * 2;
        int totalHeight = entries.size() * LINE_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, idx * LINE_HEIGHT - Math.max(0, visible - LINE_HEIGHT)));
    }

    protected int listTop() {
        return LIST_HEADER;
    }

    protected int listBottom() {
        return this.height - LIST_FOOTER_RESERVE;
    }

    protected void drawListPanel(DrawContext context) {
        ModUiTheme.drawBorderedPanel(context, PADDING, listTop(), this.width - PADDING * 2, listBottom() - listTop());
    }

    protected void renderInputChrome(DrawContext context) {
        if (inputField != null && inputField.visible) {
            ModUiTheme.drawInputFrame(
                context,
                inputField.getX(),
                inputField.getY(),
                inputField.getWidth(),
                inputField.getHeight(),
                inputField.isFocused()
            );
        }
    }

    protected void renderDefaultList(DrawContext context, int mouseX, int mouseY, EntryRenderer renderer) {
        drawListPanel(context);
        int y = listTop() + PADDING - scrollOffset;
        int textX = PADDING * 2;
        int rowLeft = PADDING + 2;
        int rowRight = this.width - PADDING - 2;

        for (String entry : entries) {
            if (y + LINE_HEIGHT > listTop() + PADDING && y < listBottom() - PADDING) {
                boolean rowHovered = ModUiTheme.isHovered(mouseX, mouseY, rowLeft, y, rowRight - rowLeft, LINE_HEIGHT);
                ModUiTheme.drawListRow(context, rowLeft, y, rowRight, y + LINE_HEIGHT, rowHovered);
                renderer.render(context, entry, textX, y, mouseX, mouseY);
            }
            y += LINE_HEIGHT;
        }
    }

    protected void drawRemoveButton(DrawContext context, int y, int mouseX, int mouseY) {
        int btnX = this.width - PADDING - 4 - REMOVE_BTN_SIZE;
        boolean hovered = ModUiTheme.isHovered(mouseX, mouseY, btnX, y, REMOVE_BTN_SIZE, REMOVE_BTN_SIZE);
        ModUiTheme.drawCompactButton(
            context,
            this.textRenderer,
            btnX,
            y,
            REMOVE_BTN_SIZE,
            REMOVE_BTN_SIZE,
            Text.literal("×"),
            hovered,
            ModUiTheme.ButtonVariant.DANGER
        );
    }

    protected boolean handleListMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        int y = listTop() + PADDING - scrollOffset;
        int btnX = this.width - PADDING - 4 - REMOVE_BTN_SIZE;
        for (String entry : entries) {
            if (y + LINE_HEIGHT > listTop() + PADDING && y < listBottom() - PADDING) {
                int btnY = y;
                if (mouseX >= btnX && mouseX <= btnX + REMOVE_BTN_SIZE
                    && mouseY >= btnY && mouseY <= btnY + REMOVE_BTN_SIZE) {
                    onRemove(entry);
                    reloadEntries();
                    return true;
                }
            }
            y += LINE_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleListMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalHeight = entries.size() * LINE_HEIGHT;
        int visible = listBottom() - listTop() - PADDING * 2;
        scrollOffset = GuiScrollbar.clampScroll(scrollOffset - (int) (verticalAmount * 20), totalHeight, visible);
        return true;
    }

    @Override
    public void close() {
        if (this.client == null) {
            return;
        }
        if (this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            this.client.setScreen(null);
        }
    }

    protected void renderThemedChrome(DrawContext context, int mouseX, int mouseY) {
        ScreenUiHelper.renderAllButtons(context, mouseX, mouseY, this);
    }

    @FunctionalInterface
    protected interface EntryRenderer {
        void render(DrawContext context, String entry, int textX, int y, int mouseX, int mouseY);
    }
}
