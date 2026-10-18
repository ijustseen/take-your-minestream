package takeyourminestream.ijustseen.integration.tiktok;

import takeyourminestream.ijustseen.TakeYourMineStreamClient;
import takeyourminestream.ijustseen.integration.chat.ChatHttp;
import takeyourminestream.ijustseen.integration.tiktok.proto.TikTokProto;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * HTTP {@code /webcast/im/fetch/}: стартовые сообщения, cursor и адрес WSS.
 * Без этого сокет часто отдаёт только часть чата.
 */
final class TikTokImFetch {
    private static final long DEFAULT_INTERVAL_MS = 2_000L;
    private static final long MIN_INTERVAL_MS = 1_500L;

    record Result(
        List<TikTokProto.ProtoMap> messages,
        String cursor,
        long fetchIntervalMs,
        String pushServer,
        String internalExt
    ) {
        boolean hasCursor() {
            return cursor != null && !cursor.isBlank();
        }

        boolean hasPushServer() {
            return pushServer != null && !pushServer.isBlank();
        }
    }

    private TikTokImFetch() {}

    static Result tryFetch(String roomId, String cursor) {
        try {
            return fetch(roomId, cursor);
        } catch (Exception e) {
            TakeYourMineStreamClient.LOGGER.debug("TikTok IM fetch skip: {}", e.getMessage());
            return null;
        }
    }

    static Result fetch(String roomId, String cursor) throws IOException {
        byte[] body = ChatHttp.getBytes(
            buildUrl(roomId, cursor),
            "https://www.tiktok.com/",
            Map.of(
                "Origin", "https://www.tiktok.com",
                "Cookie", ChatHttp.cookieHeader("https://www.tiktok.com/")
            )
        );
        if (body.length == 0 || looksLikeHtml(body)) {
            throw new IOException("empty or HTML IM fetch response");
        }
        TikTokProto.ProtoMap response = TikTokProto.decode(TikTokFrames.decompressIfGzipped(body));
        String nextCursor = firstNonBlank(response.getString(2), cursor);
        long interval = response.getVarint(3);
        return new Result(
            response.getRepeatedMessages(1),
            nextCursor,
            interval > 0L ? Math.max(MIN_INTERVAL_MS, interval) : DEFAULT_INTERVAL_MS,
            firstNonBlank(response.getString(14), response.getString(13)),
            response.getString(5)
        );
    }

    static String buildUrl(String roomId, String cursor) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("aid", "1988");
        params.put("app_language", "en");
        params.put("app_name", "tiktok_web");
        params.put("browser_language", "en-US");
        params.put("browser_name", "Mozilla");
        params.put("browser_online", "true");
        params.put("browser_platform", "Win32");
        params.put("browser_version", "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        params.put("cookie_enabled", "true");
        params.put("cursor", cursor != null ? cursor : "");
        params.put("device_platform", "web");
        params.put("focus_state", "true");
        params.put("from_page", "user");
        params.put("history_comment_count", "36");
        params.put("is_fullscreen", "false");
        params.put("is_page_visible", "true");
        params.put("did_rule", "3");
        params.put("fetch_rule", "1");
        params.put("last_rtt", "0");
        params.put("live_id", "12");
        params.put("resp_content_type", "protobuf");
        params.put("room_id", roomId);
        params.put("screen_height", "1080");
        params.put("screen_width", "1920");
        params.put("tz_name", TimeZone.getDefault().getID());
        params.put("version_code", "180800");
        params.put("webcast_language", "en");
        params.put("identity", "audience");

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return "https://webcast.tiktok.com/webcast/im/fetch/?" + query;
    }

    private static boolean looksLikeHtml(byte[] body) {
        int n = Math.min(body.length, 64);
        String head = new String(body, 0, n, StandardCharsets.US_ASCII).trim().toLowerCase();
        return head.startsWith("<!doctype") || head.startsWith("<html") || head.startsWith("{");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
