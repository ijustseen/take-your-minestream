package takeyourminestream.ijustseen.filtering;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import takeyourminestream.ijustseen.core.storage.StoragePaths;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class BlockedUsernameManager {
    private static final Logger LOGGER = Logger.getLogger(BlockedUsernameManager.class.getName());
    private static final String USER_FILE_NAME = "take-your-minestream-blocked-users.json";
    private static final Path USER_FILE_PATH;
    private static BlockedUsernameManager instance;

    static {
        Path newPath = StoragePaths.getModRootDir().resolve(USER_FILE_NAME);
        Path legacyPath = FabricLoader.getInstance().getConfigDir().resolve(USER_FILE_NAME);
        try {
            StoragePaths.ensureModRootDir();
        } catch (Exception ignored) {
        }
        StoragePaths.migrateFileIfNeeded(legacyPath, newPath);
        USER_FILE_PATH = newPath;
    }

    private final Set<String> blockedUsernames = new HashSet<>();

    private BlockedUsernameManager() {
        loadBlockedUsernames();
    }

    public static BlockedUsernameManager getInstance() {
        if (instance == null) {
            instance = new BlockedUsernameManager();
        }
        return instance;
    }

    public void loadBlockedUsernames() {
        blockedUsernames.clear();
        try {
            Path userFile = USER_FILE_PATH;
            if (Files.exists(userFile)) {
                try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(userFile), StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> names = new Gson().fromJson(reader, listType);
                    if (names != null) {
                        for (String name : names) {
                            if (name != null && !name.trim().isEmpty()) {
                                blockedUsernames.add(normalize(name));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load blocked usernames: " + e.getMessage());
        }
    }

    public boolean isBlocked(String displayName, String login) {
        if (displayName != null && blockedUsernames.contains(normalize(displayName))) {
            return true;
        }
        return login != null && blockedUsernames.contains(normalize(login));
    }

    public void addBlockedUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            blockedUsernames.add(normalize(username));
            saveBlockedUsernames();
        }
    }

    public void removeBlockedUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            if (blockedUsernames.remove(normalize(username))) {
                saveBlockedUsernames();
            }
        }
    }

    public Set<String> getBlockedUsernames() {
        return Collections.unmodifiableSet(blockedUsernames);
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private void saveBlockedUsernames() {
        try {
            Path userFile = USER_FILE_PATH;
            Files.createDirectories(userFile.getParent());
            List<String> list = blockedUsernames.stream().sorted().toList();
            try (BufferedWriter writer = Files.newBufferedWriter(userFile, StandardCharsets.UTF_8)) {
                new Gson().toJson(list, writer);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to save blocked usernames: " + e.getMessage());
        }
    }
}
