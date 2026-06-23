package takeyourminestream.ijustseen.ui.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import takeyourminestream.ijustseen.core.text.ChatMessageParser;
import takeyourminestream.ijustseen.messages.MessageEmote;
import takeyourminestream.ijustseen.messages.TwitchEmoteTextureCache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Отрисовка строки чата с Twitch/7TV-эмоутами в GUI/HUD. */
public final class MessageEmoteGuiRenderer {
    public static final int ICON_SIZE = 12;
    public static final int ICON_SPACING = 1;
    public static final int PLATFORM_ICON_SIZE = 10;

    private MessageEmoteGuiRenderer() {}

    /** Рисует пиксельную иконку платформы и возвращает X после неё. */
    public static int drawPlatformIcon(DrawContext context, String iconKey, int x, int y) {
        Identifier texture = TwitchEmoteTextureCache.getTextureIdentifier("platform", iconKey);
        if (texture == null) {
            return x;
        }
        return drawGuiIcon(context, texture, x, y, PLATFORM_ICON_SIZE);
    }

    /** Рисует квадратную GUI-иконку из ресурсов и возвращает X после неё. */
    public static int drawGuiIcon(DrawContext context, Identifier texture, int x, int y, int size) {
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            0,
            0,
            size,
            size,
            size,
            size,
            size,
            size,
            0xFFFFFFFF
        );
        return x + size + 2;
    }

    public static List<MessageEmote> remapEmotesToBody(String rawText, List<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return List.of();
        }
        int bodyStart = ChatMessageParser.getBodyStartIndex(rawText);
        List<MessageEmote> remapped = new ArrayList<>();
        for (MessageEmote emote : emotes) {
            if (emote.getEndIndex() < bodyStart) {
                continue;
            }
            int start = Math.max(emote.getStartIndex(), bodyStart) - bodyStart;
            int end = emote.getEndIndex() - bodyStart;
            String bodyText = rawText.substring(bodyStart);
            if (end >= bodyText.length()) {
                continue;
            }
            remapped.add(new MessageEmote(
                emote.getProvider(),
                emote.getEmoteId(),
                emote.getEmoteCode(),
                start,
                end
            ));
        }
        return remapped;
    }

    public static String getBodyText(String rawText) {
        int bodyStart = ChatMessageParser.getBodyStartIndex(rawText);
        return rawText.substring(bodyStart);
    }

    public static int measureLine(TextRenderer textRenderer, String text, List<MessageEmote> emotes) {
        int width = 0;
        int cursor = 0;
        for (MessageEmote emote : getSortedValidEmotes(text, emotes)) {
            if (emote.getStartIndex() > cursor) {
                width += textRenderer.getWidth(ChatMessageParser.stripFormatting(text.substring(cursor, emote.getStartIndex())));
            }
            width += ICON_SIZE + ICON_SPACING;
            cursor = emote.getEndIndex() + 1;
        }
        if (cursor < text.length()) {
            width += textRenderer.getWidth(ChatMessageParser.stripFormatting(text.substring(cursor)));
        }
        return width;
    }

    public static void drawLine(
        DrawContext context,
        TextRenderer textRenderer,
        String text,
        List<MessageEmote> emotes,
        int x,
        int y,
        int color
    ) {
        List<MessageEmote> sortedEmotes = getSortedValidEmotes(text, emotes);
        int cursor = 0;
        int drawX = x;

        for (MessageEmote emote : sortedEmotes) {
            if (emote.getStartIndex() > cursor) {
                String segment = text.substring(cursor, emote.getStartIndex());
                drawX = drawSegment(context, textRenderer, segment, drawX, y, color);
            }

            Identifier texture = TwitchEmoteTextureCache.getTextureIdentifier(emote.getProvider(), emote.getEmoteId());
            if (texture != null) {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    drawX,
                    y - 1,
                    0,
                    0,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    ICON_SIZE,
                    color
                );
                drawX += ICON_SIZE + ICON_SPACING;
            } else if (!"emoji".equals(emote.getProvider())) {
                drawX = drawSegment(context, textRenderer, emote.getEmoteCode(), drawX, y, color);
            } else {
                drawX += ICON_SIZE + ICON_SPACING;
            }
            cursor = emote.getEndIndex() + 1;
        }

        if (cursor < text.length()) {
            drawSegment(context, textRenderer, text.substring(cursor), drawX, y, color);
        }
    }

    private static int drawSegment(
        DrawContext context,
        TextRenderer textRenderer,
        String segment,
        int x,
        int y,
        int color
    ) {
        if (segment.isEmpty()) {
            return x;
        }
        Text label = Text.literal(ChatMessageParser.stripFormatting(segment));
        context.drawTextWithShadow(textRenderer, label, x, y, color);
        return x + textRenderer.getWidth(label);
    }

    private static List<MessageEmote> getSortedValidEmotes(String text, List<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return List.of();
        }

        List<MessageEmote> sorted = new ArrayList<>(emotes);
        sorted.sort(Comparator.comparingInt(MessageEmote::getStartIndex));

        List<MessageEmote> valid = new ArrayList<>();
        int nextAllowedStart = 0;
        for (MessageEmote emote : sorted) {
            if (emote.getStartIndex() < 0 || emote.getEndIndex() < emote.getStartIndex() || emote.getEndIndex() >= text.length()) {
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
