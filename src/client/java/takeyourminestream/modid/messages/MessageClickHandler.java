package takeyourminestream.modid.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.modid.utils.CameraPositionCompat;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Обработчик кликов по сообщениям
 */
public class MessageClickHandler {
    private static final float CLICK_DISTANCE = 100.0f; // Максимальная дистанция для клика
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 4;
    private static final int PIN_ICON_SIZE = 8;
    private static final int PIN_ICON_MARGIN = 1;

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
    
    /**
     * Проверяет, был ли клик по сообщению
     * @param client Minecraft клиент
     * @param message Сообщение для проверки
     * @param tickCounter Текущий счетчик тиков
     * @return true если клик попал по сообщению
     */
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
    
    /**
     * Проверяет, смотрит ли игрок на сообщение (более точная проверка)
     * @param client Minecraft клиент
     * @param message Сообщение для проверки
     * @return true если игрок смотрит на сообщение
     */
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
        
        // Получаем направление взгляда
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d toMessage = messagePos.subtract(cameraPos).normalize();
        
        // Проверяем угол между направлением взгляда и направлением к сообщению
        double dotProduct = lookVec.dotProduct(toMessage);
        double angleThreshold = Math.cos(Math.toRadians(10.0)); // 10 градусов
        
        return dotProduct >= angleThreshold;
    }

    private static MessageHit findHit(MinecraftClient client, Message message, int tickCounter) {
        if (client.player == null) {
            return null;
        }

        Vec3d rayOrigin = CameraPositionCompat.getCameraPos(client);
        Vec3d rayDir = client.player.getRotationVec(1.0f).normalize();

        Vec3d panelCenter = message.getPosition();
        if (!message.isPinned()) {
            int effectiveAge = message.getEffectiveAge(tickCounter);
            panelCenter = MessageViewDetector.calculateFallingPosition(
                message.getPosition(),
                effectiveAge,
                message.getYaw(),
                message.getPitch()
            );
        }

        double distanceToMessage = rayOrigin.distanceTo(panelCenter);
        if (distanceToMessage > CLICK_DISTANCE) {
            return null;
        }

        TextRenderer textRenderer = client.textRenderer;
        List<OrderedText> wrappedText = textRenderer.wrapLines(Text.of(message.getText()), 120);
        float totalTextHeight = wrappedText.size() * textRenderer.fontHeight;
        int maxTextWidth = 0;
        for (OrderedText line : wrappedText) {
            maxTextWidth = Math.max(maxTextWidth, textRenderer.getWidth(line));
        }

        int panelWidth = maxTextWidth + PANEL_PADDING_X * 2;
        int panelHeight = (int) totalTextHeight + PANEL_PADDING_Y * 2;
        float baseScale = 0.025f;
        float configScale = takeyourminestream.modid.ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = baseScale * configScale;
        double rectWidth = panelWidth * finalScale;
        double rectHeight = panelHeight * finalScale;

        Vec3d localOrigin = rayOrigin.subtract(panelCenter);
        Vec3d localDir = rayDir;

        double yawRad = Math.toRadians(message.getYaw());
        double pitchRad = Math.toRadians(message.getPitch());
        localOrigin = rotateX(localOrigin, -pitchRad);
        localOrigin = rotateY(localOrigin, +yawRad);
        localDir = rotateX(localDir, -pitchRad);
        localDir = rotateY(localDir, +yawRad);

        double denom = localDir.z;
        if (Math.abs(denom) < 1e-6) {
            return null;
        }

        double t = -localOrigin.z / denom;
        if (t < 0 || t > CLICK_DISTANCE) {
            return null;
        }

        Vec3d hitLocal = localOrigin.add(localDir.multiply(t));
        boolean insidePanel = Math.abs(hitLocal.x) <= rectWidth / 2.0 && Math.abs(hitLocal.y) <= rectHeight / 2.0;
        boolean insidePinIcon = false;
        if (message.isPinned()) {
            double iconCenterX = rectWidth / 2.0 + PIN_ICON_MARGIN * finalScale;
            double iconCenterYTop = -rectHeight / 2.0 - PIN_ICON_MARGIN * finalScale;
            double iconCenterYBottom = rectHeight / 2.0 + PIN_ICON_MARGIN * finalScale;
            double hitRadius = PIN_ICON_SIZE * finalScale * 0.9;

            double dxTop = hitLocal.x - iconCenterX;
            double dyTop = hitLocal.y - iconCenterYTop;
            double dxBottom = hitLocal.x - iconCenterX;
            double dyBottom = hitLocal.y - iconCenterYBottom;

            insidePinIcon = (dxTop * dxTop + dyTop * dyTop) <= (hitRadius * hitRadius)
                || (dxBottom * dxBottom + dyBottom * dyBottom) <= (hitRadius * hitRadius);
        }

        if (!insidePanel && !insidePinIcon) {
            return null;
        }

        return new MessageHit(message, t, insidePinIcon);
    }

    private static Vec3d rotateY(Vec3d v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double x = v.x * cos + v.z * sin;
        double z = -v.x * sin + v.z * cos;
        return new Vec3d(x, v.y, z);
    }

    private static Vec3d rotateX(Vec3d v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double y = v.y * cos - v.z * sin;
        double z = v.y * sin + v.z * cos;
        return new Vec3d(v.x, y, z);
    }
}
