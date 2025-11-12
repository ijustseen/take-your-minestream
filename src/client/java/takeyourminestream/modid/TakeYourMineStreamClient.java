package takeyourminestream.modid;

import net.fabricmc.api.ClientModInitializer;
import takeyourminestream.modid.messages.MessageSpawner;
import takeyourminestream.modid.messages.MessageSystemFactory;
import takeyourminestream.modid.interfaces.IConfigManager;
import takeyourminestream.modid.interfaces.ITwitchManager;
import takeyourminestream.modid.interfaces.IBanwordManager;
import takeyourminestream.modid.commands.CommandManager;
import takeyourminestream.modid.input.KeyBindingManager;
import takeyourminestream.modid.utils.Logger;

/**
 * Главный класс клиентской части мода
 */
public class TakeYourMineStreamClient implements ClientModInitializer {
    private static TakeYourMineStreamClient instance;
    private IConfigManager configManager;
    private ITwitchManager twitchManager;
    private IBanwordManager banwordManager;
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
        
        Logger.info("Все компоненты мода инициализированы");
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

    public MessageSpawner getMessageSpawner() {
        return messageSpawner;
    }
    
    /**
     * Статический метод для получения MessageSpawner
     */
    public static MessageSpawner getStaticMessageSpawner() {
        return instance != null ? instance.messageSpawner : null;
    }
}