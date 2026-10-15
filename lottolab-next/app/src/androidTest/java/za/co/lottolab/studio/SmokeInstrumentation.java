package za.co.lottolab.studio;
import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.LocalDate;
import java.util.Arrays;

/** Device-level smoke test and actual native UI screenshots. */
public class SmokeInstrumentation extends Instrumentation {
    private MainActivity activity;
    private File folder;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();SystemClock.sleep(600);
        folder=new File(getTargetContext().getExternalFilesDir(null),"qa");if(!folder.exists()&&!folder.mkdirs())throw new IOException("Cannot create QA directory");
        requireText("SAMPLE DATA");shot("home");
        click("tab_history");requireText("fictional sample");shot("history");
        click("tab_lab");requireText("RUN ANALYSIS");shot("lab");
        click("edit_line_0");SystemClock.sleep(300);shot("number-editor");getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);waitForIdleSync();SystemClock.sleep(200);
        click("run_analysis");requireText("draws checked");onMain(()->{ScrollView s=activity.findViewById(android.R.id.content).findViewWithTag("main_scroll");s.fullScroll(View.FOCUS_DOWN);});shot("analysis");
        ArchiveStore store=new ArchiveStore(getTargetContext());Entry saved=DemoArchive.initial(Game.LOTTO,0).named("My first experiment");store.save(saved);if(store.saved().isEmpty())throw new AssertionError("Saved combination lost");click("tab_vault");requireText("MY FIRST EXPERIMENT");shot("my-numbers");
        click("tab_planner");requireText("PLANNED ENTRY COST");shot("planner");
        java.util.List<Draw> imported=CsvCodec.parse("game,date,numbers,bonus,powerball\nLOTTO,2026-10-14,4 12 19 28 36 47,8,",LocalDate.of(2026,10,15));store.importDraws(imported);if(!store.imported(Game.LOTTO)||store.draws(Game.LOTTO,0).size()!=1)throw new AssertionError("Archive persistence failed");click("tab_home");requireText("IMPORTED DATA");shot("imported-home");store.clearImports();
        try(FileOutputStream out=new FileOutputStream(new File(folder,"sample-research.pdf"))){PdfReport.write(out,"Fictional sample QA report\nGame: LOTTO\nNumbers: 04 12 19 28 36 47\nDraws checked: 24\nNo verified prize entitlement.");}
        result.putString("passed","true");result.putString("screenshots",folder.getAbsolutePath());finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("passed","false");result.putString("error",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
    private void onMain(Runnable r){runOnMainSync(r);waitForIdleSync();}
    private void click(String tag){onMain(()->{View v=activity.getWindow().getDecorView().findViewWithTag(tag);if(v==null)throw new AssertionError("Missing control: "+tag);if(!v.performClick())throw new AssertionError("Cannot click "+tag);});SystemClock.sleep(200);}
    private void requireText(String value){final boolean[] found={false};onMain(()->found[0]=contains(activity.getWindow().getDecorView(),value));if(!found[0])throw new AssertionError("Missing text: "+value);}
    private boolean contains(View v,String value){if(v instanceof TextView&&((TextView)v).getText().toString().contains(value))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(contains(g.getChildAt(i),value))return true;}return false;}
    private void shot(String name)throws IOException{waitForIdleSync();SystemClock.sleep(350);Bitmap b=getUiAutomation().takeScreenshot();if(b==null)throw new IOException("Screenshot unavailable");try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
