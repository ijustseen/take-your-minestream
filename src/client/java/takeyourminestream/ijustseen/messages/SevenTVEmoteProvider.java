package takeyourminestream.ijustseen.messages;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Менеджер 7TV эмоутов — загружает глобальные и канальные наборы,
 * предоставляет поиск по имени эмоута.
 */
public final class SevenTVEmoteProvider {
    private static final Gson GSON = new Gson();

    // name → emoteId
    private static final Map<String, String> GLOBAL_EMOTES = new ConcurrentHashMap<>();
    private static final Map<String, String> CHANNEL_EMOTES = new ConcurrentHashMap<>();
    private static final Map<String, String> GLOBAL_EMOTES_LOWER = new ConcurrentHashMap<>();
    private static final Map<String, String> CHANNEL_EMOTES_LOWER = new ConcurrentHashMap<>();

    private static final AtomicBoolean GLOBALS_LOADED = new AtomicBoolean(false);
    private static final AtomicBoolean GLOBALS_LOADING = new AtomicBoolean(false);
    private static volatile String currentChannelName = null;
    private static volatile String currentTwitchRoomId = null;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "TYMS-7TV-Loader");
        t.setDaemon(true);
        return t;
    });

    private SevenTVEmoteProvider() {}

    /**
     * Инициализация: загрузить глобальные эмоуты + канальные (если указан канал).
     * Вызывается из TwitchChatClient при подключении к каналу.
     */
    public static void init(String channelName) {
        loadGlobalEmotes();
        if (channelName != null && !channelName.isBlank()) {
            loadChannelEmotes(channelName);
        }
    }

    /**
     * Инициализация с Twitch room-id (предпочтительный путь для 7TV v3).
     */
    public static void init(String channelName, String twitchRoomId) {
        loadGlobalEmotes();
        if (twitchRoomId != null && !twitchRoomId.isBlank()) {
            loadChannelEmotesByRoomId(channelName, twitchRoomId);
        } else if (channelName != null && !channelName.isBlank()) {
            loadChannelEmotes(channelName);
        }
    }

    /**
     * Загружает глобальный набор эмоутов 7TV (один раз).
     */
    public static void loadGlobalEmotes() {
        if (GLOBALS_LOADED.get() || !GLOBALS_LOADING.compareAndSet(false, true)) return;
        EXECUTOR.execute(() -> {
            boolean loadedSuccessfully = false;
            try {
                // 7TV v3 API: глобальный набор
                JsonObject json = httpGetJson("https://7tv.io/v3/emote-sets/global");
                if (json == null) return;
                JsonArray emotes = json.getAsJsonArray("emotes");
                if (emotes == null) return;
                int count = 0;
                for (JsonElement el : emotes) {
                    JsonObject emoteObj = el.getAsJsonObject();
                    String name = emoteObj.has("name") ? emoteObj.get("name").getAsString() : null;
                    String id = emoteObj.has("id") ? emoteObj.get("id").getAsString() : null;
                    if (name != null && id != null && !name.isBlank()) {
                        GLOBAL_EMOTES.put(name, id);
                        GLOBAL_EMOTES_LOWER.put(name.toLowerCase(), id);
                        count++;
                    }
                }
                GLOBALS_LOADED.set(true);
                loadedSuccessfully = true;
                System.out.println("[TYMS-7TV] Загружено глобальных эмоутов: " + count);
            } catch (Exception e) {
                System.out.println("[TYMS-7TV] Ошибка загрузки глобальных эмоутов: " + e.getMessage());
            } finally {
                if (!loadedSuccessfully) {
                    GLOBALS_LOADING.set(false); // Разрешить повторную загрузку после неуспеха
                }
            }
        });
    }

    /**
     * Загружает набор канальных эмоутов 7TV.
     * Используем поиск по логину: /v3/users/twitch/{login}
     */
    public static void loadChannelEmotes(String channelName) {
        if (channelName == null || channelName.isBlank()) return;
        String lower = channelName.toLowerCase();
        if (lower.equals(currentChannelName) && !CHANNEL_EMOTES.isEmpty()) return; // Уже загружено

        EXECUTOR.execute(() -> {
            try {
                Map<String, String> freshChannelEmotes = new java.util.HashMap<>();

                // 7TV API: получить пользователя по Twitch-логину
                JsonObject userJson = httpGetJson("https://7tv.io/v3/users/twitch/" + lower);
                if (userJson == null) {
                    System.out.println("[TYMS-7TV] Канал " + lower + " не найден на 7TV");
                    return;
                }

                JsonObject emoteSet = userJson.has("emote_set") ? userJson.getAsJsonObject("emote_set") : null;
                if (emoteSet == null) {
                    System.out.println("[TYMS-7TV] У канала " + lower + " нет набора эмоутов на 7TV");
                    return;
                }

                JsonArray emotes = emoteSet.getAsJsonArray("emotes");
                if (emotes == null) return;

                int count = 0;
                for (JsonElement el : emotes) {
                    JsonObject emoteObj = el.getAsJsonObject();
                    String name = emoteObj.has("name") ? emoteObj.get("name").getAsString() : null;
                    String id = emoteObj.has("id") ? emoteObj.get("id").getAsString() : null;
                    if (name != null && id != null && !name.isBlank()) {
                        freshChannelEmotes.put(name, id);
                        count++;
                    }
                }

                CHANNEL_EMOTES.clear();
                CHANNEL_EMOTES_LOWER.clear();
                CHANNEL_EMOTES.putAll(freshChannelEmotes);
                for (Map.Entry<String, String> entry : freshChannelEmotes.entrySet()) {
                    CHANNEL_EMOTES_LOWER.put(entry.getKey().toLowerCase(), entry.getValue());
                }
                currentChannelName = lower;
                currentTwitchRoomId = null;
                System.out.println("[TYMS-7TV] Загружено канальных эмоутов для " + lower + ": " + count);
            } catch (Exception e) {
                System.out.println("[TYMS-7TV] Ошибка загрузки канальных эмоутов для " + lower + ": " + e.getMessage());
            }
        });
    }

    /**
     * Загружает канальные эмоуты 7TV по Twitch room-id (надежнее, чем по логину).
     */
    public static void loadChannelEmotesByRoomId(String channelName, String twitchRoomId) {
        if (twitchRoomId == null || twitchRoomId.isBlank()) {
            if (channelName != null && !channelName.isBlank()) {
                loadChannelEmotes(channelName);
            }
            return;
        }

        if (twitchRoomId.equals(currentTwitchRoomId) && !CHANNEL_EMOTES.isEmpty()) {
            return;
        }

        String lower = channelName == null ? "" : channelName.toLowerCase();
        EXECUTOR.execute(() -> {
            try {
                Map<String, String> freshChannelEmotes = new java.util.HashMap<>();

                JsonObject userJson = httpGetJson("https://7tv.io/v3/users/twitch/" + twitchRoomId);
                if (userJson == null) {
                    System.out.println("[TYMS-7TV] Пользователь Twitch room-id=" + twitchRoomId + " не найден на 7TV");
                    if (!lower.isBlank()) {
                        loadChannelEmotes(lower);
                    }
                    return;
                }

                JsonObject emoteSet = userJson.has("emote_set") ? userJson.getAsJsonObject("emote_set") : null;
                if (emoteSet == null) {
                    System.out.println("[TYMS-7TV] У room-id=" + twitchRoomId + " нет набора эмоутов на 7TV");
                    if (!lower.isBlank()) {
                        loadChannelEmotes(lower);
                    }
                    return;
                }

                JsonArray emotes = emoteSet.getAsJsonArray("emotes");
                if (emotes == null) return;

                int count = 0;
                for (JsonElement el : emotes) {
                    JsonObject emoteObj = el.getAsJsonObject();
                    String name = emoteObj.has("name") ? emoteObj.get("name").getAsString() : null;
                    String id = emoteObj.has("id") ? emoteObj.get("id").getAsString() : null;
                    if (name != null && id != null && !name.isBlank()) {
                        freshChannelEmotes.put(name, id);
                        count++;
                    }
                }

                CHANNEL_EMOTES.clear();
                CHANNEL_EMOTES_LOWER.clear();
                CHANNEL_EMOTES.putAll(freshChannelEmotes);
                for (Map.Entry<String, String> entry : freshChannelEmotes.entrySet()) {
                    CHANNEL_EMOTES_LOWER.put(entry.getKey().toLowerCase(), entry.getValue());
                }
                currentChannelName = lower.isBlank() ? null : lower;
                currentTwitchRoomId = twitchRoomId;
                System.out.println("[TYMS-7TV] Загружено канальных эмоутов для room-id=" + twitchRoomId + ": " + count);
            } catch (Exception e) {
                System.out.println("[TYMS-7TV] Ошибка загрузки канальных эмоутов для room-id=" + twitchRoomId + ": " + e.getMessage());
                if (!lower.isBlank()) {
                    loadChannelEmotes(lower);
                }
            }
        });
    }

    /**
     * Ищет 7TV эмоут по имени. Сначала канальные, потом глобальные.
     * @return emoteId или null
     */
    public static String findEmoteId(String emoteName) {
        // Канальные имеют приоритет
        String id = CHANNEL_EMOTES.get(emoteName);
        if (id != null) return id;
        id = GLOBAL_EMOTES.get(emoteName);
        if (id != null) return id;

        String lower = emoteName.toLowerCase();
        id = CHANNEL_EMOTES_LOWER.get(lower);
        if (id != null) return id;
        return GLOBAL_EMOTES_LOWER.get(lower);
    }

    /**
     * Сканирует текст сообщения и возвращает найденные 7TV эмоуты.
     * Пропускает позиции, уже занятые Twitch-эмоутами.
     *
     * @param text полный текст сообщения (с цветным префиксом ника)
     * @param existingEmotes уже распарсенные Twitch-эмоуты
     * @return список найденных 7TV эмоутов (может быть пустым)
     */
    public static List<MessageEmote> scanForEmotes(String text, List<MessageEmote> existingEmotes) {
        if (!GLOBALS_LOADED.get() && CHANNEL_EMOTES.isEmpty()) {
            return Collections.emptyList();
        }

        // Создаём битовую маску "занятых" позиций (от Twitch-эмоутов)
        boolean[] occupied = new boolean[text.length()];
        if (existingEmotes != null) {
            for (MessageEmote existing : existingEmotes) {
                for (int i = existing.getStartIndex(); i <= existing.getEndIndex() && i < occupied.length; i++) {
                    occupied[i] = true;
                }
            }
        }

        List<MessageEmote> found = new java.util.ArrayList<>();

        // Разбиваем текст на слова (по пробелам) и проверяем каждое
        int i = 0;
        while (i < text.length()) {
            // Пропускаем section-code последовательности (§x)
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                i += 2;
                continue;
            }

            // Пропускаем пробелы
            if (text.charAt(i) == ' ') {
                i++;
                continue;
            }

            // Пропускаем занятые позиции
            if (occupied[i]) {
                i++;
                continue;
            }

            // Находим конец слова
            int wordStart = i;
            while (i < text.length() && text.charAt(i) != ' ' && !occupied[i]) {
                i++;
            }
            int wordEnd = i - 1;

            String word = text.substring(wordStart, wordEnd + 1);
            // Убираем section-коды из слова для сравнения
            String cleanWord = word.replaceAll("§.", "");

            if (!cleanWord.isEmpty()) {
                String emoteId = findEmoteId(cleanWord);
                if (emoteId != null) {
                    found.add(new MessageEmote("7tv", emoteId, cleanWord, wordStart, wordEnd));
                    // Предзагрузка текстуры
                    TwitchEmoteTextureCache.preload("7tv", emoteId);
                }
            }
        }

        return found.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(found);
    }

    /**
     * Проверяет, загружены ли наборы эмоутов.
     */
    public static boolean isReady() {
        return GLOBALS_LOADED.get() || !CHANNEL_EMOTES.isEmpty();
    }

    // ─── HTTP helper ───

    private static JsonObject httpGetJson(String url) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(url);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "TakeYourMineStream/1.0");
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            if (code != 200) {
                System.out.println("[TYMS-7TV] HTTP " + code + " для " + url);
                return null;
            }

            try (InputStream is = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            System.out.println("[TYMS-7TV] Ошибка HTTP-запроса " + url + ": " + e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
