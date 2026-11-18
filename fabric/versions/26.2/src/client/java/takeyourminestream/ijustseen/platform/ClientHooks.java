package takeyourminestream.ijustseen.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/** 26.x: без MultiBufferSource — world/HUD рендер в version overrides. */
public interface ClientHooks {
    static ClientHooks get() {
        return ClientHooksHolder.get();
    }

    static void install(ClientHooks hooks) {
        ClientHooksHolder.set(hooks);
    }

    KeyMapping registerKeyBinding(KeyMapping keyBinding);

    void onEndClientTick(Consumer<Minecraft> handler);

    void onClientStopping(Consumer<Minecraft> handler);

    void onPlayJoin(Runnable handler);

    void onPlayDisconnect(Runnable handler);

    void onWorldRenderAfterEntities(WorldRenderHandler handler);

    void onHudRender(HudRenderHandler handler);

    void registerClientCommands(CommandRegistration handler);

    @FunctionalInterface
    interface WorldRenderHandler {
        void render();
    }

    @FunctionalInterface
    interface HudRenderHandler {
        void render(GuiGraphicsExtractor drawContext);
    }

    @FunctionalInterface
    interface CommandRegistration {
        void register(Object dispatcher, Object registryAccess);
    }
}
