package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MessageParticleSpawner {
    public static void spawnParticlesForMessage(
        Message message,
        MessageParticleManager manager,
        MinecraftClient client,
        Vec3d panelCenter,
        int effectiveAge
    ) {
        if (client == null || client.textRenderer == null || manager == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        MessagePanelLayout.Dimensions layout = message.getWorldLayout(textRenderer);
        int panelWidth = layout.panelWidth();
        int panelHeight = layout.panelHeight();
        if (panelWidth <= 0 || panelHeight <= 0) {
            return;
        }

        Random random = new Random();
        int min = ModConfig.getPARTICLE_MIN_COUNT();
        int max = ModConfig.getPARTICLE_MAX_COUNT();
        if (max < min) {
            max = min;
        }
        int count = min + random.nextInt(max - min + 1);
        int lifetime = Math.max(10, ModConfig.getPARTICLE_LIFETIME_TICKS());

        float yaw = message.getYaw();
        float pitch = message.getPitch();
        float worldScale = MessagePanelLayout.worldScale();
        float fallOffsetY = MessagePanelLayout.fallOffsetY(effectiveAge);

        int gridX = Math.max(1, (int) Math.ceil(Math.sqrt(count)));
        int gridY = Math.max(1, (int) Math.ceil((double) count / (double) gridX));

        List<MessageParticle> particles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int cellX = i % gridX;
            int cellY = i / gridX;
            float cellW = (float) panelWidth / (float) gridX;
            float cellH = (float) panelHeight / (float) gridY;
            float localX = -panelWidth / 2f + (cellX + random.nextFloat()) * cellW;
            float localY = -panelHeight / 2f + fallOffsetY + (cellY + random.nextFloat()) * cellH;

            Vec3d worldOffset = panelLocalToWorldOffset(localX, localY, 0.0, yaw, pitch, worldScale);
            Vec3d world = panelCenter.add(worldOffset);

            double localVx = (random.nextDouble() - 0.5) * 0.18;
            double localVy = (random.nextDouble() - 0.5) * 0.18 - 0.04;
            double localVz = (random.nextDouble() - 0.5) * 0.12 + 0.06;
            Vec3d velocity = panelLocalToWorldOffset(localVx, localVy, localVz, yaw, pitch, 1.0f);

            float particleSize = 4.0f + random.nextFloat() * 5.0f;
            float rotation = random.nextFloat() * 360f;
            float rotationSpeed = (random.nextFloat() - 0.5f) * 10f;

            MessageParticle.ParticleType type = (i % 2 == 0)
                ? MessageParticle.ParticleType.TEXT_COLOR
                : MessageParticle.ParticleType.BACKGROUND_COLOR;
            Color color = type == MessageParticle.ParticleType.TEXT_COLOR
                ? getTextColor(message)
                : getPanelColor(message);

            particles.add(new MessageParticle(
                world, velocity, color, particleSize, lifetime, type,
                rotation, rotationSpeed, yaw, pitch
            ));
        }
        manager.addParticles(particles);
    }

    /** Локальные координаты панели → мировое смещение (как в {@link MessageRenderer}). */
    static Vec3d panelLocalToWorldOffset(
        double localX,
        double localY,
        double localZ,
        float yaw,
        float pitch,
        float worldScale
    ) {
        double sx = localX * worldScale;
        double sy = -localY * worldScale;
        double sz = localZ * worldScale;

        double pitchRad = Math.toRadians(pitch);
        double cosP = Math.cos(pitchRad);
        double sinP = Math.sin(pitchRad);
        double y1 = sy * cosP - sz * sinP;
        double z1 = sy * sinP + sz * cosP;

        double yawRad = Math.toRadians(-yaw);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double x2 = sx * cosY + z1 * sinY;
        double z2 = -sx * sinY + z1 * cosY;
        return new Vec3d(x2, y1, z2);
    }

    private static Color getTextColor(Message message) {
        Integer rgb = message.getAuthorColorRgb();
        if (rgb != null) {
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        return new Color(255, 255, 255);
    }

    private static Color getPanelColor(Message message) {
        int rgb = ChatPlatform.accentColorForIconKey(message.getPlatformIconKey());
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 180);
    }
}
