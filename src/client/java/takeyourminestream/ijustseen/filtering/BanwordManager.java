package takeyourminestream.ijustseen.filtering;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import takeyourminestream.ijustseen.interfaces.IBanwordManager;
import takeyourminestream.ijustseen.StoragePaths;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class BanwordManager implements IBanwordManager {
    private static final Logger LOGGER = Logger.getLogger(BanwordManager.class.getName());
    private static final String RESOURCE_PATH = "/assets/take-your-minestream/banned_words.json";
    private static final String USER_FILE_NAME = "take-your-minestream-banwords.json";
    private static final Path USER_FILE_PATH;
    private static BanwordManager instance;

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
    
    private final Set<String> banwords = new HashSet<>();

    private BanwordManager() {
        loadBanwords();
    }

    public static BanwordManager getInstance() {
        if (instance == null) {
            instance = new BanwordManager();
        }
        return instance;
    }

    @Override
    public void loadBanwords() {
        try (InputStreamReader reader = new InputStreamReader(
                BanwordManager.class.getClassLoader().getResourceAsStream("assets/take-your-minestream/banned_words.json"),
                StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> words = new Gson().fromJson(reader, listType);
            banwords.clear();
            if (words != null) {
                banwords.addAll(words);
                LOGGER.info("Загружено " + words.size() + " банвордов");
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при загрузке банвордов: " + e.getMessage());
        }

        // Загрузка пользовательского файла и слияние
        try {
            Path userFile = USER_FILE_PATH;
            if (Files.exists(userFile)) {
                try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(userFile), StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    List<String> userWords = new Gson().fromJson(reader, listType);
                    if (userWords != null) {
                        for (String w : userWords) {
                            if (w != null && !w.trim().isEmpty()) banwords.add(w.trim().toLowerCase());
                        }
                        LOGGER.info("Загружено пользовательских банвордов: " + userWords.size());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при загрузке пользовательских банвордов: " + e.getMessage());
        }
    }

    @Override
    public boolean containsBanwords(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String lower = Normalizer.normalize(message, Normalizer.Form.NFC).toLowerCase();
        for (String word : banwords) {
            String normWord = Normalizer.normalize(word, Normalizer.Form.NFC).toLowerCase();
            if (lower.contains(normWord)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String filterBanwords(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }
        
        String censored = Normalizer.normalize(message, Normalizer.Form.NFC);
        for (String word : banwords) {
            String normWord = Normalizer.normalize(word, Normalizer.Form.NFC);
            if (normWord.length() > 0) {
                // Создаем маску: первая буква + звездочки для остальных
                String firstChar = normWord.substring(0, 1);
                String stars = "*".repeat(normWord.length() - 1);
                String mask = firstChar + stars;
                censored = censored.replaceAll("(?iu)(?<!\\p{L})" + java.util.regex.Pattern.quote(normWord) + "(?!\\p{L})", mask);
            }
        }
        return censored;
    }

    @Override
    public void addBanword(String banword) {
        if (banword != null && !banword.trim().isEmpty()) {
            banwords.add(banword.trim().toLowerCase());
            LOGGER.info("Добавлен банворд: " + banword);
            saveUserBanwords();
        }
    }

    @Override
    public void removeBanword(String banword) {
        if (banword != null && !banword.trim().isEmpty()) {
            boolean removed = banwords.remove(banword.trim().toLowerCase());
            if (removed) {
                LOGGER.info("Удален банворд: " + banword);
                saveUserBanwords();
            }
        }
    }

    public Set<String> getBanwords() {
        return Collections.unmodifiableSet(banwords);
    }

    private void saveUserBanwords() {
        try {
            Path userFile = USER_FILE_PATH;
            Files.createDirectories(userFile.getParent());
            List<String> list = banwords.stream().sorted().toList();
            try (BufferedWriter writer = Files.newBufferedWriter(userFile, StandardCharsets.UTF_8)) {
                new Gson().toJson(list, writer);
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка при сохранении пользовательских банвордов: " + e.getMessage());
        }
    }
} 