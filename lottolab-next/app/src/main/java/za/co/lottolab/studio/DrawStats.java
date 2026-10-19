package za.co.lottolab.studio;

import java.util.*;
import java.time.LocalDate;

/** Descriptive statistics over locally stored, same-rule lottery draws. Never predictions. */
public final class DrawStats {
  private DrawStats() {}

  public static final class Summary {
    public final int count, distinctNumbers;
    public final LocalDate first, last;
    public final String mainLeaders, rarelySeen, popularPairs, popularTriples;
    public final double averageSum, averageOdd, averageConsecutive, averageRepeated;
    public final int repeatedComparisons;
    Summary(int count, int distinct, LocalDate first, LocalDate last, String leaders,
        String rare, String pairs, String triples, double sum, double odd,
        double consecutive, double repeated, int comparisons) {
      this.count = count;
      distinctNumbers = distinct;
      this.first = first;
      this.last = last;
      mainLeaders = leaders;
      rarelySeen = rare;
      popularPairs = pairs;
      popularTriples = triples;
      averageSum = sum;
      averageOdd = odd;
      averageConsecutive = consecutive;
      averageRepeated = repeated;
      repeatedComparisons = comparisons;
    }

    public String fullReport() {
      return "Stored draws: " + count + "\nPeriod: " + first + " to " + last
          + "\nDifferent main numbers observed: " + distinctNumbers
          + "\n\nMost frequently drawn: " + mainLeaders
          + "\nLeast frequently drawn: " + rarelySeen
          + "\nMost common pairs: " + popularPairs
          + "\nMost common triples: " + popularTriples
          + String.format(Locale.ENGLISH, "\n\nAverage main-number sum: %.1f"
              + "\nAverage odd numbers per draw: %.1f"
              + "\nAverage consecutive pairs per draw: %.2f",
              averageSum, averageOdd, averageConsecutive)
          + (repeatedComparisons > 0
              ? String.format(Locale.ENGLISH,
                  "\nAverage numbers repeated from preceding available draw: %.2f (%d comparisons)",
                  averageRepeated, repeatedComparisons)
              : "\nRepeats: not enough drawings to compare")
          + "\n\nThese are historical counts from your offline archive, not predictions."
          + " Unavailable records may change these statistics.";
    }
  }

  /**
   * The most recent game's rule era is used. limit <= 0 means every cached draw
   * in the same era. All ranks use a stable ascending-number tie break.
   */
  public static Summary summarize(Game game, List<Draw> archive, int limit) {
    if (archive == null || archive.isEmpty()) return null;
    List<Draw> selected = new ArrayList<>();
    String era = null;
    for (Draw draw : archive) {
      if (draw.game != game) continue;
      if (era == null) era = draw.ruleId;
      if (!era.equals(draw.ruleId)) continue;
      selected.add(draw);
      if (limit > 0 && selected.size() >= limit) break;
    }
    if (selected.isEmpty()) return null;
    RuleSet rules = RuleSet.forId(game, era);
    int[] count = new int[rules.pool + 1];
    Map<String, Integer> pairs = new HashMap<>();
    Map<String, Integer> triples = new HashMap<>();
    double sum = 0, odds = 0, consecutive = 0, repeated = 0;
    int comparisons = 0;
    for (int i = 0; i < selected.size(); i++) {
      Draw d = selected.get(i);
      for (int j = 0; j < d.numbers.length; j++) {
        int n = d.numbers[j];
        count[n]++;
        sum += n;
        if ((n & 1) == 1) odds++;
        if (j > 0 && d.numbers[j - 1] + 1 == n) consecutive++;
        for (int k = j + 1; k < d.numbers.length; k++) {
          String pair = label(n, d.numbers[k]);
          pairs.merge(pair, 1, Integer::sum);
          for (int m = k + 1; m < d.numbers.length; m++)
            triples.merge(label(n, d.numbers[k], d.numbers[m]), 1, Integer::sum);
        }
      }
      if (i + 1 < selected.size()) {
        repeated += AnalysisEngine.matches(d.numbers, selected.get(i + 1).numbers);
        comparisons++;
      }
    }
    List<Integer> ranked = new ArrayList<>();
    for (int n = 1; n <= rules.pool; n++) ranked.add(n);
    ranked.sort((a, b) -> count[b] != count[a]
        ? Integer.compare(count[b], count[a]) : Integer.compare(a, b));
    List<Integer> rare = new ArrayList<>(ranked);
    rare.sort((a, b) -> count[a] != count[b]
        ? Integer.compare(count[a], count[b]) : Integer.compare(a, b));
    int distinct = 0;
    for (int n = 1; n <= rules.pool; n++) if (count[n] > 0) distinct++;
    return new Summary(selected.size(), distinct,
        selected.get(selected.size() - 1).date, selected.get(0).date,
        topNumbers(ranked, count, 5), topNumbers(rare, count, 5),
        topPatterns(pairs, 3), topPatterns(triples, 3),
        sum / selected.size(), odds / selected.size(),
        consecutive / selected.size(),
        comparisons == 0 ? 0 : repeated / comparisons, comparisons);
  }

  private static String label(int... numbers) {
    StringJoiner result = new StringJoiner("–");
    for (int n : numbers) result.add(String.format(Locale.ROOT, "%02d", n));
    return result.toString();
  }

  private static String topNumbers(List<Integer> ordered, int[] counts, int max) {
    StringJoiner result = new StringJoiner(", ");
    for (int i = 0; i < Math.min(max, ordered.size()); i++) {
      int n = ordered.get(i);
      result.add(String.format(Locale.ROOT, "%02d (%d)", n, counts[n]));
    }
    return result.toString();
  }

  private static String topPatterns(Map<String, Integer> counts, int max) {
    if (counts.isEmpty()) return "not enough data";
    List<Map.Entry<String, Integer>> rows = new ArrayList<>(counts.entrySet());
    rows.sort((a, b) -> a.getValue().equals(b.getValue())
        ? a.getKey().compareTo(b.getKey())
        : Integer.compare(b.getValue(), a.getValue()));
    StringJoiner result = new StringJoiner(", ");
    for (int i = 0; i < Math.min(max, rows.size()); i++)
      result.add(rows.get(i).getKey() + " (" + rows.get(i).getValue() + ")");
    return result.toString();
  }
}
