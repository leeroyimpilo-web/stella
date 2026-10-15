package za.co.lottolab.studio;

import java.time.LocalDate;
import java.util.Arrays;

public final class Draw {
  public final Game game;
  public final LocalDate date;
  public final int[] numbers;
  public final int bonus, powerBall;
  public final String ruleId, sourceUrl, fetchedAt, verification;

  public Draw(Game game, LocalDate date, int[] numbers, int bonus, int pb) {
    this(game, date, numbers, bonus, pb, "", "", "imported");
  }

  public Draw(
      Game game,
      LocalDate date,
      int[] numbers,
      int bonus,
      int pb,
      String sourceUrl,
      String fetchedAt,
      String verification) {
    RuleSet rules = RuleSet.forDate(game, date);
    new Entry(game, numbers, pb, false, "", rules.id);
    this.ruleId = rules.id;
    this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
    this.fetchedAt = fetchedAt == null ? "" : fetchedAt;
    this.verification = verification == null ? "imported" : verification;
    this.game = game;
    this.date = date;
    this.numbers = numbers.clone();
    Arrays.sort(this.numbers);
    this.bonus = bonus;
    this.powerBall = pb;
    if (bonus < 0
        || bonus > rules.pool
        || (!game.hasBonus() && bonus != 0)
        || bonus != 0 && Arrays.binarySearch(this.numbers, bonus) >= 0)
      throw new IllegalArgumentException("Invalid or repeated bonus ball.");
  }

  public String key() {
    return game.name() + ":" + date;
  }
}
