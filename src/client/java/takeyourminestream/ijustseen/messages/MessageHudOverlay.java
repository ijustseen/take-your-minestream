package takeyourminestream.ijustseen.messages;

import net.minecraft.client.font.TextRenderer;
import takeyourminestream.ijustseen.config.HudAnchor;
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
        return prepare(textRenderer, activeMessages, tickCounter, screenWidth, 0);
    }

    public static List<PreparedCard> prepare(
        TextRenderer textRenderer,
        List<Message> activeMessages,
        int tickCounter,
        int screenWidth,
        int screenHeight
    ) {
        float hudScale = ModConfig.getMESSAGE_SCALE().getScale();
        HudAnchor anchor = ModConfig.getHUD_ANCHOR();
        int insetX = MARGIN + ModConfig.getHUD_OFFSET_X();
        int insetY = MARGIN + ModConfig.getHUD_OFFSET_Y();
        int maxCardWidth = Math.max(
            MessagePanelConstants.MESSAGE_WRAP_WIDTH + MessagePanelConstants.PADDING_X * 2,
            (int) ((screenWidth - insetX * 2) / hudScale)
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

        record SizedCard(Message message, MessageCardLayout.Layout layout, ChatMessageParser.ParsedMessage parsed,
                         int scaledWidth, int scaledHeight, float alpha, int slide) {}

        List<SizedCard> sized = new ArrayList<>();
        int stackHeight = 0;
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
            if (!sized.isEmpty()) {
                stackHeight += SPACING;
            }
            stackHeight += scaledHeight;
            sized.add(new SizedCard(message, layout, parsed, scaledWidth, scaledHeight, alpha, slide));
        }
        if (sized.isEmpty()) {
            return List.of();
        }

        int currentY = anchor.isBottom() && screenHeight > 0
            ? screenHeight - insetY - stackHeight
            : insetY;
        currentY = Math.max(0, currentY);

        List<PreparedCard> cards = new ArrayList<>();
        for (SizedCard card : sized) {
            int slide = anchor.isRight() ? card.slide() : -card.slide();
            int panelX = anchor.isRight()
                ? screenWidth - insetX - card.scaledWidth() + slide
                : insetX + slide;
            cards.add(new PreparedCard(
                card.message(),
                card.layout(),
                card.parsed(),
                panelX,
                currentY,
                card.scaledWidth(),
                card.scaledHeight(),
                card.alpha()
            ));
            currentY += card.scaledHeight() + SPACING;
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
