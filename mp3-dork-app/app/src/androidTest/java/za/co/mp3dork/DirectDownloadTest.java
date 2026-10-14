package za.co.mp3dork;

import android.app.DownloadManager;
import android.app.Instrumentation;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import java.util.ArrayList;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DirectDownloadTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final ArrayList<Long> ids=new ArrayList<>();

    private MainActivity launch(Intent intent){
        intent.setClass(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        return activity;
    }
    private <T extends View> T find(View view,Class<T> kind,String value){
        if(kind.isInstance(view)){
            if(value==null)return kind.cast(view);
            if(view instanceof EditText && value.contentEquals(((EditText)view).getHint()))return kind.cast(view);
            if(view instanceof Button && value.contentEquals(((Button)view).getText()))return kind.cast(view);
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            T result=find(((ViewGroup)view).getChildAt(i),kind,value);if(result!=null)return result;
        }
        return null;
    }
    private long record(MainActivity activity,String url)throws Exception{
        long deadline=SystemClock.elapsedRealtime()+10000;
        while(SystemClock.elapsedRealtime()<deadline){
            org.json.JSONArray rows=activity.storeForTest().list("downloads");
            for(int i=0;i<rows.length();i++){
                JSONObject row=rows.getJSONObject(i);
                if(url.equals(row.optString("url"))){long id=row.getLong("id");if(!ids.contains(id))ids.add(id);return id;}
            }
            Thread.sleep(100);
        }
        fail("The link was not queued directly");return -1;
    }
    private void completed(MainActivity activity,long id)throws Exception{
        DownloadManager manager=activity.getSystemService(DownloadManager.class);
        long deadline=SystemClock.elapsedRealtime()+45000;
        while(SystemClock.elapsedRealtime()<deadline){
            try(Cursor c=manager.query(new DownloadManager.Query().setFilterById(id))){
                if(c!=null && c.moveToFirst()){
                    int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if(status==DownloadManager.STATUS_SUCCESSFUL){
                        try(InputStream stream=activity.getContentResolver().openInputStream(manager.getUriForDownloadedFile(id))){
                            byte[] data=new byte[512];assertTrue(MediaTools.hasMp3Signature(data,stream.read(data)));
                        }
                        return;
                    }
                    if(status==DownloadManager.STATUS_FAILED)fail("Direct download failed: "+c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)));
                }
            }
            Thread.sleep(200);
        }
        fail("Direct download did not finish");
    }
    private void cleanup(MainActivity activity){
        instrumentation.runOnMainSync(()->{
            for(long id:ids){activity.getSystemService(DownloadManager.class).remove(id);activity.storeForTest().remove("downloads","id",String.valueOf(id));}
            activity.finish();
        });
        instrumentation.waitForIdleSync();
    }
    @Test public void pastedLinkDownloadsWithoutAudioCheck()throws Exception{
        MainActivity activity=launch(new Intent(Intent.ACTION_MAIN));
        String url="http://127.0.0.1:8765/no-probe.mp3?pasted=1";
        try{
            long started=SystemClock.elapsedRealtime();
            instrumentation.runOnMainSync(()->{
                View root=activity.findViewById(android.R.id.content);
                EditText input=find(root,EditText.class,"https://example.org/track.mp3");assertNotNull(input);input.setText(url);
                Button download=find(root,Button.class,"Download MP3");assertNotNull(download);assertTrue(download.performClick());
            });
            long id=record(activity,url);
            assertTrue("Pasted link waited for the source before queuing",SystemClock.elapsedRealtime()-started<5000);
            completed(activity,id);
            final long[] duplicate={-1};
            instrumentation.runOnMainSync(()->duplicate[0]=activity.startDownload(url,null));
            assertEquals("The same active download was queued twice",id,duplicate[0]);
        }finally{cleanup(activity);}
    }
    @Test public void stalledSourceDoesNotBlockNavigationOrAnotherDownload()throws Exception{
        MainActivity activity=launch(new Intent(Intent.ACTION_MAIN));
        try{
            final long[] slow={-1},next={-1};long started=SystemClock.elapsedRealtime();
            instrumentation.runOnMainSync(()->{slow[0]=activity.startDownload("http://127.0.0.1:8765/slow.mp3",null);activity.selectTab(2);});
            if(slow[0]>0)ids.add(slow[0]);assertTrue(slow[0]>0);
            assertTrue("Slow source blocked the UI",SystemClock.elapsedRealtime()-started<5000);
            instrumentation.runOnMainSync(()->next[0]=activity.startDownload("http://127.0.0.1:8765/no-probe.mp3?second=1",null));
            if(next[0]>0)ids.add(next[0]);assertTrue(next[0]>0);completed(activity,next[0]);
        }finally{cleanup(activity);}
    }
    @Test public void tappingBrowserMp3LinkQueuesDirectly()throws Exception{
        MainActivity activity=launch(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"http://127.0.0.1:8765/links"));
        try{
            boolean clicked=false;long deadline=SystemClock.elapsedRealtime()+10000;
            while(!clicked && SystemClock.elapsedRealtime()<deadline){
                AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
                if(root!=null)for(AccessibilityNodeInfo item:root.findAccessibilityNodeInfosByText("Open source page")){
                    AccessibilityNodeInfo node=item;while(node!=null && !node.isClickable())node=node.getParent();
                    if(node!=null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){clicked=true;break;}
                }
                if(!clicked)Thread.sleep(100);
            }
            assertTrue("Could not open the source page",clicked);
            final boolean[] ready={false};deadline=SystemClock.elapsedRealtime()+20000;
            while(!ready[0] && SystemClock.elapsedRealtime()<deadline){
                instrumentation.runOnMainSync(()->{
                    WebView browser=find(activity.findViewById(android.R.id.content),WebView.class,null);
                    if(browser!=null && "http://127.0.0.1:8765/links".equals(browser.getUrl()) && browser.getProgress()==100){ready[0]=true;browser.evaluateJavascript("document.getElementById('track').click()",null);}
                });
                if(!ready[0])Thread.sleep(100);
            }
            assertTrue("Source page did not load",ready[0]);
            completed(activity,record(activity,"http://127.0.0.1:8765/no-probe.mp3?browser=1"));
        }finally{cleanup(activity);}
    }
}
