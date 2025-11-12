package takeyourminestream.modid.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import takeyourminestream.modid.TakeYourMineStreamClient;
import takeyourminestream.modid.WorldEventHandler;

/**
 * Mixin для отслеживания входа в мир
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    
    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        // Снимаем систему сообщений с паузы при входе в мир
        var messageSpawner = TakeYourMineStreamClient.getStaticMessageSpawner();
        if (messageSpawner != null) {
            WorldEventHandler.onWorldJoin(messageSpawner);
        }
    }
}
