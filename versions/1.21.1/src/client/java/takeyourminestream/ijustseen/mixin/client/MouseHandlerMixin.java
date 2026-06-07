package takeyourminestream.ijustseen.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageClickHandler;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;

import java.util.ArrayList;

/** Mixin для обработки кликов по сообщениям (Minecraft 1.21.8). */
@Mixin(Mouse.class)
public class MouseHandlerMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
        if (messageSpawner == null) {
            return;
        }

        var lifecycleManager = messageSpawner.getLifecycleManager();
        if (lifecycleManager == null) {
            return;
        }

        if (button == 1) {
            var interactionManager = messageSpawner.getPinnedInteractionManager();
            if (interactionManager == null) {
                return;
            }

            if (action == 1) {
                if (!client.player.getMainHandStack().isEmpty()) {
                    return;
                }
                if (interactionManager.onRightMousePressed(client)) {
                    ci.cancel();
                }
                return;
            }

            if (action == 0) {
                interactionManager.onRightMouseReleased();
            }
            return;
        }

        if (!takeyourminestream.ijustseen.config.ModConfig.isENABLE_CLICK_TO_REMOVE()) {
            return;
        }

        if (button != 0 || action != 1) {
            return;
        }

        if (!client.player.getMainHandStack().isEmpty()) {
            return;
        }

        for (Message message : new ArrayList<>(lifecycleManager.getActiveMessages())) {
            if (message.isPinned()) {
                continue;
            }
            if (MessageClickHandler.isClickOnMessage(client, message, lifecycleManager.getTickCounter())) {
                lifecycleManager.removeMessageWithParticles(message, client);
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                player.swingHand(player.getActiveHand());
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
                ci.cancel();
                return;
            }
        }
    }
}
