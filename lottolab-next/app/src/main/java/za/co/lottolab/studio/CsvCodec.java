package za.co.lottolab.studio;

import java.time.LocalDate;
import java.util.*;

public final class CsvCodec {
  public static final String HEADER = "game,date,numbers,bonus,powerball,source_url,fetched_at";

  public static List<Draw> parse(String source, LocalDate asOf) {
    String[] lines = source.replace("\uFEFF", "").split("\\r?\\n");
    int header = -1;
    Map<String, Integer> cols = new HashMap<>();
    List<Draw> out = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].trim().isEmpty()) continue;
      try {
        List<String> c = fields(lines[i]);
        if (header < 0) {
          for (int j = 0; j < c.size(); j++) cols.put(c.get(j).trim().toLowerCase(Locale.ROOT), j);
          for (String need : new String[] {"game", "date", "numbers"})
            if (!cols.containsKey(need))
              throw new IllegalArgumentException("Missing column: " + need);
          header = i;
          continue;
        }
        Game g = Game.parse(value(c, cols, "game"));
        LocalDate date = LocalDate.parse(value(c, cols, "date"));
        if (date.isAfter(asOf)) throw new IllegalArgumentException("Future draw date.");
        Draw d =
            new Draw(
                g,
                date,
                Entry.parseNumbers(value(c, cols, "numbers")),
                number(value(c, cols, "bonus")),
                number(value(c, cols, "powerball")),
                sourceUrl(value(c, cols, "source_url")),
                value(c, cols, "fetched_at"),
                "imported");
        if (!keys.add(d.key()))
          throw new IllegalArgumentException("Duplicate game/date in this file.");
        out.add(d);
        if (out.size() > 10000)
          throw new IllegalArgumentException("Import at most 10,000 draws per file.");
      } catch (Exception e) {
        throw new IllegalArgumentException("Row " + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    if (out.isEmpty()) throw new IllegalArgumentException("The file contains no draw rows.");
    return out;
  }

  private static String sourceUrl(String s) {
    if (!s.isEmpty() && !s.startsWith("https://"))
      throw new IllegalArgumentException("Source links must use HTTPS.");
    return s;
  }

  static String value(List<String> row, Map<String, Integer> cols, String key) {
    Integer i = cols.get(key);
    return i == null ? "" : i < row.size() ? row.get(i).trim() : "";
  }

  static int number(String s) {
    return s.isEmpty() ? 0 : Integer.parseInt(s);
  }

  static List<String> fields(String line) {
    List<String> out = new ArrayList<>();
    StringBuilder s = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          s.append('"');
          i++;
        } else quoted = !quoted;
      } else if (c == ',' && !quoted) {
        out.add(s.toString());
        s.setLength(0);
      } else s.append(c);
    }
    if (quoted) throw new IllegalArgumentException("Unclosed CSV quote.");
    out.add(s.toString());
    return out;
  }

  public static String quote(String s) {
    // Prefix spreadsheet formula triggers in user-controlled textual export fields.
    if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  public static String write(List<Draw> draws) {
    StringBuilder s = new StringBuilder(HEADER + "\n");
    for (Draw d : draws) {
      s.append(d.game.name()).append(',').append(d.date).append(',');
      for (int i = 0; i < d.numbers.length; i++) {
        if (i > 0) s.append(' ');
        s.append(d.numbers[i]);
      }
      s.append(',')
          .append(d.bonus == 0 ? "" : d.bonus)
          .append(',')
          .append(d.powerBall == 0 ? "" : d.powerBall)
          .append(',')
          .append(quote(d.sourceUrl))
          .append(',')
          .append(quote(d.fetchedAt))
          .append('\n');
    }
    return s.toString();
  }
}
