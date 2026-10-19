package za.co.lottolab.studio;

import android.content.SharedPreferences;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * BetTip's publicly documented, key-free South African National Lottery feed.
 * All records are attributed third-party data, NOT independently certified prizes.
 * A failed network request never clears or replaces the existing offline archive.
 */
public final class BetTipResults {
  public static final String LATEST = "https://bettip.co.za/api/v1/lotto/latest.json";
  public static final String ARCHIVE = "https://bettip.co.za/api/v1/lotto/draws.json";
  public static final String PAYOUTS = "https://bettip.co.za/api/v1/lotto/payouts.json";
  public static final String DOCS = "https://bettip.co.za/api/";
  public static final String ATTRIBUTION = "Data by BetTip · bettip.co.za";
  private static final String ARCHIVE_STAMP = "bettip_archive_refresh";
  private static final long ARCHIVE_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000;
  private static final ZoneId SOUTH_AFRICA = ZoneId.of("Africa/Johannesburg");

  private BetTipResults() {}

  public static final class Sync {
    public final int changes, recentRecords, historicalRecords;
    public final boolean archiveChecked;
    public final String warning;
    Sync(int changes, int recent, int historical, boolean archiveChecked, String warning) {
      this.changes = changes;
      this.recentRecords = recent;
      this.historicalRecords = historical;
      this.archiveChecked = archiveChecked;
      this.warning = warning;
    }
  }

  /**
   * Download the small latest feed each time, a complete archive on first use
   * and thereafter at most once per week after a successful archive download.
   * If history is unavailable, the latest validated results remain available.
   */
  public static Sync synchronize(ArchiveStore store) throws Exception {
    LocalDate today = LocalDate.now(SOUTH_AFRICA);
    List<Draw> recent = parseLatest(download(LATEST, 2 * 1024 * 1024), today);
    // Import history first so the smaller fresh feed takes precedence on any overlap.
    int changed = 0;
    SharedPreferences prefs = store.preferences();
    long now = System.currentTimeMillis();
    long lastArchive = prefs.getLong(ARCHIVE_STAMP, 0);
    boolean shouldArchive = lastArchive == 0 || now - lastArchive >= ARCHIVE_INTERVAL_MS;
    int historical = 0;
    String warning = "";
    boolean archived = false;
    if (shouldArchive) {
      try {
        List<Draw> all = parseArchive(download(ARCHIVE, 16 * 1024 * 1024), today);
        historical = all.size();
        changed += store.importDraws(all);
        prefs.edit().putLong(ARCHIVE_STAMP, System.currentTimeMillis()).apply();
        archived = true;
      } catch (Exception e) {
        warning = "Older archive not refreshed; newer results are saved. "
            + e.getClass().getSimpleName();
      }
    }
    changed += store.importDraws(recent);
    prefs.edit()
        .putLong("last_sync", System.currentTimeMillis())
        .putString("sync_error", warning)
        .apply();
    return new Sync(changed, recent.size(), historical, archived, warning);
  }

  static String download(String endpoint, int maxBytes) throws Exception {
    // Explicit allowlist. Do not follow redirects to third-party hosts.
    if (!(LATEST.equals(endpoint) || ARCHIVE.equals(endpoint) || PAYOUTS.equals(endpoint)))
      throw new IOException("The requested results address is not approved.");
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(15000);
    connection.setReadTimeout(35000);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestProperty("User-Agent", "LottoLab/0.7 Android (BetTip attributed)");
    try {
      if (connection.getResponseCode() != 200)
        throw new IOException("BetTip results temporarily unavailable (HTTP "
            + connection.getResponseCode() + ").");
      try (InputStream input = connection.getInputStream()) {
        return FeedClient.read(input, maxBytes);
      }
    } finally {
      connection.disconnect();
    }
  }

  static List<Draw> parseLatest(String json, LocalDate asOf) throws JSONException {
    JSONObject data = envelope(json, "lotto/latest");
    JSONArray recent = data.optJSONArray("recent");
    JSONObject byGame = data.optJSONObject("byGame");
    if ((recent == null || recent.length() == 0) && (byGame == null || byGame.length() == 0))
      throw new IllegalArgumentException("BetTip provided no current results.");
    Map<String, Draw> found = new HashMap<>();
    if (recent != null)
      for (int i = 0; i < recent.length(); i++)
        collect(found, recent.getJSONObject(i), asOf);
    if (byGame != null) {
      java.util.Iterator<String> keys = byGame.keys();
      while (keys.hasNext()) {
        String name = keys.next();
        JSONObject item = byGame.optJSONObject(name);
        if (item != null) collect(found, item, asOf);
      }
    }
    return orderedResults(found);
  }

  static List<Draw> parseArchive(String json, LocalDate asOf) throws JSONException {
    JSONObject root = root(json, "lotto/draws");
    JSONArray array = root.optJSONArray("data");
    if (array == null || array.length() == 0 || array.length() > 25000)
      throw new IllegalArgumentException("BetTip historical archive has no usable draw rows.");
    Map<String, Draw> found = new HashMap<>();
    for (int i = 0; i < array.length(); i++) collect(found, array.getJSONObject(i), asOf);
    if (found.isEmpty())
      throw new IllegalArgumentException("The historical archive has no supported dates.");
    return orderedResults(found);
  }

  private static JSONObject root(String raw, String expectedResource) throws JSONException {
    JSONObject obj = new JSONObject(raw);
    if (!"BetTip Draws API".equals(obj.optString("api"))
        || !"v1".equals(obj.optString("version"))
        || !expectedResource.equals(obj.optString("resource")))
      throw new IllegalArgumentException("Unexpected results API response; existing data kept.");
    return obj;
  }

  private static JSONObject envelope(String raw, String resource) throws JSONException {
    JSONObject data = root(raw, resource).optJSONObject("data");
    if (data == null) throw new IllegalArgumentException("Results service returned no data.");
    return data;
  }

  private static List<Draw> orderedResults(Map<String, Draw> found) {
    if (found.isEmpty())
      throw new IllegalArgumentException("The results feed contained no supported draw records.");
    List<Draw> out = new ArrayList<>(found.values());
    out.sort((a, b) -> {
      int compare = b.date.compareTo(a.date);
      return compare != 0 ? compare : a.game.name().compareTo(b.game.name());
    });
    return out;
  }

  private static void collect(Map<String, Draw> rows, JSONObject record, LocalDate asOf) throws JSONException {
    String name = record.optString("game", "").trim().toLowerCase(Locale.ROOT);
    String when = record.optString("date", "");
    if (name.isEmpty() || when.isEmpty()) return; // Not a published draw.
    LocalDate date = LocalDate.parse(when);
    if (date.isBefore(RuleSet.LEGACY_START)) return; // Earlier rules not supported.
    if (date.isAfter(asOf))
      throw new IllegalArgumentException("Feed returned winning numbers dated in the future.");
    Game game = mapGame(name, date);
    if (game == null) return; // Explicitly unsupported games stay out of the database.
    JSONArray numbers = record.optJSONArray("numbers");
    if (numbers == null || numbers.length() != game.picks)
      throw new IllegalArgumentException("Invalid number count in published " + name + " result.");
    int[] main = new int[numbers.length()];
    for (int i = 0; i < main.length; i++) main[i] = FeedClient.whole(numbers.get(i));

    int special = 0;
    Object rawBonus = record.opt("bonusBall");
    if (rawBonus != null && rawBonus != JSONObject.NULL) special = FeedClient.whole(rawBonus);
    if ((game.hasPowerBall() || game.hasBonus()) && special == 0)
      throw new IllegalArgumentException("Published result missing a required special ball.");

    // BetTip's 'verified' and 'official' metadata is a provider assertion,
    // not independent confirmation. Never store it as official certification.
    Draw draw = new Draw(game, date, main, game.hasBonus() ? special : 0,
        game.hasPowerBall() ? special : 0,
        DOCS, Instant.now().toString(), "bettip");
    Draw old = rows.get(draw.key());
    if (old != null &&
        (!Arrays.equals(old.numbers, draw.numbers) ||
            old.bonus != draw.bonus || old.powerBall != draw.powerBall))
      throw new IllegalArgumentException("Contradictory draw records in BetTip response.");
    rows.put(draw.key(), draw);
  }

  static Game mapGame(String raw, LocalDate date) {
    String name = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    boolean old = date.isBefore(RuleSet.CURRENT_START);
    switch (name) {
      case "lotto": return Game.LOTTO;
      case "lotto-plus-1": return Game.PLUS1;
      case "lotto-plus-2": return old ? Game.PLUS2 : null;
      case "lotto-5-max": return old ? null : Game.MAX5;
      case "powerball": return Game.POWERBALL;
      case "powerball-plus": return old ? Game.POWERPLUS : Game.XTRA;
      case "powerball-xtra": return old ? null : Game.XTRA;
      case "daily-lotto": return Game.DAILY;
      case "daily-lotto-plus": return old ? Game.DAILYPLUS : null;
      default: return null;
    }
  }
}
