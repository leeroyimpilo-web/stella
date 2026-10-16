package za.co.lottolab.studio;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.*;

/** An explicitly configured HTTPS feed, never a guessed operator endpoint. */
public final class FeedClient {
  public static List<Draw> fetch(String url) throws Exception {
    if (OperatorResults.ENDPOINT.equals(url)) return OperatorResults.fetch();
    URL u = new URL(url);
    if (!"https".equals(u.getProtocol()))
      throw new IOException("Use an HTTPS data-source address.");
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    c.setConnectTimeout(15000);
    c.setReadTimeout(20000);
    c.setInstanceFollowRedirects(false);
    c.setRequestProperty("Accept", "application/json");
    c.setRequestProperty("User-Agent", "LottoLab/0.2 Android");
    try {
      if (c.getResponseCode() != 200)
        throw new IOException(
            "The data source returned HTTP "
                + c.getResponseCode()
                + ". Your cached results have been kept.");
      String text;
      try (InputStream in = c.getInputStream()) {
        text = read(in, 2 * 1024 * 1024);
      }
      return parse(text, url, LocalDate.now(ZoneId.of("Africa/Johannesburg")));
    } finally {
      c.disconnect();
    }
  }

  static List<Draw> parse(String raw, String url, LocalDate asOf) throws JSONException {
    JSONObject root = new JSONObject(raw);
    JSONArray a = root.getJSONArray("draws");
    if (a.length() < 1 || a.length() > 10000)
      throw new IllegalArgumentException("The feed must contain 1–10,000 draws.");
    List<Draw> out = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    String now = Instant.now().toString();
    for (int i = 0; i < a.length(); i++) {
      JSONObject v = a.getJSONObject(i);
      JSONArray n = v.getJSONArray("numbers");
      int[] nums = new int[n.length()];
      for (int j = 0; j < nums.length; j++) nums[j] = whole(n.get(j));
      LocalDate date = LocalDate.parse(v.getString("date"));
      if (date.isAfter(asOf))
        throw new IllegalArgumentException("A future draw was returned. The update was rejected.");
      String source = v.optString("source_url", url);
      if (!source.startsWith("https://"))
        throw new IllegalArgumentException("Source links must use HTTPS.");
      Draw d =
          new Draw(
              Game.parse(v.getString("game")),
              date,
              nums,
              v.isNull("bonus") ? 0 : whole(v.get("bonus")),
              v.isNull("powerball") ? 0 : whole(v.get("powerball")),
              source,
              now,
              "feed");
      if (!keys.add(d.key())) throw new IllegalArgumentException("Duplicate game/date in feed.");
      out.add(d);
    }
    return out;
  }

  static int whole(Object value) {
    if (!(value instanceof Number))
      throw new IllegalArgumentException("Draw numbers must be whole numbers.");
    double d = ((Number) value).doubleValue();
    if (!Double.isFinite(d) || d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Draw numbers must be whole numbers.");
    return ((Number) value).intValue();
  }

  public static String read(InputStream in, int max) throws IOException {
    if (in == null) throw new IOException("The file could not be opened.");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) {
      if (out.size() + n > max)
        throw new IOException("The file exceeds " + (max / 1024 / 1024) + " MB.");
      out.write(buf, 0, n);
    }
    return out.toString(StandardCharsets.UTF_8.name());
  }
}
