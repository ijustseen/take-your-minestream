package takeyourminestream.ijustseen.messages;

import net.minecraft.client.font.TextRenderer;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.ui.gui.MessageCardLayout;

import java.util.ArrayList;
import java.util.List;

/** Расчёт позиций HUD-карточек и hit-тест (общий для всех версий MC). */
public final class MessageHudOverlay {
    public static final int MAX_DISPLAYED_MESSAGES = 5;
    public static final int MARGIN = 10;
    public static final int SPACING = 4;
    private static final int SLIDE_IN_TICKS = 10;
    private static final float SLIDE_DISTANCE = 18.0f;

    public record PreparedCard(
        Message message,
        MessageCardLayout.Layout layout,
        ChatMessageParser.ParsedMessage parsed,
        int x,
        int y,
        int scaledWidth,
        int scaledHeight,
        float alpha
    ) {}

    private MessageHudOverlay() {}

    public static List<PreparedCard> prepare(
        TextRenderer textRenderer,
        List<Message> activeMessages,
        int tickCounter,
        int screenWidth
    ) {
        float hudScale = ModConfig.getMESSAGE_SCALE().getScale();
        int fixedRightEdge = screenWidth - MARGIN;
        int maxCardWidth = Math.max(
            MessagePanelConstants.MESSAGE_WRAP_WIDTH + MessagePanelConstants.PADDING_X * 2,
            (int) ((screenWidth - MARGIN * 2) / hudScale)
        );

        List<Message> candidates = new ArrayList<>();
        for (Message message : activeMessages) {
            if (!message.isPinned()) {
                candidates.add(message);
            }
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        int from = Math.max(0, candidates.size() - MAX_DISPLAYED_MESSAGES);
        List<Message> ordered = candidates.subList(from, candidates.size());

        List<PreparedCard> cards = new ArrayList<>();
        int currentY = MARGIN;

        for (int i = 0; i < ordered.size(); i++) {
            Message message = ordered.get(i);
            float alpha = computeAlpha(message, tickCounter);
            if (alpha <= 0.01f) {
                continue;
            }

            alpha *= depthFactor(i, ordered.size());

            ChatMessageParser.ParsedMessage parsed = ChatMessageParser.parse(message.getText());
            MessageCardLayout.Layout layout = message.getHudLayout(textRenderer, maxCardWidth);
            int scaledWidth = Math.round(layout.width() * hudScale);
            int scaledHeight = Math.round(layout.height() * hudScale);
            int slide = Math.round(computeSlideOffset(message, tickCounter));
            int panelX = fixedRightEdge - scaledWidth + slide;

            cards.add(new PreparedCard(message, layout, parsed, panelX, currentY, scaledWidth, scaledHeight, alpha));
            currentY += scaledHeight + SPACING;
        }

        return cards;
    }

    public static float computeAlpha(Message message, int tickCounter) {
        int age = message.getEffectiveAge(tickCounter);
        int lifetime = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        if (age < lifetime) {
            return 1.0f;
        }
        if (age < lifetime + fallTicks) {
            float t = (float) (age - lifetime) / (float) fallTicks;
            return 1.0f - t * t;
        }
        return 0.0f;
    }

    private static float depthFactor(int index, int count) {
        if (count <= 1) {
            return 1.0f;
        }
        return 0.9f + 0.1f * (index / (float) (count - 1));
    }

    private static float computeSlideOffset(Message message, int tickCounter) {
        int age = message.getEffectiveAge(tickCounter);
        float t = Math.min(1.0f, Math.max(0.0f, (float) age / SLIDE_IN_TICKS));
        float eased = 1.0f - (1.0f - t) * (1.0f - t);
        return (1.0f - eased) * SLIDE_DISTANCE;
    }
}
