package takeyourminestream.ijustseen.messages;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Отвечает за спавн новых сообщений из очереди
 */
public class MessageSpawner {
    private final MessageQueue messageQueue;
    private final MessageLifecycleManager lifecycleManager;
    private final PinnedMessageInteractionManager pinnedInteractionManager;
    private volatile boolean paused = false;
    private int lastSoundTick = -1;

    public MessageSpawner(MessageQueue messageQueue, MessageLifecycleManager lifecycleManager) {
        this.messageQueue = messageQueue;
        this.lifecycleManager = lifecycleManager;
        this.pinnedInteractionManager = new PinnedMessageInteractionManager(lifecycleManager);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                pinnedInteractionManager.tick(client);
                lifecycleManager.updateMessages(client);
                drainReadyQueue(client);
            }
        });
    }

    private void drainReadyQueue(MinecraftClient client) {
        int tick = lifecycleManager.getTickCounter();
        MessageQueue.QueuedMessage queued;
        while ((queued = messageQueue.pollReady(tick)) != null) {
            spawnQueuedMessage(client, queued, tick);
        }
    }

    private void spawnQueuedMessage(MinecraftClient client, MessageQueue.QueuedMessage queued, int tick) {
        String messageText = queued.text;
        Integer authorColor = queued.authorColorRgb;
        var spawnMode = takeyourminestream.ijustseen.config.ModConfig.getMESSAGE_SPAWN_MODE();

        if (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.HUD_WIDGET) {
            var position = Vec3d.ZERO;
            var message = new Message(
                messageText, position, tick, 0, 0, authorColor, Vec3d.ZERO, queued.emotes
            );
            lifecycleManager.addMessage(message);
        } else {
            var position = (spawnMode == takeyourminestream.ijustseen.config.MessageSpawnMode.FRONT_OF_PLAYER)
                ? MessagePosition.generatePositionInFrontOfPlayer(client)
                : MessagePosition.generateRandomPosition(client);
            var playerEyePos = client.player.getEyePos();
            double dx = playerEyePos.x - position.x;
            double dy = playerEyePos.y - position.y;
            double dz = playerEyePos.z - position.z;
            double distXZ = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) -(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
            var worldOffset = position.subtract(playerEyePos);
            var message = new Message(
                messageText, position, tick, yaw, pitch, authorColor, worldOffset, queued.emotes
            );
            lifecycleManager.addMessage(message);
        }
        playNewMessageSound(tick);
    }

    public void enqueueMessage(String message, Integer authorColorRgb, java.util.List<MessageEmote> emotes, int readyAtTick) {
        if (!paused) {
            messageQueue.enqueueMessage(message, authorColorRgb, emotes, readyAtTick);
        }
    }

    public int getTickCounter() {
        return lifecycleManager.getTickCounter();
    }

    /**
     * Устанавливает новое сообщение для отображения
     * @param message текст сообщения (пустая строка для остановки)
     */
    public void setCurrentMessage(String message) {
        if (message.isEmpty()) {
            messageQueue.clear();
            lifecycleManager.clearAllMessages();
        } else {
            enqueueMessage(message, null, java.util.Collections.emptyList(), getTickCounter());
        }
    }

    public void setCurrentMessage(String message, Integer authorColorRgb) {
        if (message.isEmpty()) {
            messageQueue.clear();
            lifecycleManager.clearAllMessages();
        } else {
            enqueueMessage(message, authorColorRgb, java.util.Collections.emptyList(), getTickCounter());
        }
    }

    public void setCurrentMessage(String message, Integer authorColorRgb, java.util.List<MessageEmote> emotes) {
        if (message.isEmpty()) {
            messageQueue.clear();
            lifecycleManager.clearAllMessages();
        } else {
            enqueueMessage(message, authorColorRgb, emotes, getTickCounter());
        }
    }

    /** Сбрасывает ожидающие сообщения, не трогая уже показанные. */
    public void clearPendingQueue() {
        messageQueue.clear();
    }

    private void playNewMessageSound(int tick) {
        if (tick == lastSoundTick) {
            return;
        }
        lastSoundTick = tick;

        if (!takeyourminestream.ijustseen.config.ModConfig.isENABLE_MESSAGE_SOUND()) {
            return;
        }

        float volume = (float) MathHelper.clamp(takeyourminestream.ijustseen.config.ModConfig.getMESSAGE_SOUND_VOLUME(), 0.0, 1.0);
        if (volume <= 0.0f) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), volume, 1.25f);
        }
    }

    public MessageLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    public PinnedMessageInteractionManager getPinnedInteractionManager() {
        return pinnedInteractionManager;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }
}
