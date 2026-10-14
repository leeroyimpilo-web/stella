package za.co.mp3dork;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MediaTools {
    private MediaTools() {}
    public static String webUrl(String value) {
        if (value == null) throw new IllegalArgumentException("Paste an MP3 URL first.");
        String text = value.trim();
        URI uri;
        try { uri = URI.create(text); } catch (Exception e) { throw new IllegalArgumentException("Enter a complete HTTP or HTTPS link."); }
        String scheme = uri.getScheme();
        if ((!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalArgumentException("Use a complete HTTP or HTTPS link without a username or password.");
        return uri.toASCIIString();
    }
    public static String unwrapGoogle(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host != null && (host.equals("google.com") || host.endsWith(".google.com") || host.equals("google.co.za") || host.endsWith(".google.co.za")) && "/url".equals(uri.getPath()) && uri.getRawQuery() != null) {
                for (String pair : uri.getRawQuery().split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2 && (kv[0].equals("q") || kv[0].equals("url"))) return webUrl(URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
        } catch (Exception ignored) {}
        return value;
    }
    public static boolean isMp3Link(String url) {
        try { return URI.create(unwrapGoogle(url)).getPath().toLowerCase(Locale.ROOT).endsWith(".mp3"); } catch (Exception e) { return false; }
    }
    public static String fileName(String url, String disposition) {
        String candidate = "";
        if (disposition != null) {
            Matcher extended = Pattern.compile("(?i)filename\\*=UTF-8''([^;]+)").matcher(disposition);
            Matcher basic = Pattern.compile("(?i)filename=\"?([^\";]+)").matcher(disposition);
            try { if (extended.find()) candidate = URLDecoder.decode(extended.group(1), "UTF-8"); else if (basic.find()) candidate = basic.group(1); } catch (Exception ignored) {}
        }
        if (candidate.isEmpty()) try {
            String path = URI.create(url).getRawPath();
            candidate = URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1).replace("+", "%2B"), "UTF-8");
        } catch (Exception ignored) {}
        return cleanFileName(candidate);
    }
    public static String cleanFileName(String value) {
        String name = value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        name = name.replace("..", "_").replaceAll("^[. ]+|[. ]+$", "").replaceAll("\\s+", " ");
        if (name.isEmpty()) name = "audio";
        if (name.toLowerCase(Locale.ROOT).endsWith(".mp3")) name = name.substring(0, name.length() - 4);
        if (name.length() > 100) name = name.substring(0, 100);
        return name + ".mp3";
    }
    public static boolean hasMp3Signature(byte[] data, int length) {
        if (length >= 3 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') return true;
        for (int i = 0; i + 3 < Math.min(length, 512); i++) {
            int b = data[i + 1] & 255, c = data[i + 2] & 255;
            if ((data[i] & 255) == 255 && (b & 224) == 224 && (b & 24) != 8 && (b & 6) == 2 && (c & 240) != 0 && (c & 240) != 240 && (c & 12) != 12) return true;
        }
        return false;
    }
    public static String size(long bytes) {
        if (bytes < 0) return "Size unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0);
    }
}
