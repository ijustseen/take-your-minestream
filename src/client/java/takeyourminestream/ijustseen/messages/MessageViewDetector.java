package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.core.MessagePanelConstants;

import java.util.List;

/**
 * Отвечает за определение, смотрит ли игрок на сообщение
 */
public class MessageViewDetector {
    
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 4;
    private static final float BASE_MESSAGE_SCALE = 0.025f;
    private static final float MAX_FALL = 20.0f; // То же значение, что и в MessageRenderer
    
    /**
     * Проверяет, смотрит ли игрок на сообщение
     * @param client Minecraft клиент
     * @param messagePosition позиция сообщения
     * @param messageYaw поворот сообщения по Y
     * @param messagePitch поворот сообщения по X
     * @param messageText текст сообщения для вычисления размеров
     * @param tickCounter текущий счетчик тиков
     * @param messageSpawnTick тик создания сообщения
     * @param frozenTicks количество замороженных тиков
     * @return true, если игрок смотрит на сообщение
     */
    public static boolean isPlayerLookingAtMessage(MinecraftClient client, Vec3d messagePosition, 
                                                  float messageYaw, float messagePitch, String messageText,
                                                  int tickCounter, long messageSpawnTick, int frozenTicks) {
        if (client.player == null) return false;
        
        // Вычисляем эффективный возраст сообщения (с учетом заморозки)
        int effectiveAge = tickCounter - (int)messageSpawnTick - frozenTicks;
        
        // Проверяем, не истек ли срок жизни сообщения
        if (effectiveAge >= ModConfig.getMESSAGE_LIFETIME_TICKS() + ModConfig.getMESSAGE_FALL_TICKS()) {
            return false; // Сообщение уже "разбилось"
        }
        
        // Вычисляем смещение при падении: центр панели уезжает вдоль локальной оси Y панели
        Vec3d adjustedCenter = calculateFallingPosition(messagePosition, effectiveAge, messageYaw, messagePitch);
        
        // Позиция глаз игрока и направление взгляда
        Vec3d playerEyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        
        // Проверяем дистанцию
        double distanceToMessage = playerEyePos.distanceTo(adjustedCenter);
        if (distanceToMessage > ModConfig.getMAX_FREEZE_DISTANCE()) return false;
        
        // Вычисляем размеры сообщения в блоках (как при рендере)
        TextRenderer textRenderer = client.textRenderer;
        List<OrderedText> wrappedText = textRenderer.wrapLines(
            Text.of(messageText),
            MessagePanelConstants.MESSAGE_WRAP_WIDTH
        );
        float totalTextHeight = wrappedText.size() * textRenderer.fontHeight;
        int maxTextWidth = 0;
        for (OrderedText line : wrappedText) {
            int w = textRenderer.getWidth(line);
            if (w > maxTextWidth) maxTextWidth = w;
        }
        int panelWidth = maxTextWidth + PANEL_PADDING_X * 2;
        int panelHeight = (int)totalTextHeight + PANEL_PADDING_Y * 2;
        float configScale = ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = BASE_MESSAGE_SCALE * configScale;
        double messageWidthBlocks = panelWidth * finalScale;
        double messageHeightBlocks = panelHeight * finalScale;
        
        // Проверяем пересечение луча с плоскостью сообщения в локальных координатах панели
        return rayHitsMessageRectLocal(playerEyePos, lookVec, adjustedCenter, messageYaw, messagePitch,
                                       messageWidthBlocks, messageHeightBlocks);
    }
    
    /**
     * Вычисляет позицию центра панели с учетом падения.
     * Смещение происходит вдоль локальной оси Y панели, чтобы совпадать с тем, как она двигается при рендере.
     */
    public static Vec3d calculateFallingPosition(Vec3d basePosition, int effectiveAge, float yaw, float pitch) {
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int fallStart = ModConfig.getMESSAGE_LIFETIME_TICKS();
        int fallAge = effectiveAge - fallStart;
        
        if (fallAge < 0) {
            return basePosition;
        }
        if (fallAge >= fallTicks) {
            return basePosition;
        }
        
        float fallProgress = (float)fallAge / (float)fallTicks;
        fallProgress = Math.min(Math.max(fallProgress, 0.0f), 1.0f);
        float fallOffsetYpx = (fallProgress * fallProgress) * MAX_FALL;
        
        float configScale = ModConfig.getMESSAGE_SCALE().getScale();
        float finalScale = BASE_MESSAGE_SCALE * configScale;
        float fallOffsetBlocks = fallOffsetYpx * finalScale;
        
        // Локальная ось Y панели после поворотов рендера: сначала yaw (-yaw в рендере не влияет на ось Y), затем pitch
        double pitchRad = Math.toRadians(pitch);
        // Ось Y после поворота вокруг X на +pitch: (0, cos(pitch), sin(pitch))
        Vec3d localYAxisWorld = new Vec3d(0.0, Math.cos(pitchRad), Math.sin(pitchRad));
        
        // В рендере используется отрицательный масштаб по Y, поэтому положительное локальное смещение по Y визуально двигает вниз по миру.
        // Чтобы совпасть, сдвигаем центр в противоположную сторону от локальной оси Y.
        Vec3d worldOffset = localYAxisWorld.multiply(-fallOffsetBlocks);
        return basePosition.add(worldOffset);
    }
    
    /**
     * Пересекает луч взгляда с плоскостью панели, вычисляя всё в локальных координатах панели.
     * Возвращает true, если пересечение попадает в прямоугольник панели.
     */
    private static boolean rayHitsMessageRectLocal(Vec3d rayOrigin, Vec3d rayDir, Vec3d panelCenter,
                                                   float panelYaw, float panelPitch,
                                                   double rectWidth, double rectHeight) {
        // Сдвигаем начало луча в систему координат с центром в центре панели
        Vec3d origin = rayOrigin.subtract(panelCenter);
        Vec3d dir = rayDir;
        
        double yawRad = Math.toRadians(panelYaw);
        double pitchRad = Math.toRadians(panelPitch);
        
        // Применяем обратные повороты рендера: сначала -pitch вокруг X, затем +yaw вокруг Y
        origin = rotateX(origin, -pitchRad);
        origin = rotateY(origin, +yawRad);
        dir = rotateX(dir, -pitchRad);
        dir = rotateY(dir, +yawRad);
        
        // Плоскость панели в локале: z = 0 (нормаль (0,0,1) или (0,0,-1) не важно для пересечения)
        double denom = dir.z;
        if (Math.abs(denom) < 1e-6) return false;
        double t = -origin.z / denom;
        if (t < 0) return false;
        
        Vec3d hitLocal = origin.add(dir.multiply(t));
        
        // Проверяем попадание в прямоугольник панели (центрирован относительно (0,0))
        return Math.abs(hitLocal.x) <= rectWidth / 2.0 && Math.abs(hitLocal.y) <= rectHeight / 2.0;
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