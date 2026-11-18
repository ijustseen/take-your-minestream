package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import takeyourminestream.ijustseen.config.ConfigManager;

/** Ползунок шанса спавна сообщений (0–100%). */
public class ChanceForSpawnSliderWidget extends AbstractSliderButton {
    public ChanceForSpawnSliderWidget(int x, int y, int width, int height) {
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
        int percent = (int) Math.round(this.value * 100.0);
        percent = Math.max(0, Math.min(100, percent));
        this.value = percent / 100.0;
        ConfigManager.getInstance().setConfigValue("chanceForSpawn", percent);
        updateMessage();
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

    private static double getInitialValue() {
        Object value = ConfigManager.getInstance().getConfigValue("chanceForSpawn");
        int percent = value instanceof Number number ? number.intValue() : 100;
        percent = Math.max(0, Math.min(100, percent));
        return percent / 100.0;
    }
}
