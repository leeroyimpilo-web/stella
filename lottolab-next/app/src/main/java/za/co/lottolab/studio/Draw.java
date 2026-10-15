package za.co.lottolab.studio;

import java.time.LocalDate;
import java.util.Arrays;

public final class Draw {
  public final Game game;
  public final LocalDate date;
  public final int[] numbers;
  public final int bonus, powerBall;

  public Draw(Game game, LocalDate date, int[] numbers, int bonus, int pb) {
    new Entry(game, numbers, pb, false, "");
    if (date.isBefore(LocalDate.of(2026, 6, 1)))
      throw new IllegalArgumentException(
          "This preview imports the current rules from June 2026 onward.");
    this.game = game;
    this.date = date;
    this.numbers = numbers.clone();
    Arrays.sort(this.numbers);
    this.bonus = bonus;
    this.powerBall = pb;
    if (bonus < 0
        || bonus > game.pool
        || (!game.hasBonus() && bonus != 0)
        || bonus != 0 && Arrays.binarySearch(this.numbers, bonus) >= 0)
      throw new IllegalArgumentException("Invalid or repeated bonus ball.");
  }

  public String key() {
    return game.name() + ":" + date;
  }
}
