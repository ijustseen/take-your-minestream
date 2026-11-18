package takeyourminestream.ijustseen.input;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.interfaces.IChatConnectionManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.platform.ClientHooks;
import takeyourminestream.ijustseen.ui.screen.MessageHistoryScreen;
import takeyourminestream.ijustseen.ui.screen.ModConfigScreen;
import takeyourminestream.ijustseen.utils.Logger;
import takeyourminestream.ijustseen.utils.ScreenNavigationCompat;

/**
 * Менеджер привязок клавиш
 */
public class KeyBindingManager {
    private static final KeyBinding.Category KEY_CATEGORY =
        KeyBinding.Category.create(Identifier.of("take-your-stream-chat", "key_category"));
    private final IChatConnectionManager chatConnectionManager;
    private final MessageSpawner messageSpawner;
    private KeyBinding openConfigScreenKeyBinding;
    private KeyBinding startAndStopMessagesKeyBinding;
    private KeyBinding openMessageHistoryKeyBinding;

    public KeyBindingManager(IChatConnectionManager chatConnectionManager, MessageSpawner messageSpawner) {
        this.chatConnectionManager = chatConnectionManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerKeyBindings() {
        ClientHooks hooks = ClientHooks.get();
        openConfigScreenKeyBinding = hooks.registerKeyBinding(new KeyBinding(
            "key.takeyourstreamchat.openconfig",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            KEY_CATEGORY
        ));

        startAndStopMessagesKeyBinding = hooks.registerKeyBinding(new KeyBinding(
            "key.takeyourstreamchat.startandstop",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,
            KEY_CATEGORY
        ));

        openMessageHistoryKeyBinding = hooks.registerKeyBinding(new KeyBinding(
            "key.takeyourstreamchat.openhistory",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            KEY_CATEGORY
        ));

        hooks.onEndClientTick(client -> {
            while (openConfigScreenKeyBinding.wasPressed()) {
                handleOpenConfigScreen();
            }
            while (startAndStopMessagesKeyBinding.wasPressed()) {
                handleChatToggle();
            }
            while (openMessageHistoryKeyBinding.wasPressed()) {
                handleOpenMessageHistory();
            }
        });
    }

    private void handleOpenConfigScreen() {
        try {
            ScreenNavigationCompat.open(net.minecraft.client.MinecraftClient.getInstance(), new ModConfigScreen());
        } catch (Exception e) {
            Logger.error("Failed to open settings screen", e);
            Logger.sendErrorToPlayer("Failed to open settings screen");
        }
    }

    private void handleOpenMessageHistory() {
        try {
            MessageSpawner spawner = this.messageSpawner != null
                ? this.messageSpawner
                : TakeYourMineStreamClient.getStaticMessageSpawner();
            if (spawner == null) {
                return;
            }
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (ScreenNavigationCompat.current(client) instanceof MessageHistoryScreen) {
                return;
            }
            var lifecycle = spawner.getLifecycleManager();
            if (client.textRenderer != null) {
                lifecycle.warmHistoryLayouts(client.textRenderer);
            }
            ScreenNavigationCompat.open(client, new MessageHistoryScreen(
                ScreenNavigationCompat.current(client),
                lifecycle
            ));
        } catch (Exception e) {
            Logger.error("Failed to open message history", e);
            Logger.sendErrorToPlayer("Failed to open message history");
        }
    }

    private void handleChatToggle() {
        try {
            MessageSpawner spawner = this.messageSpawner != null
                ? this.messageSpawner
                : TakeYourMineStreamClient.getStaticMessageSpawner();
            if (chatConnectionManager.isParserEnabled()) {
                chatConnectionManager.disconnect();
            } else if (spawner != null) {
                chatConnectionManager.connect(spawner);
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Chat connection error: ", e);
        }
    }

    public KeyBinding getOpenConfigScreenKeyBinding() {
        return openConfigScreenKeyBinding;
    }
}
