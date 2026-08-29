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

/** HUD-оверлей сообщений чата (угол и отступ задаются в настройках). */
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
        if (!MessageHudVisibility.shouldRender(client)) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float hudScale = ModConfig.getMESSAGE_SCALE().getScale();
        List<MessageHudOverlay.PreparedCard> cards = MessageHudOverlay.prepare(
            textRenderer,
            lifecycleManager.getActiveMessages().stream()
                .filter(message -> PinnedMessageStore.belongsToCurrentWorld(message, client))
                .toList(),
            lifecycleManager.getTickCounter(),
            screenWidth,
            screenHeight
        );

        for (MessageHudOverlay.PreparedCard card : cards) {
            var matrices = drawContext.getMatrices();
            matrices.pushMatrix();
            matrices.translate((float) card.x(), (float) card.y());
            matrices.scale(hudScale, hudScale);

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

            matrices.popMatrix();
        }
    }
}
