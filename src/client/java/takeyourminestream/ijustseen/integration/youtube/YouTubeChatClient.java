package takeyourminestream.ijustseen.integration.youtube;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles;
import takeyourminestream.ijustseen.integration.chat.ChatConnection;
import takeyourminestream.ijustseen.integration.chat.ChatHttp;
import takeyourminestream.ijustseen.integration.chat.ChatMessagePipeline;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.integration.chat.IncomingChatMessage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeChatClient implements ChatConnection {
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"");
    private static final Pattern LIVE_CHAT_ID_PATTERN = Pattern.compile("\"liveChatId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile(
        "(?:youtube\\.com/watch\\?.*?v=|youtu\\.be/|youtube\\.com/live/)([a-zA-Z0-9_-]{11})"
    );
    private static final Pattern BARE_VIDEO_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");
    private static final Pattern LIVE_CHAT_RENDERER = Pattern.compile("\"liveChatRenderer\"\\s*:\\s*\\{");
    private static final Pattern LIVE_CHAT_REPLAY = Pattern.compile("\"isReplay\"\\s*:\\s*true");
    private static final Pattern RELOAD_CONTINUATION = Pattern.compile(
        "\"reloadContinuationData\"\\s*:\\s*\\{\\s*\"continuation\"\\s*:\\s*\"([^\"]+)\""
    );
    private static final Pattern JSON_TRUE_FLAG = Pattern.compile(
        "\"(?:isLiveNow|isLiveContent|isLive|isLiveBroadcast)\"\\s*:\\s*true"
    );
    private static final Pattern JSON_FALSE_LIVE_FLAG = Pattern.compile(
        "\"(?:isLiveNow|isLiveContent|isLive|isLiveBroadcast)\"\\s*:\\s*false"
    );
    private static final Pattern JSON_UPCOMING = Pattern.compile("\"isUpcoming\"\\s*:\\s*true");
    private static final Pattern CURRENT_VIDEO_ID = Pattern.compile(
        "\"currentVideoEndpoint\"\\s*:\\s*\\{[^}]*\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\""
    );
    private static final Pattern LIVE_VIDEO_ID = Pattern.compile(
        "\"isLiveNow\"\\s*:\\s*true[\\s\\S]{0,1200}?\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\""
    );
    private static final String INNERTUBE_CLIENT_VERSION = "2.20250201.00.00";
    private static final String LIVE_CHAT_API = "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat";
    private static final String PLAYER_API = "https://www.youtube.com/youtubei/v1/player";
    private static final int MAX_SEEN_MESSAGE_IDS = 5000;

    private final String channelLabel;
    private final Source source;
    private final ChatMessagePipeline pipeline;
    private final Set<String> seenMessageIds = new HashSet<>();
    private volatile boolean running;
    private volatile Thread workerThread;
    private String continuation;
    private String liveChatId;
    private String activeVideoId;
    // Первый ответ содержит историю чата (~70 сообщений) — её не показываем
    private boolean initialBacklogConsumed;

    public YouTubeChatClient(String channelInput, ChatMessagePipeline pipeline) {
        this.source = parseSource(channelInput);
        this.channelLabel = source.label();
        this.pipeline = pipeline;
        this.running = true;
        workerThread = new Thread(this::pollLoop, "YouTubeChat-" + channelLabel);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private record Source(Kind kind, String label, String handle, String videoId) {
        enum Kind { HANDLE, VIDEO_ID }

        static Source handle(String handle) {
            return new Source(Kind.HANDLE, handle, handle, null);
        }

        static Source videoId(String videoId) {
            return new Source(Kind.VIDEO_ID, videoId, null, videoId);
        }
    }

    private static Source parseSource(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Source.handle("");
        }

        Matcher urlMatcher = VIDEO_URL_PATTERN.matcher(trimmed);
        if (urlMatcher.find()) {
            return Source.videoId(urlMatcher.group(1));
        }

        if (BARE_VIDEO_ID_PATTERN.matcher(trimmed).matches()) {
            return Source.videoId(trimmed);
        }

        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        } else if (trimmed.startsWith("https://www.youtube.com/@")) {
            trimmed = trimmed.substring("https://www.youtube.com/@".length());
            int slash = trimmed.indexOf('/');
            trimmed = slash >= 0 ? trimmed.substring(0, slash) : trimmed;
        }

        return Source.handle(trimmed);
    }

    private void pollLoop() {
        while (running) {
            try {
                if (liveChatId == null && continuation == null) {
                    resolveLiveChat();
                }
                if (!running) {
                    break;
                }
                if (liveChatId == null && continuation == null) {
                    sleepQuietly(10000);
                    continue;
                }
                pollOnce();
                sleepQuietly(3000);
            } catch (Exception e) {
                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (e instanceof takeyourminestream.ijustseen.integration.chat.ChatConnectException cce
                    && isOfflineError(cce)) {
                    TakeYourMineStreamClient.LOGGER.info("YouTube {} offline: {}", channelLabel, e.getMessage());
                    takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.report(ChatPlatform.YOUTUBE, e);
                } else {
                    TakeYourMineStreamClient.LOGGER.warn("YouTube chat error for {}: {}", channelLabel, e.getMessage());
                    takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.report(ChatPlatform.YOUTUBE, e);
                }
                resetSession();
                sleepQuietly(10000);
            }
        }
    }

    private void resetSession() {
        liveChatId = null;
        continuation = null;
        activeVideoId = null;
        seenMessageIds.clear();
        initialBacklogConsumed = false;
    }

    private void resolveLiveChat() throws IOException {
        String videoId = resolveVideoId();
        activeVideoId = videoId;
        String watchUrl = watchUrlFor(videoId);
        String playerResponse = fetchPlayerResponse(videoId, watchUrl);
        boolean live = isLiveBroadcast(playerResponse);

        String chatId = firstMatch(LIVE_CHAT_ID_PATTERN, playerResponse);
        if (chatId != null) {
            liveChatId = chatId;
            continuation = null;
            takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.clear(ChatPlatform.YOUTUBE);
            TakeYourMineStreamClient.LOGGER.info(
                "YouTube live chat resolved for {} via player API: {}",
                channelLabel,
                liveChatId
            );
            return;
        }

        String watchHtml = ChatHttp.get(watchUrl, watchUrl);
        String chatContinuation = fetchLiveChatContinuationFromWatchPage(watchHtml);
        if (chatContinuation != null) {
            liveChatId = null;
            continuation = chatContinuation;
            takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.clear(ChatPlatform.YOUTUBE);
            TakeYourMineStreamClient.LOGGER.info(
                "YouTube live chat resolved for {} via watch page (video {})",
                channelLabel,
                videoId
            );
            return;
        }

        if (!live) {
            throw offlineException(videoId);
        }

        String chatTarget = source.kind() == Source.Kind.HANDLE
            ? "@" + source.handle()
            : videoId;
        throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
            "takeyourstreamchat.chat.error.no_live_chat",
            "Live chat not available for " + chatTarget,
            "YouTube", chatTarget
        );
    }

    private static String fetchPlayerResponse(String videoId, String watchUrl) throws IOException {
        JsonObject body = innertubeBody();
        body.addProperty("videoId", videoId);
        return ChatHttp.postJson(PLAYER_API, body.toString(), youtubeHeaders(watchUrl));
    }

    /** Только явные флаги «сейчас в эфире»; VOD прошлых стримов с {@code liveBroadcastDetails} не считаем live. */
    private static boolean isLiveBroadcast(String playerResponse) {
        if (JSON_UPCOMING.matcher(playerResponse).find()) {
            return false;
        }
        if (JSON_FALSE_LIVE_FLAG.matcher(playerResponse).find()) {
            return false;
        }
        return JSON_TRUE_FLAG.matcher(playerResponse).find();
    }

    private static boolean htmlIndicatesLiveStream(String html) {
        if (JSON_TRUE_FLAG.matcher(html).find()
            || html.contains("BADGE_STYLE_TYPE_LIVE_NOW")
            || html.contains("\"style\":\"LIVE\"")) {
            return true;
        }
        return false;
    }

    private takeyourminestream.ijustseen.integration.chat.ChatConnectException offlineException(String videoId) {
        if (source.kind() == Source.Kind.HANDLE) {
            return new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.no_live_room",
                "No live stream for @" + source.handle(),
                "YouTube", "@" + source.handle()
            );
        }
        return new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
            "takeyourstreamchat.chat.error.not_live",
            "Stream is not live: " + videoId,
            "YouTube", videoId
        );
    }

    private static boolean isOfflineError(takeyourminestream.ijustseen.integration.chat.ChatConnectException error) {
        String key = error.getTranslationKey();
        return "takeyourstreamchat.chat.error.no_live_room".equals(key)
            || "takeyourstreamchat.chat.error.not_live".equals(key);
    }

    private String resolveVideoId() throws IOException {
        if (source.kind() == Source.Kind.VIDEO_ID) {
            return source.videoId();
        }

        String url = "https://www.youtube.com/@"
            + URLEncoder.encode(source.handle(), StandardCharsets.UTF_8) + "/live";
        String html = ChatHttp.get(url, "https://www.youtube.com/");
        if (!htmlIndicatesLiveStream(html)) {
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.no_live_room",
                "No live stream found for @" + source.handle(),
                "YouTube", "@" + source.handle()
            );
        }
        String videoId = extractLiveVideoId(html);
        if (videoId == null) {
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.no_live_room",
                "No live stream found for @" + source.handle(),
                "YouTube", "@" + source.handle()
            );
        }
        return videoId;
    }

    private static String fetchLiveChatContinuationFromWatchPage(String watchHtml) {
        int rendererIndex = indexOfPattern(LIVE_CHAT_RENDERER, watchHtml);
        if (rendererIndex < 0) {
            return null;
        }

        int chunkEnd = Math.min(rendererIndex + 12000, watchHtml.length());
        String chunk = watchHtml.substring(rendererIndex, chunkEnd);

        if (LIVE_CHAT_REPLAY.matcher(chunk).find()) {
            return null;
        }

        Matcher continuationMatcher = RELOAD_CONTINUATION.matcher(chunk);
        return continuationMatcher.find() ? continuationMatcher.group(1) : null;
    }

    private void pollOnce() throws IOException {
        String watchUrl = activeVideoId != null ? watchUrlFor(activeVideoId) : "https://www.youtube.com/";
        JsonObject body = innertubeBody();
        if (continuation != null) {
            body.addProperty("continuation", continuation);
        } else {
            body.addProperty("activeLiveChatId", liveChatId);
        }

        String raw = ChatHttp.postJson(LIVE_CHAT_API, body.toString(), youtubeHeaders(watchUrl));
        JsonObject response = JsonParser.parseString(raw).getAsJsonObject();
        boolean deliver = initialBacklogConsumed;
        if (deliver) {
            List<IncomingChatMessage> batch = new ArrayList<>();
            collectMessages(response, true, batch);
            if (batch.size() == 1) {
                pipeline.process(batch.get(0));
            } else if (!batch.isEmpty()) {
                pipeline.processYouTubeBatch(batch);
            }
        } else {
            collectMessages(response, false, null);
        }
        extractContinuation(response);
        initialBacklogConsumed = true;
    }

    private static JsonObject innertubeBody() {
        return JsonParser.parseString("""
            {"context":{"client":{"clientName":"WEB","clientVersion":"2.20250201.00.00","hl":"en","gl":"US"}}}
            """.trim()).getAsJsonObject();
    }

    private static String watchUrlFor(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private static Map<String, String> youtubeHeaders(String watchUrl) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Origin", "https://www.youtube.com");
        headers.put("Referer", watchUrl);
        headers.put("X-YouTube-Client-Name", "1");
        headers.put("X-YouTube-Client-Version", INNERTUBE_CLIENT_VERSION);
        return headers;
    }

    private void extractContinuation(JsonObject response) {
        String next = findTimedContinuation(response);
        if (next != null) {
            continuation = next;
        }
    }

    private static String findTimedContinuation(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("timedContinuationData") && obj.get("timedContinuationData").isJsonObject()) {
                JsonObject timed = obj.getAsJsonObject("timedContinuationData");
                if (timed.has("continuation") && timed.get("continuation").isJsonPrimitive()) {
                    return timed.get("continuation").getAsString();
                }
            }
            for (String key : obj.keySet()) {
                String found = findTimedContinuation(obj.get(key));
                if (found != null) {
                    return found;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String found = findTimedContinuation(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void collectMessages(JsonElement element, boolean deliver, List<IncomingChatMessage> batch) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("liveChatTextMessageRenderer")) {
                handleTextMessage(obj.getAsJsonObject("liveChatTextMessageRenderer"), deliver, batch);
            }
            for (String key : obj.keySet()) {
                collectMessages(obj.get(key), deliver, batch);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectMessages(child, deliver, batch);
            }
        }
    }

    private void handleTextMessage(JsonObject renderer, boolean deliver, List<IncomingChatMessage> batch) {
        if (renderer.has("id") && renderer.get("id").isJsonPrimitive()) {
            String messageId = renderer.get("id").getAsString();
            if (!seenMessageIds.add(messageId)) {
                return;
            }
            if (seenMessageIds.size() > MAX_SEEN_MESSAGE_IDS) {
                seenMessageIds.clear();
                seenMessageIds.add(messageId);
            }
        }

        if (!deliver) {
            // Стартовая история чата: только запоминаем ID, в игру не отправляем
            return;
        }

        String message = extractMessageText(renderer);
        if (message.isBlank()) {
            return;
        }

        String authorName = "";
        String authorChannelId = "";
        ChatAuthorRoles roles = ChatAuthorRoles.NONE;
        if (renderer.has("authorName") && renderer.get("authorName").isJsonObject()) {
            JsonObject author = renderer.getAsJsonObject("authorName");
            if (author.has("simpleText")) {
                authorName = author.get("simpleText").getAsString();
            }
        }
        if (authorName.startsWith("@")) {
            authorName = authorName.substring(1);
        }
        if (renderer.has("authorExternalChannelId")) {
            authorChannelId = renderer.get("authorExternalChannelId").getAsString();
        }
        if (renderer.has("authorBadges")) {
            roles = ChatAuthorRoles.fromBadgeHints(renderer.get("authorBadges").toString());
        }

        Long timestampMicros = parseTimestampMicros(renderer);
        IncomingChatMessage incoming = IncomingChatMessage.builder(ChatPlatform.YOUTUBE)
            .authorLogin(authorChannelId.isBlank() ? authorName : authorChannelId)
            .displayName(authorName.isBlank() ? "Viewer" : authorName)
            .text(message)
            .roles(roles)
            .sourceTimestampMicros(timestampMicros)
            .build();
        batch.add(incoming);
    }

    private static Long parseTimestampMicros(JsonObject renderer) {
        if (!renderer.has("timestampUsec") || !renderer.get("timestampUsec").isJsonPrimitive()) {
            return null;
        }
        try {
            return Long.parseLong(renderer.get("timestampUsec").getAsString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String extractMessageText(JsonObject renderer) {
        if (!renderer.has("message")) {
            return "";
        }
        JsonElement messageEl = renderer.get("message");
        if (messageEl.isJsonObject()) {
            JsonObject messageObj = messageEl.getAsJsonObject();
            if (messageObj.has("simpleText")) {
                return messageObj.get("simpleText").getAsString();
            }
            if (messageObj.has("runs") && messageObj.get("runs").isJsonArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonElement run : messageObj.getAsJsonArray("runs")) {
                    if (run.isJsonObject() && run.getAsJsonObject().has("text")) {
                        sb.append(run.getAsJsonObject().get("text").getAsString());
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    private static int indexOfPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private static String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Берёт videoId текущего эфира, а не первый попавшийся в рекомендациях. */
    private static String extractLiveVideoId(String html) {
        String videoId = firstMatch(CURRENT_VIDEO_ID, html);
        if (videoId != null) {
            return videoId;
        }
        videoId = firstMatch(LIVE_VIDEO_ID, html);
        if (videoId != null) {
            return videoId;
        }
        return firstMatch(VIDEO_ID_PATTERN, html);
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public ChatPlatform getPlatform() {
        return ChatPlatform.YOUTUBE;
    }

    @Override
    public void connect(ChatMessagePipeline pipeline) {
        // Connected in constructor
    }

    @Override
    public void disconnect() {
        running = false;
        Thread thread = workerThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isConnected() {
        return running;
    }
}
