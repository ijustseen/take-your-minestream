package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import takeyourminestream.ijustseen.config.ModConfig;

public class MessageSoundVolumeSliderWidget extends AbstractSliderButton {
    private static final double STEP = 0.05;

    public MessageSoundVolumeSliderWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), getInitialValue());
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int percent = (int) Math.round(this.value * 100.0);
        this.setMessage(Component.literal(percent + "%"));
    }

    @Override
    protected void applyValue() {
        this.value = snapToStep(this.value);
        ModConfig.setMESSAGE_SOUND_VOLUME(this.value);
        updateMessage();
    }

    private static double getInitialValue() {
        double value = ModConfig.getMESSAGE_SOUND_VOLUME();
        value = Math.max(0.0, Math.min(1.0, value));
        return snapToStep(value);
    }

    private static double snapToStep(double value) {
        double snapped = Math.round(value / STEP) * STEP;
        if (snapped < 0.0) {
            return 0.0;
        }
        if (snapped > 1.0) {
            return 1.0;
        }
        return snapped;
    }
}
