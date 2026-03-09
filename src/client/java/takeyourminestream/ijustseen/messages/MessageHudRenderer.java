package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import takeyourminestream.ijustseen.ModConfig;
import takeyourminestream.ijustseen.config.MessageSpawnMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Рендерер для отображения сообщений как HUD виджет в правом верхнем углу экрана
 */
public class MessageHudRenderer {
    private final MessageLifecycleManager lifecycleManager;
    private static final int MAX_DISPLAYED_MESSAGES = 5;
    private static final int MESSAGE_MARGIN = 10;
    private static final int MESSAGE_PADDING = 6;
    private static final int MESSAGE_SPACING = 2;
    private static final int BACKGROUND_COLOR = 0x80000000; // Полупрозрачный черный
    private static final int TEXT_COLOR = 0xFFFFFFFF; // Белый текст
    

    
    public MessageHudRenderer(MessageLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
        
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (ModConfig.getMESSAGE_SPAWN_MODE() == MessageSpawnMode.HUD_WIDGET) {
                renderHudMessages(drawContext);
            }
        });
    }
    
    private void renderHudMessages(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        List<Message> activeMessages = lifecycleManager.getActiveMessages();
        if (activeMessages.isEmpty()) return;
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Получаем последние сообщения для отображения
        List<Message> messagesToDisplay = getMessagesToDisplay(activeMessages);
        if (messagesToDisplay.isEmpty()) return;
        
        // В HUD режиме не применяем масштабирование - используем фиксированные размеры
        int padding = MESSAGE_PADDING;
        
        // Фиксированная правая граница для всех сообщений (близко к краю экрана)
        int fixedRightEdge = screenWidth - MESSAGE_MARGIN;
        
        // Рендерим каждое сообщение
        int currentY = MESSAGE_MARGIN;
        
        for (Message message : messagesToDisplay) {
            // Вычисляем альфа-канал на основе возраста сообщения
            float alpha = calculateMessageAlpha(message);
            if (alpha <= 0.0f) continue;
            
            // Разбиваем текст на строки (фиксированная ширина как в 3D режимах)
            List<OrderedText> wrappedText = textRenderer.wrapLines(Text.of(message.getText()), 120);
            
            // Находим максимальную ширину среди всех строк
            int maxTextWidth = 0;
            for (OrderedText line : wrappedText) {
                int w = textRenderer.getWidth(line);
                if (w > maxTextWidth) maxTextWidth = w;
            }
            
            // Вычисляем размеры панели на основе реального размера текста без масштабирования
            int panelWidth = maxTextWidth + padding * 2;
            int panelHeight = wrappedText.size() * textRenderer.fontHeight + padding * 2;
            
            // Позиция панели - выравниваем по фиксированной правой границе
            int panelX = fixedRightEdge - panelWidth;
            int panelY = currentY;
            
            // Рендерим фон сообщения (по флагу)
            if (ModConfig.isSHOW_MESSAGE_BACKGROUND()) {
                int backgroundColor = applyAlpha(BACKGROUND_COLOR, alpha);
                drawContext.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, backgroundColor);
            }
            
            // Рендерим текст с выравниванием по правому краю
            int textColor = applyAlpha(TEXT_COLOR, alpha);
            renderRightAlignedText(drawContext, textRenderer, wrappedText, 
                                 fixedRightEdge - padding, panelY + padding, 
                                 textColor, 1.0f);
            
            currentY += panelHeight + MESSAGE_SPACING;
        }
    }
    
    /**
     * Получает сообщения для отображения (последние N сообщений)
     */
    private List<Message> getMessagesToDisplay(List<Message> activeMessages) {
        List<Message> result = new ArrayList<>();
        
        // Берем последние сообщения (в обратном порядке)
        int startIndex = Math.max(0, activeMessages.size() - MAX_DISPLAYED_MESSAGES);
        for (int i = activeMessages.size() - 1; i >= startIndex; i--) {
            result.add(activeMessages.get(i));
        }
        
        return result;
    }
    
    /**
     * Вычисляет альфа-канал сообщения на основе его возраста
     */
    private float calculateMessageAlpha(Message message) {
        int tickCounter = lifecycleManager.getTickCounter();
        int age = message.getEffectiveAge(tickCounter);
        int lifetime = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        
        if (age < lifetime) {
            // Сообщение еще живо
            return 1.0f;
        } else if (age < lifetime + fallTicks) {
            // Сообщение исчезает
            float fallProgress = (float)(age - lifetime) / (float)fallTicks;
            return 1.0f - fallProgress;
        } else {
            // Сообщение исчезло
            return 0.0f;
        }
    }
    
    /**
     * Применяет альфа-канал к цвету
     */
    private int applyAlpha(int color, float alpha) {
        int originalAlpha = (color >> 24) & 0xFF;
        int newAlpha = (int)(originalAlpha * alpha);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }
    
    /**
     * Рендерит текст с выравниванием по правому краю
     * В HUD режиме не применяем масштабирование к тексту, только к размерам панелей
     */
    private void renderRightAlignedText(DrawContext drawContext, TextRenderer textRenderer, 
                                      List<OrderedText> lines, int rightEdge, int y, int color, float scale) {
        
        // Рендерим каждую строку, выравнивая по правому краю
        for (int i = 0; i < lines.size(); i++) {
            // Используем обычную высоту строки без масштабирования
            int lineY = y + i * textRenderer.fontHeight;
            
            // Вычисляем ширину строки и позицию X для выравнивания по правому краю
            int lineWidth = textRenderer.getWidth(lines.get(i));
            int lineX = rightEdge - lineWidth;
            
            // Рендерим текст без масштабирования - размер адаптируется к настройкам GUI игрока
            drawContext.drawText(textRenderer, lines.get(i), lineX, lineY, color, false);
        }
    }
    

}