package takeyourminestream.ijustseen.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.ui.screen.ModConfigScreen;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.utils.Logger;

public class KeyBindingManager {
    private static final String KEY_CATEGORY = "key.categories.takeyourstreamchat";
    private final IChatConnectionManager chatConnectionManager;
    private final MessageSpawner messageSpawner;
    private KeyBinding openConfigScreenKeyBinding;
    private KeyBinding startAndStopMessagesKeyBinding;

    public KeyBindingManager(IChatConnectionManager chatConnectionManager, MessageSpawner messageSpawner) {
        this.chatConnectionManager = chatConnectionManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerKeyBindings() {
        openConfigScreenKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.takeyourstreamchat.openconfig",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            KEY_CATEGORY
        ));

        startAndStopMessagesKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.takeyourstreamchat.startandstop",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,
            KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigScreenKeyBinding.wasPressed()) {
                handleOpenConfigScreen();
            }
            while (startAndStopMessagesKeyBinding.wasPressed()) {
                handleChatToggle();
            }
        });
    }

    private void handleOpenConfigScreen() {
        try {
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new ModConfigScreen());
        } catch (Exception e) {
            Logger.error("Failed to open settings screen", e);
            Logger.sendErrorToPlayer("Failed to open settings screen");
        }
    }

    private void handleChatToggle() {
        try {
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
            if (chatConnectionManager.isConnected()) {
                chatConnectionManager.disconnect();
            } else if (messageSpawner != null) {
                chatConnectionManager.connect(messageSpawner);
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Chat connection error: ", e);
        }
    }

    public KeyBinding getOpenConfigScreenKeyBinding() {
        return openConfigScreenKeyBinding;
    }
}
