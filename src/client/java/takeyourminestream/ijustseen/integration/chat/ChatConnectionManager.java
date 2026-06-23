package takeyourminestream.ijustseen.integration.chat;

import net.minecraft.text.Text;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.config.ModConfigData;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import takeyourminestream.ijustseen.integration.kick.KickChatClient;
import takeyourminestream.ijustseen.integration.tiktok.TikTokChatClient;
import takeyourminestream.ijustseen.integration.twitch.TwitchChatClient;
import takeyourminestream.ijustseen.integration.youtube.YouTubeChatClient;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.utils.PlayerMessageCompat;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class ChatConnectionManager implements IChatConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ChatConnectionManager.class.getName());
    private static ChatConnectionManager instance;

    private final IConfigManager configManager;
    private final Map<ChatPlatform, ChatConnection> connections = new EnumMap<>(ChatPlatform.class);
    private final Map<ChatPlatform, String> activeChannelIds = new EnumMap<>(ChatPlatform.class);
    private ChatMessagePipeline pipeline;
    private MessageSpawner messageSpawner;
    /** Пользователь нажал «Отключить чат» — не переподключать автоматически при закрытии настроек. */
    private boolean manualDisconnect;

    private ChatConnectionManager(IConfigManager configManager) {
        this.configManager = configManager;
    }

    public static ChatConnectionManager getInstance(IConfigManager configManager) {
        if (instance == null) {
            instance = new ChatConnectionManager(configManager);
        }
        return instance;
    }

    @Override
    public void connect(MessageSpawner spawner) {
        manualDisconnect = false;
        this.messageSpawner = spawner;
        if (this.pipeline == null) {
            this.pipeline = new ChatMessagePipeline(spawner);
        }

        ModConfigData config = ConfigManager.getInstance().getConfigData();
        int before = connections.size();

        connectPlatform(ChatPlatform.TWITCH, config.isTwitchEnabled(), config.getTwitchChannelName(),
            () -> new TwitchChatClient(config.getTwitchChannelName(), pipeline));
        connectPlatform(ChatPlatform.YOUTUBE, config.isYoutubeEnabled(), config.getYoutubeChannel(),
            () -> new YouTubeChatClient(config.getYoutubeChannel(), pipeline));
        connectPlatform(ChatPlatform.KICK, config.isKickEnabled(), config.getKickChannel(),
            () -> new KickChatClient(config.getKickChannel(), pipeline));
        connectPlatform(ChatPlatform.TIKTOK, config.isTiktokEnabled(), config.getTiktokUsername(),
            () -> new TikTokChatClient(config.getTiktokUsername(), pipeline));

        if (connections.isEmpty()) {
            ChatStatusNotifier.showTranslatableWarning("takeyourstreamchat.status.no_platforms");
        } else if (connections.size() > before) {
            ChatStatusNotifier.showTranslatableSuccess(
                "takeyourstreamchat.status.connected_count",
                connections.size()
            );
        }
    }

    private void connectPlatform(
        ChatPlatform platform,
        boolean enabled,
        String channel,
        java.util.function.Supplier<ChatConnection> factory
    ) {
        String normalized = normalizeChannel(channel);
        if (!enabled || normalized.isEmpty()) {
            return;
        }
        String activeChannel = activeChannelIds.get(platform);
        ChatConnection existing = connections.get(platform);
        if (existing != null && existing.isConnected() && normalized.equals(activeChannel)) {
            return;
        }
        if (existing != null) {
            stopConnection(platform, existing);
        }
        try {
            ChatStatusNotifier.showTranslatable(
                "takeyourstreamchat.status.connecting",
                platform.getDisplayName(),
                normalized
            );
            ChatConnection connection = factory.get();
            connections.put(platform, connection);
            activeChannelIds.put(platform, normalized);
            LOGGER.info("Started " + platform + " client for channel: " + normalized);
        } catch (Exception e) {
            LOGGER.severe("Failed to start " + platform + " client: " + e.getMessage());
            reportConnectError(platform, e);
        }
    }

    @Override
    public void disconnect() {
        if (connections.isEmpty()) {
            manualDisconnect = true;
            ChatStatusNotifier.showTranslatableWarning("takeyourstreamchat.status.not_connected");
            return;
        }
        for (ChatPlatform platform : ChatPlatform.values()) {
            ChatConnection connection = connections.get(platform);
            if (connection != null) {
                stopConnection(platform, connection);
            }
        }
        connections.clear();
        activeChannelIds.clear();
        manualDisconnect = true;
        ChatStatusNotifier.showTranslatable("takeyourstreamchat.status.disconnected");
        LOGGER.info("Disconnected from all chat platforms");
    }

    @Override
    public void reconnectChangedPlatforms() {
        reconnectChangedPlatforms(false);
    }

    public void reconnectChangedPlatforms(boolean forceReconnect) {
        if (messageSpawner == null) {
            messageSpawner = takeyourminestream.ijustseen.TakeYourMineStreamClient.getStaticMessageSpawner();
        }
        if (messageSpawner == null) {
            return;
        }
        if (this.pipeline == null) {
            this.pipeline = new ChatMessagePipeline(messageSpawner);
        }
        ModConfigData config = ConfigManager.getInstance().getConfigData();
        reconnectIfChanged(ChatPlatform.TWITCH, config.isTwitchEnabled(), config.getTwitchChannelName(),
            () -> new TwitchChatClient(config.getTwitchChannelName(), pipeline), forceReconnect);
        reconnectIfChanged(ChatPlatform.YOUTUBE, config.isYoutubeEnabled(), config.getYoutubeChannel(),
            () -> new YouTubeChatClient(config.getYoutubeChannel(), pipeline), forceReconnect);
        reconnectIfChanged(ChatPlatform.KICK, config.isKickEnabled(), config.getKickChannel(),
            () -> new KickChatClient(config.getKickChannel(), pipeline), forceReconnect);
        reconnectIfChanged(ChatPlatform.TIKTOK, config.isTiktokEnabled(), config.getTiktokUsername(),
            () -> new TikTokChatClient(config.getTiktokUsername(), pipeline), forceReconnect);
    }

    private void reconnectIfChanged(
        ChatPlatform platform,
        boolean enabled,
        String channel,
        java.util.function.Supplier<ChatConnection> factory,
        boolean forceReconnect
    ) {
        String normalized = normalizeChannel(channel);
        ChatConnection existing = connections.get(platform);

        if (!enabled || normalized.isEmpty()) {
            if (existing != null) {
                stopConnection(platform, existing);
            }
            return;
        }

        if (manualDisconnect && !forceReconnect) {
            return;
        }

        String activeChannel = activeChannelIds.get(platform);
        if (existing != null && existing.isConnected() && normalized.equals(activeChannel)) {
            return;
        }

        if (existing != null) {
            stopConnection(platform, existing);
        }
        connectPlatform(platform, true, normalized, factory);
    }

    private void stopConnection(ChatPlatform platform, ChatConnection connection) {
        try {
            connection.disconnect();
        } catch (Exception e) {
            LOGGER.warning("Disconnect error for " + platform + ": " + e.getMessage());
        }
        connections.remove(platform);
        activeChannelIds.remove(platform);
        ChatErrorReporter.clear(platform);
    }

    private static void reportConnectError(ChatPlatform platform, Exception error) {
        Text reason = error instanceof ChatConnectException cce
            ? Text.translatable(cce.getTranslationKey(), cce.getArgs())
            : Text.translatable(
                "takeyourstreamchat.chat.error.connect",
                platform.getDisplayName(),
                String.valueOf(error.getMessage())
            );
        PlayerMessageCompat.send(
            net.minecraft.client.MinecraftClient.getInstance(),
            "§c" + reason.getString()
        );
    }

    private static String normalizeChannel(String channel) {
        return channel != null ? channel.trim() : "";
    }

    @Override
    public boolean isConnected() {
        return !connections.isEmpty();
    }

    @Override
    public boolean isPlatformConnected(ChatPlatform platform) {
        ChatConnection connection = connections.get(platform);
        return connection != null && connection.isConnected();
    }

    @Override
    public int getConnectedCount() {
        return (int) connections.values().stream().filter(ChatConnection::isConnected).count();
    }
}
