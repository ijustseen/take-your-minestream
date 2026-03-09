package takeyourminestream.ijustseen.filtering;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilteringManager {
    private static final String USER_FILE_NAME = "take-your-minestream-regexps.json";

    private static FilteringManager instance;

    private final Set<String> regexps = new HashSet<>();

    private FilteringManager() {
        loadRegexp();
    }

    public static FilteringManager getInstance() {
        if (instance == null) {
            instance = new FilteringManager();
        }
        return instance;
    }

    public void loadRegexp() {
        try {
            Path userFile = FabricLoader.getInstance().getConfigDir().resolve(USER_FILE_NAME);
            if (!Files.exists(userFile)) {
                try (FileWriter writer = new FileWriter(String.valueOf(userFile))) {
                    writer.write("[]");
                }
            }
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(userFile), StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> userWords = new Gson().fromJson(reader, listType);
                if (userWords != null) {
                    for (String w : userWords) {
                        if (w != null) regexps.add(w);
                    }
                    TakeYourMineStreamClient.LOGGER.info("Загружено пользовательских regexp: {}", userWords.size());
                }
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Ошибка при загрузке пользовательских regexp: ", e);
        }
    }

    public boolean fitsRegexp(String message) {
        if (message == null || message.isEmpty() || regexps.isEmpty()) {
            return false;
        }
        String lowerCaseMessage = message.toLowerCase();
        for (String pattern : regexps) {
            if (pattern != null && !pattern.isEmpty()) {
                try {
                    if (lowerCaseMessage.matches(pattern)) {
                        return true;
                    }
                } catch (Exception e) {
                    TakeYourMineStreamClient.LOGGER.warn("Ошибка при проверке regexp '{}': {}", pattern, e.getMessage());
                }
            }
        }
        return false;
    }

    public void addRegexp(String regexp) {
        if (regexp != null) {
            regexps.add(regexp);
            TakeYourMineStreamClient.LOGGER.info("Добавлен банворд: {}", regexp);
            saveUserRegexps();
        }
    }

    public void removeRegexp(String regexp) {
        if (regexp != null) {
            boolean removed = regexps.remove(regexp);
            if (removed) {
                TakeYourMineStreamClient.LOGGER.info("Удален банворд: {}", regexp);
                saveUserRegexps();
            }
        }
    }

    public Set<String> getRegexps() {
        return Collections.unmodifiableSet(regexps);
    }

    private void saveUserRegexps() {
        try {
            Path userFile = FabricLoader.getInstance().getConfigDir().resolve(USER_FILE_NAME);
            List<String> list = regexps.stream().sorted().toList();
            try (BufferedWriter writer = Files.newBufferedWriter(userFile, StandardCharsets.UTF_8)) {
                new Gson().toJson(list, writer);
            }
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.error("Ошибка при сохранении пользовательских банвордов: " + e.getMessage());
        }
    }
}
