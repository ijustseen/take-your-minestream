package takeyourminestream.ijustseen.ui.widget;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.config.ConfigManager;

/** Ползунок шанса спавна сообщений (0–100%). */
public class ChanceForSpawnSliderWidget extends SliderWidget {
    public ChanceForSpawnSliderWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty(), getInitialValue());
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int percent = (int) Math.round(this.value * 100.0);
        this.setMessage(Text.literal(percent + "%"));
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

    private static double getInitialValue() {
        Object value = ConfigManager.getInstance().getConfigValue("chanceForSpawn");
        int percent = value instanceof Number number ? number.intValue() : 100;
        percent = Math.max(0, Math.min(100, percent));
        return percent / 100.0;
    }
}
