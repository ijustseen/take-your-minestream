package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;
import takeyourminestream.ijustseen.core.render.MessageFrame3DModel;

/**
 * Останавливает сообщение у стены по ориентированному хитбоксу панели (OBB / параллелепипед),
 * а не по дискретным точкам — без «волн» на углах блоков.
 */
public final class MessageWallClamp {
    private static final double MIN_DISTANCE = 0.05;
    private static final double SURFACE_GAP = 0.0;
    private static final int SEARCH_STEPS = 14;
    /** У 2D-панели нулевая толщина — для объёмной проверки нужен тонкий параллелепипед. */
    private static final double MIN_HALF_DEPTH = 0.01;
    private static final double SAT_EPS = 1.0e-8;

    private MessageWallClamp() {}

    /**
     * Ближайшая к {@code desiredCenter} точка на отрезке глаз → цель,
     * при которой хитбокс сообщения не пересекает блоки.
     */
    public static Vec3d clampTowardEye(
        MinecraftClient client,
        Vec3d eye,
        Vec3d desiredCenter,
        float yaw,
        float pitch,
        Message message,
        TextRenderer textRenderer,
        int tickCounter
    ) {
        if (client == null || client.player == null || client.world == null || message == null || textRenderer == null) {
            return desiredCenter;
        }
        double desiredDistance = eye.distanceTo(desiredCenter);
        if (desiredDistance < MIN_DISTANCE) {
            return desiredCenter;
        }

        PanelObb obb = PanelObb.from(buildLocalPanel(message, textRenderer, tickCounter), yaw, pitch);

        if (isClearAt(client, eye, desiredCenter, obb)) {
            return desiredCenter;
        }

        Vec3d dir = desiredCenter.subtract(eye).normalize();
        double lo = MIN_DISTANCE;
        double hi = desiredDistance;
        double best = MIN_DISTANCE;
        for (int step = 0; step < SEARCH_STEPS; step++) {
            double mid = (lo + hi) * 0.5;
            Vec3d candidate = eye.add(dir.multiply(mid));
            if (isClearAt(client, eye, candidate, obb)) {
                best = mid;
                lo = mid;
            } else {
                hi = mid;
            }
        }
        best = Math.max(MIN_DISTANCE, best - SURFACE_GAP);
        return eye.add(dir.multiply(best));
    }

    private static LocalBox buildLocalPanel(Message message, TextRenderer textRenderer, int tickCounter) {
        MessagePanelLayout.Dimensions dimensions = message.getWorldLayout(textRenderer);
        float scale = MessagePanelLayout.worldScale();
        int effectiveAge = message.isPinned() ? 0 : message.getEffectiveAge(tickCounter);
        float fallOffsetY = MessagePanelLayout.fallOffsetY(effectiveAge);
        boolean modelVolume = ModConfig.isMESSAGE_3D_FRAME() && ModConfig.isSHOW_MESSAGE_BACKGROUND();

        double halfWidth = dimensions.maxTextWidth() / 2.0;
        double halfHeight = dimensions.totalTextHeight() / 2.0;
        double minX = (-MessagePanelConstants.PADDING_X - halfWidth) * scale;
        double maxX = (halfWidth + MessagePanelConstants.PADDING_X) * scale;
        double yAtTop = (MessagePanelConstants.PADDING_Y + halfHeight - fallOffsetY) * scale;
        double yAtBottom = -(halfHeight + MessagePanelConstants.PADDING_Y + fallOffsetY) * scale;
        double minY = Math.min(yAtTop, yAtBottom);
        double maxY = Math.max(yAtTop, yAtBottom);
        double minZ;
        double maxZ;
        if (modelVolume) {
            minZ = MessageFrame3DModel.Z_BACK * scale;
            maxZ = MessageFrame3DModel.Z_FRONT * scale;
        } else {
            minZ = -MIN_HALF_DEPTH;
            maxZ = MIN_HALF_DEPTH;
        }
        return new LocalBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean isClearAt(MinecraftClient client, Vec3d eye, Vec3d center, PanelObb obb) {
        if (intersectsSolid(client, center, obb)) {
            return false;
        }
        Vec3d worldCenter = center.add(obb.centerOffset);
        if (BlockOcclusion.isBlocked(client, eye, worldCenter)) {
            return false;
        }
        for (Vec3d cornerOffset : obb.cornerOffsets) {
            if (BlockOcclusion.isBlocked(client, eye, center.add(cornerOffset))) {
                return false;
            }
        }
        return true;
    }

    private static boolean intersectsSolid(MinecraftClient client, Vec3d center, PanelObb obb) {
        LocalBox world = obb.worldAabb(center);
        return BlockOcclusion.anyCollisionBox(
            client,
            world.minX, world.minY, world.minZ,
            world.maxX, world.maxY, world.maxZ,
            (minX, minY, minZ, maxX, maxY, maxZ) ->
                obb.intersectsAabb(center, minX, minY, minZ, maxX, maxY, maxZ)
        );
    }

    private record LocalBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}

    /**
     * Ориентированный параллелепипед панели: локальный AABB, повёрнутый yaw/pitch вокруг якоря сообщения.
     */
    private static final class PanelObb {
        private final Vec3d centerOffset;
        private final Vec3d axisX;
        private final Vec3d axisY;
        private final Vec3d axisZ;
        private final double halfX;
        private final double halfY;
        private final double halfZ;
        private final Vec3d[] cornerOffsets;

        private PanelObb(
            Vec3d centerOffset,
            Vec3d axisX,
            Vec3d axisY,
            Vec3d axisZ,
            double halfX,
            double halfY,
            double halfZ,
            Vec3d[] cornerOffsets
        ) {
            this.centerOffset = centerOffset;
            this.axisX = axisX;
            this.axisY = axisY;
            this.axisZ = axisZ;
            this.halfX = halfX;
            this.halfY = halfY;
            this.halfZ = halfZ;
            this.cornerOffsets = cornerOffsets;
        }

        static PanelObb from(LocalBox local, float yaw, float pitch) {
            Matrix4f rotation = new Matrix4f()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch));
            Vector3f tmp = new Vector3f();

            double lcx = (local.minX + local.maxX) * 0.5;
            double lcy = (local.minY + local.maxY) * 0.5;
            double lcz = (local.minZ + local.maxZ) * 0.5;
            tmp.set((float) lcx, (float) lcy, (float) lcz);
            rotation.transformDirection(tmp);
            Vec3d centerOffset = new Vec3d(tmp.x, tmp.y, tmp.z);

            tmp.set(1f, 0f, 0f);
            rotation.transformDirection(tmp);
            Vec3d axisX = new Vec3d(tmp.x, tmp.y, tmp.z);
            tmp.set(0f, 1f, 0f);
            rotation.transformDirection(tmp);
            Vec3d axisY = new Vec3d(tmp.x, tmp.y, tmp.z);
            tmp.set(0f, 0f, 1f);
            rotation.transformDirection(tmp);
            Vec3d axisZ = new Vec3d(tmp.x, tmp.y, tmp.z);

            double halfX = (local.maxX - local.minX) * 0.5;
            double halfY = (local.maxY - local.minY) * 0.5;
            double halfZ = Math.max((local.maxZ - local.minZ) * 0.5, MIN_HALF_DEPTH);

            double[] xs = {local.minX, local.maxX};
            double[] ys = {local.minY, local.maxY};
            double[] zs = {local.minZ, local.maxZ};
            Vec3d[] corners = new Vec3d[8];
            int n = 0;
            for (double x : xs) {
                for (double y : ys) {
                    for (double z : zs) {
                        tmp.set((float) x, (float) y, (float) z);
                        rotation.transformDirection(tmp);
                        corners[n++] = new Vec3d(tmp.x, tmp.y, tmp.z);
                    }
                }
            }
            return new PanelObb(centerOffset, axisX, axisY, axisZ, halfX, halfY, halfZ, corners);
        }

        LocalBox worldAabb(Vec3d anchor) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (Vec3d corner : cornerOffsets) {
                double x = anchor.x + corner.x;
                double y = anchor.y + corner.y;
                double z = anchor.z + corner.z;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            }
            return new LocalBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        /** SAT: OBB панели против AABB коллизии блока. */
        boolean intersectsAabb(
            Vec3d anchor,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
        ) {
            Vec3d obbCenter = anchor.add(centerOffset);
            double aCenterX = (minX + maxX) * 0.5;
            double aCenterY = (minY + maxY) * 0.5;
            double aCenterZ = (minZ + maxZ) * 0.5;
            double ax = (maxX - minX) * 0.5;
            double ay = (maxY - minY) * 0.5;
            double az = (maxZ - minZ) * 0.5;
            double tx = obbCenter.x - aCenterX;
            double ty = obbCenter.y - aCenterY;
            double tz = obbCenter.z - aCenterZ;

            double r00 = axisX.x;
            double r01 = axisX.y;
            double r02 = axisX.z;
            double r10 = axisY.x;
            double r11 = axisY.y;
            double r12 = axisY.z;
            double r20 = axisZ.x;
            double r21 = axisZ.y;
            double r22 = axisZ.z;

            double ar00 = Math.abs(r00) + SAT_EPS;
            double ar01 = Math.abs(r01) + SAT_EPS;
            double ar02 = Math.abs(r02) + SAT_EPS;
            double ar10 = Math.abs(r10) + SAT_EPS;
            double ar11 = Math.abs(r11) + SAT_EPS;
            double ar12 = Math.abs(r12) + SAT_EPS;
            double ar20 = Math.abs(r20) + SAT_EPS;
            double ar21 = Math.abs(r21) + SAT_EPS;
            double ar22 = Math.abs(r22) + SAT_EPS;

            if (Math.abs(tx) > ax + halfX * ar00 + halfY * ar10 + halfZ * ar20) {
                return false;
            }
            if (Math.abs(ty) > ay + halfX * ar01 + halfY * ar11 + halfZ * ar21) {
                return false;
            }
            if (Math.abs(tz) > az + halfX * ar02 + halfY * ar12 + halfZ * ar22) {
                return false;
            }

            if (Math.abs(tx * r00 + ty * r01 + tz * r02) > halfX + ax * ar00 + ay * ar01 + az * ar02) {
                return false;
            }
            if (Math.abs(tx * r10 + ty * r11 + tz * r12) > halfY + ax * ar10 + ay * ar11 + az * ar12) {
                return false;
            }
            if (Math.abs(tx * r20 + ty * r21 + tz * r22) > halfZ + ax * ar20 + ay * ar21 + az * ar22) {
                return false;
            }

            if (Math.abs(tz * r01 - ty * r02) > ay * ar02 + az * ar01 + halfY * ar20 + halfZ * ar10) {
                return false;
            }
            if (Math.abs(tz * r11 - ty * r12) > ay * ar12 + az * ar11 + halfX * ar20 + halfZ * ar00) {
                return false;
            }
            if (Math.abs(tz * r21 - ty * r22) > ay * ar22 + az * ar21 + halfX * ar10 + halfY * ar00) {
                return false;
            }

            if (Math.abs(tx * r02 - tz * r00) > ax * ar02 + az * ar00 + halfY * ar21 + halfZ * ar11) {
                return false;
            }
            if (Math.abs(tx * r12 - tz * r10) > ax * ar12 + az * ar10 + halfX * ar21 + halfZ * ar01) {
                return false;
            }
            if (Math.abs(tx * r22 - tz * r20) > ax * ar22 + az * ar20 + halfX * ar11 + halfY * ar01) {
                return false;
            }

            if (Math.abs(ty * r00 - tx * r01) > ax * ar01 + ay * ar00 + halfY * ar22 + halfZ * ar12) {
                return false;
            }
            if (Math.abs(ty * r10 - tx * r11) > ax * ar11 + ay * ar10 + halfX * ar22 + halfZ * ar02) {
                return false;
            }
            if (Math.abs(ty * r20 - tx * r21) > ax * ar21 + ay * ar20 + halfX * ar12 + halfY * ar02) {
                return false;
            }

            return true;
        }
    }
}
