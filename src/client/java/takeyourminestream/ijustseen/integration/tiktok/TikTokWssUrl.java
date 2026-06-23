package takeyourminestream.ijustseen.integration.tiktok;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

final class TikTokWssUrl {
    private static final double LAST_RTT_MIN_MS = 100.0;
    private static final double LAST_RTT_RANDOM_SPREAD_MS = 100.0;
    private static final long HEARTBEAT_INTERVAL_MS = 10_000L;

    private TikTokWssUrl() {}

    static String build(String roomId) {
        String language = "en";
        String region = "US";
        String lastRtt = String.format("%.3f", LAST_RTT_MIN_MS + Math.random() * LAST_RTT_RANDOM_SPREAD_MS);
        String browserLang = language + "-" + region;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("version_code", "180800");
        params.put("device_platform", "web");
        params.put("cookie_enabled", "true");
        params.put("screen_width", "1920");
        params.put("screen_height", "1080");
        params.put("browser_language", browserLang);
        params.put("browser_platform", "Linux x86_64");
        params.put("browser_name", "Mozilla");
        params.put("browser_version", "5.0 (X11)");
        params.put("browser_online", "true");
        params.put("tz_name", TimeZone.getDefault().getID());
        params.put("app_name", "tiktok_web");
        params.put("sup_ws_ds_opt", "1");
        params.put("update_version_code", "2.0.0");
        params.put("compress", "gzip");
        params.put("webcast_language", language);
        params.put("ws_direct", "1");
        params.put("aid", "1988");
        params.put("live_id", "12");
        params.put("app_language", language);
        params.put("client_enter", "1");
        params.put("room_id", roomId);
        params.put("identity", "audience");
        params.put("history_comment_count", "6");
        params.put("last_rtt", lastRtt);
        params.put("heartbeat_duration", Long.toString(HEARTBEAT_INTERVAL_MS));
        params.put("resp_content_type", "protobuf");
        params.put("did_rule", "3");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return "wss://webcast-ws.tiktok.com/webcast/im/ws_proxy/ws_reuse_supplement/?" + sb;
    }
}
