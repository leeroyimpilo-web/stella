package za.co.lottolab.studio;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class DrawStatsTest {
  @Test public void emptyArchiveHasNoSummary() {
    assertNull(DrawStats.summarize(Game.LOTTO, Collections.emptyList(), 30));
  }

  @Test public void latestRuleEraIsNotMixedWithHistoricalNumbers() {
    Draw newest = new Draw(Game.LOTTO, LocalDate.of(2026, 10, 17),
        new int[] {2, 4, 6, 10, 15, 46}, 1, 0);
    Draw previous = new Draw(Game.LOTTO, LocalDate.of(2026, 10, 10),
        new int[] {5, 10, 14, 29, 30, 45}, 15, 0);
    Draw legacy = new Draw(Game.LOTTO, LocalDate.of(2026, 5, 30),
        new int[] {1, 2, 3, 4, 5, 58}, 7, 0);
    DrawStats.Summary s = DrawStats.summarize(Game.LOTTO,
        Arrays.asList(newest, previous, legacy), 100);
    assertNotNull(s);
    assertEquals(2, s.count);
    assertEquals(LocalDate.of(2026, 10, 10), s.first);
    assertEquals(LocalDate.of(2026, 10, 17), s.last);
    assertEquals(11, s.distinctNumbers);
    assertTrue(s.mainLeaders.startsWith("10 (2)"));
    assertEquals(108.0, s.averageSum, 0.001);
    assertEquals(2.0, s.averageOdd, 0.001);
    assertEquals(0.5, s.averageConsecutive, 0.001);
    assertEquals(1.0, s.averageRepeated, 0.001);
    assertEquals(1, s.repeatedComparisons);
    assertTrue(s.fullReport().contains("not predictions"));
  }

  @Test public void periodLimitIsHonoured() {
    Draw d1 = new Draw(Game.DAILY, LocalDate.of(2026, 10, 18),
        new int[] {9, 12, 16, 26, 36}, 0, 0);
    Draw d2 = new Draw(Game.DAILY, LocalDate.of(2026, 10, 17),
        new int[] {2, 5, 12, 20, 28}, 0, 0);
    assertEquals(1, DrawStats.summarize(Game.DAILY, Arrays.asList(d1, d2), 1).count);
    assertEquals(2, DrawStats.summarize(Game.DAILY, Arrays.asList(d1, d2), 0).count);
  }
}
