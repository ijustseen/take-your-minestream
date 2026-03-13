package takeyourminestream.ijustseen.commands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import takeyourminestream.ijustseen.interfaces.ITwitchManager;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.utils.Logger;

import java.util.Set;

/**
 * Менеджер команд мода
 */
public class CommandManager {
    private final ITwitchManager twitchManager;
    private final IBanwordManager banwordManager;
    private final MessageSpawner messageSpawner;

    public CommandManager(ITwitchManager twitchManager, IBanwordManager banwordManager, MessageSpawner messageSpawner) {
        this.twitchManager = twitchManager;
        this.banwordManager = banwordManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Основная команда minestream
            dispatcher.register(ClientCommandManager.literal("minestream")
                .then(ClientCommandManager.literal("test")
                    .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            String message = StringArgumentType.getString(context, "message");
                            return executeTestCommand(message);
                        })))
                .then(ClientCommandManager.literal("stop")
                    .executes(context -> executeStopCommand()))
                .then(ClientCommandManager.literal("twitch")
                    .then(ClientCommandManager.literal("start")
                        .executes(context -> executeTwitchStartCommand()))
                    .then(ClientCommandManager.literal("stop")
                        .executes(context -> executeTwitchStopCommand())))
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
                .then(ClientCommandManager.literal("help")
                    .executes(context -> executeHelpCommand()))
            );
        });
    }

    private int executeTestCommand(String message) {
        messageSpawner.setCurrentMessage(message);
        Logger.sendInfoToPlayer("Тестовое сообщение установлено: " + message);
        return 1;
    }

    private int executeStopCommand() {
        messageSpawner.setCurrentMessage("");
        twitchManager.disconnect();
        Logger.sendInfoToPlayer("Мод остановлен");
        return 1;
    }

    private int executeTwitchStartCommand() {
        twitchManager.connect(messageSpawner);
        return 1;
    }

    private int executeTwitchStopCommand() {
        twitchManager.disconnect();
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

    private int executeHelpCommand() {
        Logger.sendInfoToPlayer("=== Take Your MineStream - Помощь ===");
        Logger.sendInfoToPlayer("/minestream test <сообщение> - Тестовое сообщение");
        Logger.sendInfoToPlayer("/minestream stop - Остановить мод");
        Logger.sendInfoToPlayer("/minestream twitch start - Подключиться к Twitch");
        Logger.sendInfoToPlayer("/minestream twitch stop - Отключиться от Twitch");
        Logger.sendInfoToPlayer("/minestream banword add <слово> - Добавить банворд");
        Logger.sendInfoToPlayer("/minestream banword remove <слово> - Удалить банворд");
        Logger.sendInfoToPlayer("/minestream banword list - Список банвордов");
        Logger.sendInfoToPlayer("/minestream help - Показать эту справку");
        return 1;
    }
} 