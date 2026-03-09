package takeyourminestream.ijustseen.messages;

import net.minecraft.util.math.Vec3d;
import java.awt.Color;

public class MessageParticle {
    public Vec3d position;
    public Vec3d velocity;
    public Color color;
    public float size;
    public int lifetimeTicks;
    public int ageTicks;
    public ParticleType type;
    public float rotation;
    public float rotationSpeed;
    public float yaw;
    public float pitch;

    public enum ParticleType {
        TEXT_COLOR,
        BACKGROUND_COLOR
    }

    public MessageParticle(Vec3d position, Vec3d velocity, Color color, float size, int lifetimeTicks, ParticleType type, float rotation, float rotationSpeed, float yaw, float pitch) {
        this.position = position;
        this.velocity = velocity;
        this.color = color;
        this.size = size;
        this.lifetimeTicks = lifetimeTicks;
        this.ageTicks = 0;
        this.type = type;
        this.rotation = rotation;
        this.rotationSpeed = rotationSpeed;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public boolean isAlive() {
        return ageTicks < lifetimeTicks;
    }

    public void tick() {
        position = position.add(velocity);
        velocity = new Vec3d(velocity.x * 0.96, velocity.y * 0.93 - 0.006, velocity.z * 0.96);
        rotation += rotationSpeed;
        ageTicks++;
    }
} 