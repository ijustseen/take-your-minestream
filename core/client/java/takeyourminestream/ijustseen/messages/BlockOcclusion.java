package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

/** Луч до сообщения упирается в коллизию блока. */
public final class BlockOcclusion {
    /** Минимальный запас для ray clearDistance (спавн и т.п.). */
    private static final double BLOCK_GAP = 0.001;
    /** Крошечный probe: считать solid только если точка реально в коллизии, не «рядом». */
    private static final double SOLID_PROBE = 0.001;

    private BlockOcclusion() {}

    /** Точка сидит в коллизии блока (или почти вплотную к ней). */
    public static boolean isInsideSolid(MinecraftClient client, Vec3d point) {
        if (client.world == null) {
            return false;
        }
        // isSpaceEmpty быстрее, чем итератор getBlockCollisions на каждый probe при drag.
        return !client.world.isSpaceEmpty(new Box(
            point.x - SOLID_PROBE,
            point.y - SOLID_PROBE,
            point.z - SOLID_PROBE,
            point.x + SOLID_PROBE,
            point.y + SOLID_PROBE,
            point.z + SOLID_PROBE
        ));
    }

    @FunctionalInterface
    public interface BoxConsumer {
        boolean test(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }

    /**
     * Обходит AABB коллизий блоков в заданном мировом объёме.
     * {@code true}, если хотя бы один бокс удовлетворил {@code test}.
     */
    public static boolean anyCollisionBox(
        MinecraftClient client,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        BoxConsumer test
    ) {
        if (client.world == null) {
            return false;
        }
        Box worldAabb = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        for (VoxelShape shape : client.world.getBlockCollisions(null, worldAabb)) {
            for (Box box : shape.getBoundingBoxes()) {
                if (test.test(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Между {@code from} и {@code to} есть блок ближе, чем сама точка. */
    public static boolean isBlocked(MinecraftClient client, Vec3d from, Vec3d to) {
        if (from.squaredDistanceTo(to) < 1.0e-6) {
            return false;
        }
        HitResult hit = clip(client, from, to);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return from.squaredDistanceTo(hit.getPos()) + 1.0e-3 < from.squaredDistanceTo(to);
    }

    /**
     * Насколько далеко по лучу можно пройти, не войдя в блок.
     * {@code maxDistance}, если луч чистый.
     */
    public static double clearDistance(MinecraftClient client, Vec3d from, Vec3d direction, double maxDistance) {
        if (maxDistance <= 0.0) {
            return 0.0;
        }
        Vec3d dir = direction.normalize();
        HitResult hit = clip(client, from, from.add(dir.multiply(maxDistance)));
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return maxDistance;
        }
        double dist = Math.sqrt(from.squaredDistanceTo(hit.getPos())) - BLOCK_GAP;
        return Math.min(maxDistance, Math.max(BLOCK_GAP, dist));
    }

    private static HitResult clip(MinecraftClient client, Vec3d from, Vec3d to) {
        if (client.world == null || client.player == null) {
            return null;
        }
        return client.world.raycast(new RaycastContext(
            from,
            to,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));
    }
}
