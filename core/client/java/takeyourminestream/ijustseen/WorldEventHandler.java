package takeyourminestream.ijustseen;

import net.minecraft.client.MinecraftClient;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.messages.PinnedMessageStore;
import takeyourminestream.ijustseen.platform.ClientHooks;
import takeyourminestream.ijustseen.utils.Logger;

public class WorldEventHandler {
    private static String lastDimensionKey;

    public static void register(MessageSpawner messageSpawner) {
        ClientHooks hooks = ClientHooks.get();

        hooks.onPlayJoin(() -> {
            onWorldJoin(messageSpawner);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                lastDimensionKey = client.world.getRegistryKey().getValue().toString();
            }
        });

        hooks.onPlayDisconnect(() -> {
            onWorldLeave(messageSpawner);
            lastDimensionKey = null;
        });

        hooks.onEndClientTick(client -> {
            if (client == null || client.world == null || messageSpawner == null) {
                return;
            }

            String currentDimensionKey = client.world.getRegistryKey().getValue().toString();
            if (lastDimensionKey == null) {
                lastDimensionKey = currentDimensionKey;
                return;
            }

            if (!lastDimensionKey.equals(currentDimensionKey)) {
                PinnedMessageStore.saveForDimension(messageSpawner.getLifecycleManager(), lastDimensionKey);
                messageSpawner.getLifecycleManager().removeMessagesOutsideDimension(currentDimensionKey);
                lastDimensionKey = currentDimensionKey;
                PinnedMessageStore.loadForCurrentWorld(messageSpawner.getLifecycleManager());
                Logger.info("Dimension changed, pinned messages reloaded for " + currentDimensionKey);
            }
        });

        hooks.onClientStopping(client -> {
            if (messageSpawner != null) {
                var app = TakeYourMineStreamClient.getInstance();
                if (app != null && app.getChatConnectionManager() != null && app.getChatConnectionManager().isConnected()) {
                    app.getChatConnectionManager().disconnect();
                }
                PinnedMessageStore.saveForCurrentWorld(messageSpawner.getLifecycleManager());
                messageSpawner.pause();
                TakeYourMineStreamClient.LOGGER.info("Message system on pause");
            }
        });
    }

    public static void onWorldJoin(MessageSpawner messageSpawner) {
        if (messageSpawner != null) {
            if (messageSpawner.isPaused()) {
                messageSpawner.resume();
                TakeYourMineStreamClient.LOGGER.info("Message system unpaused (entered world)");
            }
            PinnedMessageStore.loadForCurrentWorld(messageSpawner.getLifecycleManager());

            var app = TakeYourMineStreamClient.getInstance();
            if (ModConfig.isAUTO_CONNECT_IRC_ON_JOIN()
                && app != null
                && app.getChatConnectionManager() != null
                && !app.getChatConnectionManager().isConnected()) {
                app.getChatConnectionManager().connect(messageSpawner);
            }
        }
    }

    public static void onWorldLeave(MessageSpawner messageSpawner) {
        if (messageSpawner != null && !messageSpawner.isPaused()) {
            var app = TakeYourMineStreamClient.getInstance();
            if (app != null && app.getChatConnectionManager() != null && app.getChatConnectionManager().isConnected()) {
                app.getChatConnectionManager().disconnect();
            }
            PinnedMessageStore.saveForCurrentWorld(messageSpawner.getLifecycleManager());
            messageSpawner.pause();
            TakeYourMineStreamClient.LOGGER.info("Message system paused (left world)");
        }
    }
}
