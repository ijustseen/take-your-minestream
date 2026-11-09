package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import takeyourminestream.ijustseen.config.ModConfig;

/**
 * Отвечает за позиционирование сообщений в мире
 */
public class MessagePosition {

    private MessagePosition() {}

    /** На сколько блоков ниже текущей базовой высоты спавнятся сообщения. */
    private static final double SPAWN_HEIGHT_OFFSET = -1.0;

    /**
     * Случайная позиция вокруг игрока (радиус, высота — как в «around»).
     */
    public static Vec3d generateRandomPosition(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return Vec3d.ZERO;
        }

        if (ModConfig.isSPAWN_ONLY_VISIBLE()) {
            return pickVisible(client, false);
        }

        Random random = client.world.random;
        for (int i = 0; i < 15; i++) {
            Vec3d point = sampleAroundPlayerPoint(client, random);
            if (i == 14 || isFreeSpot(client, point)) {
                return point;
            }
        }
        return Vec3d.ZERO;
    }

    /**
     * Как {@link #generateRandomPosition}, но только в горизонтальном FOV
     * (взгляд строго по горизонту — без учёта pitch игрока).
     */
    public static Vec3d generatePositionInFrontOfPlayer(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return Vec3d.ZERO;
        }

        if (ModConfig.isSPAWN_ONLY_VISIBLE()) {
            return pickVisible(client, true);
        }

        Random random = client.world.random;
        double halfHorizRad = getHorizontalHalfFovRad(client);

        for (int i = 0; i < 15; i++) {
            Vec3d point = sampleAroundPlayerPoint(client, random);
            if (!isWithinHorizontalFov(client, point, halfHorizRad)) {
                continue;
            }
            if (i == 14 || isFreeSpot(client, point)) {
                return point;
            }
        }

        return sampleFallbackInFov(client, random, halfHorizRad);
    }

    private static final int VISIBLE_RANGE_ATTEMPTS = 24;
    private static final int VISIBLE_RELAXED_ATTEMPTS = 40;

    /** Сначала диапазон мин/макс, затем любая видимая дистанция, если рядом блоки. */
    private static Vec3d pickVisible(MinecraftClient client, boolean frontOnly) {
        Random random = client.world.random;
        double halfHorizRad = frontOnly ? getHorizontalHalfFovRad(client) : Math.PI;
        Vec3d inRange = findClear(client, random, frontOnly, halfHorizRad, VISIBLE_RANGE_ATTEMPTS, false);
        if (inRange != null) {
            return inRange;
        }
        Vec3d relaxed = findClear(client, random, frontOnly, halfHorizRad, VISIBLE_RELAXED_ATTEMPTS, true);
        if (relaxed != null) {
            return relaxed;
        }
        return frontOnly
            ? sampleFallbackInFov(client, random, halfHorizRad)
            : sampleAroundPlayerPoint(client, random);
    }

    private static Vec3d findClear(
        MinecraftClient client,
        Random random,
        boolean frontOnly,
        double halfHorizRad,
        int attempts,
        boolean relaxDistance
    ) {
        for (int i = 0; i < attempts; i++) {
            Vec3d point = relaxDistance
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

    private static Vec3d sampleRelaxed(MinecraftClient client, Random random, boolean frontOnly, double halfHorizRad) {
        double max = Math.max(ModConfig.getMESSAGE_SPAWN_MAX_DISTANCE(), 16);
        double radius = 1.0 + (max - 1.0) * random.nextDouble();
        if (frontOnly) {
            return sampleInFov(client, random, halfHorizRad, radius);
        }
        return sampleAround(client, random, radius, random.nextDouble() * 2 * Math.PI);
    }

    private static boolean isClearSpawn(MinecraftClient client, Vec3d point) {
        return isFreeSpot(client, point)
            && !BlockOcclusion.isBlocked(client, client.player.getEyePos(), point);
    }

    private static Vec3d sampleAroundPlayerPoint(MinecraftClient client, Random random) {
        return sampleAround(client, random, sampleSpawnRadius(random), random.nextDouble() * 2 * Math.PI);
    }

    /** Точка в FOV-конусе на горизонте, если rejection sampling не нашёл свободное место. */
    private static Vec3d sampleFallbackInFov(MinecraftClient client, Random random, double halfHorizRad) {
        return sampleInFov(client, random, halfHorizRad, sampleSpawnRadius(random));
    }

    private static Vec3d sampleInFov(MinecraftClient client, Random random, double halfHorizRad, double radius) {
        Vec3d playerPos = client.player.getEyePos();
        float yaw = client.player.getYaw();
        double yawOffset = (random.nextDouble() * 2.0 - 1.0) * halfHorizRad;
        double yawRad = Math.toRadians(yaw) + yawOffset;
        double yOffset = random.nextDouble() * 2 - 1;
        return new Vec3d(
            playerPos.x + radius * -Math.sin(yawRad),
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + radius * Math.cos(yawRad)
        );
    }

    private static Vec3d sampleAround(MinecraftClient client, Random random, double radius, double angle) {
        Vec3d playerPos = client.player.getEyePos();
        double yOffset = random.nextDouble() * 2 - 1;
        return new Vec3d(
            playerPos.x + radius * Math.cos(angle),
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + radius * Math.sin(angle)
        );
    }

    private static double sampleSpawnRadius(Random random) {
        int min = ModConfig.getMESSAGE_SPAWN_MIN_DISTANCE();
        int max = ModConfig.getMESSAGE_SPAWN_MAX_DISTANCE();
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        return min + (max - min) * random.nextDouble();
    }

    private static double getHorizontalHalfFovRad(MinecraftClient client) {
        double aspect = (double) client.getWindow().getFramebufferWidth()
            / (double) client.getWindow().getFramebufferHeight();
        double vertFov = client.options.getFov().getValue();
        double halfVertRad = Math.toRadians(vertFov / 2.0);
        return Math.atan(Math.tan(halfVertRad) * aspect);
    }

    private static boolean isWithinHorizontalFov(MinecraftClient client, Vec3d point, double halfHorizRad) {
        Vec3d eye = client.player.getEyePos();
        double dx = point.x - eye.x;
        double dz = point.z - eye.z;
        double distSq = dx * dx + dz * dz;
        if (distSq < 1e-8) {
            return true;
        }

        float yaw = client.player.getYaw();
        double forwardX = -Math.sin(Math.toRadians(yaw));
        double forwardZ = Math.cos(Math.toRadians(yaw));
        double invLen = 1.0 / Math.sqrt(distSq);
        dx *= invLen;
        dz *= invLen;

        double dot = forwardX * dx + forwardZ * dz;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.acos(dot) <= halfHorizRad;
    }

    private static boolean isFreeSpot(MinecraftClient client, Vec3d point) {
        return !client.world.getBlockCollisions(
            null,
            new Box(point.x - 0.1, point.y - 0.1, point.z - 0.1, point.x + 0.1, point.y + 0.1, point.z + 0.1)
        ).iterator().hasNext();
    }
}
