package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import net.minecraft.util.math.RotationAxis;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.RenderLayerCompat;
import takeyourminestream.ijustseen.core.render.MessagePanelWorldRenderer;

/**
 * Отвечает за рендеринг сообщений в мире
 */
public class MessageRenderer {
    private final MessageLifecycleManager lifecycleManager;
    private final MessageParticleManager particleManager;
    private final java.util.WeakHashMap<Message, SmoothingState> smoothing = new java.util.WeakHashMap<>();
    // Для диагностики: логируем рендер эмоута только один раз на ID
    private static final java.util.Set<String> EMOTE_RENDER_LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final float PIN_ICON_Z_OFFSET = -0.02f;
    private static final int EMOTE_ICON_SIZE = 12;
    private static final int EMOTE_ICON_SPACING = 1;
    private static final float EMOTE_ICON_Z_OFFSET = 0.02f;

    public MessageRenderer(MessageLifecycleManager lifecycleManager, MessageParticleManager particleManager) {
        this.lifecycleManager = lifecycleManager;
        this.particleManager = particleManager;
        
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            
            // Не рендерим 3D сообщения в HUD режиме
            var spawnMode = takeyourminestream.ijustseen.config.ModConfig.getMESSAGE_SPAWN_MODE();
            if (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.HUD_WIDGET) {
                return;
            }

            MatrixStack matrices = context.matrixStack();
            TextRenderer textRenderer = client.textRenderer;
            VertexConsumerProvider consumers = context.consumers();

            List<Message> activeMessages = lifecycleManager.getActiveMessages();

            for (Message message : activeMessages) {
                renderMessage(client, message, matrices, textRenderer, consumers);
            }
            // Рендер партиклов
            if (particleManager != null) {
                particleManager.render(client, matrices, consumers);
            }
        });
    }
    
    /**
     * Рендерит одно сообщение
     */
    private void renderMessage(MinecraftClient client, Message message, MatrixStack matrices, 
                             TextRenderer textRenderer, VertexConsumerProvider consumers) {
        matrices.push();

        int tickCounter = lifecycleManager.getTickCounter();
        int age = message.isPinned() ? 0 : message.getEffectiveAge(tickCounter);
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int fallStart = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallAge = age - fallStart;
        float fallOffsetY = 0.0f;
        boolean isFalling = false;
        if (fallAge >= 0 && fallAge < fallTicks) {
            float fallProgress = (float)fallAge / (float)fallTicks;
            fallProgress = Math.min(Math.max(fallProgress, 0.0f), 1.0f);
            float maxFall = 20.0f;
            fallOffsetY = (fallProgress * fallProgress) * maxFall;
            isFalling = true;
        }
        if (fallAge >= fallTicks) {
            // Сообщение уже "разбилось" и не должно отображаться
            matrices.pop();
            return;
        }

        // Сверхплавная интерполяция на стороне рендера (кадровая)
        SmoothingState state = smoothing.computeIfAbsent(message, m -> SmoothingState.fromMessage(m));
        state.updateTowards(message);

        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);

        matrices.translate(
            state.pos.x - cameraPos.getX(),
            state.pos.y - cameraPos.getY(),
            state.pos.z - cameraPos.getZ()
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch));
        
        // Применяем масштаб из конфигурации
        float baseScale = 0.025f;
        float configScale = takeyourminestream.ijustseen.config.ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = baseScale * configScale;
        matrices.scale(finalScale, -finalScale, finalScale);

        boolean hasEmotes = !message.getEmotes().isEmpty();
        List<OrderedText> wrappedText = hasEmotes
            ? java.util.Collections.emptyList()
            : textRenderer.wrapLines(Text.of(message.getText()), MessagePanelConstants.MESSAGE_WRAP_WIDTH);
        float totalTextHeight = hasEmotes ? textRenderer.fontHeight : wrappedText.size() * textRenderer.fontHeight;
        int maxTextWidth = hasEmotes ? getEmoteAwareLineWidth(textRenderer, message.getText(), message.getEmotes()) : 0;
        if (!hasEmotes) {
            for (OrderedText line : wrappedText) {
                int w = textRenderer.getWidth(line);
                if (w > maxTextWidth) maxTextWidth = w;
            }
        }
        int panelWidth = maxTextWidth + MessagePanelConstants.PADDING_X * 2;
        int panelHeight = (int)totalTextHeight + MessagePanelConstants.PADDING_Y * 2;
        // Центрируем текст и панель + применяем падение
        matrices.translate(-maxTextWidth / 2.0f, -totalTextHeight / 2.0f + fallOffsetY, 0f);
        // Рендерим панель (по флагу)
        if (ModConfig.isSHOW_MESSAGE_BACKGROUND()) {
            MessagePanelWorldRenderer.drawPanel(
                matrices,
                MessagePanelWorldRenderer.panelConsumer(consumers),
                -MessagePanelConstants.PADDING_X,
                -MessagePanelConstants.PADDING_Y,
                panelWidth,
                panelHeight,
                1.0f,
                1.0f,
                1.0f,
                1.0f
            );
        }
        matrices.translate(0f, 0f, 0.1f);
        // Рендерим текст
        int alphaInt = 0xFF << 24;
        int color = (0xFFFFFF) | alphaInt;
        if (hasEmotes) {
            renderLineWithEmotes(matrices, textRenderer, consumers, message.getText(), message.getEmotes(), color);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                textRenderer.draw(wrappedText.get(i),
                                0.0F,
                                (float)i * textRenderer.fontHeight,
                                color,
                                true,
                                matrices.peek().getPositionMatrix(),
                                consumers,
                                TextRenderer.TextLayerType.POLYGON_OFFSET,
                                0,
                                0xF000F0
                                );
            }
        }
        if (message.isPinned()) {
            renderPinIcon(matrices, panelWidth, consumers);
        }
        matrices.pop();
    }

    // Состояние сглаживания для кадро-зависимой интерполяции
    private static class SmoothingState {
        Vec3d pos;
        float yaw;
        float pitch;
        long lastNs;

        // Константы времён сглаживания (секунды)
        private static final double TAU_POS = 0.15;   // чем больше, тем медленнее
        private static final double TAU_ANG = 0.12;

        static SmoothingState fromMessage(Message m) {
            SmoothingState s = new SmoothingState();
            s.pos = m.getPosition();
            s.yaw = m.getYaw();
            s.pitch = m.getPitch();
            s.lastNs = System.nanoTime();
            return s;
        }

        void updateTowards(Message target) {
            long now = System.nanoTime();
            double dt = Math.max(0.0, (now - lastNs) / 1_000_000_000.0);
            lastNs = now;

            // Экспоненциальное сглаживание к цели
            double alphaPos = 1.0 - Math.exp(-dt / TAU_POS);
            double alphaAng = 1.0 - Math.exp(-dt / TAU_ANG);

            Vec3d tp = target.getPosition();
            this.pos = new Vec3d(
                lerp(this.pos.x, tp.x, alphaPos),
                lerp(this.pos.y, tp.y, alphaPos),
                lerp(this.pos.z, tp.z, alphaPos)
            );

            this.yaw = lerpAngleDeg(this.yaw, target.getYaw(), (float)alphaAng);
            this.pitch = lerpAngleDeg(this.pitch, target.getPitch(), (float)alphaAng);
        }

        private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
        private static float lerpAngleDeg(float a, float b, float t) {
            float delta = net.minecraft.util.math.MathHelper.wrapDegrees(b - a);
            return a + delta * t;
        }
    }
    
    private void renderPinIcon(MatrixStack matrices, int panelWidth, VertexConsumerProvider consumers) {
        VertexConsumer consumer = consumers.getBuffer(RenderLayerCompat.getEntityTextureLayer(MessagePanelConstants.PIN_TEXTURE));
        Matrix4f mat = matrices.peek().getPositionMatrix();

        int markerX = panelWidth - MessagePanelConstants.PADDING_X - (MessagePanelConstants.PIN_ICON_SIZE / 2) + MessagePanelConstants.PIN_ICON_MARGIN;
        int markerY = -MessagePanelConstants.PADDING_Y - (MessagePanelConstants.PIN_ICON_SIZE / 2) - MessagePanelConstants.PIN_ICON_MARGIN;
        float u0 = 0f;
        float v0 = 0f;
        float u1 = 1f;
        float v1 = 1f;
        drawQuadIcon(consumer, mat, markerX, markerY, markerX + MessagePanelConstants.PIN_ICON_SIZE, markerY + MessagePanelConstants.PIN_ICON_SIZE, PIN_ICON_Z_OFFSET, u0, v0, u1, v1);
    }

    private void drawQuadIcon(VertexConsumer consumer, Matrix4f mat, int x0, int y0, int x1, int y1, float z, float u0, float v0, float u1, float v1) {
        int light = 0xF000F0;
        int overlay = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        consumer.vertex(mat, x0, y0, z).color(1f, 1f, 1f, 1.0f).texture(u0, v0).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(mat, x0, y1, z).color(1f, 1f, 1f, 1.0f).texture(u0, v1).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(mat, x1, y1, z).color(1f, 1f, 1f, 1.0f).texture(u1, v1).overlay(overlay).light(light).normal(0, 0, -1);
        consumer.vertex(mat, x1, y0, z).color(1f, 1f, 1f, 1.0f).texture(u1, v0).overlay(overlay).light(light).normal(0, 0, -1);
    }

    private int getEmoteAwareLineWidth(TextRenderer textRenderer, String text, List<MessageEmote> emotes) {
        int width = 0;
        int cursor = 0;
        for (MessageEmote emote : getSortedValidEmotes(text, emotes)) {
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

    private void renderLineWithEmotes(MatrixStack matrices,
                                      TextRenderer textRenderer,
                                      VertexConsumerProvider consumers,
                                      String text,
                                      List<MessageEmote> emotes,
                                      int color) {
        List<MessageEmote> sortedEmotes = getSortedValidEmotes(text, emotes);
        int cursor = 0;
        float x = 0.0f;

        for (MessageEmote emote : sortedEmotes) {
            if (emote.getStartIndex() > cursor) {
                String segment = text.substring(cursor, emote.getStartIndex());
                textRenderer.draw(segment,
                    x,
                    0.0F,
                    color,
                    true,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    0xF000F0
                );
                x += textRenderer.getWidth(segment);
            }

            Identifier emoteTexture = TwitchEmoteTextureCache.getTextureIdentifier(emote.getProvider(), emote.getEmoteId());
            if (emoteTexture != null) {
                if (EMOTE_RENDER_LOGGED.add("draw:" + emote.getEmoteId())) {
                    System.out.println("[TYMS-Emote-Render] Drawing emote quad id=" + emote.getEmoteId() + " tex=" + emoteTexture + " x=" + x);
                }
                VertexConsumer consumer = consumers.getBuffer(RenderLayerCompat.getTextLayer(emoteTexture));
                Matrix4f mat = matrices.peek().getPositionMatrix();
                int iconX = Math.round(x);
                int iconY = -1;
                drawQuadIcon(consumer,
                    mat,
                    iconX,
                    iconY,
                    iconX + EMOTE_ICON_SIZE,
                    iconY + EMOTE_ICON_SIZE,
                    EMOTE_ICON_Z_OFFSET,
                    0f,
                    0f,
                    1f,
                    1f
                );
                x += EMOTE_ICON_SIZE + EMOTE_ICON_SPACING;
            } else {
                if (EMOTE_RENDER_LOGGED.add("fallback:" + emote.getEmoteId())) {
                    System.out.println("[TYMS-Emote-Render] Emote " + emote.getEmoteId() + " not loaded yet, showing fallback text");
                }
                // Fallback: show emote code as text while texture is loading/failed
                String code = emote.getEmoteCode();
                textRenderer.draw(code,
                    x,
                    0.0F,
                    color,
                    true,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    0xF000F0
                );
                x += textRenderer.getWidth(code) + EMOTE_ICON_SPACING;
            }
            cursor = emote.getEndIndex() + 1;
        }

        if (cursor < text.length()) {
            String tail = text.substring(cursor);
            textRenderer.draw(tail,
                x,
                0.0F,
                color,
                true,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.POLYGON_OFFSET,
                0,
                0xF000F0
            );
        }
    }

    private List<MessageEmote> getSortedValidEmotes(String text, List<MessageEmote> emotes) {
        if (emotes == null || emotes.isEmpty()) {
            return java.util.Collections.emptyList();
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