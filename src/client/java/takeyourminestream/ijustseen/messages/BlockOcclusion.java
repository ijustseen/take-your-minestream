package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Луч до сообщения упирается в коллизию блока. */
public final class BlockOcclusion {
    /** Зазор между центром сообщения и гранью блока, в блоках. */
    private static final double BLOCK_GAP = 0.05;

    private BlockOcclusion() {}

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
