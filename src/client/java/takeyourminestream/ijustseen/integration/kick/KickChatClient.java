package takeyourminestream.ijustseen.integration.kick;

import com.google.gson.Gson;
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
import takeyourminestream.ijustseen.integration.chat.SimpleWebSocket;

public class KickChatClient implements ChatConnection {
    private static final Gson GSON = new Gson();
    private static final String PUSHER_URL =
        "wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=8.4.0&flash=false";

    private final String channelSlug;
    private final ChatMessagePipeline pipeline;
    private volatile boolean running;
    private volatile Thread workerThread;
    private SimpleWebSocket webSocket;
    private long chatroomId;

    public KickChatClient(String channelSlug, ChatMessagePipeline pipeline) {
        this.channelSlug = channelSlug.trim().toLowerCase();
        this.pipeline = pipeline;
        this.running = true;
        start();
    }

    private void start() {
        workerThread = new Thread(this::connectLoop, "KickChat-" + channelSlug);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void connectLoop() {
        while (running) {
            try {
                chatroomId = resolveChatroomId(channelSlug);
                takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.clear(ChatPlatform.KICK);
                connectWebSocket();
                while (running && webSocket != null && webSocket.isRunning()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (!running || Thread.currentThread().isInterrupted() || isInterruption(e)) {
                    break;
                }
                TakeYourMineStreamClient.LOGGER.warn("Kick chat error for {}: {}", channelSlug, e.getMessage());
                takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.report(ChatPlatform.KICK, e);
            }
            if (running) {
                sleepQuietly(5000);
            }
        }
    }

    private void connectWebSocket() {
        webSocket = new SimpleWebSocket(PUSHER_URL, () -> {
            String subscribe = GSON.toJson(java.util.Map.of(
                "event", "pusher:subscribe",
                "data", java.util.Map.of("auth", "", "channel", "chatrooms." + chatroomId + ".v2")
            ));
            webSocket.send(subscribe);
        }, this::handleMessage);
        webSocket.connect();
    }

    private void handleMessage(String raw) {
        JsonObject envelope;
        try {
            envelope = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        String event = envelope.has("event") ? envelope.get("event").getAsString() : "";
        if (!"App\\Events\\ChatMessageEvent".equals(event) && !event.endsWith("ChatMessageEvent")) {
            return;
        }

        String dataStr = envelope.has("data") ? envelope.get("data").getAsString() : null;
        if (dataStr == null || dataStr.isBlank()) {
            return;
        }

        JsonObject data;
        try {
            data = JsonParser.parseString(dataStr).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        String username = textOrEmpty(data, "username");
        if (username.isBlank()) {
            username = textOrEmpty(data, "sender", "username");
        }
        String message = textOrEmpty(data, "content");
        if (message.isBlank()) {
            message = textOrEmpty(data, "message");
        }
        if (message.isBlank()) {
            return;
        }

        String displayName = username;
        ChatAuthorRoles roles = ChatAuthorRoles.NONE;
        if (data.has("sender") && data.get("sender").isJsonObject()) {
            JsonObject sender = data.getAsJsonObject("sender");
            String senderName = textOrEmpty(sender, "username");
            if (!senderName.isBlank()) {
                username = senderName;
                displayName = senderName;
            }
            if (sender.has("identity") && sender.get("identity").isJsonObject()) {
                JsonObject identity = sender.getAsJsonObject("identity");
                String identityName = textOrEmpty(identity, "username");
                if (!identityName.isBlank()) {
                    displayName = identityName;
                }
                roles = rolesFromKickIdentity(identity);
            }
        }

        Integer rgb = null;
        if (data.has("sender") && data.get("sender").isJsonObject()) {
            JsonObject sender = data.getAsJsonObject("sender");
            if (sender.has("identity") && sender.get("identity").isJsonObject()) {
                String color = textOrEmpty(sender.getAsJsonObject("identity"), "color");
                rgb = parseHexColor(color);
            }
        }

        var builder = IncomingChatMessage.builder(ChatPlatform.KICK)
            .authorLogin(username)
            .displayName(displayName)
            .text(message)
            .emotes(KickEmoteParser.parse(message))
            .roles(roles);
        if (rgb != null) {
            builder.authorColorRgb(rgb);
        }
        pipeline.process(builder.build());
    }

    /** Kick identity.badges: subscriber / og / vip / moderator. Фолловеров в сообщении нет. */
    private static ChatAuthorRoles rolesFromKickIdentity(JsonObject identity) {
        boolean subscriber = false;
        boolean vip = false;
        boolean moderator = false;
        boolean broadcaster = false;
        if (!identity.has("badges") || !identity.get("badges").isJsonArray()) {
            return ChatAuthorRoles.NONE;
        }
        for (JsonElement element : identity.getAsJsonArray("badges")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject badge = element.getAsJsonObject();
            String type = badge.has("type") && !badge.get("type").isJsonNull()
                ? badge.get("type").getAsString().toLowerCase(java.util.Locale.ROOT)
                : "";
            switch (type) {
                case "subscriber", "og", "founder" -> subscriber = true;
                case "vip" -> vip = true;
                case "moderator" -> moderator = true;
                case "broadcaster", "host" -> broadcaster = true;
                default -> { }
            }
        }
        return new ChatAuthorRoles(false, subscriber, vip, moderator, broadcaster);
    }

    private static long resolveChatroomId(String slug) throws java.io.IOException {
        String body = ChatHttp.get("https://kick.com/api/v2/channels/" + slug);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (!json.has("chatroom") || !json.get("chatroom").isJsonObject()) {
            if (json.has("error")) {
                throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                    "takeyourstreamchat.chat.error.blocked",
                    "Kick API blocked the request: " + json.get("error").getAsString(),
                    "Kick"
                );
            }
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.user_not_found",
                "Kick channel not found or has no chatroom: " + slug,
                "Kick", slug
            );
        }
        return json.getAsJsonObject("chatroom").get("id").getAsLong();
    }

    private static String textOrEmpty(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private static String textOrEmpty(JsonObject obj, String parent, String key) {
        if (!obj.has(parent) || !obj.get(parent).isJsonObject()) {
            return "";
        }
        return textOrEmpty(obj.getAsJsonObject(parent), key);
    }

    private static Integer parseHexColor(String color) {
        if (color == null || !color.startsWith("#") || color.length() != 7) {
            return null;
        }
        try {
            return Integer.parseInt(color.substring(1), 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Смена канала прерывает HTTP-запрос и заворачивает это в IOException. Это остановка, не ошибка. */
    private static boolean isInterruption(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof InterruptedException) {
                return true;
            }
        }
        return false;
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
        return ChatPlatform.KICK;
    }

    @Override
    public void connect(ChatMessagePipeline pipeline) {
        // Connected in constructor
    }

    @Override
    public void disconnect() {
        running = false;
        if (webSocket != null) {
            webSocket.disconnect();
            webSocket = null;
        }
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
