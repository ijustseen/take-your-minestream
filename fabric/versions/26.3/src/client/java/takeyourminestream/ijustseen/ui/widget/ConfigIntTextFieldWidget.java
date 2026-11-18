package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import takeyourminestream.ijustseen.config.ConfigManager;

/** Числовое поле для целочисленных настроек. */
public class ConfigIntTextFieldWidget extends EditBox {
    private final String configKey;
    private final int min;
    private final int max;

    public ConfigIntTextFieldWidget(
        Font textRenderer,
        int x,
        int y,
        int width,
        int height,
        Component label,
        String configKey,
        int min,
        int max
    ) {
        super(textRenderer, x, y, width, height, label);
        this.configKey = configKey;
        this.min = min;
        this.max = max;
        setMaxLength(6);
        setValue(String.valueOf(readCurrentValue()));
        setResponder(this::onChanged);
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
