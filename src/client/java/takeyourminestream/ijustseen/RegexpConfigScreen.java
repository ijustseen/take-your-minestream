package takeyourminestream.ijustseen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;

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
    private static final int TOGGLE_BTN_PADDING = 3;
    private static final int BETWEEN_BUTTONS_SPACING = 4;
    private final List<String> banwordList = new ArrayList<>();
    private final java.util.Set<String> revealedWords = new java.util.HashSet<>();

    public RegexpConfigScreen(@Nullable Screen parent) {
        super(Text.translatable("takeyourminestream.banwords.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.filteringManager = TakeYourMineStreamClient.getInstance().getFilteringManager();
        reloadList();

        TextRenderer tr = this.textRenderer;

        int centerX = this.width / 2;
        int bottomY = this.height - 30;
        int buttonW = 100;
        int buttonH = 20;
        int spacing = 10;

        inputField = new TextFieldWidget(tr, PADDING, bottomY - buttonH - spacing, this.width - PADDING * 2 - buttonW - spacing, buttonH, Text.translatable("takeyourminestream.banwords.input"));
        this.addDrawableChild(inputField);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("takeyourminestream.banwords.add"), btn -> {
            String word = inputField.getText();
            if (word != null) {
                filteringManager.addRegexp(word);
                inputField.setText("");
                reloadList();
                // Прокрутить к добавленному слову, чтобы пользователь его увидел
                int idx = banwordList.indexOf(word);
                int top = 40;
                int bottom = this.height - 60;
                int visible = bottom - top - PADDING * 2;
                int totalHeight = banwordList.size() * LINE_HEIGHT;
                int maxScroll = Math.max(0, totalHeight - visible);
                int target = Math.max(0, Math.min(maxScroll, idx * LINE_HEIGHT - Math.max(0, visible - LINE_HEIGHT)));
                scrollOffset = target;
            }
        }).dimensions(this.width - PADDING - buttonW, bottomY - buttonH - spacing, buttonW, buttonH).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), btn -> this.close()).dimensions(centerX - buttonW / 2, bottomY, buttonW, buttonH).build());
    }

    private void reloadList() {
        banwordList.clear();
        banwordList.addAll(filteringManager.getRegexps());
        banwordList.sort(String::compareTo);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // Фон списка
        int top = 40;
        int bottom = this.height - 60;
        context.fill(PADDING, top, this.width - PADDING, bottom, 0x80000000); // чуть менее прозрачный фон

        int y = top + PADDING - scrollOffset;
        int textX = PADDING * 2;
        for (int i = 0; i < banwordList.size(); i++) {
            String word = banwordList.get(i);
            if (y + LINE_HEIGHT > top + PADDING && y < bottom - PADDING) {
                context.drawText(this.textRenderer, Text.literal(word), textX, y, 0xFFFFFFFF, true);

                // Кнопка удаления (крестик)
                int rightMargin = 4; // небольшой отступ от правой границы фоновой панели
                int btnX = this.width - PADDING - rightMargin - REMOVE_BTN_SIZE;
                int btnY = y; // без выхода за рамки
                boolean hovered = mouseX >= btnX && mouseX <= btnX + REMOVE_BTN_SIZE && mouseY >= btnY && mouseY <= btnY + REMOVE_BTN_SIZE;
                int bgColor = hovered ? 0xA0FF7777 : 0x80FF5555; // чуть ярче при ховере
                // Рисуем фон кнопки строго внутри панели
                context.fill(btnX, btnY, btnX + REMOVE_BTN_SIZE, btnY + REMOVE_BTN_SIZE, bgColor);
                // Рисуем X по центру
                int xCenter = btnX + (REMOVE_BTN_SIZE / 2) - 2;
                int yCenter = btnY + (REMOVE_BTN_SIZE / 2) - 4;
                context.drawText(this.textRenderer, Text.of("x"), xCenter, yCenter, 0xFFFFFFFF, true);
            }
            y += LINE_HEIGHT;
        }

        // Примитивный «клик для удаления» по правой части строки
        // Мы не создаём много кнопок, чтобы не перегружать экран.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        
        if (button == 0) {
            int top = 40;
            int bottom = this.height - 60;
            int y = top + PADDING - scrollOffset;
            int rightMargin = 4;
            int btnX = this.width - PADDING - rightMargin - REMOVE_BTN_SIZE;
            for (int i = 0; i < banwordList.size(); i++) {
                if (y + LINE_HEIGHT > top + PADDING && y < bottom - PADDING) {
                    int btnY = y;
                    String word = banwordList.get(i);

                    // Область крестика удаления
                    if (mouseX >= btnX && mouseX <= btnX + REMOVE_BTN_SIZE && mouseY >= btnY && mouseY <= btnY + REMOVE_BTN_SIZE) {
                        filteringManager.removeRegexp(word);
                        revealedWords.remove(word);
                        reloadList();
                        return true;
                    }

                    // Область кнопки Показать/Скрыть
                    String toggleLabel = revealedWords.contains(word) ? Text.translatable("takeyourminestream.banwords.hide").getString() : Text.translatable("takeyourminestream.banwords.show").getString();
                    int labelWidth = this.textRenderer.getWidth(toggleLabel);
                    int toggleW = Math.max(labelWidth + TOGGLE_BTN_PADDING * 2, 28);
                    int toggleX = btnX - BETWEEN_BUTTONS_SPACING - toggleW;
                    if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= btnY && mouseY <= btnY + REMOVE_BTN_SIZE) {
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int top = 40;
        int bottom = this.height - 60;
        int totalHeight = banwordList.size() * LINE_HEIGHT;
        int visible = bottom - top - PADDING * 2;
        int maxScroll = Math.max(0, totalHeight - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * 20)));
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


