package takeyourminestream.ijustseen.integration.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.config.ModConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Достаёт логин Twitch-канала из ключа трансляции ({@code live_<id>_<secret>}) через GQL.
 */
public final class StreamKeyResolver {
    public static final String TWITCH_WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko";
    private static final String TWITCH_GQL = "https://gql.twitch.tv/gql";
    private static final Pattern TWITCH_KEY = Pattern.compile("(?:^|[/=])(live_(\\d+)_[A-Za-z0-9]+)");

    public enum Status { EMPTY, RESOLVED, CHANNEL, INVALID }

    public record Result(Status status, String channel) {
        public boolean hasChannel() {
            return channel != null && !channel.isBlank();
        }
    }

    private StreamKeyResolver() {}

    public static void applyAsync(String raw, java.util.function.Consumer<Result> onDone) {
        String value = raw == null ? "" : raw.trim();
        Thread worker = new Thread(() -> {
            Result result = resolve(value);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    applyResult(result);
                    if (onDone != null) {
                        onDone.accept(result);
                    }
                });
            }
        }, "StreamKey-twitch");
        worker.setDaemon(true);
        worker.start();
    }

    public static Result resolve(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return new Result(Status.EMPTY, "");
        }
        Matcher key = TWITCH_KEY.matcher(value);
        if (key.find()) {
            String userId = key.group(2);
            Optional<String> login = fetchTwitchLogin(userId);
            if (login.isPresent()) {
                TakeYourMineStreamClient.LOGGER.info("Twitch stream key resolved to #{}", login.get());
                return new Result(Status.RESOLVED, login.get());
            }
            return new Result(Status.INVALID, "");
        }
        if (looksLikeIngestSecret(value)) {
            return new Result(Status.INVALID, "");
        }
        return new Result(Status.CHANNEL, stripHandle(value.replaceFirst("(?i)^https?://(?:www\\.)?twitch\\.tv/", "")));
    }

    private static void applyResult(Result result) {
        if (!result.hasChannel()) {
            return;
        }
        ModConfig.setTWITCH_CHANNEL_NAME(result.channel());
        ModConfig.setTWITCH_ENABLED(true);
    }

    private static Optional<String> fetchTwitchLogin(String userId) {
        try {
            String body = "{\"query\":\"query($id:ID!){user(id:$id){login}}\",\"variables\":{\"id\":\"" + userId + "\"}}";
            String response = ChatHttp.postJson(TWITCH_GQL, body, Map.of("Client-ID", TWITCH_WEB_CLIENT_ID));
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return Optional.empty();
            }
            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("user") || data.get("user").isJsonNull()) {
                return Optional.empty();
            }
            String login = data.getAsJsonObject("user").get("login").getAsString();
            return login == null || login.isBlank() ? Optional.empty() : Optional.of(login);
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.warn("Could not resolve Twitch stream key: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean looksLikeIngestSecret(String value) {
        String token = extractLastPath(value);
        return token.length() >= 28 && token.matches("[A-Za-z0-9_-]+") && !token.contains(".") && !TWITCH_KEY.matcher(value).find();
    }

    private static String extractLastPath(String value) {
        String token = value.trim();
        int slash = token.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < token.length()) {
            token = token.substring(slash + 1);
        }
        return token;
    }

    private static String stripHandle(String value) {
        String token = extractLastPath(value).replaceFirst("^@", "");
        int query = token.indexOf('?');
        if (query >= 0) {
            token = token.substring(0, query);
        }
        return token.toLowerCase(Locale.ROOT);
    }
}
