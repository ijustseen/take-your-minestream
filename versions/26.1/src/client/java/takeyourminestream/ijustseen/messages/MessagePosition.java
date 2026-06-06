package takeyourminestream.ijustseen.messages;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Позиционирование сообщений в мире (Minecraft 26.1). */
public class MessagePosition {
    private MessagePosition() {}

    public static Vec3 generateRandomPosition(Minecraft client) {
        if (client.player == null || client.level == null) {
            return Vec3.ZERO;
        }

        RandomSource random = client.player.getRandom();
        Vec3 playerPos = client.player.getEyePosition();

        for (int i = 0; i < 15; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 2 + (5 - 2) * random.nextDouble();
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            double yOffset = random.nextDouble() * 2 - 1;

            Vec3 point = new Vec3(
                playerPos.x + xOffset,
                playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset,
                playerPos.z + zOffset
            );
            if (i == 14 || isFreeSpot(client, point)) {
                return point;
            }
        }
        return Vec3.ZERO;
    }

    public static Vec3 generatePositionInFrontOfPlayer(Minecraft client) {
        if (client.player == null || client.level == null) {
            return Vec3.ZERO;
        }

        RandomSource random = client.player.getRandom();
        Vec3 playerEye = client.player.getEyePosition();
        float baseYaw = client.player.getYRot();

        double fov = client.options.fov().get();
        double aspect = (double) client.getWindow().getWidth() / (double) client.getWindow().getHeight();
        double vertFov = Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(fov / 2)) / aspect));

        for (int i = 0; i < 15; i++) {
            double radius = 2 + (5 - 2) * random.nextDouble();
            double screenX = random.nextDouble() * 2.0 - 1.0;
            double screenY = random.nextDouble() * 2.0 - 1.0;
            double halfFovRad = Math.toRadians(fov / 2.0);
            double halfVertFovRad = Math.toRadians(vertFov / 2.0);
            double xAngle = screenX * halfFovRad;
            double yAngle = screenY * halfVertFovRad;
            double yawRad = Math.toRadians(baseYaw) + xAngle;
            double pitchRad = yAngle;
            double xOffset = radius * -Math.sin(yawRad) * Math.cos(pitchRad);
            double yOffset = radius * -Math.sin(pitchRad);
            double zOffset = radius * Math.cos(yawRad) * Math.cos(pitchRad);
            Vec3 point = new Vec3(
                playerEye.x + xOffset,
                playerEye.y + yOffset,
                playerEye.z + zOffset
            );
            if (i == 14 || isFreeSpot(client, point)) {
                return point;
            }
        }
        return Vec3.ZERO;
    }

    private static boolean isFreeSpot(Minecraft client, Vec3 point) {
        return client.level.noCollision(new AABB(
            point.x - 0.1, point.y - 0.1, point.z - 0.1,
            point.x + 0.1, point.y + 0.1, point.z + 0.1
        ));
    }
}
