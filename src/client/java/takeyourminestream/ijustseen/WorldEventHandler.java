package takeyourminestream.ijustseen;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.messages.PinnedMessageStore;
import takeyourminestream.ijustseen.utils.Logger;

public class WorldEventHandler {
    private static String lastDimensionKey;

    public static void register(MessageSpawner messageSpawner) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            onWorldJoin(messageSpawner);
            if (client.world != null) {
                lastDimensionKey = client.world.getRegistryKey().getValue().toString();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            onWorldLeave(messageSpawner);
            lastDimensionKey = null;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.world == null || messageSpawner == null) {
                return;
            }

            String currentDimensionKey = client.world.getRegistryKey().getValue().toString();
            if (lastDimensionKey == null) {
                lastDimensionKey = currentDimensionKey;
                return;
            }

            if (!lastDimensionKey.equals(currentDimensionKey)) {
                lastDimensionKey = currentDimensionKey;
                PinnedMessageStore.loadForCurrentWorld(messageSpawner.getLifecycleManager());
                Logger.info("Dimension changed, pinned messages reloaded for " + currentDimensionKey);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (messageSpawner != null) {
                var app = TakeYourMineStreamClient.getInstance();
                if (app != null && app.getTwitchManager() != null && app.getTwitchManager().isConnected()) {
                    app.getTwitchManager().disconnect();
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
                && app.getTwitchManager() != null
                && !app.getTwitchManager().isConnected()) {
                app.getTwitchManager().connect(messageSpawner);
            }
        }
    }

    public static void onWorldLeave(MessageSpawner messageSpawner) {
        if (messageSpawner != null && !messageSpawner.isPaused()) {
            var app = TakeYourMineStreamClient.getInstance();
            if (app != null && app.getTwitchManager() != null && app.getTwitchManager().isConnected()) {
                app.getTwitchManager().disconnect();
            }
            PinnedMessageStore.saveForCurrentWorld(messageSpawner.getLifecycleManager());
            messageSpawner.pause();
            TakeYourMineStreamClient.LOGGER.info("Message system paused (left world)");
        }
    }
}
