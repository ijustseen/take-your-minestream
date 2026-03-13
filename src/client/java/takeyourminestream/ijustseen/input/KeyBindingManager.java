package takeyourminestream.ijustseen.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import takeyourminestream.ijustseen.ConfigManager;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.TwitchManager;
import takeyourminestream.ijustseen.interfaces.ITwitchManager;
import takeyourminestream.ijustseen.ModConfigScreen;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.utils.Logger;

/**
 * Менеджер привязок клавиш
 */
public class KeyBindingManager {
    private static final String KEY_CATEGORY = "key.categories.takeyourminestream";
    private final ITwitchManager twitchManager;
    private final MessageSpawner messageSpawner;
    private KeyBinding openConfigScreenKeyBinding;
    private KeyBinding startAndStopMessagesKeyBinding;

    public KeyBindingManager(ITwitchManager twitchManager, MessageSpawner messageSpawner) {
        this.twitchManager = twitchManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerKeyBindings() {
        // Регистрация KeyBinding для открытия экрана настроек
        openConfigScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.takeyourminestream.openconfig",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET, // Клавиша ']'
            KEY_CATEGORY
        ));

        startAndStopMessagesKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.takeyourminestream.startandstop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET, // Клавиша '['
                KEY_CATEGORY
        ));

        // Обработка нажатия клавиш
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigScreenKeyBinding.wasPressed()) {
                handleOpenConfigScreen();
            }
            while (startAndStopMessagesKeyBinding.wasPressed()) {
                handleTwitchToggle();
            }
        });
    }

    private void handleOpenConfigScreen() {
        try {
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new ModConfigScreen());
            Logger.info("Открыт экран настроек");
        } catch (Exception e) {
            Logger.error("Ошибка при открытии экрана настроек", e);
            Logger.sendErrorToPlayer("Ошибка при открытии экрана настроек");
        }
    }

    private void handleTwitchToggle() {
        try {
            var twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();

            if (twitchManager.isConnected()) {
                twitchManager.disconnect();
            } else {
                if (messageSpawner != null) {
                    twitchManager.connect(messageSpawner);
                }
            }
        } catch (Exception e) {
            // Логируем ошибку, но не показываем игроку в GUI
            TakeYourMineStreamClient.LOGGER.error("Twitch connection error: ", e);
        }
    }

    public KeyBinding getOpenConfigScreenKeyBinding() {
        return openConfigScreenKeyBinding;
    }
} 