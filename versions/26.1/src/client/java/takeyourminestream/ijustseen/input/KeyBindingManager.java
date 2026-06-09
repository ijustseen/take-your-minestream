package takeyourminestream.ijustseen.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.integration.twitch.TwitchManager;
import takeyourminestream.ijustseen.interfaces.ITwitchManager;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.ui.screen.ModConfigScreen;
import takeyourminestream.ijustseen.utils.Logger;

/** Менеджер привязок клавиш (Minecraft 26.1). */
public class KeyBindingManager {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("takeyourminestream", "key_category")
    );
    private final ITwitchManager twitchManager;
    private final MessageSpawner messageSpawner;
    private KeyMapping openConfigScreenKeyMapping;
    private KeyMapping startAndStopMessagesKeyMapping;

    public KeyBindingManager(ITwitchManager twitchManager, MessageSpawner messageSpawner) {
        this.twitchManager = twitchManager;
        this.messageSpawner = messageSpawner;
    }

    public void registerKeyBindings() {
        openConfigScreenKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.takeyourminestream.openconfig",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            KEY_CATEGORY
        ));

        startAndStopMessagesKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.takeyourminestream.startandstop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_BRACKET,
            KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigScreenKeyMapping.consumeClick()) {
                handleOpenConfigScreen();
            }
            while (startAndStopMessagesKeyMapping.consumeClick()) {
                handleTwitchToggle();
            }
        });
    }

    private void handleOpenConfigScreen() {
        try {
            net.minecraft.client.Minecraft.getInstance().setScreen(new ModConfigScreen());
            
        } catch (Exception e) {
            Logger.error("Failed to open settings screen", e);
            Logger.sendErrorToPlayer("Failed to open settings screen");
        }
    }

    private void handleTwitchToggle() {
        try {
            var twitchManager = TwitchManager.getInstance(ConfigManager.getInstance());
            var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();

            if (twitchManager.isConnected()) {
                twitchManager.disconnect();
            } else if (messageSpawner != null) {
                twitchManager.connect(messageSpawner);
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Twitch connection error: ", e);
        }
    }

    public KeyMapping getOpenConfigScreenKeyBinding() {
        return openConfigScreenKeyMapping;
    }
}
