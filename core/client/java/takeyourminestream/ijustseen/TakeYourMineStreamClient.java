package takeyourminestream.ijustseen;

import org.slf4j.LoggerFactory;
import takeyourminestream.ijustseen.commands.CommandManager;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.input.KeyBindingManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IConfigManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;

/**
 * Состояние клиентского мода (общее для всех лоадеров).
 */
public class TakeYourMineStreamClient {
    /** Namespace ресурсов / Fabric mod id (дефисы OK). */
    public static final String MOD_ID = "take-your-stream-chat";
    /** FML modId для Forge/NeoForge: только [a-z0-9_]. */
    public static final String FML_MOD_ID = "take_your_stream_chat";
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static TakeYourMineStreamClient instance;
    private IConfigManager configManager;
    private IChatConnectionManager chatConnectionManager;
    private IBanwordManager banwordManager;
    private BlockedUsernameManager blockedUsernameManager;
    private FilteringManager filteringManager;
    private MessageSpawner messageSpawner;
    private CommandManager commandManager;
    private KeyBindingManager keyBindingManager;

    public static TakeYourMineStreamClient createInstance() {
        instance = new TakeYourMineStreamClient();
        return instance;
    }

    void bindInitialized(
        IConfigManager configManager,
        IChatConnectionManager chatConnectionManager,
        IBanwordManager banwordManager,
        BlockedUsernameManager blockedUsernameManager,
        FilteringManager filteringManager,
        MessageSpawner messageSpawner,
        CommandManager commandManager,
        KeyBindingManager keyBindingManager
    ) {
        this.configManager = configManager;
        this.chatConnectionManager = chatConnectionManager;
        this.banwordManager = banwordManager;
        this.blockedUsernameManager = blockedUsernameManager;
        this.filteringManager = filteringManager;
        this.messageSpawner = messageSpawner;
        this.commandManager = commandManager;
        this.keyBindingManager = keyBindingManager;
    }

    public static TakeYourMineStreamClient getInstance() {
        return instance;
    }

    public IConfigManager getConfigManager() {
        return configManager;
    }

    public IChatConnectionManager getChatConnectionManager() {
        return chatConnectionManager;
    }

    /** @deprecated Use {@link #getChatConnectionManager()} */
    @Deprecated
    public IChatConnectionManager getTwitchManager() {
        return chatConnectionManager;
    }

    public IBanwordManager getBanwordManager() {
        return banwordManager;
    }

    public BlockedUsernameManager getBlockedUsernameManager() {
        return blockedUsernameManager;
    }

    public FilteringManager getFilteringManager() {
        return filteringManager;
    }

    public MessageSpawner getMessageSpawner() {
        return messageSpawner;
    }

    public static MessageSpawner getStaticMessageSpawner() {
        return instance != null ? instance.messageSpawner : null;
    }
}
