package takeyourminestream.ijustseen;

import net.minecraft.client.MinecraftClient;
import takeyourminestream.ijustseen.filtering.BanwordManager;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.messages.MessageEmote;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.messages.SevenTVEmoteProvider;
import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import takeyourminestream.ijustseen.ModConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwitchChatClient {
    private SSLSocket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private Thread listenThread;
    private volatile boolean running = false;
    private final Random random = new Random();
    private final MessageSpawner messageSpawner;
    private String channelName;
    private final Map<String, Integer> nameColorCache = new HashMap<>();

    public TwitchChatClient(String channelName, MessageSpawner messageSpawner) {
        this.channelName = channelName;
        this.messageSpawner = messageSpawner;
        connect();
    }

    private String getRandomColor() {
        return ModConfig.getNICK_COLORS()[random.nextInt(ModConfig.getNICK_COLORS().length)];
    }

    private void connect() {
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) factory.createSocket("irc.chat.twitch.tv", 6697);
            socket.startHandshake();
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Запрашиваем расширенные возможности Twitch IRC
            sendRaw("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership");

            // Анонимная авторизация (можно заменить на свой логин/токен при необходимости)
            String nick = "justinfan" + (10000 + random.nextInt(90000));
            sendRaw("PASS oauth:anonymous");
            sendRaw("NICK " + nick);
            sendRaw("JOIN #" + channelName.toLowerCase());

            running = true;
            listenThread = new Thread(this::listenLoop, "TwitchIRC-Listen");
            listenThread.setDaemon(true);
            listenThread.start();
            System.out.println("[IRC] Connected (TLS) to Twitch IRC as " + nick + ", joined #" + channelName);

            // Загружаем глобальные 7TV эмоуты; канальные подгрузим при первом PRIVMSG, когда узнаем room-id
            SevenTVEmoteProvider.init(null);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[IRC] Failed to connect to Twitch IRC");
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
                } else if (line.contains(" CLEARCHAT ")) {
                    // Сообщение о чистке чата/таймауте/бане
                    System.out.println("[IRC] CLEARCHAT: " + line);
                } else if (line.contains(" NOTICE ")) {
                    System.out.println("[IRC] NOTICE: " + line);
                }
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
        System.out.println("[IRC] Disconnected from Twitch IRC");
        // Реконнект с задержкой, если запущено
        if (running) {
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

    private void handlePrivMsg(String line) throws IOException {
        // Пример с тегами:
        // @badge-info=;badges=;color=#1E90FF;display-name=User;... :user!user@user.tmi.twitch.tv PRIVMSG #channel :message
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
        if (excl > 1 && colon > 1 && hash > 1) {
            String user = rest.substring(1, excl);
            String message = rest.substring(colon + 2);
            String channel = rest.substring(hash + 1, rest.indexOf(' ', hash));

            message = message.replace("͏", "").trim();

            // skip if regular expression bans it
            if (FilteringManager.getInstance().fitsRegexp(message)) {
                return;
            }
            // skip if chance did not proc
            if (MinecraftClient.getInstance().player.getRandom().nextInt(100) > ConfigManager.getInstance().getConfigData().getChanceForSpawn()) {
                return;
            }

            String displayName = user;
            Integer rgb = null;
            String emotesTag = null;
            String roomId = null;
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
                    }
                }
            }

            if (roomId != null) {
                SevenTVEmoteProvider.init(channelName, roomId);
            }

            System.out.println("[" + channel + "] " + displayName + ": " + message);
            String filteredMessage = message;
            if (ModConfig.isENABLE_AUTOMODERATION() && BanwordManager.getInstance().containsBanwords(message)) {
                filteredMessage = BanwordManager.getInstance().filterBanwords(message);
            }
            if (rgb != null) {
                nameColorCache.put(displayName.toLowerCase(), rgb);
            } else {
                Integer cached = nameColorCache.get(displayName.toLowerCase());
                if (cached != null) rgb = cached;
            }

            String colorPrefix = (rgb != null) ? toNearestSectionColor(rgb) : "§a";
            String coloredName = colorPrefix + displayName + ":§r ";
            List<MessageEmote> parsedEmotes = Collections.emptyList();
            if (emotesTag != null && filteredMessage.length() == message.length()) {
                parsedEmotes = parseTwitchEmotes(emotesTag, filteredMessage, coloredName.length());
                // Предзагрузка текстур эмоутов — начинаем скачивание сразу, не дожидаясь рендера
                for (MessageEmote emote : parsedEmotes) {
                    takeyourminestream.ijustseen.messages.TwitchEmoteTextureCache.preload(emote.getEmoteId());
                }
            }
            // Сканируем текст на 7TV-эмоуты (те, которые не были найдены через Twitch IRC tags)
            String fullText = coloredName + filteredMessage;
            List<MessageEmote> sevenTvEmotes = SevenTVEmoteProvider.scanForEmotes(fullText, parsedEmotes);
            if (!sevenTvEmotes.isEmpty()) {
                List<MessageEmote> allEmotes = new ArrayList<>(parsedEmotes);
                allEmotes.addAll(sevenTvEmotes);
                parsedEmotes = List.copyOf(allEmotes);
            }

            messageSpawner.setCurrentMessage(fullText, rgb, parsedEmotes);
        }
    }

    private List<MessageEmote> parseTwitchEmotes(String emotesTag, String message, int messageStartOffset) {
        if (emotesTag == null || emotesTag.isEmpty()) {
            return Collections.emptyList();
        }

        List<MessageEmote> emotes = new ArrayList<>();
        String[] emoteParts = emotesTag.split("/");
        for (String emotePart : emoteParts) {
            int colonIndex = emotePart.indexOf(':');
            if (colonIndex <= 0 || colonIndex >= emotePart.length() - 1) {
                continue;
            }

            String emoteId = emotePart.substring(0, colonIndex);
            String[] ranges = emotePart.substring(colonIndex + 1).split(",");
            for (String range : ranges) {
                int dashIndex = range.indexOf('-');
                if (dashIndex <= 0 || dashIndex >= range.length() - 1) {
                    continue;
                }

                try {
                    int start = Integer.parseInt(range.substring(0, dashIndex));
                    int end = Integer.parseInt(range.substring(dashIndex + 1));
                    if (start < 0 || end < start || end >= message.length()) {
                        continue;
                    }

                    String emoteCode = message.substring(start, end + 1);
                    emotes.add(new MessageEmote("twitch", emoteId, emoteCode, messageStartOffset + start, messageStartOffset + end));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return emotes.isEmpty() ? Collections.emptyList() : List.copyOf(emotes);
    }

    private String toNearestSectionColor(int rgb) {
        // Сопоставление базовых цветов Minecraft к hex (RGB)
        String[] codes = {"§0","§1","§2","§3","§4","§5","§6","§7","§8","§9","§a","§b","§c","§d","§e","§f"};
        int[] colors = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int bestIdx = 10; // §a (зелёный) по умолчанию
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < colors.length; i++) {
            int cr = (colors[i] >> 16) & 0xFF;
            int cg = (colors[i] >> 8) & 0xFF;
            int cb = colors[i] & 0xFF;
            long dr = r - cr;
            long dg = g - cg;
            long db = b - cb;
            long dist = dr*dr + dg*dg + db*db;
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        return codes[bestIdx];
    }

    public void disconnect() {
        running = false;
        try {
            if (writer != null) sendRaw("PART #" + channelName.toLowerCase());
        } catch (IOException ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        if (listenThread != null) {
            try { listenThread.join(1000); } catch (InterruptedException ignored) {}
        }
        System.out.println("[IRC] Disconnected from Twitch chat.");
    }
} 