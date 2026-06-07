package takeyourminestream.ijustseen.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.config.UnpinMode;
import takeyourminestream.ijustseen.utils.CameraPositionCompat;

import java.util.ArrayList;

/**
 * Управляет взаимодействием с закреплёнными сообщениями: закрепление и перетаскивание ПКМ.
 */
public class PinnedMessageInteractionManager {
    /** Короткий клик vs удержание ПКМ в режиме «всё сообщение». */
    private static final long WHOLE_MESSAGE_CLICK_THRESHOLD_MS = 200L;

    private final MessageLifecycleManager lifecycleManager;

    private Message draggedMessage;
    private double dragDistance;
    private boolean rightMouseDown;
    private boolean wholeMessageUnpinPending;
    private long wholeMessagePressTimeMs;

    public PinnedMessageInteractionManager(MessageLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    public boolean onRightMousePressed(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return false;
        }

        MessageClickHandler.MessageHit hit = MessageClickHandler.findClosestMessageHit(
            client,
            new ArrayList<>(lifecycleManager.getActiveMessages()),
            lifecycleManager.getTickCounter()
        );

        if (hit == null) {
            return false;
        }

        Message message = hit.getMessage();
        boolean wasPinned = message.isPinned();

        if (wasPinned && shouldUnpinImmediatelyOnPress(hit)) {
            lifecycleManager.unpinMessage(message, client);
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
            clearInteractionState();
            return true;
        }

        if (!wasPinned) {
            message.setPinned(true);
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
        }

        draggedMessage = message;
        dragDistance = Math.max(1.0, hit.getDistanceAlongRay());
        rightMouseDown = true;
        wholeMessageUnpinPending = wasPinned && ModConfig.getUNPIN_MODE() == UnpinMode.WHOLE_MESSAGE;
        wholeMessagePressTimeMs = System.currentTimeMillis();
        return true;
    }

    public void onRightMouseReleased() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (wholeMessageUnpinPending && draggedMessage != null && draggedMessage.isPinned() && client != null) {
            long heldMs = System.currentTimeMillis() - wholeMessagePressTimeMs;
            if (heldMs <= WHOLE_MESSAGE_CLICK_THRESHOLD_MS) {
                lifecycleManager.unpinMessage(draggedMessage, client);
            }
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
        } else if (rightMouseDown && draggedMessage != null) {
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
        }

        clearInteractionState();
    }

    public void tick(MinecraftClient client) {
        if (!rightMouseDown || draggedMessage == null) {
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            onRightMouseReleased();
            return;
        }

        Vec3d cameraPos = CameraPositionCompat.getCameraPos(client);
        Vec3d lookVec = client.player.getRotationVec(1.0f).normalize();
        Vec3d newPos = cameraPos.add(lookVec.multiply(dragDistance));
        draggedMessage.setPosition(newPos);

        Vec3d eyePos = client.player.getEyePos();
        double dx = eyePos.x - newPos.x;
        double dy = eyePos.y - newPos.y;
        double dz = eyePos.z - newPos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) -(MathHelper.atan2(dy, distXZ) * (180.0 / Math.PI));
        draggedMessage.setYaw(targetYaw);
        draggedMessage.setPitch(targetPitch);
    }

    private static boolean shouldUnpinImmediatelyOnPress(MessageClickHandler.MessageHit hit) {
        return ModConfig.getUNPIN_MODE() == UnpinMode.PIN_ICON && MessageClickHandler.isHitOnPinIcon(hit);
    }

    private void clearInteractionState() {
        rightMouseDown = false;
        draggedMessage = null;
        wholeMessageUnpinPending = false;
        wholeMessagePressTimeMs = 0L;
    }
}
