package takeyourminestream.ijustseen.integration.chat;

import net.minecraft.client.MinecraftClient;
import takeyourminestream.ijustseen.config.ConfigManager;
import takeyourminestream.ijustseen.config.ModConfig;
import takeyourminestream.ijustseen.filtering.BanwordManager;
import takeyourminestream.ijustseen.filtering.BlockedUsernameManager;
import takeyourminestream.ijustseen.filtering.FilteringManager;
import takeyourminestream.ijustseen.messages.MessageEmote;
import takeyourminestream.ijustseen.messages.MessageSpawner;
import takeyourminestream.ijustseen.messages.SevenTVEmoteProvider;
import takeyourminestream.ijustseen.messages.TwitchEmoteTextureCache;
import takeyourminestream.ijustseen.messages.UnicodeEmojiParser;
import takeyourminestream.ijustseen.messages.EmojiTextureCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChatMessagePipeline {
    /** Читаемые цвета ников для платформ, которые не присылают свой цвет. */
    private static final int[] FALLBACK_NICK_COLORS = {
        0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF,
        0xFFFF55, 0xFFAA00, 0x00AA00, 0x00AAAA, 0xAA00AA
    };

    /** 50 ms на тик (20 TPS); timestampUsec — микросекунды. */
    private static final long MICROS_PER_TICK = 50_000L;
    /** Не откладывать показ дольше 10 с из-за разрыва в timestamp. */
    private static final int MAX_GAP_TICKS = 200;

    private final MessageSpawner messageSpawner;
    private final Map<String, Integer> nameColorCache = new HashMap<>();

    public ChatMessagePipeline(MessageSpawner messageSpawner) {
        this.messageSpawner = messageSpawner;
    }

    private record PreparedSpawn(String fullText, Integer authorColorRgb, List<MessageEmote> emotes) {}

    public void process(IncomingChatMessage message) {
        if (message == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            PreparedSpawn prepared = prepare(message);
            if (prepared != null) {
                messageSpawner.setCurrentMessage(prepared.fullText(), prepared.authorColorRgb(), prepared.emotes());
            }
        });
    }

    /**
     * Пачка YouTube из одного poll: сортировка по timestamp и показ с реальными интервалами.
     */
    public void processYouTubeBatch(List<IncomingChatMessage> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            List<IncomingChatMessage> sorted = new ArrayList<>(batch);
            sorted.sort(Comparator.comparingLong(m ->
                m.sourceTimestampMicros() != null ? m.sourceTimestampMicros() : 0L
            ));

            int baseTick = messageSpawner.getTickCounter();
            int readyTick = baseTick;
            long prevTimestampMicros = -1L;
            boolean spreadByTimestamp = sorted.size() > 1;

            for (IncomingChatMessage message : sorted) {
                Long timestampMicros = message.sourceTimestampMicros();
                if (spreadByTimestamp && timestampMicros != null && prevTimestampMicros >= 0) {
                    long gapMicros = Math.max(0L, timestampMicros - prevTimestampMicros);
                    int gapTicks = (int) Math.min(gapMicros / MICROS_PER_TICK, MAX_GAP_TICKS);
                    readyTick += gapTicks;
                }
                if (timestampMicros != null) {
                    prevTimestampMicros = timestampMicros;
                }

                PreparedSpawn prepared = prepare(message);
                if (prepared != null) {
                    messageSpawner.enqueueMessage(
                        prepared.fullText(),
                        prepared.authorColorRgb(),
                        prepared.emotes(),
                        readyTick
                    );
                }
            }
        });
    }

    private PreparedSpawn prepare(IncomingChatMessage message) {
        if (message.text() == null || message.text().isBlank()) {
            return null;
        }

        String displayName = message.displayName() != null && !message.displayName().isBlank()
            ? message.displayName()
            : message.authorLogin();
        String userLogin = message.authorLogin() != null ? message.authorLogin() : displayName;
        String filteredText = message.text().replace("͏", "").trim();
        if (filteredText.isEmpty()) {
            return null;
        }

        if (ModConfig.isENABLE_USERNAME_BLOCKLIST()
            && BlockedUsernameManager.getInstance().isBlocked(displayName, userLogin)) {
            return null;
        }

        if (!message.roles().passes(ModConfig.getCHAT_ROLE_FILTER())) {
            return null;
        }

        if (FilteringManager.getInstance().fitsRegexp(filteredText)) {
            return null;
        }

        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return null;
        }
        int chance = ConfigManager.getInstance().getConfigData().getChanceForSpawn();
        if (chance <= 0) {
            return null;
        }
        if (chance < 100 && player.getRandom().nextInt(100) >= chance) {
            return null;
        }

        if (message.platform() == ChatPlatform.TWITCH) {
            if (message.twitchRoomId() != null) {
                SevenTVEmoteProvider.init(message.twitchChannelName(), message.twitchRoomId());
            }
        }

        if (ModConfig.isENABLE_AUTOMODERATION() && BanwordManager.getInstance().containsBanwords(filteredText)) {
            filteredText = BanwordManager.getInstance().filterBanwords(filteredText);
        }

        Integer rgb = message.authorColorRgb();
        String colorKey = displayName.toLowerCase();
        if (rgb != null) {
            nameColorCache.put(colorKey, rgb);
        } else {
            rgb = nameColorCache.computeIfAbsent(colorKey, key ->
                FALLBACK_NICK_COLORS[Math.floorMod(key.hashCode(), FALLBACK_NICK_COLORS.length)]);
        }

        String colorPrefix = toNearestSectionColor(rgb);
        String coloredName = colorPrefix + displayName + ":§r ";
        List<MessageEmote> parsedEmotes = new ArrayList<>();

        if (message.platform() == ChatPlatform.TWITCH
            && message.twitchEmotesTag() != null
            && filteredText.length() == message.text().length()) {
            parsedEmotes = new ArrayList<>(parseTwitchEmotes(
                message.twitchEmotesTag(),
                filteredText,
                coloredName.length()
            ));
            for (MessageEmote emote : parsedEmotes) {
                TwitchEmoteTextureCache.preload(emote.getProvider(), emote.getEmoteId());
            }
        } else if (!message.emotes().isEmpty()) {
            int bodyOffset = coloredName.length();
            for (MessageEmote emote : message.emotes()) {
                parsedEmotes.add(new MessageEmote(
                    emote.getProvider(),
                    emote.getEmoteId(),
                    emote.getEmoteCode(),
                    emote.getStartIndex() + bodyOffset,
                    emote.getEndIndex() + bodyOffset
                ));
                TwitchEmoteTextureCache.preload(emote.getProvider(), emote.getEmoteId());
            }
        }

        String fullText = coloredName + filteredText;
        if (message.platform() == ChatPlatform.TWITCH) {
            List<MessageEmote> sevenTvEmotes = SevenTVEmoteProvider.scanForEmotes(fullText, parsedEmotes);
            if (!sevenTvEmotes.isEmpty()) {
                List<MessageEmote> allEmotes = new ArrayList<>(parsedEmotes);
                allEmotes.addAll(sevenTvEmotes);
                parsedEmotes = UnicodeEmojiParser.mergeNonOverlapping(allEmotes, List.of());
            }
        }

        if (ModConfig.isENABLE_COLOR_EMOJIS()) {
            List<MessageEmote> emojiEmotes = UnicodeEmojiParser.parse(fullText, parsedEmotes);
            if (!emojiEmotes.isEmpty()) {
                parsedEmotes = UnicodeEmojiParser.mergeNonOverlapping(parsedEmotes, emojiEmotes);
                for (MessageEmote emoji : emojiEmotes) {
                    EmojiTextureCache.ensureLoaded(emoji.getEmoteId(), emoji.getEmoteCode());
                }
            }
        }

        List<MessageEmote> emotesWithIcon = new ArrayList<>(parsedEmotes);
        emotesWithIcon.add(new MessageEmote(
            "platform",
            message.platform().getIconKey(),
            "",
            -1,
            -1
        ));

        return new PreparedSpawn(fullText, rgb, emotesWithIcon);
    }

    private static List<MessageEmote> parseTwitchEmotes(String emotesTag, String message, int messageStartOffset) {
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

    private static String toNearestSectionColor(int rgb) {
        String[] codes = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};
        int[] colors = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int bestIdx = 10;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < colors.length; i++) {
            int cr = (colors[i] >> 16) & 0xFF;
            int cg = (colors[i] >> 8) & 0xFF;
            int cb = colors[i] & 0xFF;
            long dr = r - cr;
            long dg = g - cg;
            long db = b - cb;
            long dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        return codes[bestIdx];
    }
}
