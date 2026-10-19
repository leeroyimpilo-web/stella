package za.co.lottolab.studio;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.Test;

public class BetTipResultsTest {
  private static final String HEADER =
      "{\"api\":\"BetTip Draws API\",\"version\":\"v1\",\"builtAt\":\"2026-10-19T17:10:46Z\",";

  @Test public void latestUsesPublishedSchemaAndMapsNewPowerBallXtra() {
    String latest = HEADER + "\"resource\":\"lotto/latest\",\"data\":{"
        + "\"byGame\":{\"lotto\":{\"game\":\"lotto\",\"date\":\"2026-10-17\","
        + "\"numbers\":[2,4,6,10,15,46],\"bonusBall\":1,\"verified\":true,"
        + "\"sourceUrl\":\"https://www.nationallottery.co.za/#/results\"}},"
        + "\"recent\":[{\"game\":\"powerball-plus\",\"date\":\"2026-10-16\","
        + "\"numbers\":[13,33,34,35,47],\"bonusBall\":11,"
        + "\"roundTwo\":null,\"official\":true}]}}";
    List<Draw> results = BetTipResults.parseLatest(latest, LocalDate.of(2026, 10, 19));
    assertEquals(2, results.size());
    assertEquals(Game.LOTTO, results.get(0).game);
    assertEquals(1, results.get(0).bonus);
    assertEquals(Game.XTRA, results.get(1).game);
    assertEquals(11, results.get(1).powerBall);
    assertEquals("bettip", results.get(0).verification);
    assertEquals(BetTipResults.DOCS, results.get(0).sourceUrl);
  }

  @Test public void repeatedLatestRowMustMatchIdentically() {
    String row = "{\"game\":\"daily-lotto\",\"date\":\"2026-10-18\","
        + "\"numbers\":[9,12,16,26,36],\"bonusBall\":null}";
    String latest = HEADER + "\"resource\":\"lotto/latest\",\"data\":{"
        + "\"byGame\":{\"daily-lotto\":" + row + "},\"recent\":[" + row + "]}}";
    assertEquals(1, BetTipResults.parseLatest(latest, LocalDate.of(2026,10,19)).size());
  }

  @Test public void historicalDataPreservesLegacyGameNamesAndIgnoresUnsupportedEra() {
    String history = HEADER + "\"resource\":\"lotto/draws\",\"data\":["
        + "{\"game\":\"powerball-plus\",\"date\":\"2025-12-02\","
        + "\"numbers\":[2,4,6,8,10],\"bonusBall\":19},"
        + "{\"game\":\"powerball-plus\",\"date\":\"2026-10-16\","
        + "\"numbers\":[13,33,34,35,47],\"bonusBall\":11},"
        + "{\"game\":\"lotto-5-max\",\"date\":\"2026-10-17\","
        + "\"numbers\":[4,12,17,23,33,42],\"bonusBall\":15},"
        + "{\"game\":\"lotto-5-max\",\"date\":\"2026-05-30\","
        + "\"numbers\":[4,12,17,23,33,42],\"bonusBall\":15},"
        + "{\"game\":\"lotto\",\"date\":\"2025-01-01\","
        + "\"numbers\":[1,2,3,4,5,6],\"bonusBall\":7}]}";
    List<Draw> results = BetTipResults.parseArchive(history, LocalDate.of(2026,10,19));
    assertEquals(3, results.size());
    assertEquals(Game.MAX5, results.get(0).game);
    assertEquals(Game.XTRA, results.get(1).game);
    assertEquals(Game.POWERPLUS, results.get(2).game);
    assertEquals(19, results.get(2).powerBall);
  }

  @Test public void futureNumbersAndDuplicateBallAreRejected() {
    String fixture = HEADER + "\"resource\":\"lotto/latest\",\"data\":{"
        + "\"recent\":[{\"game\":\"daily-lotto\",\"date\":\"2026-10-20\","
        + "\"numbers\":[1,2,3,4,5],\"bonusBall\":null}]}}";
    try {
      BetTipResults.parseLatest(fixture, LocalDate.of(2026,10,19));
      fail("future draw should fail");
    } catch (IllegalArgumentException expected) {}
    String repeated = fixture.replace("2026-10-20", "2026-10-18")
        .replace("[1,2,3,4,5]", "[1,1,3,4,5]");
    try {
      BetTipResults.parseLatest(repeated, LocalDate.of(2026,10,19));
      fail("duplicate main numbers should fail");
    } catch (IllegalArgumentException expected) {}
  }

  @Test public void unknownAPIFormatIsNeverImported() {
    try {
      BetTipResults.parseLatest("{\"data\":{\"recent\":[]}}", LocalDate.of(2026,10,19));
      fail("wrong response envelope");
    } catch (IllegalArgumentException expected) {}
  }
}
