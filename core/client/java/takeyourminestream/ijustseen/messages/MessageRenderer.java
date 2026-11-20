package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelColors;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.text.LegacySectionText;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.RenderLayerCompat;
import takeyourminestream.ijustseen.core.render.MessagePanelWorldRenderer;
import takeyourminestream.ijustseen.platform.ClientHooks;

/**
 * Отвечает за рендеринг сообщений в мире.
 *
 * <p>Сообщения рисуются проходами по слоям (заливка всех → бортики всех → задники → текст →
 * иконки), а не сообщение за сообщением: {@code Immediate} сбрасывает слой на GPU при каждой
 * смене текстуры, и поштучный порядок давал 6–8 draw call'ов на сообщение.
 */
public class MessageRenderer {
    private final MessageLifecycleManager lifecycleManager;
    private final MessageParticleManager particleManager;
    private final java.util.WeakHashMap<Message, MessageRenderSmoothing> smoothing = new java.util.WeakHashMap<>();
    private final java.util.WeakHashMap<Message, MessageRenderLod.Level> lodState = new java.util.WeakHashMap<>();
    /** Куб редактора рисуется после всех сообщений: иначе его стенки перекрывают по глубине ещё не нарисованный текст. */
    private org.joml.Matrix4f pendingBlockCube;

    private final List<Prepared> prepared = new ArrayList<>();
    private final Map<Identifier, List<IconQuad>> textLayerIcons = new LinkedHashMap<>();
    private final Map<Identifier, List<IconQuad>> entityLayerIcons = new LinkedHashMap<>();

    private static final float PIN_ICON_Z_OFFSET = -0.02f;
    private static final int EMOTE_ICON_SPACING = 1;
    private static final float EMOTE_ICON_Z_OFFSET = 0.02f;
    private static final int MAX_ICON_TEXTURE_KEYS = 256;

    /** Сообщение после позиционирования: матрица в плоскости панели и размеры. */
    private record Prepared(
        Message message,
        MessageRenderLod.Level lod,
        Matrix4f panelMatrix,
        MessagePanelLayout.Dimensions layout,
        int panelWidth,
        int panelHeight
    ) {}

    private record IconQuad(Matrix4f matrix, int x0, int y0, int x1, int y1, float z) {}

    public MessageRenderer(MessageLifecycleManager lifecycleManager, MessageParticleManager particleManager) {
        this.lifecycleManager = lifecycleManager;
        this.particleManager = particleManager;

        ClientHooks.get().onWorldRenderAfterEntities((matrices, consumers) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            var spawnMode = ModConfig.getMESSAGE_SPAWN_MODE();
            if (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.HUD_WIDGET) {
                return;
            }

            TextRenderer textRenderer = client.textRenderer;
            prepared.clear();
            for (Message message : lifecycleManager.getActiveMessages()) {
                if (!PinnedMessageStore.belongsToCurrentWorld(message, client)) {
                    continue;
                }
                MessageRenderLod.Level drawLevel;
                if (message.isPreview()) {
                    drawLevel = MessageRenderLod.Level.FULL;
                } else {
                    // Cull/LOD по сырой позиции — до smoothing (дешевле на сотнях пинов).
                    MessageRenderLod.Level previous = lodState.getOrDefault(message, MessageRenderLod.Level.HIDDEN);
                    drawLevel = MessageRenderLod.resolve(client, message.getPosition(), previous);
                    lodState.put(message, drawLevel);
                }
                if (drawLevel == MessageRenderLod.Level.HIDDEN) {
                    continue;
                }
                Prepared entry = prepare(client, message, matrices, textRenderer, drawLevel);
                if (entry != null) {
                    prepared.add(entry);
                }
            }
            if (!prepared.isEmpty()) {
                renderPanels(consumers);
                renderTextAndCollectIcons(textRenderer, consumers);
                flushIcons(consumers);
            }
            prepared.clear();
            if (particleManager != null) {
                particleManager.render(client, matrices, consumers);
            }
            if (pendingBlockCube != null) {
                MessagePanelWorldRenderer.drawBlockCube(pendingBlockCube, consumers);
                pendingBlockCube = null;
            }
        });
    }

    private Prepared prepare(
        MinecraftClient client,
        Message message,
        MatrixStack matrices,
        TextRenderer textRenderer,
        MessageRenderLod.Level lod
    ) {
        int tickCounter = lifecycleManager.getTickCounter();
        int age = message.isPinned() ? 0 : message.getEffectiveAge(tickCounter);
        float fallOffsetY = MessagePanelLayout.fallOffsetY(age);
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int fallStart = ModConfig.getMESSAGE_LIFETIME_TICKS();
        if (age - fallStart >= fallTicks) {
            return null;
        }

        // Smoothing только для видимых.
        MessageRenderSmoothing state = smoothing.computeIfAbsent(message, MessageRenderSmoothing::fromMessage);
        state.updateTowards(message);

        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);

        matrices.push();
        matrices.translate(
            state.pos().x - cameraPos.getX(),
            state.pos().y - cameraPos.getY(),
            state.pos().z - cameraPos.getZ()
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch()));
        if (message.isPreview() && message.hasBlockCube()) {
            pendingBlockCube = new org.joml.Matrix4f(matrices.peek().getPositionMatrix());
        }

        float finalScale = MessagePanelLayout.worldScale();
        matrices.scale(finalScale, -finalScale, finalScale);

        MessagePanelLayout.Dimensions layout;
        int panelWidth;
        int panelHeight;
        if (lod == MessageRenderLod.Level.SIMPLE) {
            layout = message.peekWorldLayout();
            if (layout == null) {
                // Не форсим полный compute: фиксированный stub для дальних.
                matrices.translate(-30.0f, -8.0f + fallOffsetY, 0f);
                panelWidth = 72;
                panelHeight = 20;
            } else {
                matrices.translate(-layout.maxTextWidth() / 2.0f, -layout.totalTextHeight() / 2.0f + fallOffsetY, 0f);
                panelWidth = layout.panelWidth();
                panelHeight = layout.panelHeight();
            }
        } else {
            layout = message.getWorldLayout(textRenderer);
            matrices.translate(-layout.maxTextWidth() / 2.0f, -layout.totalTextHeight() / 2.0f + fallOffsetY, 0f);
            panelWidth = layout.panelWidth();
            panelHeight = layout.panelHeight();
        }
        Matrix4f panelMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
        matrices.pop();
        return new Prepared(message, lod, panelMatrix, layout, panelWidth, panelHeight);
    }

    private void renderPanels(VertexConsumerProvider consumers) {
        if (!ModConfig.isSHOW_MESSAGE_BACKGROUND()) {
            return;
        }
        for (Identifier texture : MessagePanelWorldRenderer.layerTextures()) {
            for (Prepared entry : prepared) {
                Message message = entry.message();
                MessagePanelColors.bindEvent(message.getEventType());
                try {
                    if (entry.lod() == MessageRenderLod.Level.SIMPLE) {
                        MessagePanelWorldRenderer.drawSimplifiedPanelLayer(
                            entry.panelMatrix(),
                            consumers,
                            texture,
                            -MessagePanelConstants.PADDING_X,
                            -MessagePanelConstants.PADDING_Y,
                            entry.panelWidth(),
                            entry.panelHeight(),
                            0.9f,
                            MessagePanelColors.baseRgb(),
                            ModAuthor.borderRgb(message)
                        );
                    } else {
                        MessagePanelWorldRenderer.drawPanelLayer(
                            entry.panelMatrix(),
                            consumers,
                            texture,
                            -MessagePanelConstants.PADDING_X,
                            -MessagePanelConstants.PADDING_Y,
                            entry.panelWidth(),
                            entry.panelHeight(),
                            1.0f,
                            ModAuthor.borderRgb(message)
                        );
                    }
                } finally {
                    MessagePanelColors.unbindEvent();
                }
            }
        }
    }

    /** Текст рисуется сразу (общий атлас шрифта), иконки/эмоуты/пины копятся по текстурам. */
    private void renderTextAndCollectIcons(TextRenderer textRenderer, VertexConsumerProvider consumers) {
        int color = 0xFFFFFFFF;
        for (Prepared entry : prepared) {
            if (entry.lod() != MessageRenderLod.Level.FULL) {
                continue;
            }
            Message message = entry.message();
            MessagePanelLayout.Dimensions layout = entry.layout();
            Matrix4f textMatrix = new Matrix4f(entry.panelMatrix()).translate(0f, 0f, 0.1f);
            int iconOffset = layout.firstLineIconOffset();

            collectIconRow(textMatrix, message);
            if (message.hasInlineEmotes()) {
                List<EmoteTextLayout.LineContent> emoteLines = message.getEmoteLines(textRenderer);
                float lineY = 0.0f;
                int gifSize = layout.gifDisplaySize();
                boolean sawContent = false;
                for (int i = 0; i < emoteLines.size(); i++) {
                    EmoteTextLayout.LineContent line = emoteLines.get(i);
                    if (line.isGifLine()) {
                        if (sawContent) {
                            lineY += MessageEmote.GIF_LINE_GAP;
                        }
                        float gifX = (layout.maxTextWidth() - gifSize) / 2.0f;
                        renderLineWithEmotes(textMatrix, textRenderer, consumers, line.text(), line.emotes(), color, gifX, lineY, gifSize);
                        lineY += gifSize;
                    } else {
                        renderLineWithEmotes(
                            textMatrix,
                            textRenderer,
                            consumers,
                            line.text(),
                            line.emotes(),
                            color,
                            i == 0 ? iconOffset : 0.0f,
                            lineY,
                            0
                        );
                        lineY += line.hasEmotes() ? MessageEmote.maxIconSize(line.emotes()) + 1 : textRenderer.fontHeight;
                    }
                    sawContent = true;
                }
            } else {
                List<OrderedText> wrappedText = message.getWorldWrappedText(
                    () -> textRenderer.wrapLines(LegacySectionText.parse(message.getText()), MessagePanelLayout.wrapWidth())
                );
                for (int i = 0; i < wrappedText.size(); i++) {
                    textRenderer.draw(wrappedText.get(i),
                        i == 0 ? (float) iconOffset : 0.0F,
                        (float) i * textRenderer.fontHeight,
                        color,
                        true,
                        textMatrix,
                        consumers,
                        TextRenderer.TextLayerType.POLYGON_OFFSET,
                        0,
                        0xF000F0
                    );
                }
            }
            if (message.isPinned()) {
                int markerX = entry.panelWidth() - MessagePanelConstants.PADDING_X - (MessagePanelConstants.PIN_ICON_SIZE / 2) + MessagePanelConstants.PIN_ICON_MARGIN;
                int markerY = -MessagePanelConstants.PADDING_Y - (MessagePanelConstants.PIN_ICON_SIZE / 2) - MessagePanelConstants.PIN_ICON_MARGIN;
                addIcon(entityLayerIcons, MessagePanelConstants.PIN_TEXTURE, textMatrix,
                    markerX, markerY, markerX + MessagePanelConstants.PIN_ICON_SIZE, markerY + MessagePanelConstants.PIN_ICON_SIZE,
                    PIN_ICON_Z_OFFSET);
            }
        }
    }

    /** Иконка платформы и бейджи ролей перед ником. */
    private void collectIconRow(Matrix4f matrix, Message message) {
        int iconX = 0;
        String platformIconKey = message.getPlatformIconKey();
        if (platformIconKey != null) {
            Identifier texture = TwitchEmoteTextureCache.getTextureIdentifier("platform", platformIconKey);
            if (texture != null) {
                addIcon(textLayerIcons, texture, matrix,
                    iconX, 0, iconX + MessagePanelLayout.PLATFORM_ICON_SIZE, MessagePanelLayout.PLATFORM_ICON_SIZE,
                    EMOTE_ICON_Z_OFFSET);
            }
            iconX += MessagePanelLayout.PLATFORM_ICON_SIZE + MessagePanelLayout.EMOTE_ICON_SPACING;
        }
        for (String badgeKey : message.getRoleBadgeKeys()) {
            addIcon(textLayerIcons, RoleBadges.iconTexture(badgeKey), matrix,
                iconX, 0, iconX + RoleBadges.ICON_SIZE, RoleBadges.ICON_SIZE,
                EMOTE_ICON_Z_OFFSET);
            iconX += RoleBadges.ICON_SIZE + RoleBadges.WORLD_SPACING;
        }
    }

    private static void addIcon(
        Map<Identifier, List<IconQuad>> sink,
        Identifier texture,
        Matrix4f matrix,
        int x0,
        int y0,
        int x1,
        int y1,
        float z
    ) {
        sink.computeIfAbsent(texture, key -> new ArrayList<>()).add(new IconQuad(matrix, x0, y0, x1, y1, z));
    }

    private void flushIcons(VertexConsumerProvider consumers) {
        flushIconLayer(consumers, textLayerIcons, true);
        flushIconLayer(consumers, entityLayerIcons, false);
    }

    private static void flushIconLayer(VertexConsumerProvider consumers, Map<Identifier, List<IconQuad>> icons, boolean textLayer) {
        for (Map.Entry<Identifier, List<IconQuad>> group : icons.entrySet()) {
            List<IconQuad> quads = group.getValue();
            if (quads.isEmpty()) {
                continue;
            }
            VertexConsumer consumer = textLayer
                ? RenderLayerCompat.getTextBuffer(consumers, group.getKey())
                : RenderLayerCompat.getEntityBuffer(consumers, group.getKey());
            for (IconQuad q : quads) {
                drawQuadIcon(consumer, q.matrix(), q.x0(), q.y0(), q.x1(), q.y1(), q.z());
            }
            quads.clear();
        }
        // Ключи эмоутов копятся со временем — периодически сбрасываем пустые списки.
        if (icons.size() > MAX_ICON_TEXTURE_KEYS) {
            icons.clear();
        }
    }

    private static void drawQuadIcon(VertexConsumer consumer, Matrix4f mat, int x0, int y0, int x1, int y1, float z) {
        int light = 0xF000F0;
        int overlay = OverlayTexture.DEFAULT_UV;
        consumer.vertex(mat, x0, y0, z).color(1f, 1f, 1f, 1.0f).texture(0f, 0f).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(mat, x0, y1, z).color(1f, 1f, 1f, 1.0f).texture(0f, 1f).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(mat, x1, y1, z).color(1f, 1f, 1f, 1.0f).texture(1f, 1f).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(mat, x1, y0, z).color(1f, 1f, 1f, 1.0f).texture(1f, 0f).overlay(overlay).light(light).normal(0, 0, -1);
    }

    private void renderLineWithEmotes(Matrix4f matrix,
                                      TextRenderer textRenderer,
                                      VertexConsumerProvider consumers,
                                      String text,
                                      List<MessageEmote> emotes,
                                      int color,
                                      float startX,
                                      float y,
                                      int gifSize) {
        List<MessageEmote> sortedEmotes = MessageEmoteSorting.sortedValid(text, emotes);
        int cursor = 0;
        float x = startX;

        for (MessageEmote emote : sortedEmotes) {
            if (emote.getStartIndex() > cursor) {
                String segment = text.substring(cursor, emote.getStartIndex());
                drawString(textRenderer, consumers, matrix, segment, x, y, color);
                x += textRenderer.getWidth(segment);
            }

            Identifier emoteTexture = TwitchEmoteTextureCache.getTextureIdentifier(emote.getProvider(), emote.getEmoteId());
            int iconSize = emote.displaySize(gifSize);
            if (emoteTexture != null) {
                int iconX = Math.round(x);
                int iconY = Math.round(y) - 1;
                addIcon(textLayerIcons, emoteTexture, matrix, iconX, iconY, iconX + iconSize, iconY + iconSize, EMOTE_ICON_Z_OFFSET);
                x += iconSize + EMOTE_ICON_SPACING;
            } else if (!"emoji".equals(emote.getProvider())) {
                String code = emote.getEmoteCode();
                drawString(textRenderer, consumers, matrix, code, x, y, color);
                x += textRenderer.getWidth(code) + EMOTE_ICON_SPACING;
            } else {
                x += iconSize + EMOTE_ICON_SPACING;
            }
            cursor = emote.getEndIndex() + 1;
        }

        if (cursor < text.length()) {
            drawString(textRenderer, consumers, matrix, text.substring(cursor), x, y, color);
        }
    }

    private static void drawString(
        TextRenderer textRenderer,
        VertexConsumerProvider consumers,
        Matrix4f matrix,
        String text,
        float x,
        float y,
        int color
    ) {
        textRenderer.draw(text,
            x,
            y,
            color,
            true,
            matrix,
            consumers,
            TextRenderer.TextLayerType.POLYGON_OFFSET,
            0,
            0xF000F0
        );
    }
}
