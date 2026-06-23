package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;

import java.util.Collection;
import java.util.Comparator;

/**
 * Обработчик кликов по 3D-сообщениям в мире.
 */
public class MessageClickHandler {
    private static final float CLICK_DISTANCE = (float) takeyourminestream.ijustseen.core.MessagePanelConstants.MESSAGE_INTERACTION_DISTANCE;

    public static final class MessageHit {
        private final Message message;
        private final double distanceAlongRay;
        private final boolean hitPinIcon;

        public MessageHit(Message message, double distanceAlongRay, boolean hitPinIcon) {
            this.message = message;
            this.distanceAlongRay = distanceAlongRay;
            this.hitPinIcon = hitPinIcon;
        }

        public Message getMessage() {
            return message;
        }

        public double getDistanceAlongRay() {
            return distanceAlongRay;
        }

        public boolean isHitPinIcon() {
            return hitPinIcon;
        }
    }

    public static boolean isClickOnMessage(MinecraftClient client, Message message, int tickCounter) {
        return findHit(client, message, tickCounter) != null;
    }

    public static MessageHit findClosestMessageHit(MinecraftClient client, Collection<Message> messages, int tickCounter) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        return messages.stream()
            .map(message -> findHit(client, message, tickCounter))
            .filter(java.util.Objects::nonNull)
            .min(Comparator.comparingDouble(MessageHit::getDistanceAlongRay))
            .orElse(null);
    }

    public static boolean isHitOnPinIcon(MessageHit hit) {
        return hit != null && hit.isHitPinIcon();
    }

    public static boolean isLookingAtMessage(MinecraftClient client, Message message) {
        if (client.player == null) {
            return false;
        }

        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);
        Vec3d messagePos = message.getPosition();
        double distance = cameraPos.distanceTo(messagePos);

        if (distance > CLICK_DISTANCE) {
            return false;
        }

        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d toMessage = messagePos.subtract(cameraPos).normalize();
        double dotProduct = lookVec.dotProduct(toMessage);
        double angleThreshold = Math.cos(Math.toRadians(10.0));
        return dotProduct >= angleThreshold;
    }

    private static MessageHit findHit(MinecraftClient client, Message message, int tickCounter) {
        if (client.player == null) {
            return null;
        }

        Vec3d rayOrigin = CameraPositionCompat.getCameraPos(client);
        Vec3d rayDir = client.player.getRotationVec(1.0f).normalize();

        Vec3d panelCenter = message.getPosition();

        if (rayOrigin.distanceTo(panelCenter) > CLICK_DISTANCE) {
            return null;
        }

        TextRenderer textRenderer = client.textRenderer;
        MessagePanelLayout.Dimensions dimensions = message.getWorldLayout(textRenderer);
        float finalScale = MessagePanelLayout.worldScale();
        int effectiveAge = message.isPinned() ? 0 : message.getEffectiveAge(tickCounter);
        float fallOffsetY = MessagePanelLayout.fallOffsetY(effectiveAge);

        Matrix4f worldFromLocal = new Matrix4f()
            .translation((float) panelCenter.x, (float) panelCenter.y, (float) panelCenter.z)
            .rotateY((float) Math.toRadians(-message.getYaw()))
            .rotateX((float) Math.toRadians(message.getPitch()));
        Matrix4f localFromWorld = new Matrix4f(worldFromLocal).invert();

        Vector3f localOriginV = localFromWorld.transformPosition(
            new Vector3f((float) rayOrigin.x, (float) rayOrigin.y, (float) rayOrigin.z)
        );
        Vec3d rayEnd = rayOrigin.add(rayDir);
        Vector3f localRayEndV = localFromWorld.transformPosition(
            new Vector3f((float) rayEnd.x, (float) rayEnd.y, (float) rayEnd.z)
        );

        Vec3d localOrigin = new Vec3d(localOriginV.x, localOriginV.y, localOriginV.z);
        Vec3d localDir = new Vec3d(
            localRayEndV.x - localOriginV.x,
            localRayEndV.y - localOriginV.y,
            localRayEndV.z - localOriginV.z
        ).normalize();

        double denom = localDir.z;
        if (Math.abs(denom) < 1e-6) {
            return null;
        }

        double t = -localOrigin.z / denom;
        if (t < 0 || t > CLICK_DISTANCE) {
            return null;
        }

        Vec3d hitLocal = localOrigin.add(localDir.multiply(t));

        // Те же координаты textSpace, что и в MessageRenderer (инверсия Y из-за scale(..., -scale, ...)).
        double textSpaceX = (hitLocal.x / finalScale) + (dimensions.maxTextWidth() / 2.0);
        double textSpaceY = (-hitLocal.y / finalScale) + (dimensions.totalTextHeight() / 2.0) - fallOffsetY;

        boolean insidePanel = MessagePanelLayout.isInsidePanel(dimensions, textSpaceX, textSpaceY);
        boolean insidePinIcon = message.isPinned()
            && MessagePanelLayout.isInsidePinIcon(dimensions, textSpaceX, textSpaceY);

        if (!insidePanel && !insidePinIcon) {
            return null;
        }

        return new MessageHit(message, t, insidePinIcon);
    }
}
