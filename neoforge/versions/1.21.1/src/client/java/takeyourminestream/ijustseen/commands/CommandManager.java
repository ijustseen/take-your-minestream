package takeyourminestream.ijustseen.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.platform.ClientHooks;
import takeyourminestream.ijustseen.utils.Logger;

import java.util.Set;

/**
 * Клиентские команды для NeoForge (RegisterClientCommandsEvent).
 */
public class CommandManager {
    private final IChatConnectionManager chatConnectionManager;
    private final IBanwordManager banwordManager;
    private final BlockedUsernameManager blockedUsernameManager;
    private final MessageSpawner messageSpawner;

    public CommandManager(
        IChatConnectionManager chatConnectionManager,
        IBanwordManager banwordManager,
        BlockedUsernameManager blockedUsernameManager,
        MessageSpawner messageSpawner
    ) {
        this.chatConnectionManager = chatConnectionManager;
        this.banwordManager = banwordManager;
        this.blockedUsernameManager = blockedUsernameManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerCommands() {
        ClientHooks.get().registerClientCommands((dispatcher, registryAccess) -> {
            @SuppressWarnings("unchecked")
            CommandDispatcher<ServerCommandSource> typed = (CommandDispatcher<ServerCommandSource>) dispatcher;
            typed.register(buildRootCommand("streamchat"));
            typed.register(buildRootCommand("minestream"));
        });
    }

    private LiteralArgumentBuilder<ServerCommandSource> buildRootCommand(String name) {
        return literal(name)
            .then(literal("test")
                .then(argument("message", StringArgumentType.greedyString())
                    .executes(context -> executeTestCommand(StringArgumentType.getString(context, "message")))))
            .then(literal("stop")
                .executes(context -> executeStopCommand()))
            .then(literal("chat")
                .then(literal("start").executes(context -> executeChatStartCommand()))
                .then(literal("stop").executes(context -> executeChatStopCommand())))
            .then(literal("twitch")
                .then(literal("start").executes(context -> executeChatStartCommand()))
                .then(literal("stop").executes(context -> executeChatStopCommand())))
            .then(literal("banword")
                .then(literal("add")
                    .then(argument("word", StringArgumentType.word())
                        .executes(context -> executeBanwordAddCommand(StringArgumentType.getString(context, "word")))))
                .then(literal("remove")
                    .then(argument("word", StringArgumentType.word())
                        .executes(context -> executeBanwordRemoveCommand(StringArgumentType.getString(context, "word")))))
                .then(literal("list").executes(context -> executeBanwordListCommand())))
            .then(literal("blockuser")
                .then(literal("add")
                    .then(argument("username", StringArgumentType.word())
                        .executes(context -> executeBlockuserAddCommand(StringArgumentType.getString(context, "username")))))
                .then(literal("remove")
                    .then(argument("username", StringArgumentType.word())
                        .executes(context -> executeBlockuserRemoveCommand(StringArgumentType.getString(context, "username")))))
                .then(literal("list").executes(context -> executeBlockuserListCommand())))
            .then(literal("help").executes(context -> executeHelpCommand()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> literal(String name) {
        return net.minecraft.server.command.CommandManager.literal(name);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, ?> argument(
        String name,
        com.mojang.brigadier.arguments.ArgumentType<?> type
    ) {
        return net.minecraft.server.command.CommandManager.argument(name, type);
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
