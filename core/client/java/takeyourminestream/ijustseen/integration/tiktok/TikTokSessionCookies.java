package takeyourminestream.ijustseen.integration.tiktok;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Разбирает вставку из DevTools: целиком {@code Cookie} или
 * {@code sessionid=…; tt-target-idc=…}. Голый токен считается {@code sessionid}.
 */
public final class TikTokSessionCookies {
    private static final Set<String> NAMES = Set.of(
        "sessionid",
        "sessionid_ss",
        "sid_tt",
        "tt-target-idc",
        "mstoken"
    );

    private TikTokSessionCookies() {}

    public static Map<String, String> parse(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        String text = raw.replace('\r', ';').replace('\n', ';').trim();
        if (text.isEmpty()) {
            return out;
        }
        if (!text.contains("=")) {
            if (looksLikeSessionId(text)) {
                out.put("sessionid", text);
            }
            return out;
        }
        for (String part : text.split(";")) {
            String piece = part.trim();
            int eq = piece.indexOf('=');
            if (eq <= 0 || eq >= piece.length() - 1) {
                continue;
            }
            String name = canonicalName(piece.substring(0, eq).trim());
            String value = stripQuotes(piece.substring(eq + 1).trim());
            if (name != null && !value.isEmpty()) {
                out.put(name, value);
            }
        }
        return out;
    }

    public static boolean isSessionName(String name) {
        return name != null && NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    public static boolean hasSessionId(String raw) {
        return parse(raw).containsKey("sessionid") || parse(raw).containsKey("sessionid_ss");
    }

    /** Значение sessionid из голого токена или строки Cookie. */
    public static String sessionIdOf(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (!text.contains("=")) {
            return text;
        }
        Map<String, String> parsed = parse(text);
        if (parsed.containsKey("sessionid")) {
            return parsed.get("sessionid");
        }
        if (parsed.containsKey("sessionid_ss")) {
            return parsed.get("sessionid_ss");
        }
        if (parsed.containsKey("sid_tt")) {
            return parsed.get("sid_tt");
        }
        return "";
    }

    /**
     * Значение {@code tt-target-idc} из строки Cookie.
     * {@code null}, если ключа нет — голое значение для этого поля не разбирается здесь.
     */
    public static String targetIdcFromBundle(String raw) {
        if (raw == null || !raw.contains("=")) {
            return null;
        }
        return parse(raw).get("tt-target-idc");
    }

    /** Значение поля tt-target-idc: голое или {@code tt-target-idc=…} / строка Cookie. */
    public static String targetIdcOf(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (!text.contains("=")) {
            return text;
        }
        String fromBundle = parse(text).get("tt-target-idc");
        return fromBundle == null ? "" : fromBundle;
    }

    public static String compose(String sessionId, String targetIdc) {
        StringBuilder out = new StringBuilder();
        if (sessionId != null && !sessionId.isBlank()) {
            out.append("sessionid=").append(sessionId.trim());
        }
        if (targetIdc != null && !targetIdc.isBlank()) {
            if (!out.isEmpty()) {
                out.append("; ");
            }
            out.append("tt-target-idc=").append(targetIdc.trim());
        }
        return out.toString();
    }

    private static String canonicalName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "sessionid" -> "sessionid";
            case "sessionid_ss" -> "sessionid_ss";
            case "sid_tt" -> "sid_tt";
            case "tt-target-idc" -> "tt-target-idc";
            case "mstoken" -> "msToken";
            default -> null;
        };
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean looksLikeSessionId(String value) {
        if (value.length() < 16 || value.length() > 128) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                return false;
            }
        }
        return true;
    }
}
