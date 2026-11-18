package takeyourminestream.ijustseen.platform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** NeoForge events → {@link ClientHooks}. */
public final class NeoForgeClientHooks implements ClientHooks {
    private final List<KeyBinding> pendingKeys = new ArrayList<>();
    private final List<Consumer<MinecraftClient>> endTick = new ArrayList<>();
    private final List<Consumer<MinecraftClient>> stopping = new ArrayList<>();
    private final List<Runnable> joins = new ArrayList<>();
    private final List<Runnable> disconnects = new ArrayList<>();
    private final List<WorldRenderHandler> worldRenders = new ArrayList<>();
    private final List<HudRenderHandler> hudRenders = new ArrayList<>();
    private final List<CommandRegistration> commands = new ArrayList<>();

    public NeoForgeClientHooks(IEventBus modBus) {
        modBus.addListener(this::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyBinding key : pendingKeys) {
            event.register(key);
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (Consumer<MinecraftClient> handler : endTick) {
            handler.accept(client);
        }
    }

    private void onLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        for (Runnable join : joins) {
            join.run();
        }
    }

    private void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Runnable disconnect : disconnects) {
            disconnect.run();
        }
    }

    private void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        MatrixStack matrices = event.getPoseStack();
        VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        for (WorldRenderHandler handler : worldRenders) {
            handler.render(matrices, consumers);
        }
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        DrawContext drawContext = event.getGuiGraphics();
        for (HudRenderHandler handler : hudRenders) {
            handler.render(drawContext);
        }
    }

    private void onRegisterCommands(RegisterClientCommandsEvent event) {
        for (CommandRegistration registration : commands) {
            registration.register(event.getDispatcher(), event.getBuildContext());
        }
    }

    @Override
    public KeyBinding registerKeyBinding(KeyBinding keyBinding) {
        pendingKeys.add(keyBinding);
        return keyBinding;
    }

    @Override
    public void onEndClientTick(Consumer<MinecraftClient> handler) {
        endTick.add(handler);
    }

    @Override
    public void onClientStopping(Consumer<MinecraftClient> handler) {
        stopping.add(handler);
    }

    @Override
    public void onPlayJoin(Runnable handler) {
        joins.add(handler);
    }

    @Override
    public void onPlayDisconnect(Runnable handler) {
        disconnects.add(handler);
    }

    @Override
    public void onWorldRenderAfterEntities(WorldRenderHandler handler) {
        worldRenders.add(handler);
    }

    @Override
    public void onHudRender(HudRenderHandler handler) {
        hudRenders.add(handler);
    }

    @Override
    public void registerClientCommands(CommandRegistration handler) {
        commands.add(handler);
    }
}
