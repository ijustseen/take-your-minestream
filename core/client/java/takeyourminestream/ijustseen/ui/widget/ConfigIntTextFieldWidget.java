package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.config.ConfigManager;

/** Числовое поле для целочисленных настроек. */
public class ConfigIntTextFieldWidget extends TextFieldWidget {
    private final String configKey;
    private final int min;
    private final int max;

    public ConfigIntTextFieldWidget(
        TextRenderer textRenderer,
        int x,
        int y,
        int width,
        int height,
        Text label,
        String configKey,
        int min,
        int max
    ) {
        super(textRenderer, x, y, width, height, label);
        this.configKey = configKey;
        this.min = min;
        this.max = max;
        setMaxLength(6);
        setText(String.valueOf(readCurrentValue()));
        setChangedListener(this::onChanged);
    }

    private int readCurrentValue() {
        Object value = ConfigManager.getInstance().getConfigValue(configKey);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return min;
    }

    private void onChanged(String text) {
        if (!text.matches("\\d+")) {
            return;
        }
        try {
            int parsed = Integer.parseInt(text);
            if (parsed >= min && parsed <= max) {
                ConfigManager.getInstance().setConfigValue(configKey, parsed);
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
