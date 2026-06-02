package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.config.MessageSpawnMode;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;

/** Действия над сообщением из экрана истории. */
public final class MessageHistoryActions {
    public enum PinToggleResult { PINNED, UNPINNED, FAILED }

    private MessageHistoryActions() {}

    public static PinToggleResult togglePin(Message historySource, MessageLifecycleManager lifecycleManager, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || historySource == null) {
            return PinToggleResult.FAILED;
        }

        Message pinnedCopy = lifecycleManager.findPinnedForHistory(historySource);
        if (pinnedCopy != null) {
            lifecycleManager.removeMessageWithoutParticles(pinnedCopy);
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
            return PinToggleResult.UNPINNED;
        }

        if (lifecycleManager.getActiveMessages().contains(historySource)) {
            if (historySource.isPinned()) {
                lifecycleManager.unpinMessage(historySource, client);
                PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
                return PinToggleResult.UNPINNED;
            }
            historySource.setPinned(true);
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
            return PinToggleResult.PINNED;
        }

        Message pinned = createWorldMessageFromSource(historySource, lifecycleManager, client, true);
        pinned.setHistorySourceId(historySource.getId());
        lifecycleManager.addPinnedMessage(pinned);
        PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
        return PinToggleResult.PINNED;
    }

    public static boolean replay(Message historySource, MessageLifecycleManager lifecycleManager, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || historySource == null) {
            return false;
        }

        Message replayed = createWorldMessageFromSource(historySource, lifecycleManager, client, false);
        lifecycleManager.addReplayMessage(replayed);
        return true;
    }

    public static boolean blockUser(String username, BlockedUsernameManager blockedUsernameManager) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String normalized = username.trim().toLowerCase();
        if (blockedUsernameManager.isBlocked(normalized, normalized)) {
            return false;
        }
        blockedUsernameManager.addBlockedUsername(normalized);
        if (!ModConfig.isENABLE_USERNAME_BLOCKLIST()) {
            ModConfig.setENABLE_USERNAME_BLOCKLIST(true);
        }
        return true;
    }

    static Message createWorldMessageFromSource(
        Message source,
        MessageLifecycleManager lifecycleManager,
        MinecraftClient client,
        boolean pinned
    ) {
        MessageSpawnMode spawnMode = ModConfig.getMESSAGE_SPAWN_MODE();
        Vec3d position;
        float yaw;
        float pitch;
        Vec3d offset = Vec3d.ZERO;

        if (spawnMode == MessageSpawnMode.HUD_WIDGET) {
            position = Vec3d.ZERO;
            yaw = 0.0f;
            pitch = 0.0f;
        } else {
            position = MessagePosition.generatePositionInFrontOfPlayer(client);
            Vec3d eyePos = client.player.getEyePos();
            double dx = eyePos.x - position.x;
            double dy = eyePos.y - position.y;
            double dz = eyePos.z - position.z;
            double distXZ = Math.sqrt(dx * dx + dz * dz);
            yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            pitch = (float) -(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
            offset = position.subtract(eyePos);
        }

        Message message = new Message(
            source.getText(),
            position,
            lifecycleManager.getTickCounter(),
            yaw,
            pitch,
            source.getAuthorColorRgb(),
            offset,
            source.getEmotes()
        );
        message.setPinned(pinned);
        return message;
    }
}
