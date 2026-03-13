package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.sound.SoundEvents;

/**
 * Отвечает за спавн новых сообщений из очереди
 */
public class MessageSpawner {
    private final MessageQueue messageQueue;
    private final MessageLifecycleManager lifecycleManager;
    private final PinnedMessageInteractionManager pinnedInteractionManager;
    private volatile boolean paused = false;
    
    public MessageSpawner(MessageQueue messageQueue, MessageLifecycleManager lifecycleManager) {
        this.messageQueue = messageQueue;
        this.lifecycleManager = lifecycleManager;
        this.pinnedInteractionManager = new PinnedMessageInteractionManager(lifecycleManager);
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                pinnedInteractionManager.tick(client);
                // Обновляем жизненный цикл существующих сообщений
                lifecycleManager.updateMessages(client);
                
                // Проверяем, можно ли спавнить новое сообщение
                if (messageQueue.canSpawnMessage(lifecycleManager.getTickCounter())) {
                    var queued = messageQueue.dequeueMessage(lifecycleManager.getTickCounter());
                    if (queued != null) {
                        String messageText = queued.text;
                        Integer authorColor = queued.authorColorRgb;
                        var spawnMode = takeyourminestream.ijustseen.ModConfig.getMESSAGE_SPAWN_MODE();
                        
                        // В HUD режиме создаем сообщение без 3D позиции
                        if (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.HUD_WIDGET) {
                            // Для HUD режима создаем сообщение с нулевой позицией
                            var position = net.minecraft.util.math.Vec3d.ZERO;
                            var message = new Message(messageText, position, lifecycleManager.getTickCounter(), 0, 0, authorColor, net.minecraft.util.math.Vec3d.ZERO, queued.emotes);
                            lifecycleManager.addMessage(message);
                        } else {
                            // Для 3D режимов генерируем позицию в пространстве
                            var position = (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.FRONT_OF_PLAYER)
                                ? MessagePosition.generatePositionInFrontOfPlayer(client)
                                : MessagePosition.generateRandomPosition(client);
                            // Вычисляем yaw/pitch на игрока
                            var playerEyePos = client.player.getEyePos();
                            double dx = playerEyePos.x - position.x;
                            double dy = playerEyePos.y - position.y;
                            double dz = playerEyePos.z - position.z;
                            double distXZ = Math.sqrt(dx * dx + dz * dz);
                            float yaw = (float)(MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                            float pitch = (float)-(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
                            // Фиксированное мировое смещение от глаз игрока (НЕ зависит от взгляда)
                            var worldOffset = position.subtract(playerEyePos);
                            var message = new Message(messageText, position, lifecycleManager.getTickCounter(), yaw, pitch, authorColor, worldOffset, queued.emotes);
                            lifecycleManager.addMessage(message);
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Устанавливает новое сообщение для отображения
     * @param message текст сообщения (пустая строка для остановки)
     */
    public void setCurrentMessage(String message) {
        if (message.isEmpty()) {
            // Очищаем очередь и все активные сообщения
            messageQueue.clear();
            lifecycleManager.clearAllMessages();
        } else {
            // Добавляем сообщение в очередь
            messageQueue.enqueueMessage(message);
            playNewMessageSound();
        }
    }

    public void setCurrentMessage(String message, Integer authorColorRgb) {
        if (message.isEmpty()) {
            messageQueue.clear();
            lifecycleManager.clearAllMessages();
        } else {
            // Не добавляем сообщения, если система на паузе
            if (!paused) {
                messageQueue.enqueueMessage(message, authorColorRgb);
                playNewMessageSound();
            }
        }
    }

    public void setCurrentMessage(String message, Integer authorColorRgb, java.util.List<MessageEmote> emotes) {
        if (message.isEmpty()) {
            messageQueue.clear();
            lifecycleManager.clearAllMessages();
        } else {
            if (!paused) {
                messageQueue.enqueueMessage(message, authorColorRgb, emotes);
                playNewMessageSound();
            }
        }
    }

    private void playNewMessageSound() {
        if (!takeyourminestream.ijustseen.ModConfig.isENABLE_MESSAGE_SOUND()) {
            return;
        }

        float volume = (float) MathHelper.clamp(takeyourminestream.ijustseen.ModConfig.getMESSAGE_SOUND_VOLUME(), 0.0, 1.0);
        if (volume <= 0.0f) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), volume, 1.25f);
            }
        });
    }
    
    /**
     * Возвращает менеджер жизненного цикла для доступа к активным сообщениям
     * @return менеджер жизненного цикла
     */
    public MessageLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    public PinnedMessageInteractionManager getPinnedInteractionManager() {
        return pinnedInteractionManager;
    }
    
    /**
     * Ставит систему сообщений на паузу
     * Новые сообщения не будут добавляться в очередь
     */
    public void pause() {
        paused = true;
    }
    
    /**
     * Снимает систему сообщений с паузы
     * Новые сообщения снова будут добавляться в очередь
     */
    public void resume() {
        paused = false;
    }
    
    /**
     * Проверяет, находится ли система на паузе
     * @return true если на паузе
     */
    public boolean isPaused() {
        return paused;
    }
} 