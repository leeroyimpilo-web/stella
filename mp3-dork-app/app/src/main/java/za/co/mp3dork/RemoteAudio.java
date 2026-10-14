package za.co.mp3dork;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

public final class RemoteAudio {
    public static final class Result {
        public final String url, name;
        public final long size;
        Result(String url, String name, long size) { this.url = url; this.name = name; this.size = size; }
    }
    private RemoteAudio() {}
    public static Result probe(String value, String cookie, String userAgent, String referer) throws IOException {
        String current = MediaTools.webUrl(MediaTools.unwrapGoogle(value));
        URI origin = URI.create(current);
        for (int redirect = 0; redirect < 7; redirect++) {
            HttpURLConnection connection = (HttpURLConnection) new URL(current).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Range", "bytes=0-4095");
            connection.setRequestProperty("User-Agent", userAgent == null ? "MP3Dork/1.0" : userAgent);
            connection.setRequestProperty("Accept-Encoding", "identity");
            URI target = URI.create(current);
            if (cookie != null && !cookie.isEmpty() && sameOrigin(origin, target)) connection.setRequestProperty("Cookie", cookie);
            if (referer != null && referer.startsWith("http")) connection.setRequestProperty("Referer", referer);
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("The source returned an empty redirect.");
                    current = MediaTools.webUrl(new URL(new URL(current), location).toString());
                    continue;
                }
                if (status != 200 && status != 206) throw new IOException("Source returned HTTP " + status + ". Open the source page and try its download link.");
                String mime = connection.getContentType();
                if (mime != null && (mime.toLowerCase(Locale.ROOT).contains("text/html") || mime.toLowerCase(Locale.ROOT).contains("application/json"))) throw new IOException("This is a web page, not an MP3 file. Open it and choose the audio link.");
                byte[] bytes = new byte[4096];
                int length = 0;
                try (InputStream stream = connection.getInputStream()) {
                    while (length < bytes.length) { int count = stream.read(bytes, length, bytes.length - length); if (count < 0) break; length += count; }
                }
                if (!MediaTools.hasMp3Signature(bytes, length)) throw new IOException("The source did not return a valid MP3. The link may be expired or require a download page.");
                long size = connection.getContentLengthLong();
                String range = connection.getHeaderField("Content-Range");
                if (range != null) { try { size = Long.parseLong(range.substring(range.lastIndexOf('/') + 1)); } catch (Exception ignored) { size = -1; } }
                return new Result(current, MediaTools.fileName(current, connection.getHeaderField("Content-Disposition")), size);
            } finally { connection.disconnect(); }
        }
        throw new IOException("The source redirected too many times.");
    }
    private static boolean sameOrigin(URI a, URI b) { return a.getScheme().equalsIgnoreCase(b.getScheme()) && a.getHost().equalsIgnoreCase(b.getHost()) && a.getPort() == b.getPort(); }
}
