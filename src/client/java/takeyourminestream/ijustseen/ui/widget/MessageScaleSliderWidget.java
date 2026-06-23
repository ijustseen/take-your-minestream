package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.config.MessageScale;

/**
 * Ползунок для настройки масштаба сообщений с 5 дискретными значениями
 */
public class MessageScaleSliderWidget extends SliderWidget {
    private static final MessageScale[] SCALES = MessageScale.values();
    
    public MessageScaleSliderWidget(int x, int y, int width, int height) {
        super(x, y, width, height, getDisplayText(ModConfig.getMESSAGE_SCALE()), getSliderValue(ModConfig.getMESSAGE_SCALE()));
    }
    
    @Override
    protected void updateMessage() {
        MessageScale currentScale = getScaleFromSliderValue(this.value);
        this.setMessage(getDisplayText(currentScale));
    }
    
    @Override
    protected void applyValue() {
        // Находим ближайшее фиксированное значение и устанавливаем его
        MessageScale newScale = getScaleFromSliderValue(this.value);
        double fixedValue = getSliderValue(newScale);
        
        // Принудительно устанавливаем ползунок в фиксированную позицию
        this.value = fixedValue;
        
        // Применяем новое значение
        ModConfig.setMESSAGE_SCALE(newScale);
        
        // Обновляем отображение
        this.updateMessage();
    }
    
    @Override
    public void onClick(Click click, boolean doubled) {
        super.onClick(click, doubled);
        applyValue();
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        boolean result = super.mouseDragged(click, deltaX, deltaY);
        if (result) {
            applyValue();
        }
        return result;
    }
    
    /**
     * Преобразует значение ползунка (0.0-1.0) в MessageScale
     */
    private static MessageScale getScaleFromSliderValue(double sliderValue) {
        int index = (int) Math.round(sliderValue * (SCALES.length - 1));
        index = Math.max(0, Math.min(SCALES.length - 1, index));
        return SCALES[index];
    }
    
    /**
     * Преобразует MessageScale в значение ползунка (0.0-1.0)
     */
    private static double getSliderValue(MessageScale scale) {
        for (int i = 0; i < SCALES.length; i++) {
            if (SCALES[i] == scale) {
                return (double) i / (SCALES.length - 1);
            }
        }
        return 0.5; // Значение по умолчанию для NORMAL
    }
    
    /**
     * Получает текст для отображения на ползунке
     */
    private static Text getDisplayText(MessageScale scale) {
        switch (scale) {
            case TINY:
                return Text.translatable("takeyourstreamchat.config.scale_tiny");
            case SMALL:
                return Text.translatable("takeyourstreamchat.config.scale_small");
            case NORMAL:
                return Text.translatable("takeyourstreamchat.config.scale_normal");
            case LARGE:
                return Text.translatable("takeyourstreamchat.config.scale_large");
            case HUGE:
                return Text.translatable("takeyourstreamchat.config.scale_huge");
            default:
                return Text.translatable("takeyourstreamchat.config.scale_normal");
        }
    }
}