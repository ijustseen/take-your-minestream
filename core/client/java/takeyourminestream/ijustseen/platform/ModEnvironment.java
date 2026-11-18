package takeyourminestream.ijustseen.platform;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Пути игры/конфига без привязки к FabricLoader / FMLPaths.
 * Entrypoint лоадера обязан вызвать {@link #bootstrap} до остальной инициализации.
 */
public final class ModEnvironment {
    private static Path gameDir;
    private static Path configDir;
    private static String loaderName = "unknown";

    private ModEnvironment() {}

    public static void bootstrap(Path gameDirectory, Path configDirectory, String loader) {
        gameDir = Objects.requireNonNull(gameDirectory, "gameDirectory");
        configDir = Objects.requireNonNull(configDirectory, "configDirectory");
        loaderName = loader == null || loader.isBlank() ? "unknown" : loader;
    }

    public static boolean isReady() {
        return gameDir != null && configDir != null;
    }

    public static Path getGameDir() {
        ensureReady();
        return gameDir;
    }

    public static Path getConfigDir() {
        ensureReady();
        return configDir;
    }

    public static String getLoaderName() {
        return loaderName;
    }

    private static void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("ModEnvironment.bootstrap() was not called");
        }
    }
}
