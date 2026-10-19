package za.co.lottolab.studio;

import java.util.*;

/** Matches dated saved tickets to actually cached draw records; never assigns money. */
public final class TicketMatches {
  private TicketMatches() {}

  public static final class Item {
    public final TicketRecord ticket;
    public final Draw draw;
    public final int mainMatches;
    public final boolean powerBallMatch, bonusMatch;

    private Item(TicketRecord ticket, Draw draw, int mainMatches,
        boolean powerBallMatch, boolean bonusMatch) {
      this.ticket = ticket;
      this.draw = draw;
      this.mainMatches = mainMatches;
      this.powerBallMatch = powerBallMatch;
      this.bonusMatch = bonusMatch;
    }

    public String text() {
      String title = ticket.entry.game.shortName + " · " + ticket.date;
      if (draw == null) return title + " · results not stored yet (outcome unknown)";
      return title + " · " + mainMatches + " main matched"
          + (ticket.entry.game.hasPowerBall()
              ? (powerBallMatch ? " + PowerBall" : " (PowerBall not matched)")
              : bonusMatch ? " + bonus" : "")
          + " · cash payout not verified";
    }
  }

  public static List<Item> evaluate(List<TicketRecord> tickets, List<Draw> archive) {
    Map<String, Draw> byId = new HashMap<>();
    for (Draw d : archive) byId.put(d.key(), d);
    List<Item> out = new ArrayList<>();
    for (TicketRecord ticket : tickets) {
      Draw draw = byId.get(ticket.entry.game.name() + ":" + ticket.date);
      if (draw == null) {
        out.add(new Item(ticket, null, 0, false, false));
        continue;
      }
      int matched = AnalysisEngine.matches(ticket.entry.numbers, draw.numbers);
      boolean pb = ticket.entry.game.hasPowerBall()
          && ticket.entry.powerBall == draw.powerBall;
      boolean bonus = draw.bonus > 0
          && Arrays.binarySearch(ticket.entry.numbers, draw.bonus) >= 0;
      out.add(new Item(ticket, draw, matched, pb, bonus));
    }
    out.sort((a, b) -> b.ticket.date.compareTo(a.ticket.date));
    return out;
  }
}
