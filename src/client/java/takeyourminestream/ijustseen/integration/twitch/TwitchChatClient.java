package takeyourminestream.ijustseen.integration.twitch;

import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.chat.ChatConnection;
import takeyourminestream.ijustseen.integration.chat.ChatMessagePipeline;
import takeyourminestream.ijustseen.integration.chat.ChatPlatform;
import takeyourminestream.ijustseen.integration.chat.IncomingChatMessage;
import takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles;
import takeyourminestream.ijustseen.messages.SevenTVEmoteProvider;

import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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
                } else if (line.startsWith("@") && line.contains(" ROOMSTATE ")) {
                    String roomId = extractTagValue(line, "room-id");
                    if (roomId != null && !roomId.isBlank()) {
                        SevenTVEmoteProvider.init(channelName, roomId);
                    }
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

    private String extractTagValue(String ircLine, String tagName) {
        if (ircLine == null || !ircLine.startsWith("@")) {
            return null;
        }

        int firstSpace = ircLine.indexOf(' ');
        if (firstSpace <= 1) {
            return null;
        }

        String tagsPart = ircLine.substring(1, firstSpace);
        String[] tags = tagsPart.split(";");
        for (String tag : tags) {
            int eq = tag.indexOf('=');
            String key = eq >= 0 ? tag.substring(0, eq) : tag;
            String val = eq >= 0 ? tag.substring(eq + 1) : "";
            if (tagName.equals(key)) {
                return val;
            }
        }
        return null;
    }

    private void handlePrivMsg(String line) {
        String tagsPart = null;
        String rest = line;
        if (line.startsWith("@")) {
            int space = line.indexOf(' ');
            tagsPart = line.substring(1, space);
            rest = line.substring(space + 1);
        }
        int excl = rest.indexOf('!');
        int colon = rest.indexOf(" :", 1);
        int hash = rest.indexOf("#");
        if (excl <= 1 || colon <= 1 || hash <= 1) {
            return;
        }

        String user = rest.substring(1, excl);
        String message = rest.substring(colon + 2);

        String displayName = user;
        Integer rgb = null;
        String emotesTag = null;
        String roomId = null;
        String badgesTag = null;
        if (tagsPart != null) {
            String[] tags = tagsPart.split(";");
            for (String tag : tags) {
                int eq = tag.indexOf('=');
                String key = eq >= 0 ? tag.substring(0, eq) : tag;
                String val = eq >= 0 ? tag.substring(eq + 1) : "";
                if (key.equals("display-name") && !val.isEmpty()) {
                    displayName = val;
                } else if (key.equals("color") && val.startsWith("#") && val.length() == 7) {
                    try {
                        rgb = Integer.parseInt(val.substring(1), 16);
                    } catch (NumberFormatException ignored) {}
                } else if (key.equals("emotes") && !val.isEmpty()) {
                    emotesTag = val;
                } else if (key.equals("room-id") && !val.isEmpty()) {
                    roomId = val;
                } else if (key.equals("badges")) {
                    badgesTag = val;
                }
            }
        }

        ChatAuthorRoles roles = rolesFromBadges(badgesTag);
        pipeline.process(IncomingChatMessage.builder(ChatPlatform.TWITCH)
            .authorLogin(user)
            .displayName(displayName)
            .text(message)
            .authorColorRgb(rgb)
            .roles(roles)
            .twitchRoomId(roomId)
            .twitchChannelName(channelName)
            .twitchEmotesTag(emotesTag)
            .build());
    }

    private static ChatAuthorRoles rolesFromBadges(String badgesTag) {
        return ChatAuthorRoles.fromBadgeHints(badgesTag);
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
