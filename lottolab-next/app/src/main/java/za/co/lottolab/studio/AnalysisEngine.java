package za.co.lottolab.studio;

import java.util.*;

public final class AnalysisEngine {
  public static final class Result {
    public final Entry entry;
    public final int checked, completeHits, containsHits, powerHits, best;
    public final int[] distribution, frequency;
    public final List<Draw> matching;

    Result(
        Entry e,
        int checked,
        int completeHits,
        int containsHits,
        int powerHits,
        int best,
        int[] dist,
        int[] freq,
        List<Draw> matching) {
      this.entry = e;
      this.checked = checked;
      this.completeHits = completeHits;
      this.containsHits = containsHits;
      this.powerHits = powerHits;
      this.best = best;
      this.distribution = dist;
      this.frequency = freq;
      this.matching = Collections.unmodifiableList(matching);
    }
  }

  public static int matches(int[] a, int[] b) {
    int n = 0;
    for (int x : a)
      for (int y : b)
        if (x == y) {
          n++;
          break;
        }
    return n;
  }

  public static Result analyse(Entry e, List<Draw> draws) {
    int count = 0, full = 0, contains = 0, power = 0, best = 0;
    int[] dist = new int[e.numbers.length + 1], freq = new int[e.game.pool + 1];
    List<Draw> matching = new ArrayList<>();
    for (Draw d : draws) {
      if (d.game != e.game) continue;
      count++;
      int hit = matches(e.numbers, d.numbers);
      dist[hit]++;
      best = Math.max(best, hit);
      for (int n : d.numbers) freq[n]++;
      boolean pb = e.powerBall == 0 || e.powerBall == d.powerBall;
      if (e.powerBall > 0 && pb) power++;
      if (hit == e.numbers.length) {
        contains++;
        if (!e.subset && pb) full++;
      }
      if (hit > 0 || (e.powerBall > 0 && pb)) matching.add(d);
    }
    matching.sort(
        (a, b) -> {
          int c = Integer.compare(matches(e.numbers, b.numbers), matches(e.numbers, a.numbers));
          return c != 0 ? c : b.date.compareTo(a.date);
        });
    return new Result(e, count, full, contains, power, best, dist, freq, matching);
  }

  public static long choose(int n, int k) {
    if (k < 0 || k > n) return 0;
    long v = 1;
    for (int i = 1; i <= Math.min(k, n - k); i++) v = v * (n - i + 1) / i;
    return v;
  }

  public static long possible(Game g) {
    return choose(g.pool, g.picks) * (g.hasPowerBall() ? g.powerPool : 1);
  }

  public static long cost(Game g, int lines, int draws) {
    if (lines < 0 || draws < 0) throw new IllegalArgumentException("Counts must be non-negative.");
    return (long) g.bundleCostCents * lines * draws;
  }

  public static int overlap(Entry a, Entry b) {
    if (a.game != b.game) throw new IllegalArgumentException("Compare entries in one game.");
    return matches(a.numbers, b.numbers);
  }

  public static int pairCount(int a, int b, List<Draw> draws) {
    int n = 0;
    for (Draw d : draws) if (matches(new int[] {a, b}, d.numbers) == 2) n++;
    return n;
  }

  public static int gap(int number, List<Draw> latestFirst) {
    int gap = 0;
    for (Draw d : latestFirst) {
      if (Arrays.binarySearch(d.numbers, number) >= 0) return gap;
      gap++;
    }
    return -1;
  }

  public static String money(long cents) {
    return String.format(Locale.ROOT, "R%,.2f", cents / 100.0);
  }
}
