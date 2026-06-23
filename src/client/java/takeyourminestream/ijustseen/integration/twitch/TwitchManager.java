package takeyourminestream.ijustseen.integration.twitch;

import takeyourminestream.ijustseen.integration.chat.ChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import takeyourminestream.ijustseen.interfaces.ITwitchManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;

/**
 * @deprecated Use {@link ChatConnectionManager} directly.
 */
@Deprecated
public class TwitchManager implements ITwitchManager {
    private static TwitchManager instance;
    private final ChatConnectionManager delegate;

    private TwitchManager(IConfigManager configManager) {
        this.delegate = ChatConnectionManager.getInstance(configManager);
    }

    public static TwitchManager getInstance(IConfigManager configManager) {
        if (instance == null) {
            instance = new TwitchManager(configManager);
        }
        return instance;
    }

    public static ChatConnectionManager getChatManager(IConfigManager configManager) {
        return ChatConnectionManager.getInstance(configManager);
    }

    @Override
    public void connect(MessageSpawner spawner) {
        delegate.connect(spawner);
    }

    @Override
    public void disconnect() {
        delegate.disconnect();
    }

    @Override
    public void onChannelNameChanged(String newChannelName) {
        delegate.reconnectChangedPlatforms();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    public IChatConnectionManager getDelegate() {
        return delegate;
    }
}
