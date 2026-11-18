package takeyourminestream.ijustseen.platform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Forge 1.20.1 events → {@link ClientHooks}. */
public final class ForgeClientHooks implements ClientHooks {
    private final List<KeyBinding> pendingKeys = new ArrayList<>();
    private final List<Consumer<MinecraftClient>> endTick = new ArrayList<>();
    private final List<Consumer<MinecraftClient>> stopping = new ArrayList<>();
    private final List<Runnable> joins = new ArrayList<>();
    private final List<Runnable> disconnects = new ArrayList<>();
    private final List<WorldRenderHandler> worldRenders = new ArrayList<>();
    private final List<HudRenderHandler> hudRenders = new ArrayList<>();
    private final List<CommandRegistration> commands = new ArrayList<>();

    public ForgeClientHooks(IEventBus modBus) {
        modBus.addListener(this::onRegisterKeys);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderLevel);
        MinecraftForge.EVENT_BUS.addListener(this::onRenderGui);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyBinding key : pendingKeys) {
            event.register(key);
        }
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

    private void onRenderGui(RenderGuiOverlayEvent.Post event) {
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
