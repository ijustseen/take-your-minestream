package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ConfigUiHelper {
    private ConfigUiHelper() {}

    public static Text onOffText(boolean enabled) {
        return Text.translatable(enabled ? "takeyourstreamchat.config.on" : "takeyourstreamchat.config.off")
            .formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }
}
