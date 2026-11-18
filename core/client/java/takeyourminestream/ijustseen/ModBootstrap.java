package takeyourminestream.ijustseen;

import takeyourminestream.ijustseen.commands.CommandManager;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.filtering.BanwordManager;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.input.KeyBindingManager;
import takeyourminestream.ijustseen.integration.chat.ChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.messages.MessageSystemFactory;
import takeyourminestream.ijustseen.messages.TwitchEmoteTextureCache;
import takeyourminestream.ijustseen.utils.Logger;

/**
 * Общая клиентская инициализация (Fabric / NeoForge / Forge вызывают после {@code ModEnvironment.bootstrap}).
 */
public final class ModBootstrap {
    private ModBootstrap() {}

    public static void initClient(TakeYourMineStreamClient app) {
        try {
            IConfigManager configManager = ConfigManager.getInstance();
            IBanwordManager banwordManager = BanwordManager.getInstance();
            BlockedUsernameManager blockedUsernameManager = BlockedUsernameManager.getInstance();
            FilteringManager filteringManager = FilteringManager.getInstance();
            IChatConnectionManager chatConnectionManager = ChatConnectionManager.getInstance(configManager);
            MessageSpawner messageSpawner = MessageSystemFactory.createMessageSystem();

            CommandManager commandManager = new CommandManager(
                chatConnectionManager, banwordManager, blockedUsernameManager, messageSpawner
            );
            KeyBindingManager keyBindingManager = new KeyBindingManager(chatConnectionManager, messageSpawner);

            commandManager.registerCommands();
            keyBindingManager.registerKeyBindings();
            WorldEventHandler.register(messageSpawner);
            TwitchEmoteTextureCache.registerClientTick();

            app.bindInitialized(
                configManager,
                chatConnectionManager,
                banwordManager,
                blockedUsernameManager,
                filteringManager,
                messageSpawner,
                commandManager,
                keyBindingManager
            );
            Logger.info("Take Your Stream Chat initialized");
            TakeYourMineStreamClient.LOGGER.info("All mod components initialized");
        } catch (Exception e) {
            Logger.error("Failed to initialize mod", e);
        }
    }
}
