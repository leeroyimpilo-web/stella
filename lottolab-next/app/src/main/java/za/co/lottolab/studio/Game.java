package za.co.lottolab.studio;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum Game {
    LOTTO("LOTTO", "Lotto", 6, 52, 0, 500, 500, "Wed & Sat"),
    PLUS1("LOTTO PLUS 1", "Plus 1", 6, 52, 0, 250, 750, "Wed & Sat"),
    MAX5("LOTTO 5 MAX", "5 Max", 6, 52, 0, 250, 1000, "Wed & Sat"),
    POWERBALL("POWERBALL", "PowerBall", 5, 50, 16, 1000, 1000, "Tue & Fri"),
    XTRA("POWERBALL XTRA", "PB Xtra", 5, 50, 16, 500, 1500, "Tue & Fri"),
    DAILY("DAILY LOTTO", "Daily Lotto", 5, 36, 0, 300, 300, "Daily");

    public final String title, shortName, days;
    public final int picks, pool, powerPool, ownCostCents, bundleCostCents;
    Game(String title, String shortName, int picks, int pool, int powerPool, int ownCost, int bundleCost, String days) {
        this.title=title; this.shortName=shortName; this.picks=picks; this.pool=pool; this.powerPool=powerPool;
        this.ownCostCents=ownCost; this.bundleCostCents=bundleCost; this.days=days;
    }
    public boolean hasPowerBall() { return powerPool>0; }
    public boolean hasBonus() { return picks==6; }
    public boolean scheduled(LocalDate d) {
        DayOfWeek w=d.getDayOfWeek();
        return this==DAILY || (hasBonus() ? w==DayOfWeek.WEDNESDAY||w==DayOfWeek.SATURDAY : w==DayOfWeek.TUESDAY||w==DayOfWeek.FRIDAY);
    }
    public String costNote() {
        if(this==PLUS1) return "Includes the required LOTTO entry.";
        if(this==MAX5) return "Includes LOTTO and Plus 1.";
        if(this==XTRA) return "Includes the required PowerBall entry.";
        return "Entry costs exclude sales-channel fees.";
    }
    public static Game parse(String s) {
        String n=s.trim().toUpperCase(java.util.Locale.ROOT).replace('_',' ').replace('-',' ').replaceAll("\\s+", " ");
        for(Game g:values()) if(g.name().equals(n)||g.title.equals(n))return g;
        throw new IllegalArgumentException("Unknown current game: "+s);
    }
}
