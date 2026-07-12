package takeyourminestream.ijustseen.messages;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;
import takeyourminestream.ijustseen.utils.SubmitGeometryHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Партиклы при удалении сообщений (Minecraft 26.2). */
public class MessageParticleManager {
    private final List<MessageParticle> particles = new ArrayList<>();
    private static final Identifier PARTICLE_TEXTURE =
        Identifier.fromNamespaceAndPath("take-your-stream-chat", "textures/particles/particle_texture.png");

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

    public void render(Minecraft client, PoseStack poseStack, SubmitNodeCollector collector) {
        if (particles.isEmpty()) {
            return;
        }

        float worldScale = MessagePanelLayout.worldScale();
        Vec3 cameraPos = CameraPositionCompat.getCameraPos(client);

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

            poseStack.pushPose();
            poseStack.translate(
                p.position.x - cameraPos.x,
                p.position.y - cameraPos.y,
                p.position.z - cameraPos.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(-p.yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(p.pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(p.rotation));

            float sz = p.size * worldScale;
            float half = sz / 2.0f;
            SubmitGeometryHelper.submitTexturedQuad(
                collector,
                poseStack,
                PARTICLE_TEXTURE,
                false,
                -half,
                -half,
                half,
                half,
                0.02f,
                0f,
                0f,
                1f,
                1f,
                fr,
                fg,
                fb,
                fa
            );
            poseStack.popPose();
        }
    }

    public List<MessageParticle> getParticles() {
        return particles;
    }
}
