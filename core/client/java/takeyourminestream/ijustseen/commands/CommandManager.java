package takeyourminestream.ijustseen.commands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.utils.Logger;

import java.util.Set;

/**
 * Менеджер команд мода
 */
public class CommandManager {
    private final IChatConnectionManager chatConnectionManager;
    private final IBanwordManager banwordManager;
    private final BlockedUsernameManager blockedUsernameManager;
    private final MessageSpawner messageSpawner;

    public CommandManager(IChatConnectionManager chatConnectionManager, IBanwordManager banwordManager, BlockedUsernameManager blockedUsernameManager, MessageSpawner messageSpawner) {
        this.chatConnectionManager = chatConnectionManager;
        this.banwordManager = banwordManager;
        this.blockedUsernameManager = blockedUsernameManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildRootCommand("streamchat"));
            dispatcher.register(buildRootCommand("minestream"));
        });
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> buildRootCommand(String name) {
        return ClientCommandManager.literal(name)
                .then(ClientCommandManager.literal("test")
                    .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            String message = StringArgumentType.getString(context, "message");
                            return executeTestCommand(message);
                        })))
                .then(ClientCommandManager.literal("stop")
                    .executes(context -> executeStopCommand()))
                .then(ClientCommandManager.literal("chat")
                    .then(ClientCommandManager.literal("start")
                        .executes(context -> executeChatStartCommand()))
                    .then(ClientCommandManager.literal("stop")
                        .executes(context -> executeChatStopCommand())))
                .then(ClientCommandManager.literal("twitch")
                    .then(ClientCommandManager.literal("start")
                        .executes(context -> executeChatStartCommand()))
                    .then(ClientCommandManager.literal("stop")
                        .executes(context -> executeChatStopCommand())))
                .then(ClientCommandManager.literal("banword")
                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("word", StringArgumentType.word())
                            .executes(context -> {
                                String word = StringArgumentType.getString(context, "word");
                                return executeBanwordAddCommand(word);
                            })))
                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("word", StringArgumentType.word())
                            .executes(context -> {
                                String word = StringArgumentType.getString(context, "word");
                                return executeBanwordRemoveCommand(word);
                            })))
                    .then(ClientCommandManager.literal("list")
                        .executes(context -> executeBanwordListCommand())))
                .then(ClientCommandManager.literal("blockuser")
                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("username", StringArgumentType.word())
                            .executes(context -> {
                                String username = StringArgumentType.getString(context, "username");
                                return executeBlockuserAddCommand(username);
                            })))
                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("username", StringArgumentType.word())
                            .executes(context -> {
                                String username = StringArgumentType.getString(context, "username");
                                return executeBlockuserRemoveCommand(username);
                            })))
                    .then(ClientCommandManager.literal("list")
                        .executes(context -> executeBlockuserListCommand())))
                .then(ClientCommandManager.literal("help")
                    .executes(context -> executeHelpCommand()));
    }

    private int executeTestCommand(String message) {
        messageSpawner.setCurrentMessage(message);
        Logger.sendInfoToPlayer("Тестовое сообщение установлено: " + message);
        return 1;
    }

    private int executeStopCommand() {
        messageSpawner.setCurrentMessage("");
        chatConnectionManager.disconnect();
        Logger.sendInfoToPlayer("Мод остановлен");
        return 1;
    }

    private int executeChatStartCommand() {
        chatConnectionManager.connect(messageSpawner);
        return 1;
    }

    private int executeChatStopCommand() {
        chatConnectionManager.disconnect();
        return 1;
    }

    private int executeBanwordAddCommand(String word) {
        banwordManager.addBanword(word);
        Logger.sendInfoToPlayer("Банворд добавлен: " + word);
        return 1;
    }

    private int executeBanwordRemoveCommand(String word) {
        banwordManager.removeBanword(word);
        Logger.sendInfoToPlayer("Банворд удален: " + word);
        return 1;
    }

    private int executeBanwordListCommand() {
        Set<String> banwords = banwordManager.getBanwords();
        if (banwords.isEmpty()) {
            Logger.sendInfoToPlayer("Список банвордов пуст");
        } else {
            Logger.sendInfoToPlayer("Список банвордов (" + banwords.size() + "):");
            for (String banword : banwords) {
                Logger.sendToPlayer("  - " + banword);
            }
        }
        return 1;
    }

    private int executeBlockuserAddCommand(String username) {
        blockedUsernameManager.addBlockedUsername(username);
        Logger.sendInfoToPlayer("Ник добавлен в чёрный список: " + username);
        return 1;
    }

    private int executeBlockuserRemoveCommand(String username) {
        blockedUsernameManager.removeBlockedUsername(username);
        Logger.sendInfoToPlayer("Ник удалён из чёрного списка: " + username);
        return 1;
    }

    private int executeBlockuserListCommand() {
        Set<String> blocked = blockedUsernameManager.getBlockedUsernames();
        if (blocked.isEmpty()) {
            Logger.sendInfoToPlayer("Чёрный список ников пуст");
        } else {
            Logger.sendInfoToPlayer("Чёрный список ников (" + blocked.size() + "):");
            for (String username : blocked) {
                Logger.sendToPlayer("  - " + username);
            }
        }
        return 1;
    }

    private int executeHelpCommand() {
        Logger.sendInfoToPlayer("=== Take Your Stream Chat — команды ===");
        Logger.sendInfoToPlayer("/streamchat test <сообщение> — тестовое сообщение");
        Logger.sendInfoToPlayer("/streamchat stop — очистить сообщения и отключить чаты");
        Logger.sendInfoToPlayer("/streamchat chat start — подключиться ко всем включённым платформам");
        Logger.sendInfoToPlayer("/streamchat chat stop — отключиться от чатов");
        Logger.sendInfoToPlayer("/streamchat banword add|remove|list — банворды");
        Logger.sendInfoToPlayer("/streamchat blockuser add|remove|list — чёрный список ников");
        Logger.sendInfoToPlayer("/streamchat help — эта справка");
        Logger.sendInfoToPlayer("Алиас: /minestream … (то же самое)");
        return 1;
    }
}
