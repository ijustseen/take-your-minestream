package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.ButtonWidget;

/** Строка настройки «включить/выключить + кнопка подробных настроек». */
public final class ToggleWithSettingsRow {
    public final ButtonWidget toggle;
    public final ButtonWidget settings;

    public ToggleWithSettingsRow(ButtonWidget toggle, ButtonWidget settings) {
        this.toggle = toggle;
        this.settings = settings;
    }
}
