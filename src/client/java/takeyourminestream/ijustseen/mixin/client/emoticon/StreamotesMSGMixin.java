package takeyourminestream.ijustseen.mixin.client.emoticon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xeed.mc.streamotes.Streamotes;

@Mixin(Streamotes.class)
@Environment(EnvType.CLIENT)
public class StreamotesMSGMixin {

    @Redirect(
            method = "msg",
            at = @At(
                    value = "INVOKE",
                    target= "Lnet/minecraft/client/toast/ToastManager;add(Lnet/minecraft/client/toast/Toast;)V"
            )
    )
    private static void minestream$streamotesMsgToastFix(ToastManager instance, Toast toast) {
        MinecraftClient.getInstance().execute(
                () -> instance.add(toast)
        );
    }

    @Redirect(
            method = "msg",
            at = @At(
                    value = "INVOKE",
                    target= "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;)V"
            )
    )
    private static void minestream$streamotesMsgChatFix(ChatHud instance, Text message) {
        MinecraftClient.getInstance().execute(
                () -> instance.addMessage(message)
        );
    }
}
