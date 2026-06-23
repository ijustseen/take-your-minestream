package takeyourminestream.ijustseen.integration.tiktok;

import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.tiktok.proto.TikTokProto;
import takeyourminestream.ijustseen.messages.MessageEmote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

final class TikTokWebSocket {
    private static final long HEARTBEAT_INTERVAL_MS = 10_000L;
    private static final long STALE_CHECK_INTERVAL_MS = 5_000L;
    private static final long STALE_TIMEOUT_MS = 60_000L;
    private static final int MAX_FRAME_BYTES = 16 * 1024 * 1024;
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    interface ChatHandler {
        void onChat(long msgId, String uniqueId, String displayName, String text,
            takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles roles,
            List<MessageEmote> emotes);

        void onLiveEnded();
    }

    private final String roomId;
    private final String ttwid;
    private final ChatHandler handler;
    private volatile boolean running;
    private volatile WebSocket webSocket;
    private Thread maintenanceThread;

    TikTokWebSocket(String roomId, String ttwid, ChatHandler handler) {
        this.roomId = roomId;
        this.ttwid = ttwid;
        this.handler = handler;
    }

    void connect() {
        running = true;
        String url = TikTokWssUrl.build(roomId);
        HTTP_CLIENT.newWebSocketBuilder()
            .header("Cookie", "ttwid=" + ttwid)
            .header("User-Agent", USER_AGENT)
            .header("Origin", "https://www.tiktok.com")
            .header("Referer", "https://www.tiktok.com/")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cache-Control", "no-cache")
            .buildAsync(URI.create(url), new Listener())
            .whenComplete((ws, error) -> {
                if (error != null && running) {
                    TakeYourMineStreamClient.LOGGER.warn("TikTok WSS handshake failed: {}", error.getMessage());
                }
            });
    }

    void disconnect() {
        running = false;
        WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
        Thread thread = maintenanceThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    boolean isRunning() {
        return running;
    }

    private final class Listener implements WebSocket.Listener {
        private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();
        private final AtomicLong lastDataMs = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong lastHeartbeatSentMs = new AtomicLong(0L);

        @Override
        public void onOpen(WebSocket ws) {
            webSocket = ws;
            sendBinary(ws, TikTokFrames.buildHeartbeat(roomId));
            sendBinary(ws, TikTokFrames.buildEnterRoom(roomId));
            lastHeartbeatSentMs.set(System.currentTimeMillis());
            startMaintenance(ws);
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            lastDataMs.set(System.currentTimeMillis());
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            if (binaryBuffer.size() + chunk.length > MAX_FRAME_BYTES) {
                binaryBuffer.reset();
                TakeYourMineStreamClient.LOGGER.warn("TikTok WSS frame too large");
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "frame too large");
                ws.request(1);
                return null;
            }
            binaryBuffer.write(chunk, 0, chunk.length);

            if (last) {
                byte[] raw = binaryBuffer.toByteArray();
                binaryBuffer.reset();
                try {
                    processFrame(raw, ws);
                } catch (Exception e) {
                    TakeYourMineStreamClient.LOGGER.debug("TikTok WSS frame skip: {}", e.getMessage());
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            webSocket = null;
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            if (running) {
                TakeYourMineStreamClient.LOGGER.warn("TikTok WSS error: {}", error.getMessage());
            }
        }

        private void startMaintenance(WebSocket ws) {
            maintenanceThread = Thread.ofVirtual().name("TikTokWss-" + roomId).start(() -> {
                while (running && webSocket == ws) {
                    try {
                        Thread.sleep(STALE_CHECK_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (!running || webSocket != ws) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastDataMs.get() > STALE_TIMEOUT_MS) {
                        TakeYourMineStreamClient.LOGGER.info("TikTok WSS stale for @{}, reconnecting", roomId);
                        ws.sendClose(WebSocket.NORMAL_CLOSURE, "stale");
                        return;
                    }
                    if (now - lastHeartbeatSentMs.get() >= HEARTBEAT_INTERVAL_MS) {
                        sendBinary(ws, TikTokFrames.buildHeartbeat(roomId));
                        lastHeartbeatSentMs.set(now);
                    }
                }
            });
        }
    }

    private void processFrame(byte[] raw, WebSocket ws) throws IOException {
        TikTokProto.ProtoMap frame;
        try {
            frame = TikTokProto.decode(raw);
        } catch (IllegalArgumentException e) {
            TakeYourMineStreamClient.LOGGER.debug("TikTok WSS outer frame parse skip: {}", e.getMessage());
            return;
        }
        if (!"msg".equals(frame.getString(7))) {
            return;
        }

        byte[] payload = TikTokFrames.decompressIfGzipped(frame.getRawBytes(8));
        TikTokProto.ProtoMap response;
        try {
            response = TikTokProto.decode(payload);
        } catch (IllegalArgumentException e) {
            TakeYourMineStreamClient.LOGGER.debug("TikTok WSS payload parse skip: {}", e.getMessage());
            return;
        }

        boolean needsAck = response.getBool(9);
        byte[] internalExt = response.getRawBytes(5);
        if (needsAck && internalExt.length > 0) {
            long logId = frame.getVarint(2);
            sendBinary(ws, TikTokFrames.buildAck(logId, internalExt));
        }

        for (TikTokProto.ProtoMap message : response.getRepeatedMessages(1)) {
            dispatchMessage(message);
        }
    }

    private void dispatchMessage(TikTokProto.ProtoMap message) {
        try {
            String method = message.getString(1);
            byte[] payload = message.getRawBytes(2);
            if ("WebcastChatMessage".equals(method)) {
                handleChat(payload);
            } else if ("WebcastEmoteChatMessage".equals(method)) {
                handleEmoteChat(payload);
            } else if ("WebcastControlMessage".equals(method)) {
                TikTokProto.ProtoMap control = TikTokProto.decode(payload);
                if (control.getInt(2) == 3) {
                    handler.onLiveEnded();
                }
            }
        } catch (IllegalArgumentException e) {
            TakeYourMineStreamClient.LOGGER.debug("TikTok WSS message parse skip: {}", e.getMessage());
        }
    }

    private void handleChat(byte[] payload) {
        TikTokProto.ProtoMap chat = TikTokProto.decode(payload);
        String text = chat.getString(3);
        if (text.isBlank()) {
            return;
        }

        TikTokProto.ProtoMap common = chat.getMessage(1);
        long msgId = common.getVarint(2);

        TikTokProto.ProtoMap user = chat.getMessage(2);
        String displayName = user.getString(3);
        if (displayName.isBlank()) {
            displayName = "Viewer";
        }
        String uniqueId = user.getString(38);
        if (uniqueId.isBlank()) {
            uniqueId = displayName;
        }

        List<MessageEmote> emotes = TikTokEmoteParser.parseChatEmotes(chat, text);
        String roleHints = collectTikTokUserRoleHints(user);
        handler.onChat(
            msgId,
            uniqueId,
            displayName,
            text,
            takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles.fromBadgeHints(roleHints),
            emotes
        );
    }

    private void handleEmoteChat(byte[] payload) {
        TikTokProto.ProtoMap chat = TikTokProto.decode(payload);
        TikTokProto.ProtoMap emoteDetails = chat.getMessage(3);
        MessageEmote emote = TikTokEmoteParser.parseStandaloneEmote(emoteDetails);
        if (emote == null) {
            return;
        }

        TikTokProto.ProtoMap common = chat.getMessage(1);
        long msgId = common.getVarint(2);

        TikTokProto.ProtoMap user = chat.getMessage(2);
        String displayName = user.getString(3);
        if (displayName.isBlank()) {
            displayName = "Viewer";
        }
        String uniqueId = user.getString(38);
        if (uniqueId.isBlank()) {
            uniqueId = displayName;
        }

        String roleHints = collectTikTokUserRoleHints(user);
        handler.onChat(
            msgId,
            uniqueId,
            displayName,
            TikTokEmoteParser.standaloneEmoteText(),
            takeyourminestream.ijustseen.integration.chat.ChatAuthorRoles.fromBadgeHints(roleHints),
            List.of(emote)
        );
    }

    /** Собирает текстовые подсказки о ролях из protobuf-пользователя TikTok. */
    private static String collectTikTokUserRoleHints(TikTokProto.ProtoMap user) {
        StringBuilder hints = new StringBuilder();
        for (int field : new int[] {9, 11, 22, 46, 61, 102}) {
            appendIfPresent(hints, user.getString(field));
            TikTokProto.ProtoMap nested = user.getMessage(field);
            appendIfPresent(hints, nested.getString(1));
            appendIfPresent(hints, nested.getString(2));
            appendIfPresent(hints, nested.getString(3));
            for (TikTokProto.ProtoMap badge : nested.getRepeatedMessages(1)) {
                appendIfPresent(hints, badge.getString(1));
                appendIfPresent(hints, badge.getString(2));
                appendIfPresent(hints, badge.getString(3));
            }
        }
        return hints.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

    private void sendBinary(WebSocket ws, byte[] payload) {
        if (ws == null || ws.isOutputClosed()) {
            return;
        }
        ws.sendBinary(ByteBuffer.wrap(payload), true).exceptionally(error -> {
            if (running) {
                TakeYourMineStreamClient.LOGGER.warn("TikTok WSS send failed: {}", error.getMessage());
            }
            return null;
        });
    }
}
