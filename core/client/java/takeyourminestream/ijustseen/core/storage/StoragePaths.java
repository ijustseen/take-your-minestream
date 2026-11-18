package takeyourminestream.ijustseen.core.storage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class StoragePaths {
    private static final String MOD_DIR_NAME = "take-your-stream-chat";
    private static final String LEGACY_MOD_DIR_NAME = "take-your-minestream";

    private StoragePaths() {
    }

    public static Path getModRootDir() {
        return FabricLoader.getInstance().getGameDir().resolve(MOD_DIR_NAME);
    }

    public static Path getLegacyModRootDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(LEGACY_MOD_DIR_NAME);
    }

    public static Path getLegacyGameModRootDir() {
        return FabricLoader.getInstance().getGameDir().resolve(LEGACY_MOD_DIR_NAME);
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

    /** Перенос данных из папки Take Your MineStream (1.x) при первом запуске 2.0. */
    public static void migrateLegacyModRootIfNeeded() {
        Path newRoot = getModRootDir();
        if (Files.exists(newRoot)) {
            return;
        }
        migrateDirectoryIfNeeded(getLegacyGameModRootDir(), newRoot);
        if (!Files.exists(newRoot)) {
            migrateDirectoryIfNeeded(getLegacyModRootDir(), newRoot);
        }
    }
}
