package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.integration.twitch.TwitchManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;

/** Кнопка/логика переключения Twitch IRC для экранов настроек. */
public final class TwitchToggleHelper {
    private TwitchToggleHelper() {}

    public static Text buttonLabel() {
        boolean connected = TwitchManager.getInstance(ConfigManager.getInstance()).isConnected();
        String key = connected ? "takeyourminestream.config.twitch_on" : "takeyourminestream.config.twitch_off";
        return Text.translatable(key).append(Text.literal(" ●").formatted(connected ? Formatting.GREEN : Formatting.RED));
    }

    public static void toggle() {
        try {
            TwitchManager twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
            MessageSpawner spawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            if (twitchManager.isConnected()) {
                twitchManager.disconnect();
            } else if (spawner != null) {
                twitchManager.connect(spawner);
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Twitch toggle exception: ", e);
        }
    }
}
