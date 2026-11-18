package takeyourminestream.ijustseen.messages;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/** Конструктор динамической текстуры до 1.21.8 — только изображение. */
final class EmoteTextureFactory {
    private EmoteTextureFactory() {}

    static NativeImageBackedTexture create(String debugName, NativeImage image) {
        return new NativeImageBackedTexture(image);
    }
}
