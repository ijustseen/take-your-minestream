package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import net.minecraft.client.gui.widget.ButtonWidget;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;

import java.util.ArrayList;
import java.util.List;

public class RegexpConfigScreen extends Screen {
    private final @Nullable Screen parent;
    private FilteringManager filteringManager;
    private TextFieldWidget inputField;
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 10;
    private static final int REMOVE_BTN_SIZE = 12;
    private static final int LIST_TOP = 40;
    private static final int LIST_BOTTOM_OFFSET = 60;
    private final List<String> regexpList = new ArrayList<>();

    public RegexpConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourminestream.regexps.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.filteringManager = TakeYourMineStreamClient.getInstance().getFilteringManager();
        reloadList();

        int centerX = this.width / 2;
        int bottomY = this.height - 30;
        int buttonW = 100;
        int buttonH = 20;
        int spacing = 10;

        inputField = new TextFieldWidget(this.textRenderer, PADDING, bottomY - buttonH - spacing, this.width - PADDING * 2 - buttonW - spacing, buttonH, Text.translatable("takeyourminestream.banwords.input"));
        this.addDrawableChild(inputField);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("takeyourminestream.banwords.add"), btn -> {
            String pattern = inputField.getText();
            if (pattern != null && !pattern.trim().isEmpty()) {
                filteringManager.addRegexp(pattern);
                inputField.setText("");
                reloadList();
                int idx = regexpList.indexOf(pattern);
                int top = LIST_TOP;
                int bottom = this.height - LIST_BOTTOM_OFFSET;
                int visible = bottom - top - PADDING * 2;
                int totalHeight = regexpList.size() * LINE_HEIGHT;
                int maxScroll = Math.max(0, totalHeight - visible);
                scrollOffset = Math.max(0, Math.min(maxScroll, idx * LINE_HEIGHT - Math.max(0, visible - LINE_HEIGHT)));
            }
        }).dimensions(this.width - PADDING - buttonW, bottomY - buttonH - spacing, buttonW, buttonH).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), btn -> this.close()).dimensions(centerX - buttonW / 2, bottomY, buttonW, buttonH).build());
    }

    private void reloadList() {
        regexpList.clear();
        regexpList.addAll(filteringManager.getRegexps());
        regexpList.sort(String::compareTo);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var hiddenButtons = ScreenUiHelper.hideButtons(this);
        super.render(context, mouseX, mouseY, delta);
        ScreenUiHelper.restoreButtons(hiddenButtons);

        ModUiTheme.drawTitle(context, this.textRenderer, this.title, this.width);

        int top = LIST_TOP;
        int bottom = this.height - LIST_BOTTOM_OFFSET;
        ModUiTheme.drawBorderedPanel(context, PADDING, top, this.width - PADDING * 2, bottom - top);

        int rowLeft = PADDING + 2;
        int rowRight = this.width - PADDING - 2;
        int y = top + PADDING - scrollOffset;
        int textX = PADDING * 2;
        for (String pattern : regexpList) {
            if (y + LINE_HEIGHT > top + PADDING && y < bottom - PADDING) {
                boolean rowHovered = ModUiTheme.isHovered(mouseX, mouseY, rowLeft, y, rowRight - rowLeft, LINE_HEIGHT);
                ModUiTheme.drawListRow(context, rowLeft, y, rowRight, y + LINE_HEIGHT, rowHovered);
                context.drawTextWithShadow(this.textRenderer, Text.literal(pattern), textX, y, ModUiTheme.TEXT_PRIMARY);

                int btnX = this.width - PADDING - 4 - REMOVE_BTN_SIZE;
                boolean removeHovered = ModUiTheme.isHovered(mouseX, mouseY, btnX, y, REMOVE_BTN_SIZE, REMOVE_BTN_SIZE);
                ModUiTheme.drawCompactButton(
                    context,
                    this.textRenderer,
                    btnX,
                    y,
                    REMOVE_BTN_SIZE,
                    REMOVE_BTN_SIZE,
                    Text.literal("×"),
                    removeHovered,
                    ModUiTheme.ButtonVariant.DANGER
                );
            }
            y += LINE_HEIGHT;
        }

        if (inputField != null) {
            ModUiTheme.drawInputFrame(
                context,
                inputField.getX(),
                inputField.getY(),
                inputField.getWidth(),
                inputField.getHeight(),
                inputField.isFocused()
            );
        }

        ScreenUiHelper.renderAllButtons(context, mouseX, mouseY, this);
    }

    private boolean handleRegexpMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        int top = LIST_TOP;
        int bottom = this.height - LIST_BOTTOM_OFFSET;
        int y = top + PADDING - scrollOffset;
        int btnX = this.width - PADDING - 4 - REMOVE_BTN_SIZE;
        for (String pattern : regexpList) {
            if (y + LINE_HEIGHT > top + PADDING && y < bottom - PADDING) {
                if (mouseX >= btnX && mouseX <= btnX + REMOVE_BTN_SIZE
                    && mouseY >= y && mouseY <= y + REMOVE_BTN_SIZE) {
                    filteringManager.removeRegexp(pattern);
                    reloadList();
                    return true;
                }
            }
            y += LINE_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (handleRegexpMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int top = LIST_TOP;
        int bottom = this.height - LIST_BOTTOM_OFFSET;
        int totalHeight = regexpList.size() * LINE_HEIGHT;
        int visible = bottom - top - PADDING * 2;
        int maxScroll = Math.max(0, totalHeight - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 20)));
        return true;
    }

    @Override
    public void close() {
        if (this.parent != null) {
            MinecraftClient.getInstance().setScreen(this.parent);
        } else {
            MinecraftClient.getInstance().setScreen(null);
        }
    }
}
