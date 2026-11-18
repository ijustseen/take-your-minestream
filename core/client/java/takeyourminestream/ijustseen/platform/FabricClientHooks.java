package takeyourminestream.ijustseen.platform;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/** Fabric API → {@link ClientHooks}. */
public final class FabricClientHooks implements ClientHooks {
    @Override
    public KeyBinding registerKeyBinding(KeyBinding keyBinding) {
        return KeyBindingHelper.registerKeyBinding(keyBinding);
    }

    @Override
    public void onEndClientTick(Consumer<MinecraftClient> handler) {
        ClientTickEvents.END_CLIENT_TICK.register(handler::accept);
    }

    @Override
    public void onClientStopping(Consumer<MinecraftClient> handler) {
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
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MatrixStack matrices = context.matrices();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices != null && consumers != null) {
                handler.render(matrices, consumers);
            }
        });
    }

    @Override
    public void onHudRender(HudRenderHandler handler) {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> handler.render(drawContext));
    }

    @Override
    public void registerClientCommands(CommandRegistration handler) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            handler.register(dispatcher, registryAccess)
        );
    }
}
