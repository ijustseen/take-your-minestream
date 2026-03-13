package takeyourminestream.ijustseen;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.LoggerFactory;
import takeyourminestream.ijustseen.filtering.BanwordManager;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.messages.MessageSystemFactory;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import takeyourminestream.ijustseen.interfaces.ITwitchManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.commands.CommandManager;
import takeyourminestream.ijustseen.input.KeyBindingManager;
import takeyourminestream.ijustseen.utils.Logger;

/**
 * Главный класс клиентской части мода
 */
public class TakeYourMineStreamClient implements ClientModInitializer {
    public static final String MOD_ID = "take-your-minestream";
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static TakeYourMineStreamClient instance;
    private IConfigManager configManager;
    private ITwitchManager twitchManager;
    private IBanwordManager banwordManager;
    private FilteringManager filteringManager;
    private MessageSpawner messageSpawner;
    private CommandManager commandManager;
    private KeyBindingManager keyBindingManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        try {
            initializeMod();
            Logger.info("Take Your MineStream успешно инициализирован");
        } catch (Exception e) {
            Logger.error("Ошибка при инициализации мода", e);
        }
    }

    private void initializeMod() {
        // Инициализация менеджеров
        configManager = ConfigManager.getInstance();
        banwordManager = BanwordManager.getInstance();
        filteringManager = FilteringManager.getInstance();
        twitchManager = TwitchManager.getInstance(configManager);
        
        // Инициализация системы сообщений
        messageSpawner = MessageSystemFactory.createMessageSystem();
        
        // Инициализация менеджеров команд и клавиш
        commandManager = new CommandManager(twitchManager, banwordManager, messageSpawner);
        keyBindingManager = new KeyBindingManager(twitchManager, messageSpawner);
        
        // Регистрация команд и клавиш
        commandManager.registerCommands();
        keyBindingManager.registerKeyBindings();
        
        // Регистрация обработчиков событий мира
        WorldEventHandler.register(messageSpawner);

        LOGGER.info("Все компоненты мода инициализированы");
    }

    public static TakeYourMineStreamClient getInstance() {
        return instance;
    }

    public IConfigManager getConfigManager() {
        return configManager;
    }

    public ITwitchManager getTwitchManager() {
        return twitchManager;
    }

    public IBanwordManager getBanwordManager() {
        return banwordManager;
    }

    public FilteringManager getFilteringManager() {return filteringManager;}

    public MessageSpawner getMessageSpawner() {
        return messageSpawner;
    }

    public static MessageSpawner getStaticMessageSpawner() {
        return instance != null ? instance.messageSpawner : null;
    }
}