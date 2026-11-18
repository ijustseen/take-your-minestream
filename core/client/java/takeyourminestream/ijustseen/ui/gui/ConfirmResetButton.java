package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Кнопка сброса: первое нажатие просит подтверждение, второе выполняет действие. */
public final class ConfirmResetButton {
    private ConfirmResetButton() {}

    public static ButtonWidget create(int x, int y, int width, int height, Text idleLabel, Runnable onConfirm) {
        boolean[] armed = {false};
        return ButtonWidget.builder(idleLabel, button -> {
            if (!armed[0]) {
                armed[0] = true;
                button.setMessage(Text.translatable("takeyourstreamchat.config.reset_confirm_short"));
                return;
            }
            onConfirm.run();
            armed[0] = false;
            button.setMessage(idleLabel);
        }).dimensions(x, y, width, height).build();
    }
}
