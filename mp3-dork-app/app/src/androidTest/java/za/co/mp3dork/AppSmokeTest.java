package za.co.mp3dork;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.app.Instrumentation;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {
    private Instrumentation getInstrumentation(){return InstrumentationRegistry.getInstrumentation();}
    @Test public void testDownloadPersistenceAndOfflinePlayback() throws Exception {
        Intent launch=new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(launch);
        getInstrumentation().waitForIdleSync();
        for(int tab=0;tab<4;tab++){final int index=tab;getInstrumentation().runOnMainSync(()->activity.selectTab(index));getInstrumentation().waitForIdleSync();capture(activity,"tab-"+tab);}
        RemoteAudio.Result result=RemoteAudio.probe("http://127.0.0.1:8765/tone.mp3",null,"MP3DorkTest",null);
        assertTrue(result.size>1000);assertEquals("tone.mp3",result.name);
        try{RemoteAudio.probe("http://127.0.0.1:8765/fake.mp3",null,"MP3DorkTest",null);fail("HTML accepted as MP3");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("not an MP3"));}
        final long[] id={-1};getInstrumentation().runOnMainSync(()->id[0]=activity.enqueueDownload(result,"MP3Dork-smoke.mp3"));assertTrue(id[0]>0);
        DownloadManager manager=activity.getSystemService(DownloadManager.class);
        long deadline=System.currentTimeMillis()+60000;boolean done=false;
        while(System.currentTimeMillis()<deadline){try(Cursor c=manager.query(new DownloadManager.Query().setFilterById(id[0]))){if(c.moveToFirst()){int status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));if(status==DownloadManager.STATUS_SUCCESSFUL){done=true;break;}if(status==DownloadManager.STATUS_FAILED)fail("Download failed: "+c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)));}}Thread.sleep(500);}
        assertTrue("Download did not complete",done);
        Uri uri=manager.getUriForDownloadedFile(id[0]);assertNotNull(uri);
        try(InputStream stream=activity.getContentResolver().openInputStream(uri)){byte[] header=new byte[512];int count=stream.read(header);assertTrue(MediaTools.hasMp3Signature(header,count));}
        assertEquals(1,new Store(activity).list("downloads").length());
        getInstrumentation().runOnMainSync(()->PlaybackService.play(activity,uri.toString(),"Offline smoke test",null,null));
        deadline=System.currentTimeMillis()+12000;
        while(System.currentTimeMillis()<deadline && (PlaybackService.instance==null || !PlaybackService.instance.isPlaying()))Thread.sleep(150);
        assertNotNull(PlaybackService.instance);assertTrue("Offline audio did not start",PlaybackService.instance.isPlaying());assertTrue(PlaybackService.instance.duration()>0);
        getInstrumentation().runOnMainSync(()->activity.selectTab(2));getInstrumentation().waitForIdleSync();Thread.sleep(1000);capture(activity,"offline-player");
        getInstrumentation().runOnMainSync(()->PlaybackService.instance.pause());assertFalse(PlaybackService.instance.isPlaying());
        getInstrumentation().runOnMainSync(()->PlaybackService.instance.resume());assertTrue(PlaybackService.instance.isPlaying());
        getInstrumentation().runOnMainSync(()->activity.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)));
        Thread.sleep(800);assertTrue("Background playback stopped",PlaybackService.instance.isPlaying());
        getInstrumentation().runOnMainSync(()->{PlaybackService.stop(activity);manager.remove(id[0]);activity.storeForTest().remove("downloads","id",String.valueOf(id[0]));activity.finish();});
    }
    private void capture(MainActivity activity,String name) throws Exception {
        Thread.sleep(200);
        android.graphics.Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull("Screen capture was unavailable",bitmap);
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,name+".png");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/MP3DorkQA/");
        values.put(android.provider.MediaStore.Images.Media.IS_PENDING,1);
        Uri image=activity.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull(image);
        try(java.io.OutputStream out=activity.getContentResolver().openOutputStream(image)){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}
        values.clear();values.put(android.provider.MediaStore.Images.Media.IS_PENDING,0);
        activity.getContentResolver().update(image,values,null,null);bitmap.recycle();
    }
}
