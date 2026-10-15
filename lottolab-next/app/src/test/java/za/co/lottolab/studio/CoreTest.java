package za.co.lottolab.studio;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.security.SecureRandom;
import java.util.*;

public class CoreTest {
    private Draw lotto(int[] a,int bonus){return new Draw(Game.LOTTO,LocalDate.of(2026,10,14),a,bonus,0);}
    @Test public void jackpotSpacesFollowCurrentRules(){assertEquals(20358520L,AnalysisEngine.possible(Game.LOTTO));assertEquals(33900160L,AnalysisEngine.possible(Game.POWERBALL));assertEquals(376992L,AnalysisEngine.possible(Game.DAILY));assertEquals(28,AnalysisEngine.choose(8,6));}
    @Test public void requiredAddonCostsIncluded(){assertEquals(6000,AnalysisEngine.cost(Game.MAX5,6,1));assertEquals(48000,AnalysisEngine.cost(Game.MAX5,6,8));assertEquals(9000,AnalysisEngine.cost(Game.XTRA,6,1));assertEquals(750,AnalysisEngine.cost(Game.PLUS1,1,1));}
    @Test public void numberOrderDoesNotChangeLine(){Entry a=new Entry(Game.LOTTO,new int[]{47,36,28,19,12,4},0,false,"");assertArrayEquals(new int[]{4,12,19,28,36,47},a.numbers);assertEquals(a.key(),DemoArchive.initial(Game.LOTTO,0).key());}
    @Test public void invalidAndRepeatedNumbersRejected(){for(int[] a:new int[][]{{1,1,2,3,4,5},{1,2,3,4,5,53},{1,2},{0,2,3,4,5,6}}){try{new Entry(Game.LOTTO,a,0,false,"");fail("Invalid input accepted");}catch(IllegalArgumentException expected){}}}
    @Test public void powerBallMayEqualMainNumber(){Entry e=new Entry(Game.POWERBALL,new int[]{4,12,19,28,47},4,false,"");assertEquals(4,e.powerBall);}
    @Test public void bonusIsExcludedFromMainMatches(){Entry e=new Entry(Game.LOTTO,new int[]{1,2,3,4,5,6},0,false,"");AnalysisEngine.Result r=AnalysisEngine.analyse(e,Arrays.asList(lotto(new int[]{2,3,4,5,6,7},1)));assertEquals(5,r.best);assertEquals(0,r.completeHits);assertEquals(0,r.frequency[1]);assertEquals(1,r.distribution[5]);}
    @Test public void powerBallIsRequiredForCompleteEntry(){Entry e=DemoArchive.initial(Game.POWERBALL,0);Draw a=new Draw(Game.POWERBALL,LocalDate.of(2026,10,13),e.numbers,0,9);AnalysisEngine.Result r=AnalysisEngine.analyse(e,Arrays.asList(a));assertEquals(5,r.best);assertEquals(1,r.containsHits);assertEquals(0,r.completeHits);assertEquals(0,r.powerHits);}
    @Test public void subsetCountsContainingDraws(){Entry e=new Entry(Game.LOTTO,new int[]{4,12},0,true,"");AnalysisEngine.Result r=AnalysisEngine.analyse(e,Arrays.asList(lotto(new int[]{4,12,19,28,36,47},8),lotto(new int[]{1,4,13,23,34,45},8)));assertEquals(2,r.checked);assertEquals(1,r.containsHits);assertEquals(1,r.distribution[2]);assertEquals(1,r.distribution[1]);assertEquals(0,r.completeHits);}
    @Test public void distinctGamesNeverMerge(){Entry e=DemoArchive.initial(Game.LOTTO,0);Draw d=new Draw(Game.PLUS1,LocalDate.of(2026,10,14),e.numbers,8,0);AnalysisEngine.Result r=AnalysisEngine.analyse(e,Arrays.asList(d));assertEquals(0,r.checked);assertEquals(0,r.containsHits);}
    @Test public void pairCountsAreOncePerDraw(){java.util.List<Draw> d=Arrays.asList(lotto(new int[]{4,12,19,28,36,47},8),lotto(new int[]{1,2,3,4,12,50},8));assertEquals(2,AnalysisEngine.pairCount(4,12,d));assertEquals(0,AnalysisEngine.pairCount(4,8,d));}
    @Test public void missingGapIsNotZero(){java.util.List<Draw> d=Arrays.asList(lotto(new int[]{4,12,19,28,36,47},8));assertEquals(-1,AnalysisEngine.gap(8,d));assertEquals(0,AnalysisEngine.gap(4,d));}
    @Test public void csvRoundTripAndQuotedNumbers(){String csv="game,date,numbers,bonus,powerball\nLOTTO,2026-10-14,\"4,12,19,28,36,47\",8,\nPOWERBALL,2026-10-13,4 12 19 28 47,,8\n";java.util.List<Draw> ds=CsvCodec.parse(csv,LocalDate.of(2026,10,15));assertEquals(2,ds.size());assertEquals(ds.get(1).key(),CsvCodec.parse(CsvCodec.write(ds),LocalDate.of(2026,10,15)).get(1).key());}
    @Test public void invalidCsvCannotBecomePartialSuccess(){for(String tail:new String[]{"LOTTO,2026-10-10,1 1 2 3 4 5,8,","LOTTO,2027-01-01,1 2 3 4 5 6,8,","LOTTO,2026-10-14,1 2 3 4 5 6,8,"}){try{CsvCodec.parse("game,date,numbers,bonus,powerball\nLOTTO,2026-10-14,4 12 19 28 36 47,8,\n"+tail,LocalDate.of(2026,10,15));fail("Malformed/duplicate file accepted");}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().startsWith("Row 3:"));}}}
    @Test public void formerRuleEntriesAreRejectedInCurrentImporter(){try{new Draw(Game.LOTTO,LocalDate.of(2026,5,30),new int[]{1,2,3,4,5,6},8,0);fail();}catch(IllegalArgumentException expected){}}
    @Test public void generatedEntriesRespectPoolsAndUniqueness(){SecureRandom rng=new SecureRandom();for(Game g:Game.values())for(int i=0;i<100;i++){Entry e=Entry.random(g,rng);assertEquals(g.picks,e.numbers.length);assertTrue(e.numbers[e.numbers.length-1]<=g.pool);assertTrue(e.numbers[0]>0);if(g.hasPowerBall())assertTrue(e.powerBall>=1&&e.powerBall<=16);for(int j=1;j<e.numbers.length;j++)assertTrue(e.numbers[j]>e.numbers[j-1]);}}
    @Test public void sampleArchivesStayExplicitAndCompatible(){for(Game g:Game.values()){java.util.List<Draw> ds=DemoArchive.forGame(g);assertEquals(24,ds.size());Set<String> keys=new HashSet<>();for(Draw dr:ds){assertEquals(g,dr.game);assertTrue(g.scheduled(dr.date));assertTrue(keys.add(dr.key()));}}}
}
