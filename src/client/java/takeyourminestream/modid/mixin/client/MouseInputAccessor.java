package takeyourminestream.modid.mixin.client;

import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseInput.class)
public interface MouseInputAccessor {
    @Accessor("button")
    int getButton();
}
