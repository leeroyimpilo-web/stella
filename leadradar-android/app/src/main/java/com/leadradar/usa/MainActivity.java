package com.leadradar.usa;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
public class MainActivity extends Activity {
 private static final String HOME="https://appassets.androidplatform.net/assets/index.html";
 private WebView web;
 @Override public void onCreate(Bundle savedInstanceState){
  super.onCreate(savedInstanceState);
  getWindow().setStatusBarColor(Color.rgb(9,28,53));
  getWindow().setNavigationBarColor(Color.rgb(9,28,53));
  FrameLayout root=new FrameLayout(this);
  if(Build.VERSION.SDK_INT>=30){ root.setOnApplyWindowInsetsListener((view,insets)->{
   android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
   view.setPadding(0,bars.top,0,bars.bottom);return insets; }); }
  web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);
  web.setBackgroundColor(Color.rgb(9,28,53));
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
  s.setAllowFileAccess(false);s.setAllowContentAccess(false);
  s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);
  s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  if(Build.VERSION.SDK_INT>=26)s.setSafeBrowsingEnabled(true);
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){
    Uri u=req.getUrl();
    if("https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&"/assets/index.html".equals(u.getPath()))
      try{return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));}catch(Exception ignored){}
    return null;
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri u=request.getUrl();
    if("appassets.androidplatform.net".equals(u.getHost()))return !HOME.equals(u.toString());
    if("https".equals(u.getScheme())&&request.isForMainFrame())
      try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
    return true;
   }
  });
  web.loadUrl(HOME);
 }
 @Override protected void onDestroy(){if(web!=null){web.destroy();web=null;}super.onDestroy();}
}
