package takeyourminestream.modid.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import takeyourminestream.modid.TakeYourMineStreamClient;
import takeyourminestream.modid.WorldEventHandler;

/**
 * Mixin для отслеживания выхода из мира
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo ci) {
        // Ставим систему сообщений на паузу при выходе из мира
        var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
        if (messageSpawner != null) {
            WorldEventHandler.onWorldLeave(messageSpawner);
        }
    }
}
