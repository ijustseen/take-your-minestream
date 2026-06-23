package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.integration.chat.ChatConnectionManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;

/** Кнопка и логика подключения ко всем включённым платформам. */
public final class ChatConnectToggleHelper {
    private ChatConnectToggleHelper() {}

    public static Text buttonLabel() {
        ChatConnectionManager manager = ChatConnectionManager.getInstance(ConfigManager.getInstance());
        boolean connected = manager.isConnected();
        int count = manager.getConnectedCount();

        String key = connected
            ? "takeyourstreamchat.config.chat_toggle_on"
            : "takeyourstreamchat.config.chat_toggle_off";
        MutableText label = Text.translatable(key);
        if (connected && count > 0) {
            label.append(Text.literal(" (" + count + ")"));
        }
        label.append(Text.literal(" ●").formatted(connected ? Formatting.GREEN : Formatting.RED));
        return label;
    }

    public static void toggle() {
        try {
            ChatConnectionManager manager = ChatConnectionManager.getInstance(ConfigManager.getInstance());
            MessageSpawner spawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            if (manager.isConnected()) {
                manager.disconnect();
            } else if (spawner != null) {
                manager.connect(spawner);
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Chat toggle error", e);
        }
    }
}
