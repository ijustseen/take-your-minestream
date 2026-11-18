package takeyourminestream.ijustseen.messages;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import takeyourminestream.ijustseen.config.ModConfig;

/** Позиционирование сообщений в мире (Minecraft 26.x). */
public class MessagePosition {
    private MessagePosition() {}

    /** На сколько блоков ниже текущей базовой высоты спавнятся сообщения. */
    private static final double SPAWN_HEIGHT_OFFSET = -1.0;
    private static final int VISIBLE_RANGE_ATTEMPTS = 24;
    private static final int VISIBLE_RELAXED_ATTEMPTS = 40;

    public static Vec3 generateRandomPosition(Minecraft client) {
        if (client.player == null || client.level == null) {
            return Vec3.ZERO;
        }

        if (ModConfig.isSPAWN_ONLY_VISIBLE()) {
            return pickVisible(client, false);
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

        if (ModConfig.isSPAWN_ONLY_VISIBLE()) {
            return pickVisible(client, true);
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

    /** Сначала диапазон мин/макс, затем любая видимая дистанция, если рядом блоки. */
    private static Vec3 pickVisible(Minecraft client, boolean frontOnly) {
        RandomSource random = client.player.getRandom();
        double halfHorizRad = frontOnly ? getHorizontalHalfFovRad(client) : Math.PI;
        Vec3 inRange = findClear(client, random, frontOnly, halfHorizRad, VISIBLE_RANGE_ATTEMPTS, false);
        if (inRange != null) {
            return inRange;
        }
        Vec3 relaxed = findClear(client, random, frontOnly, halfHorizRad, VISIBLE_RELAXED_ATTEMPTS, true);
        if (relaxed != null) {
            return relaxed;
        }
        return frontOnly
            ? sampleFallbackInFov(client, random, halfHorizRad)
            : sampleAroundPlayerPoint(client, random);
    }

    private static Vec3 findClear(
        Minecraft client,
        RandomSource random,
        boolean frontOnly,
        double halfHorizRad,
        int attempts,
        boolean relaxDistance
    ) {
        for (int i = 0; i < attempts; i++) {
            Vec3 point = relaxDistance
                ? sampleRelaxed(client, random, frontOnly, halfHorizRad)
                : sampleAroundPlayerPoint(client, random);
            if (frontOnly && !isWithinHorizontalFov(client, point, halfHorizRad)) {
                continue;
            }
            if (isClearSpawn(client, point)) {
                return point;
            }
        }
        return null;
    }

    private static Vec3 sampleRelaxed(Minecraft client, RandomSource random, boolean frontOnly, double halfHorizRad) {
        double max = Math.max(ModConfig.getMESSAGE_SPAWN_MAX_DISTANCE(), 16);
        double radius = 1.0 + (max - 1.0) * random.nextDouble();
        if (frontOnly) {
            return sampleInFov(client, random, halfHorizRad, radius);
        }
        return sampleAround(client, random, radius, random.nextDouble() * 2 * Math.PI);
    }

    private static boolean isClearSpawn(Minecraft client, Vec3 point) {
        return isFreeSpot(client, point)
            && !BlockOcclusion.isBlocked(client, client.player.getEyePosition(), point);
    }

    private static Vec3 sampleAroundPlayerPoint(Minecraft client, RandomSource random) {
        return sampleAround(client, random, sampleSpawnRadius(random), random.nextDouble() * 2 * Math.PI);
    }

    private static Vec3 sampleFallbackInFov(Minecraft client, RandomSource random, double halfHorizRad) {
        return sampleInFov(client, random, halfHorizRad, sampleSpawnRadius(random));
    }

    private static Vec3 sampleInFov(Minecraft client, RandomSource random, double halfHorizRad, double radius) {
        Vec3 playerPos = client.player.getEyePosition();
        float yaw = client.player.getYRot();
        double yawOffset = (random.nextDouble() * 2.0 - 1.0) * halfHorizRad;
        double yawRad = Math.toRadians(yaw) + yawOffset;
        double yOffset = random.nextDouble() * 2 - 1;
        return new Vec3(
            playerPos.x + radius * -Math.sin(yawRad),
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + radius * Math.cos(yawRad)
        );
    }

    private static Vec3 sampleAround(Minecraft client, RandomSource random, double radius, double angle) {
        Vec3 playerPos = client.player.getEyePosition();
        double yOffset = random.nextDouble() * 2 - 1;
        return new Vec3(
            playerPos.x + radius * Math.cos(angle),
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + radius * Math.sin(angle)
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
