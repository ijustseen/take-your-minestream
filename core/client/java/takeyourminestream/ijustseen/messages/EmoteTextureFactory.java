package takeyourminestream.ijustseen.messages;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/** Конструктор динамической текстуры: с 1.21.8 нужен debug-name. */
final class EmoteTextureFactory {
    private EmoteTextureFactory() {}

    static NativeImageBackedTexture create(String debugName, NativeImage image) {
        return new NativeImageBackedTexture(() -> debugName, image);
    }
}
