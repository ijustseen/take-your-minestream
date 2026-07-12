package takeyourminestream.ijustseen.messages;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.render.MessagePanelWorldRenderer;
import takeyourminestream.ijustseen.core.text.LegacySectionText;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.SubmitGeometryHelper;

import java.util.List;

/** Отвечает за рендеринг сообщений в мире (Minecraft 26.2). */
public class MessageRenderer {
    private final MessageLifecycleManager lifecycleManager;
    private final MessageParticleManager particleManager;
    private final java.util.WeakHashMap<Message, MessageRenderSmoothing> smoothing = new java.util.WeakHashMap<>();

    private static final float PIN_ICON_Z_OFFSET = -0.02f;
    private static final int EMOTE_ICON_SIZE = 12;
    private static final int EMOTE_ICON_SPACING = 1;
    private static final float EMOTE_ICON_Z_OFFSET = 0.02f;

    public MessageRenderer(MessageLifecycleManager lifecycleManager, MessageParticleManager particleManager) {
        this.lifecycleManager = lifecycleManager;
        this.particleManager = particleManager;

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
                return;
            }

            var spawnMode = ModConfig.getMESSAGE_SPAWN_MODE();
            if (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.HUD_WIDGET) {
                return;
            }

            PoseStack poseStack = context.poseStack();
            Font textRenderer = client.font;
            SubmitNodeCollector collector = context.submitNodeCollector();

            for (Message message : lifecycleManager.getActiveMessages()) {
                if (!PinnedMessageStore.belongsToCurrentWorld(message, client)) {
                    continue;
                }
                renderMessage(client, message, poseStack, textRenderer, collector);
            }
            if (particleManager != null) {
                particleManager.render(client, poseStack, collector);
            }
        });
    }

    private void renderMessage(
        Minecraft client,
        Message message,
        PoseStack poseStack,
        Font textRenderer,
        SubmitNodeCollector collector
    ) {
        poseStack.pushPose();

        int tickCounter = lifecycleManager.getTickCounter();
        int age = message.isPinned() ? 0 : message.getEffectiveAge(tickCounter);
        float fallOffsetY = MessagePanelLayout.fallOffsetY(age);
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int fallStart = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallAge = age - fallStart;
        if (fallAge >= fallTicks) {
            poseStack.popPose();
            return;
        }

        MessageRenderSmoothing state = smoothing.computeIfAbsent(message, MessageRenderSmoothing::fromMessage);
        state.updateTowards(message);

        var cameraPos = CameraPositionCompat.getCameraPos(client);
        poseStack.translate(
            state.pos().x - cameraPos.x,
            state.pos().y - cameraPos.y,
            state.pos().z - cameraPos.z
        );
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch()));

        float finalScale = MessagePanelLayout.worldScale();
        poseStack.scale(finalScale, -finalScale, finalScale);

        MessagePanelLayout.Dimensions layout = message.getWorldLayout(textRenderer);
        int maxTextWidth = layout.maxTextWidth();
        float totalTextHeight = layout.totalTextHeight();
        int panelWidth = layout.panelWidth();
        int panelHeight = layout.panelHeight();
        String platformIconKey = message.getPlatformIconKey();
        int iconOffset = layout.firstLineIconOffset();

        boolean hasEmotes = message.hasInlineEmotes();
        List<EmoteTextLayout.LineContent> emoteLines = hasEmotes
            ? message.getEmoteLines(textRenderer)
            : java.util.Collections.emptyList();
        List<FormattedCharSequence> wrappedText = hasEmotes
            ? java.util.Collections.emptyList()
            : textRenderer.split(LegacySectionText.parse(message.getText()), MessagePanelConstants.MESSAGE_WRAP_WIDTH);

        poseStack.translate(-maxTextWidth / 2.0f, -totalTextHeight / 2.0f + fallOffsetY, 0f);
        if (ModConfig.isSHOW_MESSAGE_BACKGROUND()) {
            MessagePanelWorldRenderer.drawPanelWithBorder(
                poseStack,
                collector,
                -MessagePanelConstants.PADDING_X,
                -MessagePanelConstants.PADDING_Y,
                panelWidth,
                panelHeight,
                1.0f,
                takeyourminestream.ijustseen.integration.chat.ChatPlatform.accentColorForIconKey(platformIconKey)
            );
        }
        poseStack.translate(0f, 0f, 0.1f);

        int color = 0xFFFFFFFF;
        if (platformIconKey != null) {
            renderPlatformIcon(poseStack, collector, platformIconKey);
        }
        if (hasEmotes) {
            float lineY = 0.0f;
            for (int i = 0; i < emoteLines.size(); i++) {
                EmoteTextLayout.LineContent line = emoteLines.get(i);
                renderLineWithEmotes(
                    poseStack,
                    textRenderer,
                    collector,
                    line.text(),
                    line.emotes(),
                    color,
                    i == 0 ? iconOffset : 0.0f,
                    lineY
                );
                lineY += line.hasEmotes() ? MessagePanelLayout.EMOTE_ICON_SIZE + 1 : textRenderer.lineHeight;
            }
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                SubmitGeometryHelper.submitText(
                    collector,
                    poseStack,
                    wrappedText.get(i),
                    i == 0 ? iconOffset : 0.0F,
                    i * textRenderer.lineHeight,
                    color
                );
            }
        }
        if (message.isPinned()) {
            renderPinIcon(poseStack, collector, panelWidth);
        }
        poseStack.popPose();
    }

    private void renderPinIcon(PoseStack poseStack, SubmitNodeCollector collector, int panelWidth) {
        int markerX = panelWidth - MessagePanelConstants.PADDING_X - (MessagePanelConstants.PIN_ICON_SIZE / 2) + MessagePanelConstants.PIN_ICON_MARGIN;
        int markerY = -MessagePanelConstants.PADDING_Y - (MessagePanelConstants.PIN_ICON_SIZE / 2) - MessagePanelConstants.PIN_ICON_MARGIN;
        SubmitGeometryHelper.submitTexturedQuad(
            collector,
            poseStack,
            MessagePanelConstants.PIN_TEXTURE,
            false,
            markerX,
            markerY,
            markerX + MessagePanelConstants.PIN_ICON_SIZE,
            markerY + MessagePanelConstants.PIN_ICON_SIZE,
            PIN_ICON_Z_OFFSET,
            0f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f
        );
    }

    private void renderPlatformIcon(PoseStack poseStack, SubmitNodeCollector collector, String iconKey) {
        Identifier texture = TwitchEmoteTextureCache.getTextureIdentifier("platform", iconKey);
        if (texture == null) {
            return;
        }
        SubmitGeometryHelper.submitTexturedQuad(
            collector,
            poseStack,
            texture,
            true,
            0,
            0,
            MessagePanelLayout.PLATFORM_ICON_SIZE,
            MessagePanelLayout.PLATFORM_ICON_SIZE,
            EMOTE_ICON_Z_OFFSET,
            0f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f
        );
    }

    private void renderLineWithEmotes(
        PoseStack poseStack,
        Font textRenderer,
        SubmitNodeCollector collector,
        String text,
        List<MessageEmote> emotes,
        int color,
        float startX,
        float y
    ) {
        List<MessageEmote> sortedEmotes = MessageEmoteSorting.sortedValid(text, emotes);
        int cursor = 0;
        float x = startX;

        for (MessageEmote emote : sortedEmotes) {
            if (emote.getStartIndex() > cursor) {
                String segment = text.substring(cursor, emote.getStartIndex());
                SubmitGeometryHelper.submitText(collector, poseStack, segment, x, y, color);
                x += textRenderer.width(segment);
            }

            Identifier emoteTexture = TwitchEmoteTextureCache.getTextureIdentifier(emote.getProvider(), emote.getEmoteId());
            if (emoteTexture != null) {
                int iconX = Math.round(x);
                int iconY = Math.round(y) - 1;
                SubmitGeometryHelper.submitTexturedQuad(
                    collector,
                    poseStack,
                    emoteTexture,
                    true,
                    iconX,
                    iconY,
                    iconX + EMOTE_ICON_SIZE,
                    iconY + EMOTE_ICON_SIZE,
                    EMOTE_ICON_Z_OFFSET,
                    0f,
                    0f,
                    1f,
                    1f,
                    1f,
                    1f,
                    1f,
                    1f
                );
                x += EMOTE_ICON_SIZE + EMOTE_ICON_SPACING;
            } else if (!"emoji".equals(emote.getProvider())) {
                String code = emote.getEmoteCode();
                SubmitGeometryHelper.submitText(collector, poseStack, code, x, y, color);
                x += textRenderer.width(code) + EMOTE_ICON_SPACING;
            } else {
                x += EMOTE_ICON_SIZE + EMOTE_ICON_SPACING;
            }
            cursor = emote.getEndIndex() + 1;
        }

        if (cursor < text.length()) {
            SubmitGeometryHelper.submitText(collector, poseStack, text.substring(cursor), x, y, color);
        }
    }
}
