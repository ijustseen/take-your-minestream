package takeyourminestream.ijustseen.integration.chat;

import takeyourminestream.ijustseen.TakeYourMineStreamClient;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP-утилиты чат-интеграций. Использует java.net.http.HttpClient:
 * legacy HttpURLConnection блокируется анти-бот защитой некоторых платформ (Kick).
 */
public final class ChatHttp {
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final String TTWID_REGISTER_URL = "https://ttwid.bytedance.com/ttwid/union/register/";
    private static final String TTWID_REGISTER_BODY =
        "{\"aid\":1988,\"service\":\"www.tiktok.com\",\"union\":true,\"needFid\":false}";

    private static final CookieManager COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(TIMEOUT)
        .cookieHandler(COOKIE_MANAGER)
        .build();

    private ChatHttp() {}

    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, String referer) throws IOException {
        HttpRequest.Builder builder = baseBuilder(url, referer).GET();
        return sendForBody(builder.build(), url);
    }

    public static String fetchTtwidCookie() throws IOException {
        return fetchTtwidCookie(null);
    }

    /**
     * Получает {@code ttwid} для TikTok WebSocket.
     * Сначала прогревает сессию через страницу TikTok (с cookie jar на редиректах),
     * затем при необходимости запрашивает cookie через ByteDance register API.
     */
    public static String fetchTtwidCookie(String tiktokUsername) throws IOException {
        warmUpTikTokSession("https://www.tiktok.com/");
        String ttwid = findTtwidInCookieJar();
        if (ttwid != null) {
            return ttwid;
        }

        if (tiktokUsername != null && !tiktokUsername.isBlank()) {
            String user = tiktokUsername.trim().replace("@", "");
            warmUpTikTokSession(
                "https://www.tiktok.com/@"
                    + URLEncoder.encode(user, StandardCharsets.UTF_8)
                    + "/live"
            );
            ttwid = findTtwidInCookieJar();
            if (ttwid != null) {
                return ttwid;
            }
        }

        ttwid = fetchTtwidFromRegisterApi();
        if (ttwid != null) {
            return ttwid;
        }

        throw new IOException("ttwid cookie missing in TikTok response");
    }

    public static String postJson(String url, String jsonBody) throws IOException {
        return postJson(url, jsonBody, Map.of());
    }

    public static String postJson(String url, String jsonBody, Map<String, String> extraHeaders) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        return sendForBody(builder.build(), url);
    }

    private static void warmUpTikTokSession(String url) throws IOException {
        HttpRequest request = tiktokDocumentBuilder(url).GET().build();
        HttpResponse<String> response = send(request, url);
        if (response.statusCode() >= 400) {
            TakeYourMineStreamClient.LOGGER.debug("TikTok warm-up HTTP {} for {}", response.statusCode(), url);
            return;
        }
        for (String header : response.headers().allValues("set-cookie")) {
            String ttwid = parseTtwidFromSetCookieHeader(header);
            if (ttwid != null) {
                storeTtwidCookie(url, ttwid);
                return;
            }
        }
    }

    private static String fetchTtwidFromRegisterApi() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(TTWID_REGISTER_URL))
            .timeout(TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "https://www.tiktok.com")
            .header("Referer", "https://www.tiktok.com/")
            .POST(HttpRequest.BodyPublishers.ofString(TTWID_REGISTER_BODY))
            .build();

        HttpResponse<String> response = send(request, TTWID_REGISTER_URL);
        if (response.statusCode() >= 400) {
            TakeYourMineStreamClient.LOGGER.debug("TikTok ttwid register HTTP {}", response.statusCode());
            return null;
        }

        for (String header : response.headers().allValues("set-cookie")) {
            String ttwid = parseTtwidFromSetCookieHeader(header);
            if (ttwid != null) {
                storeTtwidCookie(TTWID_REGISTER_URL, ttwid);
                return ttwid;
            }
        }
        return findTtwidInCookieJar();
    }

    private static void storeTtwidCookie(String url, String value) {
        HttpCookie cookie = new HttpCookie("ttwid", value);
        cookie.setDomain(".tiktok.com");
        cookie.setPath("/");
        COOKIE_MANAGER.getCookieStore().add(URI.create(url), cookie);
    }

    private static String findTtwidInCookieJar() {
        for (HttpCookie cookie : COOKIE_MANAGER.getCookieStore().getCookies()) {
            if ("ttwid".equalsIgnoreCase(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static String parseTtwidFromSetCookieHeader(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            for (HttpCookie cookie : HttpCookie.parse(header)) {
                if ("ttwid".equalsIgnoreCase(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        } catch (IllegalArgumentException ignored) {
            // SameSite=None и прочие атрибуты иногда ломают HttpCookie.parse
        }

        String lower = header.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("ttwid=");
        if (idx < 0) {
            return null;
        }
        int start = idx + "ttwid=".length();
        int end = header.indexOf(';', start);
        String value = end < 0 ? header.substring(start) : header.substring(start, end);
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static HttpRequest.Builder baseBuilder(String url, String referer) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/html, */*")
            .header("Accept-Language", "en-US,en;q=0.9");
        if (referer != null && !referer.isBlank()) {
            builder.header("Referer", referer);
        }
        return builder;
    }

    private static HttpRequest.Builder tiktokDocumentBuilder(String url) {
        HttpRequest.Builder builder = baseBuilder(url, "https://www.tiktok.com/");
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        builder.header("Sec-Fetch-Dest", "document");
        builder.header("Sec-Fetch-Mode", "navigate");
        builder.header("Sec-Fetch-Site", "none");
        builder.header("Upgrade-Insecure-Requests", "1");
        return builder;
    }

    private static String sendForBody(HttpRequest request, String url) throws IOException {
        HttpResponse<String> response = send(request, url);
        String body = response.body();
        if (response.statusCode() >= 400 && (body == null || body.isBlank())) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return body;
    }

    private static HttpResponse<String> send(HttpRequest request, String url) throws IOException {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while requesting " + url, e);
        }
    }
}
