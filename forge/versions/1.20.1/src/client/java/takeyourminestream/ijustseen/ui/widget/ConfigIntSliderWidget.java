package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.config.ConfigManager;

import java.util.function.IntFunction;

/** Ползунок целочисленной настройки с диапазоном и шагом; пишет значение в конфиг по ключу. */
public class ConfigIntSliderWidget extends SliderWidget {
    private final String configKey;
    private final int min;
    private final int max;
    private final int step;
    private final IntFunction<Text> label;

    public ConfigIntSliderWidget(
        int x, int y, int width, int height,
        String configKey, int min, int max, int step, int fallback,
        IntFunction<Text> label
    ) {
        super(x, y, width, height, Text.empty(), 0.0);
        this.configKey = configKey;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.label = label;
        Object stored = ConfigManager.getInstance().getConfigValue(configKey);
        this.value = toSlider(stored instanceof Number number ? number.intValue() : fallback);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(label.apply(fromSlider(this.value)));
    }

    @Override
    protected void applyValue() {
        int current = fromSlider(this.value);
        this.value = toSlider(current);
        Object stored = ConfigManager.getInstance().getConfigValue(configKey);
        if (!(stored instanceof Number number) || number.intValue() != current) {
            ConfigManager.getInstance().setConfigValue(configKey, current);
        }
        updateMessage();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        applyValue();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        boolean result = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        if (result) {
            applyValue();
        }
        return result;
    }

    private int fromSlider(double sliderValue) {
        int raw = (int) Math.round(min + sliderValue * (max - min));
        int snapped = min + Math.round((raw - min) / (float) step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    private double toSlider(int configValue) {
        int clamped = Math.max(min, Math.min(max, configValue));
        return max == min ? 0.0 : (clamped - min) / (double) (max - min);
    }
}
