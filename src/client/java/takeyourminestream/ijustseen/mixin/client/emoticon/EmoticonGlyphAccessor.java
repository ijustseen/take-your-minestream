package takeyourminestream.ijustseen.mixin.client.emoticon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xeed.mc.streamotes.EmoticonGlyph;
import xeed.mc.streamotes.emoticon.Emoticon;

@Mixin(EmoticonGlyph.class)
@Environment(EnvType.CLIENT)
public interface EmoticonGlyphAccessor {
    @Accessor("x")
    float minestream$getX();

    @Accessor("icon")
    Emoticon minestream$getEmoticon();
}
