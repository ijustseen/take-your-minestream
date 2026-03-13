package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import takeyourminestream.ijustseen.ModConfig;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;

/**
 * Управляет жизненным циклом сообщений
 */
public class MessageLifecycleManager {
    private final List<Message> activeMessages = new ArrayList<>();
    private final List<Message> messageHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100; // Максимальное количество сообщений в истории
    private int tickCounter = 0;
    private final MessageParticleManager particleManager;
    private final Set<Message> spawnedParticlesForMessages = new HashSet<>();
    
    public MessageLifecycleManager(MessageParticleManager particleManager) {
        this.particleManager = particleManager;
    }
    
    /**
     * Обновляет состояние всех активных сообщений
     * @param client Minecraft клиент
     */
    public void updateMessages(MinecraftClient client) {
        if (particleManager != null) {
            particleManager.tick();
        }
        if (client.player == null || client.world == null) return;
        
        tickCounter++;
        // Плавное следование за игроком (только для 3D режимов)
        if (takeyourminestream.ijustseen.ModConfig.isFOLLOW_PLAYER()
                && takeyourminestream.ijustseen.ModConfig.getMESSAGE_SPAWN_MODE() != takeyourminestream.ijustseen.config.MessageSpawnMode.HUD_WIDGET) {
            var eyePos = client.player.getEyePos();
            // Время сглаживания (в тиках)
            float positionSmoothTime = 14.0f;   // больше инерции
            float yawSmoothTime = 12.0f;
            float pitchSmoothTime = 12.0f;

            // Коэффициенты экспон. сглаживания на тик
            float positionLerp = clamp01(1.0f - (float)Math.exp(-1.0f / positionSmoothTime));
            float yawLerp = clamp01(1.0f - (float)Math.exp(-1.0f / yawSmoothTime));
            float pitchLerp = clamp01(1.0f - (float)Math.exp(-1.0f / pitchSmoothTime));

            for (Message message : activeMessages) {
                if (message.isPinned()) {
                    continue;
                }
                Vec3d local = message.getTargetOffsetFromEye();
                if (local == null) continue; // для HUD или старых сообщений

                // Целевая мировая позиция = глаза игрока + фиксированное мировое смещение (НЕ зависящее от yaw)
                Vec3d targetWorld = eyePos.add(local);

                // Плавное сближение к цели с нарастающей/затухающей скоростью (без клипа скорости)
                Vec3d currentPos = message.getPosition();
                Vec3d toTarget = targetWorld.subtract(currentPos);
                Vec3d newPos = currentPos.add(toTarget.multiply(positionLerp));
                message.setPosition(newPos);

                // Ориентация: не мгновенно, а сглаженно смотрит на глаза игрока
                double dx = eyePos.x - newPos.x;
                double dy = eyePos.y - newPos.y;
                double dz = eyePos.z - newPos.z;
                double distXZ = Math.sqrt(dx * dx + dz * dz);
                float targetYaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                float targetPitch = (float)-(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
                message.setYaw(lerpAngleDegrees(message.getYaw(), targetYaw, yawLerp));
                message.setPitch(lerpAngleDegrees(message.getPitch(), targetPitch, pitchLerp));
            }
        }
        
        // Обновляем замороженное время для всех сообщений
        if (ModConfig.isENABLE_FREEZING_ON_VIEW()) {
            for (Message message : activeMessages) {
                if (message.isPinned()) {
                    continue;
                }
                if (MessageViewDetector.isPlayerLookingAtMessage(client, message.getPosition(), message.getYaw(), message.getPitch(), message.getText(), 
                                                               tickCounter, message.getSpawnTick(), message.getFrozenTicks())) {
                    message.incrementFrozenTicks();
                }
            }
        }
        
        // Удаляем старые сообщения с учетом замороженного времени
        int fallTicks = ModConfig.getMESSAGE_FALL_TICKS();
        int removeAfter = ModConfig.getMESSAGE_LIFETIME_TICKS() + fallTicks;
        if (ModConfig.isENABLE_FREEZING_ON_VIEW()) {
            activeMessages.removeIf(message -> {
                if (message.isPinned()) {
                    return false;
                }
                int effectiveAge = message.getEffectiveAge(tickCounter);
                // Спавним партиклы за тик до удаления
                if (effectiveAge == removeAfter - 1 && !spawnedParticlesForMessages.contains(message)) {
                    if (particleManager != null) {
                        Vec3d finalPos = MessageViewDetector.calculateFallingPosition(
                            message.getPosition(),
                            effectiveAge,
                            message.getYaw(),
                            message.getPitch()
                        );
                        MessageParticleSpawner.spawnParticlesForMessage(message, particleManager, client, finalPos);
                        spawnedParticlesForMessages.add(message);
                    }
                }
                if (effectiveAge > removeAfter) {
                    spawnedParticlesForMessages.remove(message);
                    return true;
                }
                return false;
            });
        } else {
            activeMessages.removeIf(message -> {
                if (message.isPinned()) {
                    return false;
                }
                int effectiveAge = tickCounter - (int)message.getSpawnTick();
                // Спавним партиклы за тик до удаления
                if (effectiveAge == removeAfter - 1 && !spawnedParticlesForMessages.contains(message)) {
                    if (particleManager != null) {
                        Vec3d finalPos = MessageViewDetector.calculateFallingPosition(
                            message.getPosition(),
                            effectiveAge,
                            message.getYaw(),
                            message.getPitch()
                        );
                        MessageParticleSpawner.spawnParticlesForMessage(message, particleManager, client, finalPos);
                        spawnedParticlesForMessages.add(message);
                    }
                }
                if (effectiveAge > removeAfter) {
                    spawnedParticlesForMessages.remove(message);
                    return true;
                }
                return false;
            });
        }
    }
    
    /**
     * Добавляет новое сообщение в список активных
     * @param message сообщение для добавления
     */
    public void addMessage(Message message) {
        activeMessages.add(message);
        
        // Добавляем в историю
        messageHistory.add(message);
        
        // Ограничиваем размер истории
        while (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.remove(0);
        }
    }
    
    /**
     * Очищает все активные сообщения
     */
    public void clearAllMessages() {
        activeMessages.clear();
        tickCounter = 0;
    }

    public void clearTransientMessages() {
        activeMessages.removeIf(message -> !message.isPinned());
        spawnedParticlesForMessages.removeIf(message -> !message.isPinned());
    }
    
    /**
     * Возвращает список активных сообщений
     * @return список активных сообщений
     */
    public List<Message> getActiveMessages() {
        return activeMessages;
    }
    
    /**
     * Возвращает текущий счетчик тиков
     * @return текущий счетчик тиков
     */
    public int getTickCounter() {
        return tickCounter;
    }
    
    /**
     * Возвращает все сообщения из истории
     * @return список всех сообщений в истории
     */
    public List<Message> getAllMessages() {
        return new ArrayList<>(messageHistory);
    }
    
    /**
     * Возвращает размер истории сообщений
     * @return количество сообщений в истории
     */
    public int getMessageHistorySize() {
        return messageHistory.size();
    }
    
    /**
     * Очищает историю сообщений
     */
    public void clearMessageHistory() {
        messageHistory.clear();
    }
    
    /**
     * Удаляет сообщение с немедленным спавном партиклов
     * @param message Сообщение для удаления
     * @param client Minecraft клиент
     */
    public void removeMessageWithParticles(Message message, MinecraftClient client) {
        if (activeMessages.contains(message)) {
            // Спавним партиклы на текущей позиции
            if (particleManager != null) {
                MessageParticleSpawner.spawnParticlesForMessage(message, particleManager, client, message.getPosition());
            }
            
            // Удаляем сообщение
            activeMessages.remove(message);
            spawnedParticlesForMessages.remove(message);
        }
    }

    public void removeMessageWithoutParticles(Message message) {
        activeMessages.remove(message);
        spawnedParticlesForMessages.remove(message);
    }

    public void addPinnedMessage(Message message) {
        message.setPinned(true);
        activeMessages.add(message);
    }

    public Message unpinMessage(Message message, MinecraftClient client) {
        if (message == null || !activeMessages.contains(message)) {
            return null;
        }

        Vec3d targetOffset = Vec3d.ZERO;
        if (client != null && client.player != null) {
            targetOffset = message.getPosition().subtract(client.player.getEyePos());
        }

        Message replacement = new Message(
            message.getText(),
            message.getPosition(),
            tickCounter,
            message.getYaw(),
            message.getPitch(),
            message.getAuthorColorRgb(),
            targetOffset,
            message.getEmotes()
        );
        replacement.setPinned(false);

        int idx = activeMessages.indexOf(message);
        activeMessages.set(idx, replacement);
        spawnedParticlesForMessages.remove(message);
        return replacement;
    }

    private static float lerpAngleDegrees(float a, float b, float t) {
        float delta = MathHelper.wrapDegrees(b - a);
        return a + delta * MathHelper.clamp(t, 0.0f, 1.0f);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }
} 