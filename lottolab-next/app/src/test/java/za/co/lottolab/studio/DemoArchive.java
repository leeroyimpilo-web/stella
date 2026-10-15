package za.co.lottolab.studio;

import java.time.LocalDate;
import java.util.*;

/** Fictional design fixtures. Never published as official lottery results. */
public final class DemoArchive {
  public static List<Draw> forGame(Game g) {
    List<Draw> out = new ArrayList<>();
    Random random = new Random(20261015L + g.ordinal());
    LocalDate date = g.active() ? LocalDate.of(2026, 10, 14) : LocalDate.of(2026, 5, 30);
    while (out.size() < 24) {
      if (g.scheduled(date)) {
        TreeSet<Integer> values = new TreeSet<>();
        while (values.size() < g.picks) values.add(1 + random.nextInt(g.pool));
        int[] nums = values.stream().mapToInt(Integer::intValue).toArray();
        int bonus = 0;
        if (g.hasBonus()) {
          do {
            bonus = 1 + random.nextInt(g.pool);
          } while (values.contains(bonus));
        }
        int power = g.hasPowerBall() ? 1 + random.nextInt(g.powerPool) : 0;
        if (out.isEmpty() && g.hasBonus()) {
          nums = new int[] {4, 12, 19, 28, 36, 47};
          bonus = 8;
        }
        if (out.isEmpty() && g == Game.DAILY) nums = new int[] {4, 12, 19, 28, 36};
        if (out.isEmpty() && g.hasPowerBall()) {
          nums = new int[] {4, 12, 19, 28, 47};
          power = 8;
        }
        out.add(new Draw(g, date, nums, bonus, power));
      }
      date = date.minusDays(1);
    }
    return out;
  }

  public static Entry initial(Game g, int row) {
    int[] a =
        g.picks == 6
            ? (row % 2 == 0 ? new int[] {4, 12, 19, 28, 36, 47} : new int[] {7, 14, 21, 32, 40, 51})
            : g == Game.DAILY
                ? (row % 2 == 0 ? new int[] {4, 12, 19, 28, 36} : new int[] {3, 9, 16, 24, 31})
                : (row % 2 == 0 ? new int[] {4, 12, 19, 28, 47} : new int[] {7, 14, 21, 32, 40});
    return new Entry(g, a, g.hasPowerBall() ? (row % 2 == 0 ? 8 : 12) : 0, false, "");
  }
}
