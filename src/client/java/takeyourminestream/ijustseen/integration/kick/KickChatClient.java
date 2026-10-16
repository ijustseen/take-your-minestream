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

public class KickChatClient implements ChatConnection {
    private static final Gson GSON = new Gson();
    private static final String PUSHER_URL =
        "wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=8.4.0&flash=false";

    private final String channelSlug;
    private final ChatMessagePipeline pipeline;
    private volatile boolean running;
    private volatile Thread workerThread;
    private SimpleWebSocket webSocket;
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

    private void connectWebSocket() {
        webSocket = new SimpleWebSocket(PUSHER_URL, () -> {
            subscribePusher("chatrooms." + chatroomId + ".v2");
            subscribePusher("chatrooms." + chatroomId);
            if (channelId > 0) {
                subscribePusher("channel." + channelId);
            }
        }, this::handleMessage);
        webSocket.connect();
    }

    private void subscribePusher(String channel) {
        webSocket.send(GSON.toJson(java.util.Map.of(
            "event", "pusher:subscribe",
            "data", java.util.Map.of("auth", "", "channel", channel)
        )));
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
        long chatroomId = json.getAsJsonObject("chatroom").get("id").getAsLong();
        long channelId = json.has("id") && json.get("id").isJsonPrimitive()
            ? json.get("id").getAsLong()
            : 0L;
        return new KickIds(channelId, chatroomId);
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
