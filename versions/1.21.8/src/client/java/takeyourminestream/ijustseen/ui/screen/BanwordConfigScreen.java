package takeyourminestream.ijustseen.ui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.ui.gui.ModUiTheme;
import net.minecraft.client.gui.widget.ButtonWidget;
import takeyourminestream.ijustseen.ui.gui.ScreenUiHelper;

import java.util.ArrayList;
import java.util.List;

public class BanwordConfigScreen extends Screen {
    private final @Nullable Screen parent;
    private IBanwordManager banwordManager;
    private TextFieldWidget inputField;
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 14;
    private static final int PADDING = 10;
    private static final int REMOVE_BTN_SIZE = 12;
    private static final int TOGGLE_BTN_PADDING = 3;
    private static final int BETWEEN_BUTTONS_SPACING = 4;
    private static final int LIST_TOP = 40;
    private static final int LIST_BOTTOM_OFFSET = 60;
    private final List<String> banwordList = new ArrayList<>();
    private final java.util.Set<String> revealedWords = new java.util.HashSet<>();

    public BanwordConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourstreamchat.banwords.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.banwordManager = TakeYourMineStreamClient.getInstance().getBanwordManager();
        reloadList();

        int centerX = this.width / 2;
        int bottomY = this.height - 30;
        int buttonW = 100;
        int buttonH = 20;
        int spacing = 10;

        inputField = new TextFieldWidget(this.textRenderer, PADDING, bottomY - buttonH - spacing, this.width - PADDING * 2 - buttonW - spacing, buttonH, Text.translatable("takeyourstreamchat.banwords.input"));
        this.addDrawableChild(inputField);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("takeyourstreamchat.banwords.add"), btn -> {
            String word = inputField.getText();
            if (word != null && !word.trim().isEmpty()) {
                String normalized = word.trim().toLowerCase();
                banwordManager.addBanword(normalized);
                inputField.setText("");
                reloadList();
                int idx = banwordList.indexOf(normalized);
                int top = LIST_TOP;
                int bottom = this.height - LIST_BOTTOM_OFFSET;
                int visible = bottom - top - PADDING * 2;
                int totalHeight = banwordList.size() * LINE_HEIGHT;
                int maxScroll = Math.max(0, totalHeight - visible);
                scrollOffset = Math.max(0, Math.min(maxScroll, idx * LINE_HEIGHT - Math.max(0, visible - LINE_HEIGHT)));
            }
        }).dimensions(this.width - PADDING - buttonW, bottomY - buttonH - spacing, buttonW, buttonH).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), btn -> this.close()).dimensions(centerX - buttonW / 2, bottomY, buttonW, buttonH).build());
    }

    private void reloadList() {
        banwordList.clear();
        banwordList.addAll(banwordManager.getBanwords());
        banwordList.sort(String::compareTo);
    }

    private static String maskWord(String word) {
        if (word == null) {
            return "";
        }
        String trimmed = word.trim();
        if (trimmed.length() <= 2) {
            return trimmed;
        }
        return trimmed.substring(0, 2) + "*".repeat(trimmed.length() - 2);
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
        for (String word : banwordList) {
            if (y + LINE_HEIGHT > top + PADDING && y < bottom - PADDING) {
                boolean rowHovered = ModUiTheme.isHovered(mouseX, mouseY, rowLeft, y, rowRight - rowLeft, LINE_HEIGHT);
                ModUiTheme.drawListRow(context, rowLeft, y, rowRight, y + LINE_HEIGHT, rowHovered);

                boolean isRevealed = revealedWords.contains(word);
                String toDisplay = isRevealed ? word : maskWord(word);
                context.drawText(this.textRenderer, Text.literal(toDisplay), textX, y, ModUiTheme.TEXT_PRIMARY, true);

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

                String toggleLabel = isRevealed
                    ? Text.translatable("takeyourstreamchat.banwords.hide").getString()
                    : Text.translatable("takeyourstreamchat.banwords.show").getString();
                int labelWidth = this.textRenderer.getWidth(toggleLabel);
                int toggleW = Math.max(labelWidth + TOGGLE_BTN_PADDING * 2, 28);
                int toggleX = btnX - BETWEEN_BUTTONS_SPACING - toggleW;
                boolean toggleHovered = ModUiTheme.isHovered(mouseX, mouseY, toggleX, y, toggleW, REMOVE_BTN_SIZE);
                ModUiTheme.drawCompactButton(
                    context,
                    this.textRenderer,
                    toggleX,
                    y,
                    toggleW,
                    REMOVE_BTN_SIZE,
                    Text.of(toggleLabel),
                    toggleHovered,
                    ModUiTheme.ButtonVariant.ACCENT
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

    private boolean handleBanwordMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        int top = LIST_TOP;
        int bottom = this.height - LIST_BOTTOM_OFFSET;
        int y = top + PADDING - scrollOffset;
        int btnX = this.width - PADDING - 4 - REMOVE_BTN_SIZE;
        for (String word : banwordList) {
            if (y + LINE_HEIGHT > top + PADDING && y < bottom - PADDING) {
                if (mouseX >= btnX && mouseX <= btnX + REMOVE_BTN_SIZE
                    && mouseY >= y && mouseY <= y + REMOVE_BTN_SIZE) {
                    banwordManager.removeBanword(word);
                    revealedWords.remove(word);
                    reloadList();
                    return true;
                }

                String toggleLabel = revealedWords.contains(word)
                    ? Text.translatable("takeyourstreamchat.banwords.hide").getString()
                    : Text.translatable("takeyourstreamchat.banwords.show").getString();
                int labelWidth = this.textRenderer.getWidth(toggleLabel);
                int toggleW = Math.max(labelWidth + TOGGLE_BTN_PADDING * 2, 28);
                int toggleX = btnX - BETWEEN_BUTTONS_SPACING - toggleW;
                if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= y && mouseY <= y + REMOVE_BTN_SIZE) {
                    if (revealedWords.contains(word)) {
                        revealedWords.remove(word);
                    } else {
                        revealedWords.add(word);
                    }
                    return true;
                }
            }
            y += LINE_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleBanwordMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int top = LIST_TOP;
        int bottom = this.height - LIST_BOTTOM_OFFSET;
        int totalHeight = banwordList.size() * LINE_HEIGHT;
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
