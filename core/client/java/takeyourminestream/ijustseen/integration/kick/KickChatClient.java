package takeyourminestream.ijustseen.integration.kick;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.Text;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles;
import takeyourminestream.ijustseen.integration.chat.ChatConnection;
import takeyourminestream.ijustseen.integration.chat.ChatEventType;
import takeyourminestream.ijustseen.integration.chat.ChatHttp;
import takeyourminestream.ijustseen.integration.chat.ChatMessagePipeline;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.integration.chat.IncomingChatMessage;
import takeyourminestream.ijustseen.integration.chat.SimpleWebSocket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class KickChatClient implements ChatConnection {
    private static final Gson GSON = new Gson();
    private static final String REALTIME_API = "https://web.kick.com/api/v1/realtime";

    private final String channelSlug;
    private final ChatMessagePipeline pipeline;
    private volatile boolean running;
    private volatile Thread workerThread;
    private SimpleWebSocket webSocket;
    private final AtomicInteger nextCommandId = new AtomicInteger();
    private long channelId;
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
                KickIds ids = resolveIds(channelSlug);
                channelId = ids.channelId();
                chatroomId = ids.chatroomId();
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

    private void connectWebSocket() throws java.io.IOException {
        String clientId = UUID.randomUUID().toString();
        String url = fetchRealtimeUrl(clientId);
        String token = fetchRealtimeToken(clientId);
        nextCommandId.set(0);
        webSocket = new SimpleWebSocket(url, Map.of("Origin", "https://kick.com"), () -> sendCommand(
            "\"connect\":{\"token\":" + GSON.toJson(token) + ",\"name\":\"js\"}"
        ), this::handleSocketFrame);
        webSocket.connect();
    }

    private void sendCommand(String body) {
        SimpleWebSocket socket = webSocket;
        if (socket == null || !running) {
            return;
        }
        int id = nextCommandId.incrementAndGet();
        socket.send("{\"id\":" + id + "," + body + "}");
    }

    private void subscribeChat(String channel) {
        sendCommand("\"subscribe\":{\"channel\":" + GSON.toJson(channel) + "}");
    }

    /** Кадр Centrifugo: команды с id, пинг {@code {}} и публикации чата. */
    private void handleSocketFrame(String raw) {
        if (!running || raw == null || raw.isBlank()) {
            return;
        }
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if ("{}".equals(trimmed)) {
                SimpleWebSocket socket = webSocket;
                if (socket != null) {
                    socket.send("{}");
                }
                continue;
            }
            JsonObject message;
            try {
                message = JsonParser.parseString(trimmed).getAsJsonObject();
            } catch (RuntimeException ignored) {
                continue;
            }
            if (message.has("id") && message.get("id").isJsonPrimitive()) {
                handleCommandReply(message);
                continue;
            }
            if (message.has("push") && message.get("push").isJsonObject()) {
                handlePush(message.getAsJsonObject("push"));
            }
        }
    }

    private void handleCommandReply(JsonObject message) {
        if (message.has("error") && message.get("error").isJsonObject()) {
            String reason = textOrEmpty(message.getAsJsonObject("error"), "message");
            TakeYourMineStreamClient.LOGGER.warn("Kick realtime refused a command for {}: {}", channelSlug, reason);
            SimpleWebSocket socket = webSocket;
            if (socket != null) {
                socket.disconnect();
            }
            return;
        }
        if (!message.has("connect")) {
            return;
        }
        TakeYourMineStreamClient.LOGGER.info("Kick chat connected for {}", channelSlug);
        takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.clear(ChatPlatform.KICK);
        subscribeChat("chatrooms." + chatroomId + ".v2");
        subscribeChat("chatrooms." + chatroomId);
        subscribeChat("chatroom_" + chatroomId);
        if (channelId > 0) {
            subscribeChat("channel." + channelId);
        }
    }

    private void handlePush(JsonObject push) {
        if (push.has("disconnect")) {
            SimpleWebSocket socket = webSocket;
            if (socket != null) {
                socket.disconnect();
            }
            return;
        }
        if (!push.has("pub") || !push.get("pub").isJsonObject()) {
            return;
        }
        JsonObject publication = push.getAsJsonObject("pub");
        if (!publication.has("data") || publication.get("data").isJsonNull()) {
            return;
        }
        JsonElement data = publication.get("data");
        handleMessage(data.isJsonPrimitive() ? data.getAsString() : data.toString());
    }

    private void handleMessage(String raw) {
        JsonObject envelope;
        try {
            envelope = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        String event = envelope.has("event") ? envelope.get("event").getAsString() : "";
        JsonObject data = parseData(envelope);
        if (data == null) {
            return;
        }

        String eventName = shortEventName(event);
        if ("ChatMessageEvent".equals(eventName)) {
            handleChatMessage(data);
            return;
        }
        handleLiveEvent(eventName, data);
    }

    private void handleChatMessage(JsonObject data) {
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

    /**
     * Публичные Pusher-события чата Kick: подписки и гифты на {@code chatrooms.{id}.v2},
     * хосты и фолловы иногда на {@code channel.{id}}.
     */
    private void handleLiveEvent(String eventName, JsonObject data) {
        String username = firstNonBlank(
            textOrEmpty(data, "username"),
            textOrEmpty(data, "gifter_username"),
            textOrEmpty(data, "host_username"),
            textOrEmpty(data, "user", "username"),
            textOrEmpty(data, "sender", "username")
        );
        String text;
        ChatEventType type;
        switch (eventName) {
            case "SubscriptionEvent", "ChannelSubscriptionEvent" -> {
                int months = intOrZero(data, "months");
                type = ChatEventType.SUBSCRIBE;
                text = months > 1
                    ? Text.translatable("takeyourstreamchat.event.kick.resub", months).getString()
                    : Text.translatable("takeyourstreamchat.event.kick.sub").getString();
            }
            case "GiftedSubscriptionsEvent" -> {
                type = ChatEventType.GIFT;
                int count = giftedCount(data);
                String recipient = firstGiftedUsername(data);
                text = count <= 1 && !recipient.isBlank()
                    ? Text.translatable("takeyourstreamchat.event.kick.subgift_one", recipient).getString()
                    : Text.translatable("takeyourstreamchat.event.kick.subgift", Math.max(count, 1)).getString();
            }
            case "StreamHostEvent" -> {
                type = ChatEventType.RAID;
                text = Text.translatable(
                    "takeyourstreamchat.event.kick.host",
                    Math.max(intOrZero(data, "number_viewers"), 0)
                ).getString();
            }
            case "FollowersUpdated" -> {
                if (username.isBlank()) {
                    return;
                }
                type = ChatEventType.FOLLOW;
                text = Text.translatable("takeyourstreamchat.event.kick.follow").getString();
            }
            default -> {
                return;
            }
        }
        if (username.isBlank()) {
            username = "Viewer";
        }
        pipeline.process(IncomingChatMessage.builder(ChatPlatform.KICK)
            .authorLogin(username)
            .displayName(username)
            .text(text)
            .eventType(type)
            .build());
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

    private record KickIds(long channelId, long chatroomId) {}

    private static KickIds resolveIds(String slug) throws java.io.IOException {
        if (!slug.matches("[a-z0-9_-]{2,25}")) {
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.user_not_found",
                "Kick channel not found or has no chatroom: " + slug,
                "Kick", slug
            );
        }
        String url = "https://kick.com/api/v2/channels/" + slug;
        String referer = "https://kick.com/" + slug;
        String body = null;
        java.io.IOException javaError = null;
        try {
            body = ChatHttp.get(url, referer);
        } catch (java.io.IOException e) {
            javaError = e;
        }
        if (body == null || isBlockedChannelPayload(body)) {
            try {
                TakeYourMineStreamClient.LOGGER.info(
                    "Kick channel lookup was blocked for {}, retrying with curl",
                    slug
                );
                body = ChatHttp.getWithSystemCurl(url, referer);
            } catch (java.io.IOException curlError) {
                if (body != null) {
                    // Java уже вернула отказ Kick — его и покажем игроку.
                } else if (javaError != null) {
                    throw javaError;
                } else {
                    throw curlError;
                }
            }
        }
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
        long chatroomId = json.getAsJsonObject("chatroom").get("id").getAsLong();
        long channelId = json.has("id") && json.get("id").isJsonPrimitive()
            ? json.get("id").getAsLong()
            : 0L;
        return new KickIds(channelId, chatroomId);
    }

    private String fetchRealtimeUrl(String clientId) throws java.io.IOException {
        String endpoint = channelId > 0
            ? REALTIME_API + "/channels/" + channelId + "/chat/connection"
            : REALTIME_API + "/connection";
        JsonObject data = realtimeData(ChatHttp.postJson(endpoint, clientBody(clientId), kickHeaders()));
        if (data.has("connections") && data.get("connections").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("connections")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject connection = element.getAsJsonObject();
                if (!"centrifugo".equals(textOrEmpty(connection, "provider"))) {
                    continue;
                }
                if (!connection.has("credentials") || !connection.get("credentials").isJsonObject()) {
                    continue;
                }
                String url = textOrEmpty(connection.getAsJsonObject("credentials"), "url");
                if (url.startsWith("wss://")) {
                    return url;
                }
            }
        }
        throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
            "takeyourstreamchat.chat.error.generic",
            "Kick realtime has no websocket url",
            "Kick", "no chat socket"
        );
    }

    private static String fetchRealtimeToken(String clientId) throws java.io.IOException {
        JsonObject data = realtimeData(ChatHttp.postJson(
            REALTIME_API + "/auth/connection",
            GSON.toJson(Map.of("client_id", clientId)),
            kickHeaders()
        ));
        String token = textOrEmpty(data, "token");
        if (token.isBlank()) {
            throw new takeyourminestream.ijustseen.integration.chat.ChatConnectException(
                "takeyourstreamchat.chat.error.generic",
                "Kick realtime token is missing",
                "Kick", "no chat token"
            );
        }
        return token;
    }

    private static String clientBody(String clientId) {
        return GSON.toJson(Map.of(
            "client", Map.of("id", clientId, "type", "web"),
            "capabilities", Map.of("accepted_providers", java.util.List.of(Map.of("provider", "centrifugo")))
        ));
    }

    private static Map<String, String> kickHeaders() {
        return Map.of("Origin", "https://kick.com", "Referer", "https://kick.com/");
    }

    private static JsonObject realtimeData(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (json.has("data") && json.get("data").isJsonObject()) {
            return json.getAsJsonObject("data");
        }
        return json;
    }

    private static boolean isBlockedChannelPayload(String body) {
        if (body == null || body.isBlank() || body.charAt(0) != '{') {
            return false;
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            boolean hasChatroom = json.has("chatroom") && json.get("chatroom").isJsonObject();
            return !hasChatroom && json.has("error");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static JsonObject parseData(JsonObject envelope) {
        if (!envelope.has("data") || envelope.get("data").isJsonNull()) {
            return null;
        }
        JsonElement data = envelope.get("data");
        if (data.isJsonObject()) {
            return data.getAsJsonObject();
        }
        if (!data.isJsonPrimitive()) {
            return null;
        }
        String dataStr = data.getAsString();
        if (dataStr == null || dataStr.isBlank()) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(dataStr);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String shortEventName(String event) {
        if (event == null || event.isBlank()) {
            return "";
        }
        int slash = Math.max(event.lastIndexOf('\\'), event.lastIndexOf('.'));
        return slash >= 0 ? event.substring(slash + 1) : event;
    }

    private static int giftedCount(JsonObject data) {
        if (data.has("gifted_usernames") && data.get("gifted_usernames").isJsonArray()) {
            return data.getAsJsonArray("gifted_usernames").size();
        }
        int total = intOrZero(data, "gifter_total");
        return Math.max(total, 0);
    }

    private static String firstGiftedUsername(JsonObject data) {
        if (!data.has("gifted_usernames") || !data.get("gifted_usernames").isJsonArray()) {
            return "";
        }
        for (JsonElement element : data.getAsJsonArray("gifted_usernames")) {
            if (element.isJsonPrimitive()) {
                String name = element.getAsString();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
        }
        return "";
    }

    private static int intOrZero(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            try {
                return Integer.parseInt(obj.get(key).getAsString());
            } catch (NumberFormatException ignoredAgain) {
                return 0;
            }
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String textOrEmpty(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonPrimitive()) {
            return "";
        }
        try {
            return obj.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return "";
        }
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
