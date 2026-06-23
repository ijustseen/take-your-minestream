package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.RenderLayerCompat;

public class MessageParticleManager {
    private final List<MessageParticle> particles = new ArrayList<>();
    private static final Identifier PARTICLE_TEXTURE =
        Identifier.of("take-your-stream-chat", "textures/particles/particle_texture.png");

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
        if (particles.isEmpty()) {
            return;
        }

        float worldScale = MessagePanelLayout.worldScale();
        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);

        for (MessageParticle p : particles) {
            float lifeProgress = p.lifetimeTicks <= 0
                ? 1.0f
                : (float) p.ageTicks / (float) p.lifetimeTicks;
            lifeProgress = Math.max(0.0f, Math.min(1.0f, lifeProgress));
            float alpha = 1.0f - lifeProgress * lifeProgress;
            if (alpha <= 0.01f) {
                continue;
            }

            float fr = p.color.getRed() / 255.0f;
            float fg = p.color.getGreen() / 255.0f;
            float fb = p.color.getBlue() / 255.0f;
            float fa = (p.color.getAlpha() / 255.0f) * alpha;

            matrices.push();
            matrices.translate(
                p.position.x - cameraPos.getX(),
                p.position.y - cameraPos.getY(),
                p.position.z - cameraPos.getZ()
            );
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-p.yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.pitch));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotation));

            float sz = p.size * worldScale;
            Matrix4f mat = matrices.peek().getPositionMatrix();
            VertexConsumer consumer = RenderLayerCompat.getEntityBuffer(consumers, PARTICLE_TEXTURE);
            int light = 0xF000F0;
            int overlay = 0;
            float half = sz / 2.0f;
            float z = 0.02f;
            consumer.vertex(mat, -half, -half, z).color(fr, fg, fb, fa).texture(0, 0).overlay(overlay).light(light).normal(0, 0, -1);
            consumer.vertex(mat, -half,  half, z).color(fr, fg, fb, fa).texture(0, 1).overlay(overlay).light(light).normal(0, 0, -1);
            consumer.vertex(mat,  half,  half, z).color(fr, fg, fb, fa).texture(1, 1).overlay(overlay).light(light).normal(0, 0, -1);
            consumer.vertex(mat,  half, -half, z).color(fr, fg, fb, fa).texture(1, 0).overlay(overlay).light(light).normal(0, 0, -1);
            matrices.pop();
        }
    }

    public List<MessageParticle> getParticles() {
        return particles;
    }
}
