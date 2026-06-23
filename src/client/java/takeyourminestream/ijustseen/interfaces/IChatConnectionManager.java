package takeyourminestream.ijustseen.interfaces;

import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.messages.MessageSpawner;

/**
 * Управление подключениями к чатам стриминговых платформ.
 */
public interface IChatConnectionManager {
    void connect(MessageSpawner spawner);

    void disconnect();

    boolean isConnected();

    boolean isPlatformConnected(ChatPlatform platform);

    int getConnectedCount();

    void reconnectChangedPlatforms();
}
