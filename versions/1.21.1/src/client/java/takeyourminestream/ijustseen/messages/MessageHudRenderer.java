package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.ui.gui.MessageCardRenderer;

import java.util.List;

/** HUD-овerлей (MatrixStack API, MC 1.21–1.21.4). */
public class MessageHudRenderer {
    private final MessageLifecycleManager lifecycleManager;
    private final BlockedUsernameManager blockedUsernameManager = BlockedUsernameManager.getInstance();

    public MessageHudRenderer(MessageLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (ModConfig.getMESSAGE_SPAWN_MODE() == MessageSpawnMode.HUD_WIDGET) {
                renderHudMessages(drawContext);
            }
        });
    }

    private void renderHudMessages(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        float hudScale = ModConfig.getMESSAGE_SCALE().getScale();
        List<MessageHudOverlay.PreparedCard> cards = MessageHudOverlay.prepare(
            textRenderer,
            lifecycleManager.getActiveMessages(),
            lifecycleManager.getTickCounter(),
            screenWidth
        );

        for (MessageHudOverlay.PreparedCard card : cards) {
            var matrices = drawContext.getMatrices();
            matrices.push();
            matrices.translate((float) card.x(), (float) card.y(), 0f);
            matrices.scale(hudScale, hudScale, 1f);

            MessageCardRenderer.drawHudCard(
                drawContext,
                textRenderer,
                blockedUsernameManager,
                card.message(),
                card.parsed(),
                card.layout(),
                0,
                0,
                card.alpha()
            );

            matrices.pop();
        }
    }
}
