package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.ui.gui.MessageCardLayout;
import takeyourminestream.ijustseen.ui.gui.MessageCardRenderer;

import java.util.ArrayList;
import java.util.List;

/** HUD-оверлей сообщений чата (правый верхний угол). */
public class MessageHudRenderer {
    private static final Identifier HUD_ELEMENT_ID = Identifier.fromNamespaceAndPath(
        "take-your-minestream",
        "message_hud"
    );
    private static final int MAX_DISPLAYED_MESSAGES = 5;
    private static final int MESSAGE_MARGIN = 10;
    private static final int MESSAGE_SPACING = 4;

    private final MessageLifecycleManager lifecycleManager;
    private final BlockedUsernameManager blockedUsernameManager = BlockedUsernameManager.getInstance();

    public MessageHudRenderer(MessageLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;

        HudElementRegistry.attachElementAfter(
            VanillaHudElements.BOSS_BAR,
            HUD_ELEMENT_ID,
            (graphics, deltaTracker) -> {
                if (ModConfig.getMESSAGE_SPAWN_MODE() == MessageSpawnMode.HUD_WIDGET) {
                    renderHudMessages(graphics);
                }
            }
        );
    }

    private void renderHudMessages(GuiGraphicsExtractor drawContext) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) {
            return;
        }

        List<Message> activeMessages = lifecycleManager.getActiveMessages();
        if (activeMessages.isEmpty()) {
            return;
        }

        Font textRenderer = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        float hudScale = ModConfig.getMESSAGE_SCALE().getScale();
        int fixedRightEdge = screenWidth - MESSAGE_MARGIN;
        int maxCardWidth = Math.max(
            MessagePanelConstants.MESSAGE_WRAP_WIDTH + MessagePanelConstants.PADDING_X * 2,
            (int) ((screenWidth - MESSAGE_MARGIN * 2) / hudScale)
        );

        List<Message> messagesToDisplay = new ArrayList<>();
        for (int i = activeMessages.size() - 1; i >= 0 && messagesToDisplay.size() < MAX_DISPLAYED_MESSAGES; i--) {
            Message message = activeMessages.get(i);
            if (!message.isPinned()) {
                messagesToDisplay.add(message);
            }
        }
        if (messagesToDisplay.isEmpty()) {
            return;
        }

        int currentY = MESSAGE_MARGIN;

        for (Message message : messagesToDisplay) {
            float alpha = calculateMessageAlpha(message);
            if (alpha <= 0.0f) {
                continue;
            }

            ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(message.getText());
            MessageCardLayout.Layout layout = MessageCardLayout.computeHud(textRenderer, message, maxCardWidth);
            int scaledWidth = Math.round(layout.width() * hudScale);
            int scaledHeight = Math.round(layout.height() * hudScale);
            int panelX = fixedRightEdge - scaledWidth;

            var matrices = drawContext.pose();
            matrices.pushMatrix();
            matrices.translate((float) panelX, (float) currentY);
            matrices.scale(hudScale, hudScale);

            MessageCardRenderer.drawHudCard(
                drawContext,
                textRenderer,
                blockedUsernameManager,
                message,
                parsed,
                layout,
                0,
                0,
                alpha
            );

            matrices.popMatrix();
            currentY += scaledHeight + MESSAGE_SPACING;
        }
    }

    private float calculateMessageAlpha(Message message) {
        int tickCounter = lifecycleManager.getTickCounter();
        int age = message.getEffectiveAge(tickCounter);
        int lifetime = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();

        if (age < lifetime) {
            return 1.0f;
        }
        if (age < lifetime + fallTicks) {
            float fallProgress = (float) (age - lifetime) / (float) fallTicks;
            return 1.0f - fallProgress;
        }
        return 0.0f;
    }
}
