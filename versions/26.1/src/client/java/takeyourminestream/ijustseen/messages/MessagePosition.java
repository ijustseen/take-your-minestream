package takeyourminestream.ijustseen.messages;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import takeyourminestream.ijustseen.config.ModConfig;

/** Позиционирование сообщений в мире (Minecraft 26.1). */
public class MessagePosition {
    private MessagePosition() {}

    /** На сколько блоков ниже текущей базовой высоты спавнятся сообщения. */
    private static final double SPAWN_HEIGHT_OFFSET = -1.0;

    public static Vec3 generateRandomPosition(Minecraft client) {
        if (client.player == null || client.level == null) {
            return Vec3.ZERO;
        }

        RandomSource random = client.player.getRandom();
        for (int i = 0; i < 15; i++) {
            Vec3 point = sampleAroundPlayerPoint(client, random);
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
        double halfHorizRad = getHorizontalHalfFovRad(client);

        for (int i = 0; i < 15; i++) {
            Vec3 point = sampleAroundPlayerPoint(client, random);
            if (!isWithinHorizontalFov(client, point, halfHorizRad)) {
                continue;
            }
            if (i == 14 || isFreeSpot(client, point)) {
                return point;
            }
        }

        return sampleFallbackInFov(client, random, halfHorizRad);
    }

    private static Vec3 sampleAroundPlayerPoint(Minecraft client, RandomSource random) {
        Vec3 playerPos = client.player.getEyePosition();
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = sampleSpawnRadius(random);
        double xOffset = radius * Math.cos(angle);
        double zOffset = radius * Math.sin(angle);
        double yOffset = random.nextDouble() * 2 - 1;

        return new Vec3(
            playerPos.x + xOffset,
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + zOffset
        );
    }

    private static Vec3 sampleFallbackInFov(Minecraft client, RandomSource random, double halfHorizRad) {
        Vec3 playerPos = client.player.getEyePosition();
        float yaw = client.player.getYRot();
        double radius = sampleSpawnRadius(random);
        double yawOffset = (random.nextDouble() * 2.0 - 1.0) * halfHorizRad;
        double yawRad = Math.toRadians(yaw) + yawOffset;
        double xOffset = radius * -Math.sin(yawRad);
        double zOffset = radius * Math.cos(yawRad);
        double yOffset = random.nextDouble() * 2 - 1;

        return new Vec3(
            playerPos.x + xOffset,
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + zOffset
        );
    }

    private static double sampleSpawnRadius(RandomSource random) {
        int min = ModConfig.getMESSAGE_SPAWN_MIN_DISTANCE();
        int max = ModConfig.getMESSAGE_SPAWN_MAX_DISTANCE();
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        return min + (max - min) * random.nextDouble();
    }

    private static double getHorizontalHalfFovRad(Minecraft client) {
        double aspect = (double) client.getWindow().getWidth() / (double) client.getWindow().getHeight();
        double vertFov = client.options.fov().get();
        double halfVertRad = Math.toRadians(vertFov / 2.0);
        return Math.atan(Math.tan(halfVertRad) * aspect);
    }

    private static boolean isWithinHorizontalFov(Minecraft client, Vec3 point, double halfHorizRad) {
        Vec3 eye = client.player.getEyePosition();
        double dx = point.x - eye.x;
        double dz = point.z - eye.z;
        double distSq = dx * dx + dz * dz;
        if (distSq < 1e-8) {
            return true;
        }

        float yaw = client.player.getYRot();
        double forwardX = -Math.sin(Math.toRadians(yaw));
        double forwardZ = Math.cos(Math.toRadians(yaw));
        double invLen = 1.0 / Math.sqrt(distSq);
        dx *= invLen;
        dz *= invLen;

        double dot = forwardX * dx + forwardZ * dz;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.acos(dot) <= halfHorizRad;
    }

    private static boolean isFreeSpot(Minecraft client, Vec3 point) {
        return client.level.noCollision(new AABB(
            point.x - 0.1, point.y - 0.1, point.z - 0.1,
            point.x + 0.1, point.y + 0.1, point.z + 0.1
        ));
    }
}
