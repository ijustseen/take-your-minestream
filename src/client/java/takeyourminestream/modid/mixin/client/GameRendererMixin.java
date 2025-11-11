package takeyourminestream.modid.mixin.client;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import takeyourminestream.modid.rendering.CustomRenderPipeline;

/**
 * Mixin для очистки ресурсов кастомного рендер-пайплайна
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    
    @Inject(method = "close", at = @At("TAIL"))
    private void onClose(CallbackInfo ci) {
        CustomRenderPipeline instance = CustomRenderPipeline.getInstance();
        if (instance != null) {
            instance.close();
        }
    }
}
