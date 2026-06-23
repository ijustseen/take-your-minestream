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

    private static Vec3d sampleAroundPlayerPoint(MinecraftClient client, Random random) {
        Vec3d playerPos = client.player.getEyePos();
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = sampleSpawnRadius(random);
        double xOffset = radius * Math.cos(angle);
        double zOffset = radius * Math.sin(angle);
        double yOffset = random.nextDouble() * 2 - 1;

        return new Vec3d(
            playerPos.x + xOffset,
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + zOffset
        );
    }

    /** Точка в FOV-конусе на горизонте, если rejection sampling не нашёл свободное место. */
    private static Vec3d sampleFallbackInFov(MinecraftClient client, Random random, double halfHorizRad) {
        Vec3d playerPos = client.player.getEyePos();
        float yaw = client.player.getYaw();
        double radius = sampleSpawnRadius(random);
        double yawOffset = (random.nextDouble() * 2.0 - 1.0) * halfHorizRad;
        double yawRad = Math.toRadians(yaw) + yawOffset;
        double xOffset = radius * -Math.sin(yawRad);
        double zOffset = radius * Math.cos(yawRad);
        double yOffset = random.nextDouble() * 2 - 1;

        return new Vec3d(
            playerPos.x + xOffset,
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset + SPAWN_HEIGHT_OFFSET,
            playerPos.z + zOffset
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
