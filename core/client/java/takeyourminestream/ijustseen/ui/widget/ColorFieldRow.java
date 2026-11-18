package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

/** Строка цвета: HEX-поле и кнопка, открывающая color picker. */
public final class ColorFieldRow {
    public final TextFieldWidget field;
    public final ButtonWidget picker;

    public ColorFieldRow(TextFieldWidget field, ButtonWidget picker) {
        this.field = field;
        this.picker = picker;
    }
}
