package takeyourminestream.ijustseen.messages;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;

import java.util.Comparator;
import java.util.List;

/**
 * Размеры 3D-панели сообщения — единый расчёт для рендера, кликов и «заморозки при взгляде».
 */
public final class MessagePanelLayout {
    public static final int EMOTE_ICON_SIZE = 12;
    public static final int EMOTE_ICON_SPACING = 1;
    public static final int PLATFORM_ICON_SIZE = 8;
    public static final float BASE_WORLD_SCALE = 0.025f;

    public record Dimensions(
        int maxTextWidth,
        float totalTextHeight,
        int panelWidth,
        int panelHeight,
        int firstLineIconOffset
    ) {}

    private MessagePanelLayout() {}

    public static Dimensions compute(TextRenderer textRenderer, Message message) {
        boolean hasInlineEmotes = message.hasInlineEmotes();
        String platformIconKey = message.getPlatformIconKey();
        int iconOffset = platformIconKey != null ? PLATFORM_ICON_SIZE + EMOTE_ICON_SPACING : 0;

        float totalTextHeight;
        int maxTextWidth;

        if (hasInlineEmotes) {
            List<EmoteTextLayout.LineContent> lines = EmoteTextLayout.wrap(
                s -> textRenderer.getWidth(s),
                message.getText(),
                message.getEmotes(),
                EMOTE_ICON_SIZE + EMOTE_ICON_SPACING,
                MessagePanelConstants.MESSAGE_WRAP_WIDTH,
                iconOffset
            );
            totalTextHeight = 0.0f;
            maxTextWidth = 0;
            for (int i = 0; i < lines.size(); i++) {
                EmoteTextLayout.LineContent line = lines.get(i);
                totalTextHeight += line.hasEmotes() ? EMOTE_ICON_SIZE + 1 : textRenderer.fontHeight;
                int lineWidth = measureEmoteLineWidth(textRenderer, line.text(), line.emotes())
                    + (i == 0 ? iconOffset : 0);
                maxTextWidth = Math.max(maxTextWidth, lineWidth);
            }
        } else {
            List<OrderedText> wrappedText = textRenderer.wrapLines(
                Text.of(message.getText()),
                MessagePanelConstants.MESSAGE_WRAP_WIDTH
            );
            totalTextHeight = wrappedText.size() * textRenderer.fontHeight;
            maxTextWidth = 0;
            for (int i = 0; i < wrappedText.size(); i++) {
                int lineWidth = textRenderer.getWidth(wrappedText.get(i)) + (i == 0 ? iconOffset : 0);
                maxTextWidth = Math.max(maxTextWidth, lineWidth);
            }
        }

        int panelWidth = maxTextWidth + MessagePanelConstants.PADDING_X * 2;
        int panelHeight = (int) totalTextHeight + MessagePanelConstants.PADDING_Y * 2;
        return new Dimensions(maxTextWidth, totalTextHeight, panelWidth, panelHeight, iconOffset);
    }

    public static float fallOffsetY(int effectiveAge) {
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int fallStart = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallAge = effectiveAge - fallStart;
        if (fallAge < 0 || fallAge >= fallTicks) {
            return 0.0f;
        }
        float fallProgress = (float) fallAge / (float) fallTicks;
        fallProgress = Math.min(Math.max(fallProgress, 0.0f), 1.0f);
        return (fallProgress * fallProgress) * 20.0f;
    }

    public static float worldScale() {
        return BASE_WORLD_SCALE * ModConfig.getMESSAGE_SCALE().getScale();
    }

    /** Координаты textSpace: (0,0) — левый верх текста, как в {@code MessageRenderer}. */
    public static boolean isInsidePanel(Dimensions dimensions, double textSpaceX, double textSpaceY) {
        double panelX0 = -MessagePanelConstants.PADDING_X;
        double panelY0 = -MessagePanelConstants.PADDING_Y;
        double panelX1 = dimensions.maxTextWidth() + MessagePanelConstants.PADDING_X;
        double panelY1 = dimensions.totalTextHeight() + MessagePanelConstants.PADDING_Y;
        return textSpaceX >= panelX0 && textSpaceX <= panelX1
            && textSpaceY >= panelY0 && textSpaceY <= panelY1;
    }

    public static boolean isInsidePinIcon(Dimensions dimensions, double textSpaceX, double textSpaceY) {
        double pinX0 = dimensions.panelWidth()
            - MessagePanelConstants.PADDING_X
            - (MessagePanelConstants.PIN_ICON_SIZE / 2.0)
            + MessagePanelConstants.PIN_ICON_MARGIN;
        double pinY0 = -MessagePanelConstants.PADDING_Y
            - (MessagePanelConstants.PIN_ICON_SIZE / 2.0)
            - MessagePanelConstants.PIN_ICON_MARGIN;
        double pinX1 = pinX0 + MessagePanelConstants.PIN_ICON_SIZE;
        double pinY1 = pinY0 + MessagePanelConstants.PIN_ICON_SIZE;
        return textSpaceX >= pinX0 && textSpaceX <= pinX1
            && textSpaceY >= pinY0 && textSpaceY <= pinY1;
    }

    public static int measureEmoteLineWidth(TextRenderer textRenderer, String text, List<MessageEmote> emotes) {
        int width = 0;
        int cursor = 0;
        for (MessageEmote emote : sortedValidEmotes(text, emotes)) {
            if (emote.getStartIndex() > cursor) {
                width += textRenderer.getWidth(text.substring(cursor, emote.getStartIndex()));
            }
            width += EMOTE_ICON_SIZE + EMOTE_ICON_SPACING;
            cursor = emote.getEndIndex() + 1;
        }
        if (cursor < text.length()) {
            width += textRenderer.getWidth(text.substring(cursor));
        }
        return width;
    }

    private static List<MessageEmote> sortedValidEmotes(String text, List<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return List.of();
        }
        List<MessageEmote> sorted = new java.util.ArrayList<>(emotes);
        sorted.sort(Comparator.comparingInt(MessageEmote::getStartIndex));
        List<MessageEmote> valid = new java.util.ArrayList<>();
        int nextAllowedStart = 0;
        for (MessageEmote emote : sorted) {
            if (emote.getStartIndex() < 0
                || emote.getEndIndex() < emote.getStartIndex()
                || emote.getEndIndex() >= text.length()) {
                continue;
            }
            if (emote.getStartIndex() < nextAllowedStart) {
                continue;
            }
            valid.add(emote);
            nextAllowedStart = emote.getEndIndex() + 1;
        }
        return valid;
    }
}
