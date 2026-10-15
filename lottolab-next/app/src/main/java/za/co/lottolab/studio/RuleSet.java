package za.co.lottolab.studio;

import java.time.LocalDate;

/** Explicit supported eras. Earlier undocumented rules are rejected rather than guessed. */
public final class RuleSet {
  public static final LocalDate LEGACY_START = LocalDate.of(2025, 9, 21);
  public static final LocalDate CURRENT_START = LocalDate.of(2026, 6, 1);
  public final String id;
  public final int pool, powerPool, picks, bundleCents;
  public final LocalDate start, end;

  private RuleSet(Game g, boolean legacy) {
    id = legacy ? "2025-09" : "2026-06";
    picks = g.picks;
    pool = legacy && g.hasBonus() ? 58 : g.pool;
    powerPool = g.hasPowerBall() ? (legacy ? 20 : 16) : 0;
    bundleCents = legacy && g == Game.POWERBALL ? 500 : g.bundleCostCents;
    start = legacy ? LEGACY_START : CURRENT_START;
    end = legacy ? CURRENT_START.minusDays(1) : LocalDate.of(9999, 12, 31);
  }

  public static RuleSet forDate(Game g, LocalDate date) {
    if (date.isBefore(LEGACY_START))
      throw new IllegalArgumentException(
          "Supported history begins 21 September 2025. Earlier rules are not verified in this"
              + " build.");
    boolean legacy = date.isBefore(CURRENT_START);
    if (legacy && (g == Game.MAX5 || g == Game.XTRA))
      throw new IllegalArgumentException(
          "Use the historical Plus 2 or PowerBall Plus game before June 2026.");
    if (!legacy && !g.active())
      throw new IllegalArgumentException("This archive-only game ended in May 2026.");
    return new RuleSet(g, legacy);
  }

  public static RuleSet forId(Game g, String id) {
    if (!"2025-09".equals(id) && !"2026-06".equals(id))
      throw new IllegalArgumentException("Unknown rule period: " + id);
    return forDate(g, "2025-09".equals(id) || !g.active() ? LEGACY_START : CURRENT_START);
  }

  public static RuleSet current(Game g) {
    return forId(g, "2026-06");
  }

  public boolean accepts(LocalDate date) {
    return !date.isBefore(start) && !date.isAfter(end);
  }

  public String description() {
    return picks + " from " + pool + (powerPool > 0 ? " + PowerBall 1–" + powerPool : "");
  }
}
