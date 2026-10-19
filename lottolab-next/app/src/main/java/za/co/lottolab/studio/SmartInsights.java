package za.co.lottolab.studio;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/** Explains recent results using reproducible arithmetic, not lottery prediction. */
public final class SmartInsights {
  public final int drawCount, mostNumber, mostCount, leastNumber, leastCount;
  public final int lastOdd, lastEven, lastTotal, repeatsFromPrevious;
  public final String firstDate, lastDate;

  private SmartInsights(int count, int hot, int hotCount, int cold, int coldCount,
      int odd, int even, int total, int repeats, String first, String last) {
    drawCount = count;
    mostNumber = hot;
    mostCount = hotCount;
    leastNumber = cold;
    leastCount = coldCount;
    lastOdd = odd;
    lastEven = even;
    lastTotal = total;
    repeatsFromPrevious = repeats;
    firstDate = first;
    lastDate = last;
  }

  public static SmartInsights analyze(Game game, List<Draw> all, int limit) {
    if (all == null || all.isEmpty()) return null;
    // Never mix the 6/58 and 6/52 rule periods when counting number appearances.
    String ruleId = all.get(0).ruleId;
    List<Draw> draws = new ArrayList<>();
    for (Draw d : all) {
      if (d.game == game && d.ruleId.equals(ruleId)) {
        draws.add(d);
        if (draws.size() == limit) break;
      }
    }
    if (draws.isEmpty()) return null;
    RuleSet rules = RuleSet.forId(game, ruleId);
    int[] freq = new int[rules.pool + 1];
    for (Draw d : draws) for (int number : d.numbers) freq[number]++;
    int hot = 1, cold = 1;
    for (int number = 2; number <= rules.pool; number++) {
      if (freq[number] > freq[hot]) hot = number;
      if (freq[number] < freq[cold]) cold = number;
    }
    Draw latest = draws.get(0);
    int odd = 0, sum = 0, repeated = -1;
    for (int n : latest.numbers) {
      if (n % 2 != 0) odd++;
      sum += n;
    }
    if (draws.size() > 1)
      repeated = AnalysisEngine.matches(latest.numbers, draws.get(1).numbers);
    return new SmartInsights(draws.size(), hot, freq[hot], cold, freq[cold],
        odd, latest.numbers.length - odd, sum, repeated,
        draws.get(draws.size() - 1).date.toString(), latest.date.toString());
  }
}
