package za.co.lottolab.studio;

import static org.junit.Assert.*;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class ToolsTest {
  @Test
  public void historicalPowerPoolIsSeparateAndDated() {
    assertEquals(20, RuleSet.forDate(Game.POWERBALL, LocalDate.of(2026, 5, 29)).powerPool);
    assertEquals(16, RuleSet.forDate(Game.POWERBALL, LocalDate.of(2026, 6, 2)).powerPool);
    Draw d = new Draw(Game.POWERPLUS, LocalDate.of(2026, 5, 29), new int[] {1, 2, 3, 4, 5}, 0, 20);
    assertEquals("2025-09", d.ruleId);
  }

  @Test
  public void incompatibleDatesAndLegacyNamesAreRejected() {
    for (Game g : new Game[] {Game.PLUS2, Game.POWERPLUS, Game.DAILYPLUS}) {
      try {
        RuleSet.forDate(g, LocalDate.of(2026, 6, 1));
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
    for (Game g : new Game[] {Game.MAX5, Game.XTRA}) {
      try {
        RuleSet.forDate(g, LocalDate.of(2026, 5, 31));
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
    try {
      RuleSet.forDate(Game.LOTTO, LocalDate.of(2025, 9, 20));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void probabilitiesSumToOneForEachRuleAndGame() {
    for (Game g : Game.values()) {
      RuleSet r = RuleSet.current(g);
      double sum = 0;
      for (int m = 0; m <= r.picks; m++) {
        sum += ToolEngine.categoryProbability(g, r, m, false);
        if (g.hasPowerBall() || g.hasBonus()) sum += ToolEngine.categoryProbability(g, r, m, true);
      }
      assertEquals(g.name(), 1.0, sum, 1e-12);
      double jackpot = ToolEngine.categoryProbability(g, r, r.picks, g.hasPowerBall());
      assertEquals(
          1.0 / (AnalysisEngine.choose(r.pool, r.picks) * (r.powerPool > 0 ? r.powerPool : 1)),
          jackpot,
          1e-12);
    }
  }

  @Test
  public void generatorHonoursLocksExclusionsAndUniqueEntries() {
    List<Entry> es =
        ToolEngine.generate(
            Game.LOTTO,
            "2026-06",
            100,
            new int[] {1, 2},
            new int[] {51, 52},
            0,
            3,
            new SecureRandom());
    Set<String> keys = new HashSet<>();
    for (Entry e : es) {
      assertTrue(keys.add(e.key()));
      assertTrue(Arrays.binarySearch(e.numbers, 1) >= 0);
      assertTrue(Arrays.binarySearch(e.numbers, 2) >= 0);
      assertTrue(Arrays.binarySearch(e.numbers, 51) < 0);
      assertTrue(Arrays.binarySearch(e.numbers, 52) < 0);
      int odd = 0;
      for (int n : e.numbers) if (n % 2 == 1) odd++;
      assertEquals(3, odd);
    }
  }

  @Test
  public void impossibleGeneratorPreferencesFailWithoutHanging() {
    try {
      ToolEngine.generate(
          Game.LOTTO,
          "2026-06",
          2,
          new int[] {1, 2, 3, 4, 5, 6},
          new int[0],
          0,
          -1,
          new SecureRandom());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      ToolEngine.generate(
          Game.LOTTO, "2026-06", 1, new int[] {1}, new int[] {1}, 0, -1, new SecureRandom());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      ToolEngine.generate(
          Game.LOTTO, "2026-06", 1, new int[] {1, 3, 5, 7}, new int[0], 0, 2, new SecureRandom());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void lockedPowerBallDoesNotExcludeSameMainNumber() {
    List<Entry> es =
        ToolEngine.generate(
            Game.POWERBALL, "2026-06", 10, new int[] {4}, new int[0], 4, -1, new SecureRandom());
    for (Entry e : es) {
      assertEquals(4, e.powerBall);
      assertTrue(Arrays.binarySearch(e.numbers, 4) >= 0);
    }
  }

  @Test
  public void fullWheelEnumeratesEverySubsetOnce() {
    List<Entry> es = ToolEngine.wheel(Game.LOTTO, new int[] {1, 2, 3, 4, 5, 6, 7, 8}, new int[0]);
    assertEquals(28, es.size());
    Set<String> keys = new HashSet<>();
    for (Entry e : es) assertTrue(keys.add(e.key()));
    assertEquals(14000, es.size() * Game.LOTTO.bundleCostCents);
  }

  @Test
  public void separateBallChoicesMultiplyWheelCount() {
    assertEquals(
        12,
        ToolEngine.wheel(Game.POWERBALL, new int[] {1, 2, 3, 4, 5, 6}, new int[] {1, 2}).size());
  }

  @Test
  public void oversizedWheelsAreBounded() {
    try {
      ToolEngine.wheel(
          Game.LOTTO, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, new int[0]);
      fail();
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("2,000"));
    }
  }

  @Test
  public void triplesCountMainNumbersOnly() {
    List<Draw> d =
        Arrays.asList(
            new Draw(Game.LOTTO, LocalDate.of(2026, 10, 14), new int[] {1, 2, 3, 4, 5, 6}, 7, 0));
    assertEquals(1, ToolEngine.subsetCount(new int[] {1, 2, 3}, d));
    assertEquals(0, ToolEngine.subsetCount(new int[] {1, 2, 7}, d));
  }

  @Test
  public void emptyHistoryIsUnknown() {
    assertEquals("Unknown", ToolEngine.percent(0, 0));
    assertEquals(-1, AnalysisEngine.gap(1, Collections.emptyList()));
  }

  @Test
  public void eligibleWindowDoesNotMixRuleEras() {
    List<Draw> d =
        Arrays.asList(
            new Draw(Game.LOTTO, LocalDate.of(2026, 5, 30), new int[] {1, 2, 3, 4, 5, 58}, 57, 0),
            new Draw(Game.LOTTO, LocalDate.of(2026, 10, 14), new int[] {1, 2, 3, 4, 5, 6}, 7, 0));
    assertEquals(1, ToolEngine.eligible(Game.LOTTO, "2026-06", d, 100).size());
    assertEquals(1, ToolEngine.eligible(Game.LOTTO, "2025-09", d, 100).size());
  }

  @Test
  public void gapsAreMeasuredInsideAvailableSpan() {
    List<Draw> d =
        Arrays.asList(
            new Draw(Game.LOTTO, LocalDate.of(2026, 10, 14), new int[] {1, 2, 3, 4, 5, 6}, 7, 0),
            new Draw(Game.LOTTO, LocalDate.of(2026, 10, 7), new int[] {1, 2, 3, 4, 5, 6}, 7, 0));
    assertEquals(1, ToolEngine.missingScheduled(Game.LOTTO, d));
  }

  @Test
  public void archiveSourceMetadataRoundTrips() {
    String input =
        "game,date,numbers,bonus,powerball,source_url,fetched_at\n"
            + "LOTTO,2026-10-14,1 2 3 4 5 6,7,,https://example.com/test,2026-10-15T10:00:00Z\n";
    List<Draw> d = CsvCodec.parse(input, LocalDate.of(2026, 10, 15));
    Draw r = CsvCodec.parse(CsvCodec.write(d), LocalDate.of(2026, 10, 15)).get(0);
    assertEquals(d.get(0).sourceUrl, r.sourceUrl);
    assertEquals(d.get(0).fetchedAt, r.fetchedAt);
  }

  @Test
  public void preAndPostTransitionImportsStaySeparate() {
    String s =
        "game,date,numbers,bonus,powerball\n"
            + "LOTTO PLUS 2,2026-05-30,1 2 3 4 5 58,57,\n"
            + "LOTTO 5 MAX,2026-06-03,1 2 3 4 5 6,7,\n";
    List<Draw> d = CsvCodec.parse(s, LocalDate.of(2026, 10, 15));
    assertNotEquals(d.get(0).game, d.get(1).game);
  }

  @Test
  public void snapshotRejectsSubsetAndMismatchedEra() {
    try {
      new TicketRecord(
          "1",
          new Entry(Game.LOTTO, new int[] {1, 2}, 0, true, ""),
          LocalDate.of(2026, 10, 14),
          500,
          false,
          "");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      new TicketRecord(
          "2",
          new Entry(Game.LOTTO, new int[] {1, 2, 3, 4, 5, 6}, 0, false, ""),
          LocalDate.of(2026, 5, 30),
          500,
          false,
          "");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void savedTicketSnapshotDoesNotAliasInputArray() {
    int[] n = {1, 2, 3, 4, 5, 6};
    Entry e = new Entry(Game.LOTTO, n, 0, false, "Original");
    TicketRecord t = new TicketRecord("1", e, LocalDate.of(2026, 10, 14), 500, false, "");
    n[0] = 50;
    assertEquals(1, t.entry.numbers[0]);
    assertEquals("Original", t.entry.name);
    assertEquals("New name", e.named("New name").name);
    assertEquals("Original", t.entry.name);
  }

  @Test
  public void ChristmasIsNotAssumedToBeADraw() {
    assertFalse(Game.DAILY.scheduled(LocalDate.of(2026, 12, 25)));
  }

  @Test
  public void invalidCsvSourceIsRejected() {
    try {
      CsvCodec.parse(
          "game,date,numbers,bonus,source_url\nLOTTO,2026-10-14,1 2 3 4 5 6,7,http://example.com",
          LocalDate.of(2026, 10, 15));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void spreadsheetFormulaTextIsNeutralised() {
    assertTrue(CsvCodec.quote("=1+1").startsWith("\"'="));
  }
}
