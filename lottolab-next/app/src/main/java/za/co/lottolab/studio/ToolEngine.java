package za.co.lottolab.studio;

import java.security.SecureRandom;
import java.util.*;

/** Pure, bounded calculations used by the native tools and their unit tests. */
public final class ToolEngine {
  private ToolEngine() {}

  public static List<Entry> generate(
      Game game,
      String ruleId,
      int count,
      int[] locked,
      int[] excluded,
      int fixedPowerBall,
      int oddCount,
      SecureRandom rng) {
    RuleSet r = RuleSet.forId(game, ruleId);
    if (count < 1 || count > 100)
      throw new IllegalArgumentException("Generate 1–100 lines at a time.");
    Set<Integer> lock = checkedSet(locked, r.pool), exclude = checkedSet(excluded, r.pool);
    if (lock.size() > r.picks) throw new IllegalArgumentException("Too many locked numbers.");
    for (int n : lock)
      if (exclude.contains(n))
        throw new IllegalArgumentException("A number cannot be both locked and excluded.");
    if (fixedPowerBall < 0 || fixedPowerBall > r.powerPool)
      throw new IllegalArgumentException("PowerBall is outside its separate pool.");
    if (oddCount < -1 || oddCount > r.picks)
      throw new IllegalArgumentException("Choose a valid odd-number count.");
    List<Integer> available = new ArrayList<>();
    for (int n = 1; n <= r.pool; n++)
      if (!lock.contains(n) && !exclude.contains(n)) available.add(n);
    int need = r.picks - lock.size();
    if (available.size() < need)
      throw new IllegalArgumentException("Not enough available numbers after exclusions.");
    long possible;
    if (oddCount < 0) possible = AnalysisEngine.choose(available.size(), need);
    else {
      int lockedOdds = 0, availableOdds = 0;
      for (int n : lock) if (n % 2 == 1) lockedOdds++;
      for (int n : available) if (n % 2 == 1) availableOdds++;
      possible =
          AnalysisEngine.choose(availableOdds, oddCount - lockedOdds)
              * AnalysisEngine.choose(
                  available.size() - availableOdds, need - oddCount + lockedOdds);
    }
    if (r.powerPool > 0 && fixedPowerBall == 0) possible *= r.powerPool;
    if (count > possible)
      throw new IllegalArgumentException(
          "These preferences allow only "
              + possible
              + " unique lines. Reduce the count or relax your choices.");
    LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    for (int attempts = 0; entries.size() < count && attempts < 200000; attempts++) {
      List<Integer> selected = new ArrayList<>(lock);
      if (oddCount < 0) {
        List<Integer> bag = new ArrayList<>(available);
        Collections.shuffle(bag, rng);
        selected.addAll(bag.subList(0, need));
      } else {
        List<Integer> odds = new ArrayList<>(), evens = new ArrayList<>();
        int lockedOdds = 0;
        for (int n : lock) if (n % 2 == 1) lockedOdds++;
        for (int n : available) (n % 2 == 1 ? odds : evens).add(n);
        Collections.shuffle(odds, rng);
        Collections.shuffle(evens, rng);
        int on = oddCount - lockedOdds;
        selected.addAll(odds.subList(0, on));
        selected.addAll(evens.subList(0, need - on));
      }
      Entry e =
          new Entry(
              game,
              selected.stream().mapToInt(Integer::intValue).toArray(),
              r.powerPool == 0
                  ? 0
                  : fixedPowerBall > 0 ? fixedPowerBall : 1 + rng.nextInt(r.powerPool),
              false,
              "",
              ruleId);
      entries.put(e.key(), e);
    }
    if (entries.size() != count)
      throw new IllegalArgumentException("Could not complete this selection. Try fewer lines.");
    return new ArrayList<>(entries.values());
  }

  private static Set<Integer> checkedSet(int[] numbers, int max) {
    Set<Integer> values = new TreeSet<>();
    for (int n : numbers) {
      if (n < 1 || n > max || !values.add(n))
        throw new IllegalArgumentException("Use distinct whole numbers from 1–" + max + ".");
    }
    return values;
  }

  public static long wheelCount(Game g, int poolSize, int powerChoices) {
    if (poolSize < g.picks || poolSize > RuleSet.current(g).pool)
      throw new IllegalArgumentException("Choose at least " + g.picks + " pool numbers.");
    if (g.hasPowerBall() && (powerChoices < 1 || powerChoices > RuleSet.current(g).powerPool))
      throw new IllegalArgumentException("Choose a separate PowerBall pool.");
    return AnalysisEngine.choose(poolSize, g.picks) * (g.hasPowerBall() ? powerChoices : 1);
  }

  public static List<Entry> wheel(Game g, int[] pool, int[] powers) {
    Set<Integer> set = checkedSet(pool, RuleSet.current(g).pool);
    Set<Integer> pb =
        g.hasPowerBall()
            ? checkedSet(powers, RuleSet.current(g).powerPool)
            : Collections.singleton(0);
    long count = wheelCount(g, set.size(), pb.size());
    if (count > 2000)
      throw new IllegalArgumentException(
          "This system needs "
              + count
              + " lines. Use a smaller pool; this build exports at most 2,000 lines.");
    List<Entry> out = new ArrayList<>();
    combine(g, set.stream().mapToInt(Integer::intValue).toArray(), new int[g.picks], 0, 0, pb, out);
    return out;
  }

  private static void combine(
      Game g, int[] pool, int[] selected, int start, int at, Set<Integer> pb, List<Entry> out) {
    if (at == selected.length) {
      for (int p : pb) out.add(new Entry(g, selected, p, false, ""));
      return;
    }
    for (int i = start; i <= pool.length - (selected.length - at); i++) {
      selected[at] = pool[i];
      combine(g, pool, selected, i + 1, at + 1, pb, out);
    }
  }

  public static double mainProbability(RuleSet r, int matches) {
    return (double) AnalysisEngine.choose(r.picks, matches)
        * AnalysisEngine.choose(r.pool - r.picks, r.picks - matches)
        / AnalysisEngine.choose(r.pool, r.picks);
  }

  public static double categoryProbability(Game g, RuleSet r, int matches, boolean extra) {
    double main = mainProbability(r, matches);
    if (g.hasPowerBall())
      return main * (extra ? 1.0 / r.powerPool : (r.powerPool - 1.0) / r.powerPool);
    if (g.hasBonus()) {
      double bonus = (r.picks - matches) / (double) (r.pool - r.picks);
      return main * (extra ? bonus : 1 - bonus);
    }
    return extra ? 0 : main;
  }

  public static List<Draw> eligible(Game g, String ruleId, List<Draw> draws, int limit) {
    List<Draw> out = new ArrayList<>();
    for (Draw d : draws) if (d.game == g && d.ruleId.equals(ruleId)) out.add(d);
    out.sort((a, b) -> b.date.compareTo(a.date));
    return limit > 0 && out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
  }

  public static int subsetCount(int[] subset, List<Draw> draws) {
    int n = 0;
    for (Draw d : draws) if (AnalysisEngine.matches(subset, d.numbers) == subset.length) n++;
    return n;
  }

  public static int missingScheduled(Game g, List<Draw> draws) {
    if (draws.isEmpty()) return 0;
    LocalRange range = new LocalRange(draws);
    Set<java.time.LocalDate> dates = new HashSet<>();
    for (Draw d : draws) dates.add(d.date);
    int missing = 0;
    for (java.time.LocalDate d = range.first; !d.isAfter(range.last); d = d.plusDays(1))
      if (g.scheduled(d) && !dates.contains(d)) missing++;
    return missing;
  }

  private static class LocalRange {
    java.time.LocalDate first = java.time.LocalDate.MAX, last = java.time.LocalDate.MIN;

    LocalRange(List<Draw> ds) {
      for (Draw d : ds) {
        if (d.date.isBefore(first)) first = d.date;
        if (d.date.isAfter(last)) last = d.date;
      }
    }
  }

  public static String odds(double probability) {
    return probability <= 0
        ? "Not possible"
        : String.format(Locale.ENGLISH, "1 in %,.2f", 1 / probability);
  }

  public static String percent(int n, int denominator) {
    return denominator == 0
        ? "Unknown"
        : String.format(
            Locale.ENGLISH, "%.1f%% (%d of %d)", 100.0 * n / denominator, n, denominator);
  }
}
