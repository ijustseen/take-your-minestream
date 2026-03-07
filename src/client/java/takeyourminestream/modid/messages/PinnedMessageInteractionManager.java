package takeyourminestream.modid.messages;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

/**
 * Управляет взаимодействием с закреплёнными сообщениями: закрепление и перетаскивание ПКМ.
 */
public class PinnedMessageInteractionManager {
    private final MessageLifecycleManager lifecycleManager;

    private Message draggedMessage;
    private double dragDistance;
    private boolean rightMouseDown;

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
        if (message.isPinned() && MessageClickHandler.isHitOnPinIcon(hit)) {
            lifecycleManager.unpinMessage(message, client);
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
            draggedMessage = null;
            rightMouseDown = false;
            return true;
        }

        if (!message.isPinned()) {
            message.setPinned(true);
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
        }

        draggedMessage = message;
        dragDistance = Math.max(1.0, hit.getDistanceAlongRay());
        rightMouseDown = true;
        return true;
    }

    public void onRightMouseReleased() {
        if (rightMouseDown && draggedMessage != null) {
            PinnedMessageStore.saveForCurrentWorld(lifecycleManager);
        }
        rightMouseDown = false;
        draggedMessage = null;
    }

    public void tick(MinecraftClient client) {
        if (!rightMouseDown || draggedMessage == null) {
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            onRightMouseReleased();
            return;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
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
}
