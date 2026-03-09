package takeyourminestream.ijustseen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.text.OrderedText;
import org.jetbrains.annotations.Nullable;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageLifecycleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран для просмотра истории сообщений
 */
public class MessageHistoryScreen extends Screen {
    private final @Nullable Screen parent;
    private final MessageLifecycleManager lifecycleManager;
    private int scrollOffset = 0;
    private final int lineHeight = 12;
    private final int padding = 10;
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    
    public MessageHistoryScreen(@Nullable Screen parent, MessageLifecycleManager lifecycleManager) {
        super(Text.translatable("takeyourminestream.history.title"));
        this.parent = parent;
        this.lifecycleManager = lifecycleManager;
        // Не вызываем updateHistoryEntries() здесь, так как textRenderer еще не инициализирован
    }
    
    @Override
    protected void init() {
        // Инициализируем историю после того, как textRenderer стал доступен
        updateHistoryEntries();
        
        // Кнопки с таким же расстоянием как в ModConfigScreen
        int centerX = this.width / 2;
        int buttonY = this.height - 30;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonSpacing = 10; // Такое же расстояние как в ModConfigScreen
        
        // Кнопка "Очистить историю" (слева)
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("takeyourminestream.history.clear"), 
            btn -> {
                lifecycleManager.clearMessageHistory();
                updateHistoryEntries();
            }
        ).dimensions(centerX - buttonWidth * 3 / 2 - buttonSpacing, buttonY, buttonWidth, buttonHeight).build());
        
        // Кнопка переключения Twitch (по центру)
        this.addDrawableChild(ButtonWidget.builder(
            getTwitchToggleButtonText(),
            btn -> {
                handleTwitchToggle();
                btn.setMessage(getTwitchToggleButtonText());
            }
        ).dimensions(centerX - buttonWidth / 2, buttonY, buttonWidth, buttonHeight).build());
        
        // Кнопка "Закрыть" (справа)
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("takeyourminestream.history.close"), 
            btn -> this.close()
        ).dimensions(centerX + buttonWidth / 2 + buttonSpacing, buttonY, buttonWidth, buttonHeight).build());
    }
    
    private void updateHistoryEntries() {
        historyEntries.clear();
        
        // Проверяем, что textRenderer инициализирован
        if (this.textRenderer == null) {
            return;
        }
        
        List<Message> allMessages = lifecycleManager.getAllMessages();
        
        // Вычисляем максимальную ширину текста с учетом реальных размеров фонового блока
        int contentWidth = this.width - padding * 2; // Ширина фонового блока
        int textAreaWidth = contentWidth - padding * 2 - 10; // Отступы внутри блока + место для скроллбара
        
        for (Message message : allMessages) {
            // Разбиваем длинные сообщения на строки с учетом реальной ширины
            List<OrderedText> wrappedLines = this.textRenderer.wrapLines(
                Text.of(message.getText()), 
                textAreaWidth
            );
            
            for (OrderedText line : wrappedLines) {
                historyEntries.add(new HistoryEntry(line, message.getSpawnTick()));
            }
        }
        
        // Сбрасываем прокрутку если история стала короче
        int maxScroll = Math.max(0, historyEntries.size() * lineHeight - (this.height - 90));
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Автоматически обновляем историю при каждом рендере
        updateHistoryEntries();
        
        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        
        // Информация о количестве сообщений
        String countText = "Сообщений в истории: " + lifecycleManager.getMessageHistorySize();
        context.drawText(this.textRenderer, Text.of(countText), padding, 25, 0xAAAAAA, true);
        
        // Область для отображения сообщений - от верха до кнопок внизу
        int contentTop = 40;
        int contentBottom = this.height - 50;
        int contentHeight = contentBottom - contentTop;
        
        // Рендерим фон области сообщений
        context.fill(padding, contentTop, this.width - padding, contentBottom, 0x40000000);
        
        // Рендерим сообщения с учетом прокрутки и границ фонового блока
        int y = contentTop + padding - scrollOffset;
        int textX = padding * 2; // Отступ от левого края фонового блока
        int visibleLines = 0;
        
        for (HistoryEntry entry : historyEntries) {
            // Проверяем, что сообщение находится в видимой области фонового блока
            if (y + lineHeight > contentTop + padding && y < contentBottom - padding) {
                // Сообщение видимо на экране и внутри фонового блока
                int textColor = getMessageColor(entry.spawnTick);
                context.drawText(this.textRenderer, entry.text, textX, y, textColor, true);
                visibleLines++;
            }
            y += lineHeight;
            
            // Прекращаем рендеринг если вышли за пределы видимой области
            if (y > contentBottom - padding) break;
        }
        
        // Индикатор прокрутки
        if (historyEntries.size() * lineHeight > contentHeight) {
            drawScrollbar(context, contentTop, contentBottom);
        }
        
        // TODO: Добавить сообщение для пустой истории
        
        // Подсказка по управлению
        String helpText = "Используйте колесо мыши для прокрутки";
        int textWidth = this.textRenderer.getWidth(helpText);
        context.drawText(this.textRenderer, Text.of(helpText), 
            this.width / 2 - textWidth / 2, 
            contentBottom + 5, 
            0x888888, true);
    }
    
    private void drawScrollbar(DrawContext context, int contentTop, int contentBottom) {
        int scrollbarX = this.width - padding - 6;
        int scrollbarWidth = 4;
        int scrollbarHeight = contentBottom - contentTop;
        
        // Фон скроллбара
        context.fill(scrollbarX, contentTop, scrollbarX + scrollbarWidth, contentBottom, 0x40FFFFFF);
        
        // Ползунок скроллбара
        int totalContentHeight = historyEntries.size() * lineHeight;
        int visibleContentHeight = contentBottom - contentTop;
        
        if (totalContentHeight > visibleContentHeight) {
            int thumbHeight = Math.max(10, (visibleContentHeight * scrollbarHeight) / totalContentHeight);
            int thumbY = contentTop + (scrollOffset * (scrollbarHeight - thumbHeight)) / (totalContentHeight - visibleContentHeight);
            
            context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }
    
    private int getMessageColor(long spawnTick) {
        // Более старые сообщения отображаются более тусклым цветом
        long currentTick = lifecycleManager.getTickCounter();
        long age = currentTick - spawnTick;
        
        if (age < 100) return 0xFFFFFFFF; // Белый - новые сообщения
        if (age < 500) return 0xFFCCCCCC; // Светло-серый
        if (age < 1000) return 0xFFAAAAAA; // Серый
        return 0xFF888888; // Темно-серый - старые сообщения
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, historyEntries.size() * lineHeight - (this.height - 90));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * 30)));
        return true;
    }
    
    private Text getTwitchToggleButtonText() {
        var twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
        boolean isConnected = twitchManager.isConnected();
        
        String statusKey = isConnected ? "takeyourminestream.config.twitch_on" : "takeyourminestream.config.twitch_off";
        MutableText statusText = Text.translatable(statusKey);

        MutableText indicator = Text.literal(" ●").formatted(isConnected ? Formatting.GREEN : Formatting.RED);
        
        return statusText.append(indicator);
    }
    
    private void handleTwitchToggle() {
        try {
            var twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            
            if (twitchManager.isConnected()) {
                twitchManager.disconnect();
            } else {
                if (messageSpawner != null) {
                    twitchManager.connect(messageSpawner);
                }
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Twitch toggle exception: ", e);
        }
    }

    @Override
    public void close() {
        if (this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            this.client.setScreen(null);
        }
    }

    /**
     * Внутренний класс для хранения записи истории
     */
    private static class HistoryEntry {
        final OrderedText text;
        final long spawnTick;
        
        HistoryEntry(OrderedText text, long spawnTick) {
            this.text = text;
            this.spawnTick = spawnTick;
        }
    }
}