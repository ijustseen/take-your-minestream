package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

public final class PlatformChannelRow {
    public final TextFieldWidget field;
    public final ButtonWidget toggle;
    /** Ключ пиксельной иконки платформы (textures/platform/<key>.png). */
    public final String platformIconKey;

    public PlatformChannelRow(TextFieldWidget field, ButtonWidget toggle, String platformIconKey) {
        this.field = field;
        this.toggle = toggle;
        this.platformIconKey = platformIconKey;
    }
}
