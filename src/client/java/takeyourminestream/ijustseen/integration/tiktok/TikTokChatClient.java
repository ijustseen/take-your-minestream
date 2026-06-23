package takeyourminestream.ijustseen.integration.tiktok;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.chat.ChatConnection;
import takeyourminestream.ijustseen.integration.chat.ChatHttp;
import takeyourminestream.ijustseen.integration.chat.ChatMessagePipeline;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.integration.chat.IncomingChatMessage;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikTokChatClient implements ChatConnection {
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("\"roomId\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Pattern ROOM_ID_ALT = Pattern.compile("room_id=(\\d+)");

    private final String username;
    private final ChatMessagePipeline pipeline;
    private volatile boolean running;
    private volatile Thread workerThread;
    private volatile TikTokWebSocket webSocket;
    private String roomId;
    private final Set<Long> seenMessageIds = new HashSet<>();

    public TikTokChatClient(String username, ChatMessagePipeline pipeline) {
        this.username = username.trim().replace("@", "");
        this.pipeline = pipeline;
        this.running = true;
        start();
    }

    private void start() {
        workerThread = new Thread(this::connectLoop, "TikTokChat-" + username);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void connectLoop() {
        while (running) {
            try {
                if (roomId == null) {
                    roomId = resolveRoomId(username);
                    TakeYourMineStreamClient.LOGGER.info("TikTok room resolved for @{}: {}", username, roomId);
                    takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.clear(ChatPlatform.TIKTOK);
                }

                String ttwid = ChatHttp.fetchTtwidCookie(username);
                connectWebSocket(ttwid);

                while (running && webSocket != null && webSocket.isRunning()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                TakeYourMineStreamClient.LOGGER.warn("TikTok chat error for @{}: {}", username, e.getMessage());
                takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.report(ChatPlatform.TIKTOK, e);
                disconnectWebSocket();
                roomId = null;
                seenMessageIds.clear();
                sleepQuietly(10000);
            }
        }
    }

    private void connectWebSocket(String ttwid) {
        String currentRoomId = roomId;
        webSocket = new TikTokWebSocket(currentRoomId, ttwid, new TikTokWebSocket.ChatHandler() {
            @Override
            public void onChat(long msgId, String uniqueId, String displayName, String text,
                takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles roles,
                java.util.List<takeyourminestream.ijustseen.messages.MessageEmote> emotes) {
                if (msgId != 0 && !seenMessageIds.add(msgId)) {
                    return;
                }
                pipeline.process(IncomingChatMessage.builder(ChatPlatform.TIKTOK)
                    .authorLogin(uniqueId)
                    .displayName(displayName)
                    .text(text)
                    .roles(roles)
                    .emotes(emotes)
                    .build());
            }

            @Override
            public void onLiveEnded() {
                TakeYourMineStreamClient.LOGGER.info("TikTok live ended for @{}", username);
                roomId = null;
                seenMessageIds.clear();
                disconnectWebSocket();
            }
        });
        webSocket.connect();
    }

    private void disconnectWebSocket() {
        TikTokWebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            ws.disconnect();
        }
    }

    private static String resolveRoomId(String user) throws IOException {
        // Основной способ: api-live (страница /live теперь закрыта WAF-заглушкой).
        // sourceType=54 обязателен, без него API возвращает params_error.
        String apiUrl = "https://www.tiktok.com/api-live/user/room/?aid=1988&sourceType=54&uniqueId="
            + URLEncoder.encode(user, StandardCharsets.UTF_8);
        String apiBody = ChatHttp.get(apiUrl, "https://www.tiktok.com/");
        String roomId = null;
        Integer status = null;
        try {
            JsonObject json = JsonParser.parseString(apiBody).getAsJsonObject();
            if (json.has("data") && json.get("data").isJsonObject()) {
                JsonObject data = json.getAsJsonObject("data");
                if (data.has("user") && data.get("user").isJsonObject()) {
                    JsonObject userObj = data.getAsJsonObject("user");
                    if (userObj.has("roomId") && !userObj.get("roomId").isJsonNull()) {
                        roomId = userObj.get("roomId").getAsString();
                    }
                    if (userObj.has("status") && !userObj.get("status").isJsonNull()) {
                        status = userObj.get("status").getAsInt();
                    }
                }
            }
        } catch (Exception ignored) {
            // не-JSON ответ: пробуем запасной способ ниже
        }

        // Статус 2 = идёт эфир; 4 = эфир завершён/оффлайн
        if (status != null && status != 2) {
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.not_live",
                "@" + user + " is not live right now (status " + status + ")",
                "TikTok", "@" + user
            );
        }
        if (roomId != null && !roomId.isBlank() && !"0".equals(roomId)) {
            return roomId;
        }

        // Запасной способ: парсинг HTML страницы /live (может быть закрыт WAF)
        String liveUrl = "https://www.tiktok.com/@" + URLEncoder.encode(user, StandardCharsets.UTF_8) + "/live";
        String html = ChatHttp.get(liveUrl, "https://www.tiktok.com/");
        roomId = firstMatch(ROOM_ID_PATTERN, html);
        if (roomId == null) {
            roomId = firstMatch(ROOM_ID_ALT, html);
        }
        if (roomId == null || roomId.isBlank()) {
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.no_live_room",
                "No live room found for @" + user + " (user not found or not live)",
                "TikTok", "@" + user
            );
        }
        return roomId;
    }

    private static String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
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
        return ChatPlatform.TIKTOK;
    }

    @Override
    public void connect(ChatMessagePipeline pipeline) {
        // Connected in constructor
    }

    @Override
    public void disconnect() {
        running = false;
        disconnectWebSocket();
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
