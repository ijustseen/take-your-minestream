package takeyourminestream.ijustseen.integration.twitch;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.interfaces.ITwitchManager;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.utils.PlayerMessageCompat;
import java.util.logging.Logger;

public class TwitchManager implements ITwitchManager {
    private static final Logger LOGGER = Logger.getLogger(TwitchManager.class.getName());
    private static TwitchManager instance;
    
    private TwitchChatClient twitchChatClient;
    private String lastTwitchChannelName;
    private boolean twitchConnected = false;
    private MessageSpawner messageSpawner;
    private final IConfigManager configManager;

    private TwitchManager(IConfigManager configManager) {
        this.configManager = configManager;
        this.lastTwitchChannelName = (String) configManager.getConfigValue("twitchChannelName");
    }

    public static TwitchManager getInstance(IConfigManager configManager) {
        if (instance == null) {
            instance = new TwitchManager(configManager);
        }
        return instance;
    }

    @Override
    public void connect(MessageSpawner spawner) {
        this.messageSpawner = spawner;
        String channelName = (String) configManager.getConfigValue("twitchChannelName");
        
        if (twitchChatClient == null) {
            try {
                sendPlayerMessage("§a" + Text.translatable("takeyourminestream.twitch.connecting", channelName).getString());
                twitchChatClient = new TwitchChatClient(channelName, messageSpawner);
                twitchConnected = true;
                lastTwitchChannelName = channelName;
                sendPlayerMessage("§a" + Text.translatable("takeyourminestream.twitch.connected").getString());
                LOGGER.info("Connected to Twitch channel: " + channelName);
            } catch (Exception e) {
                LOGGER.severe("Failed to connect to Twitch: " + e.getMessage());
                sendPlayerMessage("§c" + Text.translatable("takeyourminestream.twitch.error.connect", e.getMessage()).getString());
            }
        } else {
            sendPlayerMessage("§e" + Text.translatable("takeyourminestream.twitch.already_connected").getString());
        }
    }

    @Override
    public void disconnect() {
        if (twitchChatClient != null) {
            try {
                twitchChatClient.disconnect();
                twitchChatClient = null;
                twitchConnected = false;
                sendPlayerMessage("§a" + Text.translatable("takeyourminestream.twitch.disconnected").getString());
                LOGGER.info("Disconnected from Twitch channel");
            } catch (Exception e) {
                LOGGER.severe("Failed to disconnect from Twitch: " + e.getMessage());
                sendPlayerMessage("§c" + Text.translatable("takeyourminestream.twitch.error.disconnect", e.getMessage()).getString());
            }
        } else {
            sendPlayerMessage("§e" + Text.translatable("takeyourminestream.twitch.not_connected").getString());
        }
    }

    @Override
    public void onChannelNameChanged(String newChannelName) {
        if (twitchConnected && !newChannelName.equals(lastTwitchChannelName)) {
            try {
                // Переподключаемся к новому каналу
                if (twitchChatClient != null) {
                    twitchChatClient.disconnect();
                }
                twitchChatClient = new TwitchChatClient(newChannelName, messageSpawner);
                lastTwitchChannelName = newChannelName;
                sendPlayerMessage("§a" + Text.translatable("takeyourminestream.twitch.reconnected", newChannelName).getString());
                LOGGER.info("Reconnected to Twitch channel: " + newChannelName);
            } catch (Exception e) {
                LOGGER.severe("Failed to reconnect to Twitch: " + e.getMessage());
                sendPlayerMessage("§c" + Text.translatable("takeyourminestream.twitch.error.reconnect", e.getMessage()).getString());
            }
        }
        lastTwitchChannelName = newChannelName;
    }

    @Override
    public boolean isConnected() {
        return twitchConnected;
    }

    private void sendPlayerMessage(String message) {
        PlayerMessageCompat.send(MinecraftClient.getInstance(), message);
    }
} 