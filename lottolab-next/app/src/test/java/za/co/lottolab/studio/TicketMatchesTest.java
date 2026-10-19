package za.co.lottolab.studio;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TicketMatchesTest {
  @Test public void reportsUnknownWhenDatedDrawIsNotAvailable() {
    Entry pick = new Entry(Game.DAILY, new int[] {9,12,16,26,36}, 0, false, "");
    TicketRecord record = new TicketRecord("ticket1", pick,
        LocalDate.of(2026, 10, 18), 300, false, "");
    TicketMatches.Item outcome = TicketMatches.evaluate(
        Collections.singletonList(record), Collections.emptyList()).get(0);
    assertNull(outcome.draw);
    assertTrue(outcome.text().contains("unknown"));
  }

  @Test public void countsMainAndPowerballIndependentlyWithoutCashClaim() {
    Entry selected = new Entry(Game.POWERBALL, new int[] {15,33,38,42,49}, 4, false, "");
    TicketRecord ticket = new TicketRecord("ticket2", selected,
        LocalDate.of(2026, 10, 16), 1000, false, "");
    Draw draw = new Draw(Game.POWERBALL, LocalDate.of(2026,10,16),
        new int[] {15,33,38,42,49}, 0, 4);
    List<TicketMatches.Item> items =
        TicketMatches.evaluate(Collections.singletonList(ticket), Collections.singletonList(draw));
    assertEquals(5, items.get(0).mainMatches);
    assertTrue(items.get(0).powerBallMatch);
    assertTrue(items.get(0).text().contains("cash payout not verified"));
  }
}
