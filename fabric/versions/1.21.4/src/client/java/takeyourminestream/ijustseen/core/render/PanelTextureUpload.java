package takeyourminestream.ijustseen.core.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Заливает ARGB-буфер в динамическую текстуру. До 1.21.8 setColor ждёт ABGR. */
public final class PanelTextureUpload {
    private PanelTextureUpload() {}

    public static void upload(Identifier id, int[] argb, int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || argb == null || size <= 0) {
            return;
        }
        NativeImage image = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setColorArgb(x, y, argb[y * size + x]);
            }
        }
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        client.getTextureManager().destroyTexture(id);
        client.getTextureManager().registerTexture(id, texture);
        texture.upload();
    }

    public static void free(Identifier id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || id == null) {
            return;
        }
        client.getTextureManager().destroyTexture(id);
    }
}
