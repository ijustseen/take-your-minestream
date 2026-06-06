package takeyourminestream.ijustseen.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.messages.Message;
import takeyourminestream.ijustseen.messages.MessageClickHandler;

import java.util.ArrayList;

/** Mixin для обработки кликов по сообщениям (Minecraft 26.1). */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        int button = input.button();

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (minecraft.screen != null) {
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
                if (!minecraft.player.getMainHandItem().isEmpty()) {
                    return;
                }
                if (interactionManager.onRightMousePressed(minecraft)) {
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

        if (!minecraft.player.getMainHandItem().isEmpty()) {
            return;
        }

        for (Message message : new ArrayList<>(lifecycleManager.getActiveMessages())) {
            if (message.isPinned()) {
                continue;
            }
            if (MessageClickHandler.isClickOnMessage(minecraft, message, lifecycleManager.getTickCounter())) {
                lifecycleManager.removeMessageWithParticles(message, minecraft);
                LocalPlayer player = Minecraft.getInstance().player;
                player.swing(player.getUsedItemHand());
                player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
                ci.cancel();
                return;
            }
        }
    }
}
