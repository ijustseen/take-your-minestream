package takeyourminestream.modid.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Отвечает за позиционирование сообщений в мире
 */
public class MessagePosition {
    
    /**
     * Генерирует случайную позицию вокруг игрока для отображения сообщения
     * @param client Minecraft клиент
     * @return позиция для сообщения
     */
    public static Vec3d generateRandomPosition(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return Vec3d.ZERO;
        }
        
        Random random = client.world.random;
        Vec3d playerPos = client.player.getEyePos();

        // Генерируем случайный угол и радиус
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = 2 + (5 - 2) * random.nextDouble(); // Случайный радиус между 2 и 5
        double xOffset = radius * Math.cos(angle);
        double zOffset = radius * Math.sin(angle);
        double yOffset = (random.nextDouble() * 2 - 1); // Случайное смещение по Y между -1 и 1

        return new Vec3d(
            playerPos.x + xOffset, 
            playerPos.y + client.player.getEyeHeight(client.player.getPose()) + yOffset, 
            playerPos.z + zOffset
        );
    }

    /**
     * Генерирует позицию только в зоне видимости игрока (спереди, в пределах угла обзора)
     * @param client Minecraft клиент
     * @return позиция для сообщения
     */
    public static Vec3d generatePositionInFrontOfPlayer(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return Vec3d.ZERO;
        }
        Random random = client.world.random;
        Vec3d playerEye = client.player.getEyePos();
        float baseYaw = client.player.getYaw();
        // Радиус спавна (как и раньше)
        double radius = 2 + (5 - 2) * random.nextDouble();
        // Получаем FOV игрока (по горизонтали)
        double fov = client.options.getFov().getValue();
        double aspect = (double)client.getWindow().getFramebufferWidth() / (double)client.getWindow().getFramebufferHeight();
        double vertFov = Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(fov / 2)) / aspect));
        // Генерируем случайную точку на экране [-1, 1] по X и Y (экранные нормализованные координаты)
        double screenX = (random.nextDouble() * 2.0) - 1.0;
        double screenY = (random.nextDouble() * 2.0) - 1.0;
        // Переводим экранные координаты в углы смещения
        double halfFovRad = Math.toRadians(fov / 2.0);
        double halfVertFovRad = Math.toRadians(vertFov / 2.0);
        double xAngle = screenX * halfFovRad;
        double yAngle = screenY * halfVertFovRad;
        double yawRad = Math.toRadians(baseYaw) + xAngle;
        double pitchRad = yAngle; // всегда относительно горизонта
        // Переводим сферические координаты в декартовы
        double xOffset = radius * -Math.sin(yawRad) * Math.cos(pitchRad);
        double yOffset = radius * -Math.sin(pitchRad);
        double zOffset = radius * Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3d(
            playerEye.x + xOffset,
            playerEye.y + yOffset,
            playerEye.z + zOffset
        );
    }
} 