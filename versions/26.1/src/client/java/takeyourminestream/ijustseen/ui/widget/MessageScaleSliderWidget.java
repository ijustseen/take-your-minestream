package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import takeyourminestream.ijustseen.config.MessageScale;
import takeyourminestream.ijustseen.config.ModConfig;

/**
 * Ползунок для настройки масштаба сообщений с 5 дискретными значениями
 */
public class MessageScaleSliderWidget extends AbstractSliderButton {
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
        MessageScale newScale = getScaleFromSliderValue(this.value);
        double fixedValue = getSliderValue(newScale);
        this.value = fixedValue;
        ModConfig.setMESSAGE_SCALE(newScale);
        this.updateMessage();
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        super.onClick(click, doubled);
        applyValue();
    }

    @Override
    public void onDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        super.onDrag(click, deltaX, deltaY);
        applyValue();
    }

    private static MessageScale getScaleFromSliderValue(double sliderValue) {
        int index = (int) Math.round(sliderValue * (SCALES.length - 1));
        index = Math.max(0, Math.min(SCALES.length - 1, index));
        return SCALES[index];
    }

    private static double getSliderValue(MessageScale scale) {
        for (int i = 0; i < SCALES.length; i++) {
            if (SCALES[i] == scale) {
                return (double) i / (SCALES.length - 1);
            }
        }
        return 0.5;
    }

    private static Component getDisplayText(MessageScale scale) {
        return switch (scale) {
            case TINY -> Component.translatable("takeyourstreamchat.config.scale_tiny");
            case SMALL -> Component.translatable("takeyourstreamchat.config.scale_small");
            case NORMAL -> Component.translatable("takeyourstreamchat.config.scale_normal");
            case LARGE -> Component.translatable("takeyourstreamchat.config.scale_large");
            case HUGE -> Component.translatable("takeyourstreamchat.config.scale_huge");
        };
    }
}
