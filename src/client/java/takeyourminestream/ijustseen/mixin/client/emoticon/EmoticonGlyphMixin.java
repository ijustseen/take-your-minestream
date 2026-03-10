package takeyourminestream.ijustseen.mixin.client.emoticon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import xeed.mc.streamotes.EmoticonGlyph;

import java.util.Objects;

@Mixin(targets = "xeed/mc/streamotes/EmoticonGlyph$EmoticonDrawable")
@Environment(EnvType.CLIENT)
public class EmoticonGlyphMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lxeed/mc/streamotes/EmoticonGlyph;x:F",
                    opcode = Opcodes.GETFIELD
            )
    )
    private float minestream$zerowidthGlyphRenderModifier(EmoticonGlyph instance) {
        if (((EmoticonGlyphAccessor)instance).minestream$getEmoticon().getChatRenderWidth() == 0.0F) {
            float x = ((EmoticonGlyphAccessor)instance).minestream$getX();
            MinecraftClient client = MinecraftClient.getInstance();
            Objects.requireNonNull(client.textRenderer);
            float height = (float)(9.0 + (Double)client.options.getChatLineSpacing().getValue() * 8.0);
            float width = ((EmoticonGlyphAccessor)instance).minestream$getEmoticon().getRenderWidth(height);
            return x - width;
        }
        return ((EmoticonGlyphAccessor)instance).minestream$getX();
    }
}
