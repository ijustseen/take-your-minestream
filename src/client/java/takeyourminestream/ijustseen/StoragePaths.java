package takeyourminestream.ijustseen;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class StoragePaths {
    private static final String MOD_DIR_NAME = "take-your-minestream";

    private StoragePaths() {
    }

    public static Path getModRootDir() {
        return FabricLoader.getInstance().getGameDir().resolve(MOD_DIR_NAME);
    }

    public static Path getLegacyModRootDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_DIR_NAME);
    }

    public static Path ensureModRootDir() throws IOException {
        Path root = getModRootDir();
        Files.createDirectories(root);
        return root;
    }

    public static void migrateFileIfNeeded(Path legacyPath, Path newPath) {
        try {
            if (Files.exists(newPath) || !Files.exists(legacyPath)) {
                return;
            }
            Files.createDirectories(newPath.getParent());
            Files.move(legacyPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    public static void migrateDirectoryIfNeeded(Path legacyDir, Path newDir) {
        try {
            if (Files.exists(newDir) || !Files.exists(legacyDir)) {
                return;
            }
            Files.createDirectories(newDir.getParent());
            Files.move(legacyDir, newDir, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }
}
