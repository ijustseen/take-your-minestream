package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;

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
    private static final int EMOTE_ICON_SIZE = 12;
    private static final int EMOTE_ICON_SPACING = 1;

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
        boolean hasEmotes = !message.getEmotes().isEmpty();
        float totalTextHeight;
        int maxTextWidth;
        if (hasEmotes) {
            totalTextHeight = textRenderer.fontHeight;
            maxTextWidth = getEmoteAwareLineWidth(textRenderer, message.getText(), message.getEmotes());
        } else {
            List<OrderedText> wrappedText = textRenderer.wrapLines(Text.of(message.getText()), 120);
            totalTextHeight = wrappedText.size() * textRenderer.fontHeight;
            maxTextWidth = 0;
            for (OrderedText line : wrappedText) {
                maxTextWidth = Math.max(maxTextWidth, textRenderer.getWidth(line));
            }
        }

        int panelWidth = maxTextWidth + PANEL_PADDING_X * 2;
        int panelHeight = (int) totalTextHeight + PANEL_PADDING_Y * 2;
        float baseScale = 0.025f;
        float configScale = takeyourminestream.ijustseen.ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = baseScale * configScale;
        double rectWidth = panelWidth * finalScale;
        double rectHeight = panelHeight * finalScale;

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

        // Переводим попадание из world-local в "пиксельные" координаты панели,
        // используя те же формулы, что и в MessageRenderer (включая инверсию Y).
        double textSpaceX = (hitLocal.x / finalScale) + (maxTextWidth / 2.0);
        double textSpaceY = (-hitLocal.y / finalScale) + (totalTextHeight / 2.0);

        double panelX0 = -PANEL_PADDING_X;
        double panelY0 = -PANEL_PADDING_Y;
        double panelX1 = panelX0 + panelWidth;
        double panelY1 = panelY0 + panelHeight;

        boolean insidePanel = textSpaceX >= panelX0 && textSpaceX <= panelX1
            && textSpaceY >= panelY0 && textSpaceY <= panelY1;
        boolean insidePinIcon = false;
        if (message.isPinned()) {
            double pinX0 = panelWidth - PANEL_PADDING_X - (PIN_ICON_SIZE / 2.0) + PIN_ICON_MARGIN;
            double pinY0 = -PANEL_PADDING_Y - (PIN_ICON_SIZE / 2.0) - PIN_ICON_MARGIN;
            double pinX1 = pinX0 + PIN_ICON_SIZE;
            double pinY1 = pinY0 + PIN_ICON_SIZE;

            insidePinIcon = textSpaceX >= pinX0 && textSpaceX <= pinX1
                && textSpaceY >= pinY0 && textSpaceY <= pinY1;
        }

        if (!insidePanel && !insidePinIcon) {
            return null;
        }

        return new MessageHit(message, t, true);
    }

    private static int getEmoteAwareLineWidth(TextRenderer textRenderer, String text, java.util.List<MessageEmote> emotes) {
        int width = 0;
        int cursor = 0;
        java.util.List<MessageEmote> sorted = new java.util.ArrayList<>(emotes);
        sorted.sort(Comparator.comparingInt(MessageEmote::getStartIndex));
        for (MessageEmote emote : sorted) {
            if (emote.getStartIndex() < 0 || emote.getEndIndex() < emote.getStartIndex() || emote.getEndIndex() >= text.length()) continue;
            if (emote.getStartIndex() < cursor) continue;
            if (emote.getStartIndex() > cursor) {
                width += textRenderer.getWidth(text.substring(cursor, emote.getStartIndex()));
            }
            width += EMOTE_ICON_SIZE + EMOTE_ICON_SPACING;
            cursor = emote.getEndIndex() + 1;
        }
        if (cursor < text.length()) {
            width += textRenderer.getWidth(text.substring(cursor));
        }
        return width;
    }
}
