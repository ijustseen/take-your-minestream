package takeyourminestream.modid.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.RenderLayer;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.modid.utils.CameraPositionCompat;
import takeyourminestream.modid.utils.RenderLayerCompat;

public class MessageParticleManager {
    private final List<MessageParticle> particles = new ArrayList<>();
    private static final Identifier PANEL_TEXTURE = Identifier.of("take-your-minestream", "textures/gui/message_panel.png");
    private static final Identifier PARTICLE_TEXTURE = Identifier.of("take-your-minestream", "textures/particles/particle_texture.png");

    public void addParticle(MessageParticle particle) {
        particles.add(particle);
    }

    public void addParticles(List<MessageParticle> newParticles) {
        particles.addAll(newParticles);
    }

    public void tick() {
        Iterator<MessageParticle> it = particles.iterator();
        while (it.hasNext()) {
            MessageParticle p = it.next();
            p.tick();
            if (!p.isAlive()) {
                it.remove();
            }
        }
    }

    public void render(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider consumers) {
        if (particles.isEmpty()) return;
        matrices.push();
        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);
        for (MessageParticle p : particles) {
            float lifeProgress = p.lifetimeTicks <= 0 ? 1.0f : (float)p.ageTicks / (float)p.lifetimeTicks;
            lifeProgress = Math.max(0.0f, Math.min(1.0f, lifeProgress));
            float alpha = 1.0f - (lifeProgress * lifeProgress);
            float fr = p.color.getRed() / 255.0f;
            float fg = p.color.getGreen() / 255.0f;
            float fb = p.color.getBlue() / 255.0f;
            // Переводим мировые координаты в локальные относительно камеры
            double x = p.position.x - cameraPos.getX();
            double y = p.position.y - cameraPos.getY();
            double z = p.position.z - cameraPos.getZ();
            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-p.yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.pitch));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));
            // Квадрат всегда смотрит в том же направлении, что и сообщение
            // Размер в блоках
            float sz = p.size * 0.025f;
            // Получаем матрицу
            Matrix4f mat = matrices.peek().getPositionMatrix();
            VertexConsumer consumer = consumers.getBuffer(RenderLayerCompat.getEntityTextureLayer(PARTICLE_TEXTURE));
            int light = 0xF000F0;
            int overlay = 0;
            consumer.vertex(mat, -sz/2, -sz/2, 0).color(fr, fg, fb, alpha).texture(0, 0).overlay(overlay).light(light).normal(0, 0, -1);
            consumer.vertex(mat, -sz/2,  sz/2, 0).color(fr, fg, fb, alpha).texture(0, 1).overlay(overlay).light(light).normal(0, 0, -1);
            consumer.vertex(mat,  sz/2,  sz/2, 0).color(fr, fg, fb, alpha).texture(1, 1).overlay(overlay).light(light).normal(0, 0, -1);
            consumer.vertex(mat,  sz/2, -sz/2, 0).color(fr, fg, fb, alpha).texture(1, 0).overlay(overlay).light(light).normal(0, 0, -1);
            matrices.pop();
        }
        matrices.pop();
    }

    public List<MessageParticle> getParticles() {
        return particles;
    }
} 