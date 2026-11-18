package takeyourminestream.ijustseen.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.function.Consumer;

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
        void render(PoseStack matrices, MultiBufferSource consumers);
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
