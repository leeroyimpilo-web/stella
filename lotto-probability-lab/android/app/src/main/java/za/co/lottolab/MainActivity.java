package za.co.lottolab;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.util.Base64;
import android.webkit.*;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static final String HOST="appassets.androidplatform.net";
    static final String HOME="https://"+HOST+"/assets/index.html";
    static final int SAVE_PDF=2001;
    WebView web;
    byte[] pendingPdf;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web=new WebView(this);
        setContentView(web);
        web.setBackgroundColor(0xfff4f5f0);
        web.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setTextZoom(100);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",path->{
            // Explicit JavaScript MIME is necessary for strict module and Worker loading.
            if(path.contains("..")||path.startsWith("/"))return blocked();
            try{String mime=path.endsWith(".mjs")?"application/javascript":path.endsWith(".css")?"text/css":path.endsWith(".html")?"text/html":"application/octet-stream";
                return new WebResourceResponse(mime,"UTF-8",getAssets().open(path));
            }catch(IOException e){return blocked();}
        }).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                super.onPageFinished(view,url);
                String tab=getIntent().getStringExtra("lotto_tab");
                String game=getIntent().getStringExtra("lotto_game");
                if(tab!=null || game!=null){
                    String t=tab!=null?tab:"numbers";
                    String g=game!=null?game:"";
                    String call="window.lottoOpen("+JSONObject.quote(t)+","+JSONObject.quote(g)+")";
                    view.evaluateJavascript("(function(){if(window.lottoOpen){"+call+";}else{window.addEventListener('lotto-ready',function(){"+call+";},{once:true});}})()",null);
                }
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){WebResourceResponse response=loader.shouldInterceptRequest(req.getUrl());return response!=null?response:blocked();}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){Uri u=request.getUrl();if(isLocal(u))return false;if(request.isForMainFrame()&&"https".equals(u.getScheme())){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){message("No browser is available to open this link.");}}return true;}
        });
        ServiceWorkerController.getInstance().setServiceWorkerClient(new ServiceWorkerClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebResourceRequest req){WebResourceResponse response=loader.shouldInterceptRequest(req.getUrl());return response!=null?response:blocked();}
        });
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(),"Android");
        web.loadUrl(HOME);
    }
    static boolean isLocal(Uri uri){return "https".equals(uri.getScheme())&&HOST.equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/assets/");}
    static WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",null,new ByteArrayInputStream(new byte[0]));}
    void message(String text){runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_LONG).show());}
    void callback(String text){runOnUiThread(()->{if(!isDestroyed())web.evaluateJavascript("window.onNativePdfSaved && window.onNativePdfSaved("+JSONObject.quote(text)+")",null);});}
    final class Bridge {
        @JavascriptInterface public void copyText(String text){if(text==null||text.length()>200000)return;runOnUiThread(()->{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Lotto Lab numbers",text));});}
        @JavascriptInterface public void savePdf(String name,String encoded){
            if(encoded==null||encoded.length()>4000000){callback("PDF is too large to save.");return;}
            final byte[] data;
            try{data=Base64.decode(encoded,Base64.DEFAULT);if(data.length<8||!new String(data,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))throw new IllegalArgumentException();}catch(Exception e){callback("PDF could not be created. Please try again.");return;}
            final String filename=(name==null?"LottoLab.pdf":name).replaceAll("[^A-Za-z0-9._-]","_");
            runOnUiThread(()->{if(pendingPdf!=null){callback("Complete the current save dialog first.");return;}pendingPdf=data;try{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,filename);startActivityForResult(intent,SAVE_PDF);}catch(Exception e){pendingPdf=null;callback("The Android file picker is unavailable.");}});
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_PDF)return;
        final byte[] bytes=pendingPdf;pendingPdf=null;
        if(result!=RESULT_OK||data==null||data.getData()==null){callback("PDF save cancelled.");return;}
        if(bytes==null){callback("Please export the PDF again.");return;}
        final Uri uri=data.getData();
        new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(bytes);out.flush();callback("PDF saved successfully.");}catch(Exception e){callback("Could not save the PDF. Please choose another folder.");}},"pdf-save").start();
    }
    @Override public void onBackPressed(){web.evaluateJavascript("window.lottoBack ? window.lottoBack() : false",result->{if(!"true".equals(result))super.onBackPressed();});}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("Android");web.destroy();}super.onDestroy();}
}
