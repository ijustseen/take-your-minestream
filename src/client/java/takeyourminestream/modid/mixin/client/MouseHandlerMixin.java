package takeyourminestream.modid.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import takeyourminestream.modid.messages.Message;
import takeyourminestream.modid.messages.MessageClickHandler;
import takeyourminestream.modid.TakeYourMineStreamClient;

import java.util.ArrayList;

/**
 * Mixin для обработки кликов по сообщениям
 */
@Mixin(Mouse.class)
public class MouseHandlerMixin {
    @Shadow @Final private MinecraftClient client;
    
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        // Проверяем, что игрок в игре
        if (client.player == null || client.world == null) {
            return;
        }
        
        // Проверяем, что не открыт GUI
        if (client.currentScreen != null) {
            return;
        }
        
        // Получаем систему сообщений
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
                // Разрешаем интеракцию с сообщениями только с пустой главной рукой
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

        // Проверяем, включена ли функция удаления кликом
        if (!takeyourminestream.modid.ModConfig.isENABLE_CLICK_TO_REMOVE()) {
            return;
        }

        // Проверяем только левую кнопку мыши (button == 0) и нажатие (action == 1)
        if (button != 0 || action != 1) {
            return;
        }

        // Разрешаем удаление сообщений только с пустой главной рукой
        if (!client.player.getMainHandStack().isEmpty()) {
            return;
        }
        
        // Проверяем каждое активное сообщение
        for (Message message : new ArrayList<>(lifecycleManager.getActiveMessages())) {
            if (message.isPinned()) {
                continue;
            }
            if (MessageClickHandler.isClickOnMessage(client, message, lifecycleManager.getTickCounter())) {
                // Удаляем сообщение с партиклами
                lifecycleManager.removeMessageWithParticles(message, client);
                
                // Отменяем обычный клик (чтобы не ломать блоки)
                ci.cancel();
                return;
            }
        }
    }
}
