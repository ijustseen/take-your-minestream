package takeyourminestream.modid.rendering;

import net.fabricmc.api.ClientModInitializer;

/**
 * Debug-рендер выключен: класс оставлен как no-op заглушка для совместимости API.
 */
public class CustomRenderPipeline implements ClientModInitializer {
    private static CustomRenderPipeline instance;

    public static CustomRenderPipeline getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
    }

    public void close() {
    }
}
