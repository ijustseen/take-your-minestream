package takeyourminestream.modid.messages;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import takeyourminestream.modid.ModConfig;
import net.minecraft.util.math.RotationAxis;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.math.Vec3d;

/**
 * Отвечает за рендеринг сообщений в мире
 */
public class MessageRenderer {
    private final MessageLifecycleManager lifecycleManager;
    private final MessageParticleManager particleManager;
    private final java.util.WeakHashMap<Message, SmoothingState> smoothing = new java.util.WeakHashMap<>();
    
    private static final Identifier PANEL_TEXTURE = Identifier.of("take-your-minestream", "textures/gui/message_panel.png");
    private static final int PANEL_TEX_SIZE = 32;
    private static final int PANEL_CORNER = 8;
    private static final int PANEL_MIN = 2 * PANEL_CORNER;
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 4;

    public MessageRenderer(MessageLifecycleManager lifecycleManager, MessageParticleManager particleManager) {
        this.lifecycleManager = lifecycleManager;
        this.particleManager = particleManager;
        
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            
            // Не рендерим 3D сообщения в HUD режиме
            var spawnMode = takeyourminestream.modid.ModConfig.getMESSAGE_SPAWN_MODE();
            if (spawnMode == takeyourminestream.modid.config.MessageSpawnMode.HUD_WIDGET) {
                return;
            }

            MatrixStack matrices = context.matrices();
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
        int age = message.getEffectiveAge(tickCounter);
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

        matrices.translate(
            state.pos.x - client.gameRenderer.getCamera().getPos().getX(),
            state.pos.y - client.gameRenderer.getCamera().getPos().getY(),
            state.pos.z - client.gameRenderer.getCamera().getPos().getZ()
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch));
        
        // Применяем масштаб из конфигурации
        float baseScale = 0.025f;
        float configScale = takeyourminestream.modid.ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = baseScale * configScale;
        matrices.scale(finalScale, -finalScale, finalScale);

        List<OrderedText> wrappedText = textRenderer.wrapLines(Text.of(message.getText()), 120);
        float totalTextHeight = wrappedText.size() * textRenderer.fontHeight;
        // Найти максимальную ширину среди всех строк
        int maxTextWidth = 0;
        for (OrderedText line : wrappedText) {
            int w = textRenderer.getWidth(line);
            if (w > maxTextWidth) maxTextWidth = w;
        }
        int panelWidth = maxTextWidth + PANEL_PADDING_X * 2;
        int panelHeight = (int)totalTextHeight + PANEL_PADDING_Y * 2;
        // Центрируем текст и панель + применяем падение
        matrices.translate(-maxTextWidth / 2.0f, -totalTextHeight / 2.0f + fallOffsetY, 0);
        // Рендерим панель (по флагу)
        if (ModConfig.isSHOW_MESSAGE_BACKGROUND()) {
            renderPanel9Slice(matrices, -PANEL_PADDING_X, -PANEL_PADDING_Y, panelWidth, panelHeight, 1.0f, consumers);
        }
        // Рендерим текст
        for (int i = 0; i < wrappedText.size(); i++) {
            int alphaInt = 0xFF << 24;
            int color = (0xFFFFFF) | alphaInt;
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
    
    /**
     * Рендерит 9-slice панель
     */
    private void renderPanel9Slice(MatrixStack matrices, int x, int y, int width, int height, float alpha, VertexConsumerProvider consumers) {
        if (width < PANEL_MIN) width = PANEL_MIN;
        if (height < PANEL_MIN) height = PANEL_MIN;
        VertexConsumer consumer = consumers.getBuffer(net.minecraft.client.render.RenderLayer.getEntityTranslucent(PANEL_TEXTURE));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        int x0 = x;
        int x1 = x + PANEL_CORNER;
        int x2 = x + width - PANEL_CORNER;
        int x3 = x + width;
        int y0 = y;
        int y1 = y + PANEL_CORNER;
        int y2 = y + height - PANEL_CORNER;
        int y3 = y + height;
        float u0 = 0f;
        float u1 = 8f / (float)PANEL_TEX_SIZE;
        float u2 = 24f / (float)PANEL_TEX_SIZE;
        float u3 = 1f;
        float v0 = 0f;
        float v1 = 8f / (float)PANEL_TEX_SIZE;
        float v2 = 24f / (float)PANEL_TEX_SIZE;
        float v3 = 1f;
        // 1. Углы
        drawQuadGL(consumer, mat, x0, y0, x1, y1, u0, v0, u1, v1, alpha); // левый верх
        drawQuadGL(consumer, mat, x2, y0, x3, y1, u2, v0, u3, v1, alpha); // правый верх
        drawQuadGL(consumer, mat, x0, y2, x1, y3, u0, v2, u1, v3, alpha); // левый низ
        drawQuadGL(consumer, mat, x2, y2, x3, y3, u2, v2, u3, v3, alpha); // правый низ
        // 2. Края
        drawQuadGL(consumer, mat, x1, y0, x2, y1, u1, v0, u2, v1, alpha); // верх
        drawQuadGL(consumer, mat, x1, y2, x2, y3, u1, v2, u2, v3, alpha); // низ
        drawQuadGL(consumer, mat, x0, y1, x1, y2, u0, v1, u1, v2, alpha); // левый
        drawQuadGL(consumer, mat, x2, y1, x3, y2, u2, v1, u3, v2, alpha); // правый
        // 3. Центр
        drawQuadGL(consumer, mat, x1, y1, x2, y2, u1, v1, u2, v2, alpha); // центр
    }
    /**
     * Рисует один quad
     */
    private void drawQuadGL(VertexConsumer consumer, Matrix4f mat, int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1, float alpha) {
        int light = 0xF000F0;
        int overlay = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        // Левый верх
        consumer.vertex(mat, x0, y0, 0)
                .color(1f, 1f, 1f, alpha)
                .texture(u0, v0)
                .overlay(overlay)
                .light(light)
                .normal(0, 0, -1);
        // Левый низ
        consumer.vertex(mat, x0, y1, 0)
                .color(1f, 1f, 1f, alpha)
                .texture(u0, v1)
                .overlay(overlay)
                .light(light)
                .normal(0, 0, -1);
        // Правый низ
        consumer.vertex(mat, x1, y1, 0)
                .color(1f, 1f, 1f, alpha)
                .texture(u1, v1)
                .overlay(overlay)
                .light(light)
                .normal(0, 0, -1);
        // Правый верх
        consumer.vertex(mat, x1, y0, 0)
                .color(1f, 1f, 1f, alpha)
                .texture(u1, v0)
                .overlay(overlay)
                .light(light)
                .normal(0, 0, -1);
    }
} 