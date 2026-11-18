package takeyourminestream.ijustseen.integration.twitch;

import net.minecraft.text.Text;
import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles;
import takeyourminestream.ijustseen.integration.chat.ChatConnection;
import takeyourminestream.ijustseen.integration.chat.ChatEventType;
import takeyourminestream.ijustseen.integration.chat.ChatMessagePipeline;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.integration.chat.IncomingChatMessage;
import takeyourminestream.ijustseen.messages.SevenTVEmoteProvider;

import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TwitchChatClient implements ChatConnection {
    private SSLSocket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private Thread listenThread;
    private volatile boolean running = false;
    private final Random random = new Random();
    private final ChatMessagePipeline pipeline;
    private String channelName;

    public TwitchChatClient(String channelName, ChatMessagePipeline pipeline) {
        this.channelName = channelName;
        this.pipeline = pipeline;
        connect();
    }

    @Override
    public ChatPlatform getPlatform() {
        return ChatPlatform.TWITCH;
    }

    @Override
    public void connect(ChatMessagePipeline pipeline) {
        // Already connected in constructor
    }

    private void connect() {
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) factory.createSocket("irc.chat.twitch.tv", 6697);
            socket.startHandshake();
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            sendRaw("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership");

            String nick = "justinfan" + (10000 + random.nextInt(90000));
            sendRaw("PASS oauth:anonymous");
            sendRaw("NICK " + nick);
            sendRaw("JOIN #" + channelName.toLowerCase());

            running = true;
            listenThread = new Thread(this::listenLoop, "TwitchIRC-Listen");
            listenThread.setDaemon(true);
            listenThread.start();
            TakeYourMineStreamClient.LOGGER.info("Connected to Twitch IRC as {} in #{}", nick, channelName);
            takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.clear(ChatPlatform.TWITCH);

            SevenTVEmoteProvider.init(null);
        } catch (IOException e) {
            TakeYourMineStreamClient.LOGGER.error("Failed to connect to Twitch IRC", e);
            takeyourminestream.ijustseen.integration.chat.ChatErrorReporter.report(ChatPlatform.TWITCH, e);
        }
    }

    private void sendRaw(String line) throws IOException {
        writer.write(line + "\r\n");
        writer.flush();
    }

    private void listenLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.startsWith("PING ")) {
                    sendRaw("PONG " + line.substring(5));
                } else if (line.contains(" ROOMSTATE ")) {
                    String roomId = parseTags(line).get("room-id");
                    if (roomId != null && !roomId.isBlank()) {
                        SevenTVEmoteProvider.init(channelName, roomId);
                    }
                } else if (line.contains(" USERNOTICE ")) {
                    handleUserNotice(line);
                } else if (line.contains(" PRVMSG ") || line.contains(" PRIVMSG ")) {
                    handlePrivMsg(line);
                }
            }
        } catch (IOException e) {
            if (running) {
                TakeYourMineStreamClient.LOGGER.warn("Twitch IRC connection lost", e);
            }
        }
        if (running) {
            TakeYourMineStreamClient.LOGGER.info("Reconnecting to Twitch IRC in 3 seconds");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
            connect();
        }
    }

    private void handlePrivMsg(String line) {
        String rest = stripTags(line);
        int excl = rest.indexOf('!');
        int colon = rest.indexOf(" :", 1);
        int hash = rest.indexOf('#');
        if (excl <= 1 || colon <= 1 || hash <= 1) {
            return;
        }

        Map<String, String> tags = parseTags(line);
        String user = rest.substring(1, excl);
        String message = rest.substring(colon + 2);
        String displayName = firstNonBlank(tags.get("display-name"), user);
        String bits = tags.get("bits");
        if (bits != null && !bits.isBlank()) {
            String eventText = message.isBlank()
                ? Text.translatable("takeyourstreamchat.event.twitch.bits", bits).getString()
                : Text.translatable("takeyourstreamchat.event.twitch.bits_message", bits, message).getString();
            emit(user, displayName, eventText, tags, ChatEventType.BITS, null);
            return;
        }

        emit(user, displayName, message, tags, ChatEventType.CHAT, tags.get("emotes"));
    }

    /**
     * Подписки, гифты и рейды приходят как USERNOTICE при CAP twitch.tv/commands —
     * без OAuth, тем же justinfan-сокеттом, что и обычный чат.
     */
    private void handleUserNotice(String line) {
        Map<String, String> tags = parseTags(line);
        String msgId = tags.getOrDefault("msg-id", "");
        ChatEventType eventType = eventTypeForUserNotice(msgId);
        if (eventType == null) {
            return;
        }

        String login = firstNonBlank(tags.get("login"), tags.get("display-name"), "Viewer");
        String displayName = firstNonBlank(tags.get("display-name"), login);
        String text = userNoticeText(msgId, tags, trailingIrcMessage(stripTags(line), " USERNOTICE "));
        if (text.isBlank()) {
            return;
        }
        emit(login, displayName, text, tags, eventType, null);
    }

    private void emit(
        String login,
        String displayName,
        String text,
        Map<String, String> tags,
        ChatEventType eventType,
        String emotesTag
    ) {
        String gifsTag = firstNonBlank(tags.get("gifs"), tags.get("gif"));
        if ((text == null || text.isBlank()) && gifsTag.isBlank()) {
            return;
        }
        if (text == null || text.isBlank()) {
            text = "\uFFFC";
        }
        IncomingChatMessage.Builder builder = IncomingChatMessage.builder(ChatPlatform.TWITCH)
            .authorLogin(login)
            .displayName(displayName)
            .text(text)
            .roles(rolesFromBadges(tags.get("badges")))
            .twitchRoomId(emptyToNull(tags.get("room-id")))
            .twitchChannelName(channelName)
            .twitchEmotesTag(emptyToNull(emotesTag))
            .twitchGifsTag(emptyToNull(gifsTag))
            .eventType(eventType);
        Integer rgb = parseHexColor(tags.get("color"));
        if (rgb != null) {
            builder.authorColorRgb(rgb);
        }
        pipeline.process(builder.build());
    }

    private static ChatEventType eventTypeForUserNotice(String msgId) {
        return switch (msgId) {
            case "sub", "resub", "giftpaidupgrade", "primepaidupgrade",
                 "communitypayforward", "standardpayforward" -> ChatEventType.SUBSCRIBE;
            case "subgift", "anonsubgift", "submysterygift", "rewardgift",
                 "anongiftpaidupgrade" -> ChatEventType.GIFT;
            case "raid" -> ChatEventType.RAID;
            default -> null;
        };
    }

    private static String userNoticeText(String msgId, Map<String, String> tags, String userMessage) {
        String text = switch (msgId) {
            case "resub" -> Text.translatable(
                "takeyourstreamchat.event.twitch.resub",
                firstNonBlank(tags.get("msg-param-cumulative-months"), tags.get("msg-param-months"), "1")
            ).getString();
            case "subgift", "anonsubgift" -> Text.translatable(
                "takeyourstreamchat.event.twitch.subgift",
                firstNonBlank(tags.get("msg-param-recipient-display-name"), tags.get("msg-param-recipient-user-name"), "?")
            ).getString();
            case "submysterygift" -> Text.translatable(
                "takeyourstreamchat.event.twitch.submystery",
                firstNonBlank(tags.get("msg-param-mass-gift-count"), "1")
            ).getString();
            case "raid" -> Text.translatable(
                "takeyourstreamchat.event.twitch.raid",
                firstNonBlank(tags.get("msg-param-viewerCount"), tags.get("msg-param-viewer-count"), "0")
            ).getString();
            case "sub", "giftpaidupgrade", "primepaidupgrade",
                 "communitypayforward", "standardpayforward" ->
                Text.translatable("takeyourstreamchat.event.twitch.sub").getString();
            case "rewardgift", "anongiftpaidupgrade" ->
                Text.translatable("takeyourstreamchat.event.twitch.subgift_unknown").getString();
            default -> firstNonBlank(tags.get("system-msg"), "");
        };
        if (userMessage != null && !userMessage.isBlank()) {
            return text + " — " + userMessage;
        }
        return text;
    }

    private static Map<String, String> parseTags(String line) {
        Map<String, String> tags = new HashMap<>();
        if (line == null || !line.startsWith("@")) {
            return tags;
        }
        int firstSpace = line.indexOf(' ');
        if (firstSpace <= 1) {
            return tags;
        }
        for (String tag : line.substring(1, firstSpace).split(";")) {
            int eq = tag.indexOf('=');
            if (eq >= 0) {
                tags.put(tag.substring(0, eq), unescapeIrc(tag.substring(eq + 1)));
            } else if (!tag.isEmpty()) {
                tags.put(tag, "");
            }
        }
        return tags;
    }

    private static String stripTags(String line) {
        if (line != null && line.startsWith("@")) {
            int space = line.indexOf(' ');
            if (space > 0 && space + 1 < line.length()) {
                return line.substring(space + 1);
            }
        }
        return line == null ? "" : line;
    }

    private static String trailingIrcMessage(String rest, String command) {
        int cmd = rest.indexOf(command);
        if (cmd < 0) {
            return "";
        }
        int trail = rest.indexOf(" :", cmd + 1);
        return trail >= 0 ? rest.substring(trail + 2) : "";
    }

    private static String unescapeIrc(String value) {
        if (value == null || value.indexOf('\\') < 0) {
            return value == null ? "" : value;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                out.append(switch (next) {
                    case ':' -> ';';
                    case 's' -> ' ';
                    case 'r' -> '\r';
                    case 'n' -> '\n';
                    default -> next;
                });
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static Integer parseHexColor(String color) {
        if (color == null || !color.startsWith("#") || color.length() != 7) {
            return null;
        }
        try {
            return Integer.parseInt(color.substring(1), 16);
        } catch (NumberFormatException ignored) {
            return null;
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

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static ChatAuthorRoles rolesFromBadges(String badgesTag) {
        return ChatAuthorRoles.fromTwitchBadges(badgesTag);
    }

    @Override
    public void disconnect() {
        running = false;
        try {
            if (writer != null) {
                sendRaw("PART #" + channelName.toLowerCase());
            }
        } catch (IOException ignored) {}
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {}
        if (listenThread != null) {
            try {
                listenThread.join(1000);
            } catch (InterruptedException ignored) {}
        }
        TakeYourMineStreamClient.LOGGER.info("Disconnected from Twitch IRC");
    }

    @Override
    public boolean isConnected() {
        return running;
    }
}
