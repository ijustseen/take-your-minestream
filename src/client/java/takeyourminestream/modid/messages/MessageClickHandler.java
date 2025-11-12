package takeyourminestream.modid.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Обработчик кликов по сообщениям
 */
public class MessageClickHandler {
    private static final float CLICK_DISTANCE = 100.0f; // Максимальная дистанция для клика
    
    /**
     * Проверяет, был ли клик по сообщению
     * @param client Minecraft клиент
     * @param message Сообщение для проверки
     * @param tickCounter Текущий счетчик тиков
     * @return true если клик попал по сообщению
     */
    public static boolean isClickOnMessage(MinecraftClient client, Message message, int tickCounter) {
        if (client.player == null || client.crosshairTarget == null) {
            return false;
        }
        
        // Проверяем дистанцию до сообщения
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        Vec3d messagePos = message.getPosition();
        double distance = cameraPos.distanceTo(messagePos);
        
        if (distance > CLICK_DISTANCE) {
            return false;
        }
        
        // Получаем направление взгляда
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        Vec3d rayStart = cameraPos;
        Vec3d rayEnd = rayStart.add(lookVec.multiply(CLICK_DISTANCE));
        
        // Вычисляем размеры сообщения
        TextRenderer textRenderer = client.textRenderer;
        List<OrderedText> wrappedText = textRenderer.wrapLines(Text.of(message.getText()), 120);
        
        float totalTextHeight = wrappedText.size() * textRenderer.fontHeight;
        int maxTextWidth = 0;
        for (OrderedText line : wrappedText) {
            int w = textRenderer.getWidth(line);
            if (w > maxTextWidth) maxTextWidth = w;
        }
        
        // Масштаб сообщения
        float baseScale = 0.025f;
        float configScale = takeyourminestream.modid.ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = baseScale * configScale;
        
        // Размеры в мировых координатах
        float worldWidth = maxTextWidth * finalScale;
        float worldHeight = totalTextHeight * finalScale;
        
        // Создаем bounding box вокруг сообщения
        // Учитываем ориентацию сообщения (yaw и pitch)
        Vec3d center = messagePos;
        
        // Упрощенная проверка - используем сферу вокруг сообщения
        float radius = Math.max(worldWidth, worldHeight) / 2.0f;
        
        // Проверяем пересечение луча со сферой
        Vec3d toCenter = center.subtract(rayStart);
        double projection = toCenter.dotProduct(lookVec);
        
        if (projection < 0) {
            return false; // Сообщение позади камеры
        }
        
        Vec3d closestPoint = rayStart.add(lookVec.multiply(projection));
        double distanceToRay = closestPoint.distanceTo(center);
        
        return distanceToRay <= radius;
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
        
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
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
}
