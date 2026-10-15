package za.co.lottolab.studio;

import java.time.LocalDate;

/** The immutable selection and dated cost snapshot remain independent of a saved favourite. */
public final class TicketRecord {
  public final String id, note;
  public final Entry entry;
  public final LocalDate date;
  public final long costCents;
  public final boolean purchased;
  public final LocalDate purchaseDate;

  public TicketRecord(
      String id, Entry entry, LocalDate date, long costCents, boolean purchased, String note) {
    this(
        id,
        entry,
        date,
        costCents,
        purchased,
        note,
        purchased ? LocalDate.now(java.time.ZoneId.of("Africa/Johannesburg")) : null);
  }

  public TicketRecord(
      String id,
      Entry entry,
      LocalDate date,
      long costCents,
      boolean purchased,
      String note,
      LocalDate purchaseDate) {
    if (entry.subset) throw new IllegalArgumentException("A ticket needs a complete line.");
    RuleSet r = RuleSet.forDate(entry.game, date);
    if (!r.id.equals(entry.ruleId))
      throw new IllegalArgumentException(
          "The selected line uses different rules from this draw date. Create a line for that rule"
              + " period.");
    if (!entry.game.scheduled(date))
      throw new IllegalArgumentException(
          "This is not a scheduled draw date. Check the official calendar.");
    if (costCents < 0 || costCents > 100000000L)
      throw new IllegalArgumentException("Enter a valid non-negative cost.");
    if (id == null || id.trim().isEmpty())
      throw new IllegalArgumentException("A ticket needs an identifier.");
    if (purchased
        && (purchaseDate == null
            || purchaseDate.isAfter(LocalDate.now(java.time.ZoneId.of("Africa/Johannesburg")))))
      throw new IllegalArgumentException("Check the recorded purchase date.");
    this.id = id;
    this.entry = entry;
    this.date = date;
    this.costCents = costCents;
    this.purchased = purchased;
    this.note = note == null ? "" : note;
    this.purchaseDate = purchaseDate;
  }
}
