package takeyourminestream.ijustseen.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.ModConfig;
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
    public void onClick(net.minecraft.client.gui.Click click, boolean rightClick) {
        // Переопределяем клик для фиксированных позиций
        super.onClick(click, rightClick);
        // После клика принудительно устанавливаем в ближайшую фиксированную позицию
        applyValue();
    }
    
    @Override
    protected void onDrag(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        // Переопределяем перетаскивание для фиксированных позиций
        super.onDrag(click, deltaX, deltaY);
        // После перетаскивания принудительно устанавливаем в ближайшую фиксированную позицию
        applyValue();
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
                return Text.translatable("takeyourminestream.config.scale_tiny");
            case SMALL:
                return Text.translatable("takeyourminestream.config.scale_small");
            case NORMAL:
                return Text.translatable("takeyourminestream.config.scale_normal");
            case LARGE:
                return Text.translatable("takeyourminestream.config.scale_large");
            case HUGE:
                return Text.translatable("takeyourminestream.config.scale_huge");
            default:
                return Text.translatable("takeyourminestream.config.scale_normal");
        }
    }
}