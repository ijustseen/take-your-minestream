package takeyourminestream.ijustseen.platform;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

/** Fabric API → {@link ClientHooks} для Minecraft 26.x. */
public final class FabricClientHooks implements ClientHooks {
    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyBinding) {
        return KeyMappingHelper.registerKeyMapping(keyBinding);
    }

    @Override
    public void onEndClientTick(Consumer<Minecraft> handler) {
        ClientTickEvents.END_CLIENT_TICK.register(handler::accept);
    }

    @Override
    public void onClientStopping(Consumer<Minecraft> handler) {
        ClientLifecycleEvents.CLIENT_STOPPING.register(handler::accept);
    }

    @Override
    public void onPlayJoin(Runnable handler) {
        ClientPlayConnectionEvents.JOIN.register((h, sender, client) -> handler.run());
    }

    @Override
    public void onPlayDisconnect(Runnable handler) {
        ClientPlayConnectionEvents.DISCONNECT.register((h, client) -> handler.run());
    }

    @Override
    public void onWorldRenderAfterEntities(WorldRenderHandler handler) {
        // 26.x: MessageRenderer override регистрирует LevelRenderEvents.COLLECT_SUBMITS сам.
    }

    @Override
    public void onHudRender(HudRenderHandler handler) {
        // 26.x: MessageHudRenderer override регистрирует HudElementRegistry сам.
    }

    @Override
    public void registerClientCommands(CommandRegistration handler) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            handler.register(dispatcher, registryAccess)
        );
    }
}
