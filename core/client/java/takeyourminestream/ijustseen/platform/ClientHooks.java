package takeyourminestream.ijustseen.platform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

/**
 * Лоадер-агностичные клиентские хуки. Entrypoint ставит реализацию до {@link takeyourminestream.ijustseen.ModBootstrap}.
 */
public interface ClientHooks {
    static ClientHooks get() {
        return ClientHooksHolder.get();
    }

    static void install(ClientHooks hooks) {
        ClientHooksHolder.set(hooks);
    }

    KeyBinding registerKeyBinding(KeyBinding keyBinding);

    void onEndClientTick(Consumer<MinecraftClient> handler);

    void onClientStopping(Consumer<MinecraftClient> handler);

    void onPlayJoin(Runnable handler);

    void onPlayDisconnect(Runnable handler);

    void onWorldRenderAfterEntities(WorldRenderHandler handler);

    void onHudRender(HudRenderHandler handler);

    void registerClientCommands(CommandRegistration handler);

    @FunctionalInterface
    interface WorldRenderHandler {
        void render(MatrixStack matrices, VertexConsumerProvider consumers);
    }

    @FunctionalInterface
    interface HudRenderHandler {
        void render(DrawContext drawContext);
    }

    @FunctionalInterface
    interface CommandRegistration {
        void register(Object dispatcher, Object registryAccess);
    }
}
