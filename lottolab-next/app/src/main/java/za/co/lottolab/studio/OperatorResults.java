package za.co.lottolab.studio;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.*;

/**
 * Optional adapter for the public results endpoint used by the South African
 * National Lottery website. The operator may change this undocumented API.
 * Data is marked as downloaded, never as an independently verified prize claim.
 */
public final class OperatorResults {
  public static final String ENDPOINT =
      "https://www.nationallottery.co.za/api/engine/draw/issueWinPoolInfoPageQuery";
  public static final String SOURCE = "https://www.nationallottery.co.za/";
  private static final int[] GAME_IDS = {11101, 11201, 11001};
  private OperatorResults() {}

  /** Fetch only recent results. No credentials, SMS access or account required. */
  public static List<Draw> fetch() throws Exception {
    List<Draw> all = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    LocalDate asOf = LocalDate.now(ZoneId.of("Africa/Johannesburg"));
    for (int gameId : GAME_IDS) {
      JSONObject payload = new JSONObject();
      payload.put("gameId", gameId);
      payload.put("pageNum", 1);
      payload.put("pageSize", 60);
      HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(20000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Origin", SOURCE.substring(0, SOURCE.length() - 1));
      connection.setRequestProperty("Referer", SOURCE);
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 LottoLab/0.3");
      try {
        try (OutputStream out = connection.getOutputStream()) {
          out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        if (status != 200)
          throw new IOException("Operator results are currently unavailable (HTTP " + status + ").");
        String response;
        try (InputStream in = connection.getInputStream()) {
          response = FeedClient.read(in, 1024 * 1024);
        }
        for (Draw draw : parse(response, asOf)) {
          if (!keys.add(draw.key()))
            throw new IllegalArgumentException("Repeated game/date across operator result pages.");
          all.add(draw);
        }
      } finally {
        connection.disconnect();
      }
    }
    if (all.isEmpty())
      throw new IOException("No published results were returned; existing history was kept.");
    return all;
  }

  /** Strict parsing: never replace good cached results with partial or invented draws. */
  static List<Draw> parse(String json, LocalDate asOf) throws JSONException {
    JSONObject root = new JSONObject(json);
    if (root.getInt("code") != 0)
      throw new IllegalArgumentException("The operator results service reported an error.");
    JSONArray rows = root.getJSONObject("data").getJSONArray("list");
    List<Draw> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      Game game = gameForPool(row.optString("winPoolName", ""));
      if (game == null) continue;
      JSONArray selected = row.optJSONArray("winNumList");
      if (selected == null || selected.length() == 0) continue; // upcoming, not drawn
      String dateText = row.getString("drawTime");
      if (dateText.length() < 10)
        throw new IllegalArgumentException("A published result has an invalid date.");
      LocalDate date = LocalDate.parse(dateText.substring(0, 10));
      if (date.isAfter(asOf))
        throw new IllegalArgumentException("Operator feed listed future winning numbers.");
      if (date.isBefore(RuleSet.CURRENT_START)) continue; // new feed begins June 2026
      int expected = game.picks + (game.hasBonus() || game.hasPowerBall() ? 1 : 0);
      if (selected.length() != expected)
        throw new IllegalArgumentException("Unexpected published number count for " + game.title);
      int[] numbers = new int[game.picks];
      for (int j = 0; j < numbers.length; j++) numbers[j] = strictNumber(selected.get(j));
      int special = selected.length() > numbers.length
          ? strictNumber(selected.get(numbers.length)) : 0;
      Draw draw = new Draw(game, date, numbers,
          game.hasBonus() ? special : 0,
          game.hasPowerBall() ? special : 0,
          SOURCE, Instant.now().toString(), "feed");
      if (!seen.add(draw.key()))
        throw new IllegalArgumentException("The feed repeated " + draw.key() + ".");
      result.add(draw);
    }
    return result;
  }

  private static int strictNumber(Object value) {
    String raw = String.valueOf(value);
    if (!raw.matches("[0-9]{1,3}"))
      throw new IllegalArgumentException("Published numbers must be whole numbers.");
    return Integer.parseInt(raw);
  }

  static Game gameForPool(String name) {
    String label = name.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    switch (label) {
      case "LOTTO": return Game.LOTTO;
      case "LOTTO PLUS 1": return Game.PLUS1;
      case "LOTTO 5 MAX": return Game.MAX5;
      case "POWERBALL": return Game.POWERBALL;
      case "POWERBALL XTRA": return Game.XTRA;
      case "DAILY LOTTO": return Game.DAILY;
      default: return null;
    }
  }
}
