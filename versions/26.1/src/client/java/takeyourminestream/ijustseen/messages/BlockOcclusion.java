package takeyourminestream.ijustseen.messages;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Луч до сообщения упирается в коллизию блока (Minecraft 26.x). */
public final class BlockOcclusion {
    /** Зазор между центром сообщения и гранью блока, в блоках. */
    private static final double BLOCK_GAP = 0.05;

    private BlockOcclusion() {}

    public static boolean isBlocked(Minecraft client, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) < 1.0e-6) {
            return false;
        }
        HitResult hit = clip(client, from, to);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return from.distanceToSqr(hit.getLocation()) + 1.0e-3 < from.distanceToSqr(to);
    }

    public static double clearDistance(Minecraft client, Vec3 from, Vec3 direction, double maxDistance) {
        if (maxDistance <= 0.0) {
            return 0.0;
        }
        Vec3 dir = direction.normalize();
        HitResult hit = clip(client, from, from.add(dir.scale(maxDistance)));
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return maxDistance;
        }
        double dist = Math.sqrt(from.distanceToSqr(hit.getLocation())) - BLOCK_GAP;
        return Math.min(maxDistance, Math.max(BLOCK_GAP, dist));
    }

    private static HitResult clip(Minecraft client, Vec3 from, Vec3 to) {
        if (client.level == null || client.player == null) {
            return null;
        }
        return client.level.clip(new ClipContext(
            from,
            to,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            client.player
        ));
    }
}
