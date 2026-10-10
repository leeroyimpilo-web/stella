package za.co.lottolab;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class OfflineSmokeTest {
    Instrumentation inst=InstrumentationRegistry.getInstrumentation();
    String js(MainActivity a,String script)throws Exception{CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();inst.runOnMainSync(()->a.web.evaluateJavascript(script,v->{result.set(v);done.countDown();}));assertTrue("JavaScript callback timeout",done.await(10,TimeUnit.SECONDS));return result.get();}
    void waitJs(MainActivity a,String condition)throws Exception{for(int i=0;i<80;i++){if("true".equals(js(a,condition)))return;Thread.sleep(250);}fail("Condition failed: "+condition+"; body: "+js(a,"document.body.innerText.slice(-1200)"));}
    @Test public void offlineNumbersCoverageSaveAndPdf()throws Exception{
        Intent launch=new Intent(inst.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity a=(MainActivity)inst.startActivitySync(launch);
        try{
            waitJs(a,"document.querySelectorAll('.board').length===5");
            assertEquals("true",js(a,"!!crypto.getRandomValues && !!window.Android.savePdf"));
            js(a,"document.getElementById('save').click()");waitJs(a,"Number(document.getElementById('savedCount').textContent)>0");
            js(a,"document.getElementById('analyze').click()");waitJs(a,"document.getElementById('analysisResult').querySelector('strong')!==null");
            js(a,"document.getElementById('generateAll').click()");waitJs(a,"document.querySelectorAll('.board').length===30");
            assertEquals("true",js(a,"document.getElementById('selectionTitle').textContent==='All South African games'"));
            IntentFilter pdfFilter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);pdfFilter.addCategory(Intent.CATEGORY_OPENABLE);pdfFilter.addDataType("application/pdf");
            Instrumentation.ActivityMonitor monitor=inst.addMonitor(pdfFilter,new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);
            js(a,"document.getElementById('export').click()");
            for(int i=0;i<40&&monitor.getHits()==0;i++)Thread.sleep(250);
            assertTrue("PDF export should open Android Save As; notice="+js(a,"document.getElementById('notice').textContent"),monitor.getHits()>0);inst.removeMonitor(monitor);
            waitJs(a,"document.getElementById('notice').textContent.includes('cancelled')");
        }finally{inst.runOnMainSync(a::finish);}
    }
}
