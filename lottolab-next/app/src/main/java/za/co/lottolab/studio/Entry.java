package za.co.lottolab.studio;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.TreeSet;

public final class Entry {
  public final Game game;
  public final int[] numbers;
  public final int powerBall;
  public final boolean subset;
  public final String name;

  public Entry(Game game, int[] numbers, int powerBall, boolean subset, String name) {
    this.game = game;
    this.numbers = numbers.clone();
    Arrays.sort(this.numbers);
    this.powerBall = powerBall;
    this.subset = subset;
    this.name = name == null ? "" : name;
    if (numbers.length < 1
        || numbers.length > game.picks
        || (!subset && numbers.length != game.picks))
      throw new IllegalArgumentException(
          "Choose " + (subset ? "1 to " : "") + game.picks + " main numbers.");
    int prev = 0;
    for (int n : this.numbers) {
      if (n < 1 || n > game.pool)
        throw new IllegalArgumentException("Main numbers must be 1-" + game.pool + ".");
      if (n == prev) throw new IllegalArgumentException("Main numbers must be distinct.");
      prev = n;
    }
    if (game.hasPowerBall()) {
      if (powerBall > game.powerPool || powerBall < 0 || (!subset && powerBall == 0))
        throw new IllegalArgumentException("Choose a PowerBall from 1-" + game.powerPool + ".");
    } else if (powerBall != 0)
      throw new IllegalArgumentException("This game has no selected PowerBall.");
  }

  public Entry named(String s) {
    return new Entry(game, numbers, powerBall, subset, s);
  }

  public String key() {
    return game.name() + Arrays.toString(numbers) + ":" + powerBall + ":" + subset;
  }

  public static int[] parseNumbers(String input) {
    String s = input.trim();
    if (s.isEmpty()) return new int[0];
    String[] tokens = s.split("[\\s,;]+");
    int[] a = new int[tokens.length];
    try {
      for (int i = 0; i < a.length; i++) a[i] = Integer.parseInt(tokens[i]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Enter whole numbers separated by spaces or commas.");
    }
    return a;
  }

  public static Entry random(Game g, SecureRandom rng) {
    TreeSet<Integer> set = new TreeSet<>();
    while (set.size() < g.picks) set.add(rng.nextInt(g.pool) + 1);
    return new Entry(
        g,
        set.stream().mapToInt(Integer::intValue).toArray(),
        g.hasPowerBall() ? rng.nextInt(g.powerPool) + 1 : 0,
        false,
        "");
  }

  public String display() {
    StringBuilder s = new StringBuilder();
    for (int n : numbers) {
      if (s.length() > 0) s.append("  ");
      s.append(String.format(java.util.Locale.ROOT, "%02d", n));
    }
    if (powerBall > 0)
      s.append("  | PB ").append(String.format(java.util.Locale.ROOT, "%02d", powerBall));
    return s.toString();
  }
}
