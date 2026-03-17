package springVibe.dev.users.youtubeComment.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class YoutubeUrlParser {
    private YoutubeUrlParser() {
    }

    public static String extractVideoId(String inputUrl) {
        if (inputUrl == null) {
            return null;
        }
        String url = inputUrl.trim();
        if (url.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(url);

            // https://youtu.be/{id}
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();
            if (host != null && host.contains("youtu.be")) {
                String p = path.startsWith("/") ? path.substring(1) : path;
                return firstSegment(p);
            }

            // https://www.youtube.com/watch?v={id}
            Map<String, String> q = parseQuery(uri.getRawQuery());
            if (q.containsKey("v")) {
                return q.get("v");
            }

            // https://www.youtube.com/shorts/{id} , /live/{id} , /embed/{id}
            if (path.startsWith("/shorts/")) {
                return firstSegment(path.substring("/shorts/".length()));
            }
            if (path.startsWith("/live/")) {
                return firstSegment(path.substring("/live/".length()));
            }
            if (path.startsWith("/embed/")) {
                return firstSegment(path.substring("/embed/".length()));
            }
        } catch (Exception ignored) {
            // fallthrough
        }

        return null;
    }

    private static String firstSegment(String p) {
        if (p == null) {
            return null;
        }
        int slash = p.indexOf('/');
        return (slash >= 0 ? p.substring(0, slash) : p);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> out = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return out;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = decode(pair.substring(0, idx));
            String v = decode(pair.substring(idx + 1));
            if (!k.isBlank()) {
                out.put(k, v);
            }
        }
        return out;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}

