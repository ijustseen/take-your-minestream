package takeyourminestream.modid.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import takeyourminestream.modid.interfaces.ITwitchManager;
import takeyourminestream.modid.ModConfigScreen;
import takeyourminestream.modid.messages.MessageSpawner;
import takeyourminestream.modid.utils.Logger;

/**
 * Менеджер привязок клавиш
 */
public class KeyBindingManager {
    private final ITwitchManager twitchManager;
    private final MessageSpawner messageSpawner;
    private KeyBinding openConfigScreenKeyBinding;

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
            KeyBinding.Category.MISC
        ));

        // Обработка нажатия клавиш
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigScreenKeyBinding.wasPressed()) {
                handleOpenConfigScreen();
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

    public KeyBinding getOpenConfigScreenKeyBinding() {
        return openConfigScreenKeyBinding;
    }
} 