package za.co.mp3dork;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(13,17,26), CARD = Color.rgb(23,31,44), BORDER = Color.rgb(42,54,70), TEXT = Color.rgb(243,246,253), MUTED = Color.rgb(158,173,193), ACCENT = Color.rgb(100,229,220);
    private static final int IMPORT = 201, STORAGE = 202, NOTIFICATIONS = 203;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private Store store;
    private DownloadManager downloads;
    private LinearLayout root, nav, mini;
    private FrameLayout body;
    private TextView miniTitle, miniTime;
    private Button miniToggle;
    private SeekBar seek;
    private EditText words, domain, exclusions, query, address;
    private Switch exact;
    private Spinner presets;
    private int tab = 0, lastPreset;
    private boolean browserVisible, seeking, busy, restoring;
    private WebView browser;
    private LinearLayout browserLayout;
    private ProgressBar browserProgress;
    private RemoteAudio.Result pendingDownload;
    private String pendingName;
    private final List<DownloadRow> downloadRows = new ArrayList<>();
    private final Runnable ticker = new Runnable() { @Override public void run() { updatePlayer(); if (tab == 1) updateDownloads(); handler.postDelayed(this, 1000); } };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this); downloads = getSystemService(DownloadManager.class);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) { android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()); int keyboard = insets.getInsets(WindowInsets.Type.ime()).bottom; view.setPadding(i.left, i.top, i.right, Math.max(i.bottom, keyboard)); }
            else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        LinearLayout brand = row(); brand.setPadding(dp(22), dp(12), dp(20), dp(12)); brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.ic_app); brand.addView(icon, new LinearLayout.LayoutParams(dp(45), dp(45)));
        LinearLayout titles = column(); titles.setPadding(dp(8),0,0,0); titles.addView(text("MP3 DORK", 19, TEXT, true)); titles.addView(text("FIND. LISTEN. KEEP.", 10, MUTED, false)); brand.addView(titles, weight());
        Button help = ghost("?"); help.setContentDescription("Help"); brand.addView(help, new LinearLayout.LayoutParams(dp(42), dp(42))); help.setOnClickListener(v -> help()); root.addView(brand);
        body = new FrameLayout(this); root.addView(body, new LinearLayout.LayoutParams(-1,0,1));
        buildPlayer();
        nav = row(); nav.setPadding(dp(10),dp(4),dp(10),dp(6)); nav.setBackgroundColor(BG); root.addView(nav);
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
        tab = state == null ? 0 : state.getInt("tab",0); selectTab(tab);
        handleShared(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); handleShared(intent); }
    private void handleShared(Intent intent) {
        if (!Intent.ACTION_SEND.equals(intent.getAction())) return;
        String value = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (value != null) { java.util.regex.Matcher match = java.util.regex.Pattern.compile("https?://[^\\s<>\"]+").matcher(value); if (match.find()) showLinkActions(match.group(), null); else toast("Share a complete MP3 link to this app."); }
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putInt("tab",tab); saveDraft(); }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(ticker); handler.post(ticker); }
    @Override protected void onPause() { saveDraft(); handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onDestroy() { handler.removeCallbacks(ticker); network.shutdownNow(); if (browser != null) browser.destroy(); super.onDestroy(); }
    @Override public void onBackPressed() { back(); }
    private void back() { if (browserVisible) { if (browser.canGoBack()) browser.goBack(); else selectTab(0); } else if (tab != 0) selectTab(0); else finish(); }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0,-2,1); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (color == CARD) d.setStroke(dp(1),BORDER); return d; }
    private TextView text(String value, float size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setFontFeatureSettings("kern"); if (bold) t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); return t; }
    private Button button(String value, boolean primary) { Button b = new Button(this); b.setText(value); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); b.setTextColor(primary ? BG : TEXT); b.setBackground(shape(primary ? ACCENT : BORDER,14)); b.setMinHeight(dp(48)); b.setMinimumWidth(0); b.setPadding(dp(14),dp(4),dp(14),dp(4)); return b; }
    private Button ghost(String value) { Button b = button(value,false); b.setBackground(shape(Color.TRANSPARENT,12)); b.setTextColor(ACCENT); return b; }
    private EditText input(String hint, boolean url) { EditText e = new EditText(this); e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setTextSize(15); e.setHint(hint); e.setSingleLine(true); e.setPadding(dp(14),dp(13),dp(14),dp(13)); e.setBackground(shape(BG,12)); if (url) e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI); return e; }
    private LinearLayout card(LinearLayout parent) { LinearLayout c = column(); c.setPadding(dp(17),dp(17),dp(17),dp(17)); c.setBackground(shape(CARD,20)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(14)); parent.addView(c,lp); return c; }
    private void gap(LinearLayout parent, int height) { parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height))); }
    private ScrollView page() { ScrollView s = new ScrollView(this); s.setFillViewport(true); s.setClipToPadding(false); body.addView(s,new FrameLayout.LayoutParams(-1,-1)); return s; }
    private LinearLayout pageColumn(ScrollView s) { LinearLayout p = column(); p.setPadding(dp(21),dp(13),dp(21),dp(24)); s.addView(p); return p; }
    private void heading(LinearLayout parent, String eyebrow, String title, String description) { TextView e = text(eyebrow,10,ACCENT,true); e.setLetterSpacing(0.15f); parent.addView(e); gap(parent,8); parent.addView(text(title,31,TEXT,true)); gap(parent,7); parent.addView(text(description,14,MUTED,false)); gap(parent,22); }
    private void label(LinearLayout parent, String title) { parent.addView(text(title,12,MUTED,true)); gap(parent,7); }
    private Spinner spinner(String[] options) { Spinner s = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(adapter); s.setMinimumHeight(dp(48)); s.setBackground(shape(BG,12)); s.setPadding(dp(8),0,dp(8),0); return s; }
    private void pair(LinearLayout parent, Button a, Button b) { LinearLayout r = row(); LinearLayout.LayoutParams x = weight(); x.setMargins(0,0,dp(8),0); r.addView(a,x); r.addView(b,weight()); parent.addView(r); }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private void error(String message) { if (!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this).setTitle("Could not open audio").setMessage(message).setPositiveButton("OK",null).show(); }
    private void copy(String value) { getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("MP3 Dork",value)); toast("Copied"); }
    private void hideKeyboard() { getSystemService(InputMethodManager.class).hideSoftInputFromWindow(root.getWindowToken(),0); root.clearFocus(); }
    private void external(String value) { try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(MediaTools.webUrl(value)))); } catch (Exception e) { toast("No browser is available for this link."); } }

    void selectTab(int index) {
        saveDraft(); tab = index; browserVisible = false; body.removeAllViews(); downloadRows.clear(); nav.removeAllViews();
        String[] names = {"Search","Downloads","Library","Settings"}; String[] symbols = {"⌕","↓","♫","⚙"};
        for (int i=0;i<4;i++) { final int choice=i; Button b=ghost(symbols[i]+"\n"+names[i]); b.setTextSize(12); b.setPadding(dp(2),dp(4),dp(2),dp(4)); b.setMaxLines(2); b.setTextColor(tab == i ? ACCENT : MUTED); if (tab == i) b.setBackground(shape(CARD,13)); nav.addView(b,new LinearLayout.LayoutParams(0,dp(59),1)); b.setContentDescription(names[i]); b.setOnClickListener(v -> selectTab(choice)); }
        if (index==0) renderSearch(); else if (index==1) renderDownloads(); else if (index==2) renderLibrary(); else renderSettings();
    }
    private void saveDraft() {
        if (words != null && query != null) store.settings().edit().putString("draft_words",words.getText().toString()).putString("draft_domain",domain.getText().toString()).putString("draft_exclusions",exclusions.getText().toString()).putString("draft_query",query.getText().toString()).putBoolean("draft_exact",exact.isChecked()).putInt("draft_preset",presets.getSelectedItemPosition()).apply();
    }
    private void renderSearch() {
        restoring=true;
        LinearLayout p=pageColumn(page()); heading(p,"MUSIC DISCOVERY","Find your next track.","Search precisely. Preview it. Make it yours.");
        LinearLayout c=card(p); label(c,"SONG, ARTIST OR KEYWORDS"); words=input("What are you looking for?",false); words.setText(store.settings().getString("draft_words","")); c.addView(words); gap(c,14);
        label(c,"SEARCH PRESET"); presets=spinner(QueryBuilder.PRESETS); lastPreset=Math.max(0,Math.min(3,store.settings().getInt("draft_preset",0))); presets.setSelection(lastPreset); c.addView(presets);
        LinearLayout filters=column(); filters.setVisibility(View.GONE); Button advanced=ghost("Show search filters +"); c.addView(advanced); advanced.setOnClickListener(v -> { boolean show=filters.getVisibility()!=View.VISIBLE; filters.setVisibility(show?View.VISIBLE:View.GONE); advanced.setText(show?"Hide search filters −":"Show search filters +"); });
        exact=new Switch(this); exact.setText("Match the exact phrase"); exact.setTextColor(TEXT); exact.setTextSize(14); exact.setPadding(0,dp(8),0,dp(8)); exact.setChecked(store.settings().getBoolean("draft_exact",false)); filters.addView(exact);
        domain=input("Website only: example.org",true); domain.setText(store.settings().getString("draft_domain","")); filters.addView(domain); gap(filters,10);
        exclusions=input("Exclude words: remix, live",false); exclusions.setText(store.settings().getString("draft_exclusions","")); filters.addView(exclusions); gap(filters,12); c.addView(filters);
        label(c,"YOUR GOOGLE QUERY · EDITABLE"); query=input("Your search query",false); query.setSingleLine(false); query.setMinLines(2); query.setTextSize(13); query.setTypeface(Typeface.MONOSPACE); String generated; try{generated=QueryBuilder.build(words.getText().toString(),presets.getSelectedItemPosition(),exact.isChecked(),domain.getText().toString(),exclusions.getText().toString());}catch(Exception ignored){generated="filetype:mp3";} query.setText(store.settings().getString("draft_query",generated)); c.addView(query); gap(c,14);
        Button search=button("Search Google  ↗",true); c.addView(search,new LinearLayout.LayoutParams(-1,dp(51))); search.setOnClickListener(v -> searchGoogle(query.getText().toString())); gap(c,9);
        Button copy=ghost("Copy query"), save=ghost("Save search"); pair(c,copy,save); copy.setOnClickListener(v -> copy(query.getText().toString())); save.setOnClickListener(v -> { String q=query.getText().toString().trim(); if(q.isEmpty()){toast("Enter a query first.");return;} store.put("saved_searches",Store.object("query",q,"time",System.currentTimeMillis()),"query",40); toast("Search saved"); selectTab(0); });
        TextWatcher watcher=new TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int count,int after){} public void onTextChanged(CharSequence s,int st,int before,int count){regenerate();} public void afterTextChanged(Editable e){} }; words.addTextChangedListener(watcher); domain.addTextChangedListener(watcher); exclusions.addTextChangedListener(watcher); exact.setOnCheckedChangeListener((b,value)->regenerate()); presets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ public void onNothingSelected(AdapterView<?> parent){} public void onItemSelected(AdapterView<?> parent,View view,int position,long id){ if(!restoring && position!=lastPreset){lastPreset=position;regenerate();} }});
        words.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH); words.setOnEditorActionListener((v,id,event)->{ if(id==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){searchGoogle(query.getText().toString());return true;}return false;});
        LinearLayout direct=card(p); direct.addView(text("Already have an MP3 link?",17,TEXT,true)); gap(direct,6); direct.addView(text("Paste a direct URL to preview or download.",13,MUTED,false)); gap(direct,12);
        EditText link=input("https://example.org/track.mp3",true); direct.addView(link); gap(direct,9); Button paste=ghost("Paste link"), use=button("Open audio",false); pair(direct,paste,use); paste.setOnClickListener(v -> { ClipboardManager cm=getSystemService(ClipboardManager.class); if(cm.hasPrimaryClip() && cm.getPrimaryClip()!=null)link.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this)); else toast("The clipboard is empty."); }); use.setOnClickListener(v->showLinkActions(link.getText().toString(),null));
        LinearLayout saved=card(p); LinearLayout sr=row(); sr.addView(text("Saved searches",17,TEXT,true),weight()); Button history=ghost("History"); sr.addView(history); history.setOnClickListener(v->showHistory()); saved.addView(sr);
        JSONArray items=store.list("saved_searches"); if(items.length()==0){gap(saved,6);saved.addView(text("Keep useful searches here for next time.",13,MUTED,false));}
        for(int i=0;i<Math.min(items.length(),5);i++){JSONObject item=items.optJSONObject(i); if(item==null)continue; String q=item.optString("query"); Button b=ghost(q); b.setTextColor(TEXT); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setTextSize(12); b.setMaxLines(2); saved.addView(b,new LinearLayout.LayoutParams(-1,-2)); b.setOnClickListener(v->searchGoogle(q)); b.setOnLongClickListener(v->{new AlertDialog.Builder(this).setItems(new String[]{"Run search","Copy query","Remove saved search"},(d,n)->{if(n==0)searchGoogle(q);else if(n==1)copy(q);else{store.remove("saved_searches","query",q);selectTab(0);}}).show();return true;});}
        if(items.length()>5){Button all=ghost("View all "+items.length()+" saved searches");saved.addView(all);all.setOnClickListener(v->showSearchList("Saved searches","saved_searches"));}
        TextView hint=text("Google results open in the browser below. Use Find MP3 links on a source page to collect its audio links.",12,MUTED,false); p.addView(hint); restoring=false;
    }
    private void regenerate(){if(restoring || query==null)return;try{query.setText(QueryBuilder.build(words.getText().toString(),presets.getSelectedItemPosition(),exact.isChecked(),domain.getText().toString(),exclusions.getText().toString()));domain.setError(null);}catch(IllegalArgumentException e){domain.setError(e.getMessage());}}
    private void searchGoogle(String raw){String q=raw.trim();if(q.isEmpty()){toast("Enter a search first.");return;}saveDraft();store.put("history",Store.object("query",q,"time",System.currentTimeMillis()),"query",40);openBrowser("https://www.google.com/search?q="+Uri.encode(q)+"&udm=14");}
    private void showHistory(){showSearchList("Recent searches","history");}
    private void showSearchList(String title,String key){JSONArray items=store.list(key);if(items.length()==0){toast("No searches yet.");return;}String[] options=new String[items.length()];for(int i=0;i<items.length();i++)options[i]=items.optJSONObject(i).optString("query");new AlertDialog.Builder(this).setTitle(title).setItems(options,(d,i)->searchGoogle(options[i])).setNegativeButton("Close",null).setNeutralButton("Clear",(d,w)->new AlertDialog.Builder(this).setTitle("Clear "+title.toLowerCase(Locale.ROOT)+"?").setPositiveButton("Clear",(a,b)->{store.save(key,new JSONArray());selectTab(0);}).setNegativeButton("Cancel",null).show()).show();}

    private void createBrowser(){
        browserLayout=column();browserLayout.setBackgroundColor(BG);
        LinearLayout bar=row();bar.setPadding(dp(6),dp(4),dp(6),dp(6));Button back=ghost("‹");back.setContentDescription("Browser back");bar.addView(back,new LinearLayout.LayoutParams(dp(40),dp(46)));back.setOnClickListener(v->{if(browser.canGoBack())browser.goBack();else selectTab(0);});
        address=input("Website or search",true);address.setTextSize(12);bar.addView(address,weight());Button go=ghost("→");go.setContentDescription("Go to address");bar.addView(go,new LinearLayout.LayoutParams(dp(40),dp(46)));go.setOnClickListener(v->loadAddress());address.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);address.setOnEditorActionListener((v,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_GO){loadAddress();return true;}return false;});
        Button close=ghost("×");close.setContentDescription("Close browser");bar.addView(close,new LinearLayout.LayoutParams(dp(40),dp(46)));close.setOnClickListener(v->selectTab(0));browserLayout.addView(bar);
        LinearLayout tools=row();tools.setPadding(dp(12),0,dp(12),dp(6));Button find=button("Find MP3 links",false),out=ghost("Open externally ↗");tools.addView(find,weight());tools.addView(out,weight());find.setOnClickListener(v->findAudioLinks());out.setOnClickListener(v->{if(browser.getUrl()!=null)external(browser.getUrl());});browserLayout.addView(tools);
        browserProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);browserProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));browserLayout.addView(browserProgress,new LinearLayout.LayoutParams(-1,dp(3)));
        browser=new WebView(this);browser.setBackgroundColor(Color.WHITE);browserLayout.addView(browser,new LinearLayout.LayoutParams(-1,0,1));
        WebSettings settings=browser.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSafeBrowsingEnabled(true);settings.setSupportZoom(true);settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);CookieManager.getInstance().setAcceptCookie(true);
        browser.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView view,int progress){browserProgress.setProgress(progress);browserProgress.setVisibility(progress>=100?View.GONE:View.VISIBLE);}});
        browser.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){if(!request.isForMainFrame())return false;String link=MediaTools.unwrapGoogle(request.getUrl().toString());try{MediaTools.webUrl(link);}catch(Exception e){toast("Open this link with your external browser.");return true;}if(MediaTools.isMp3Link(link)){showLinkActions(link,null);return true;}return false;}
            @Override public void onPageFinished(WebView view,String url){address.setText(url);}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,android.webkit.WebResourceError error){if(request.isForMainFrame())toast("Page could not load. Check your connection, or open it externally.");}
        });
        browser.setDownloadListener((url,agent,disposition,mime,length)->{if(MediaTools.isMp3Link(url) || (mime!=null && (mime.contains("audio/mpeg") || mime.contains("audio/mp3"))) || (disposition!=null && disposition.toLowerCase(Locale.ROOT).contains(".mp3")))showLinkActions(url,MediaTools.fileName(url,disposition));else toast("This app downloads MP3 audio. Open other files externally.");});
        browser.setOnLongClickListener(v->{WebView.HitTestResult hit=browser.getHitTestResult();if(hit!=null && hit.getExtra()!=null && hit.getExtra().startsWith("http")){showLinkActions(MediaTools.unwrapGoogle(hit.getExtra()),null);return true;}return false;});
    }
    private void openBrowser(String url){hideKeyboard();if(browser==null)createBrowser();body.removeAllViews();ViewGroup parent=(ViewGroup)browserLayout.getParent();if(parent!=null)parent.removeView(browserLayout);body.addView(browserLayout,new FrameLayout.LayoutParams(-1,-1));browserVisible=true;browser.loadUrl(MediaTools.webUrl(url));}
    private void loadAddress(){hideKeyboard();String value=address.getText().toString().trim();try{browser.loadUrl(MediaTools.webUrl(value));}catch(Exception e){if(!value.contains(" ") && value.contains(".")){try{browser.loadUrl(MediaTools.webUrl("https://"+value));return;}catch(Exception ignored){}}browser.loadUrl("https://www.google.com/search?q="+Uri.encode(value)+"&udm=14");}}
    private void findAudioLinks(){
        if(browser==null)return;
        browser.evaluateJavascript("JSON.stringify(Array.from(document.querySelectorAll('a[href],audio[src],source[src]')).map(e=>{try{return new URL(e.getAttribute('href')||e.getAttribute('src'),document.baseURI).href}catch(x){return ''}}).filter(Boolean).slice(0,1500))",value->{
            try{Object decoded=new JSONTokener(value).nextValue();JSONArray all=new JSONArray(String.valueOf(decoded));LinkedHashSet<String> links=new LinkedHashSet<>();for(int i=0;i<all.length();i++){String link=MediaTools.unwrapGoogle(all.optString(i));if(MediaTools.isMp3Link(link)){try{links.add(MediaTools.webUrl(link));}catch(Exception ignored){}}if(links.size()>=100)break;}
                if(links.isEmpty()){toast("No direct MP3 links found on this page. Open a result or long-press its audio link.");return;}
                String[] urls=links.toArray(new String[0]),names=new String[urls.length];for(int i=0;i<urls.length;i++)names[i]=MediaTools.fileName(urls[i],null);
                new AlertDialog.Builder(this).setTitle(urls.length+" MP3 links found").setItems(names,(d,i)->showLinkActions(urls[i],names[i])).setNegativeButton("Close",null).show();
            }catch(Exception e){toast("Could not read links on this page. Long-press a link instead.");}
        });
    }

    private String agent(){return browser==null?WebSettings.getDefaultUserAgent(this):browser.getSettings().getUserAgentString();}
    private String cookie(String url){try{return CookieManager.getInstance().getCookie(url);}catch(Exception e){return null;}}
    private void showLinkActions(String value,String name){
        String url;try{url=MediaTools.webUrl(MediaTools.unwrapGoogle(value));}catch(Exception e){toast(e.getMessage());return;}
        String title=name==null?MediaTools.fileName(url,null):name;
        new AlertDialog.Builder(this).setTitle(title).setItems(new String[]{"Preview audio","Download MP3","Save link","Open source page","Copy link"},(d,i)->{if(i==0)probe(url,false);else if(i==1)probe(url,true);else if(i==2){store.put("links",Store.object("url",url,"name",title,"time",System.currentTimeMillis()),"url",200);toast("Link saved in Library");}else if(i==3)openBrowser(url);else copy(url);}).setNegativeButton("Close",null).show();
    }
    private void probe(String url,boolean download){
        if(busy){toast("Checking an audio link. Please wait.");return;}busy=true;
        String cookies=cookie(url),ua=agent(),referer=browser!=null?browser.getUrl():null;
        AlertDialog checking=new AlertDialog.Builder(this).setTitle("Checking MP3…").setMessage("Verifying that the link returns audio.").setCancelable(false).create();checking.show();
        network.execute(()->{try{RemoteAudio.Result result=RemoteAudio.probe(url,cookies,ua,referer);runOnUiThread(()->{busy=false;if(isFinishing()||isDestroyed())return;checking.dismiss();if(download)confirmDownload(result);else play(result.url,result.name);});}catch(Exception e){runOnUiThread(()->{busy=false;if(isFinishing()||isDestroyed())return;checking.dismiss();error(e.getMessage()==null?"Check your connection and try again.":e.getMessage());});}});
    }
    private void confirmDownload(RemoteAudio.Result result){
        JSONArray records=store.list("downloads");for(int i=0;i<records.length();i++){JSONObject record=records.optJSONObject(i);if(record==null || !record.optString("url").equals(result.url))continue;DownloadInfo info=downloadInfo(record.optLong("id"));if(info.status!=DownloadManager.STATUS_FAILED && info.status!=0){new AlertDialog.Builder(this).setTitle("Already in your downloads").setMessage("This link is already queued or downloaded.").setPositiveButton("View downloads",(d,w)->selectTab(1)).setNegativeButton("Close",null).show();return;}}
        LinearLayout panel=column();panel.setPadding(dp(24),dp(8),dp(24),dp(8));panel.addView(text(MediaTools.size(result.size)+" · MP3 audio",13,MUTED,false));gap(panel,12);EditText filename=input("File name",false);filename.setText(result.name);panel.addView(filename);gap(panel,8);panel.addView(text("Save to "+destination()+"/MP3Dork",12,MUTED,false));
        new AlertDialog.Builder(this).setTitle("Download audio").setView(panel).setPositiveButton("Download",(d,w)->{String name=MediaTools.cleanFileName(filename.getText().toString());if(Build.VERSION.SDK_INT<29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){pendingDownload=result;pendingName=name;requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE);}else enqueueDownload(result,name);}).setNegativeButton("Cancel",null).show();
    }
    String destination(){return store.settings().getBoolean("music_folder",false)?Environment.DIRECTORY_MUSIC:Environment.DIRECTORY_DOWNLOADS;}
    long enqueueDownload(RemoteAudio.Result result,String name){
        try{
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(result.url));request.setTitle(name).setDescription("MP3 Dork · audio download").setMimeType("audio/mpeg").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            String filename=System.currentTimeMillis()+"_"+MediaTools.cleanFileName(name);request.setDestinationInExternalPublicDir(destination(),"MP3Dork/"+filename);request.setAllowedOverMetered(!store.settings().getBoolean("wifi_only",false));request.setAllowedOverRoaming(false);if(store.settings().getBoolean("wifi_only",false))request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            String cookies=cookie(result.url);if(cookies!=null && !cookies.isEmpty())request.addRequestHeader("Cookie",cookies);request.addRequestHeader("User-Agent",agent());if(browser!=null && browser.getUrl()!=null && browser.getUrl().startsWith("http"))request.addRequestHeader("Referer",browser.getUrl());
            long id=downloads.enqueue(request);store.put("downloads",Store.object("id",id,"url",result.url,"name",name,"file",filename,"size",result.size,"time",System.currentTimeMillis()),"id",500);toast("Download queued");selectTab(1);notificationPermission();return id;
        }catch(Exception e){error("Could not queue this download: "+e.getMessage());return -1;}
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==STORAGE){if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED && pendingDownload!=null)enqueueDownload(pendingDownload,pendingName);else toast("Storage permission is needed on this Android version to save audio.");pendingDownload=null;}}
    private void notificationPermission(){if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && !store.settings().getBoolean("asked_notifications",false)){store.settings().edit().putBoolean("asked_notifications",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS);}}
    private void play(String url,String name){try{PlaybackService.play(this,url,name,cookie(url),agent());mini.setVisibility(View.VISIBLE);miniTitle.setText(name);miniTime.setText("Loading audio…");notificationPermission();}catch(Exception e){error("Could not start the audio player.");}}

    private static final class DownloadInfo {int status,reason;long downloaded,total=-1;Uri uri;}
    private DownloadInfo downloadInfo(long id){DownloadInfo info=new DownloadInfo();try(Cursor cursor=downloads.query(new DownloadManager.Query().setFilterById(id))){if(cursor!=null && cursor.moveToFirst()){info.status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));info.reason=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));info.downloaded=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));info.total=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));if(info.status==DownloadManager.STATUS_SUCCESSFUL)info.uri=downloads.getUriForDownloadedFile(id);}}catch(Exception ignored){}return info;}
    private static final class DownloadRow {long id;TextView status;ProgressBar progress;Button action;JSONObject record;int lastStatus;}
    private void renderDownloads(){
        LinearLayout p=pageColumn(page());heading(p,"DOWNLOAD MANAGER","Bring it offline.","Downloads continue while you use other apps.");JSONArray records=store.list("downloads");if(records.length()==0){empty(p,"Your queue is clear.","Find an MP3, choose Download, and it will appear here.","Find music",()->selectTab(0));return;}
        for(int i=0;i<records.length();i++){JSONObject record=records.optJSONObject(i);if(record==null)continue;LinearLayout c=card(p);TextView title=text(record.optString("label",record.optString("name")),16,TEXT,true);c.addView(title);gap(c,6);c.addView(text(host(record.optString("url")),12,MUTED,false));gap(c,10);
            DownloadRow row=new DownloadRow();row.id=record.optLong("id");row.record=record;row.progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);row.progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));c.addView(row.progress,new LinearLayout.LayoutParams(-1,dp(5)));gap(c,8);row.status=text("Checking download…",12,MUTED,false);c.addView(row.status);gap(c,8);row.action=button("View",false);Button more=ghost("Options");pair(c,row.action,more);row.action.setOnClickListener(v->{DownloadInfo info=downloadInfo(row.id);if(info.status==DownloadManager.STATUS_SUCCESSFUL && info.uri!=null)play(info.uri.toString(),record.optString("label",record.optString("name")));else if(info.status==DownloadManager.STATUS_FAILED || info.status==0)retry(record);else toast("Android is managing this download. It will retry when the source and connection are available.");});more.setOnClickListener(v->downloadOptions(record));downloadRows.add(row);
        }updateDownloads();
    }
    private void updateDownloads(){for(DownloadRow r:downloadRows){DownloadInfo info=downloadInfo(r.id);int progress=info.total>0?(int)Math.min(100,info.downloaded*100/info.total):0;r.progress.setIndeterminate(info.total<=0 && (info.status==DownloadManager.STATUS_RUNNING || info.status==DownloadManager.STATUS_PENDING));r.progress.setProgress(progress);String status;
        if(info.status==DownloadManager.STATUS_SUCCESSFUL){status="Downloaded · "+MediaTools.size(info.total);r.action.setText("Play offline");r.progress.setProgress(100);}
        else if(info.status==DownloadManager.STATUS_RUNNING){status=MediaTools.size(info.downloaded)+" / "+MediaTools.size(info.total)+(info.total>0?" · "+progress+"%":"");r.action.setText("Downloading…");}
        else if(info.status==DownloadManager.STATUS_PENDING){status="Queued · waiting to start";r.action.setText("Queued");}
        else if(info.status==DownloadManager.STATUS_PAUSED){status=info.reason==DownloadManager.PAUSED_QUEUED_FOR_WIFI?"Waiting for Wi-Fi":info.reason==DownloadManager.PAUSED_WAITING_FOR_NETWORK?"Waiting for a connection":"Paused by Android · will retry";r.action.setText("Waiting");}
        else{status=info.status==0?"Removed from the phone":"Failed · "+downloadReason(info.reason);r.action.setText("Retry");}
        r.status.setText(status);r.lastStatus=info.status;
    }}
    private String downloadReason(int code){if(code>=400 && code<=599)return "HTTP "+code;if(code==DownloadManager.ERROR_INSUFFICIENT_SPACE)return "not enough storage";if(code==DownloadManager.ERROR_DEVICE_NOT_FOUND)return "storage unavailable";if(code==DownloadManager.ERROR_FILE_ALREADY_EXISTS)return "file exists";if(code==DownloadManager.ERROR_CANNOT_RESUME)return "source could not resume";return "source or connection error ("+code+")";}
    private void retry(JSONObject record){downloads.remove(record.optLong("id"));store.remove("downloads","id",record.optString("id"));probe(record.optString("url"),true);}
    private void downloadOptions(JSONObject record){DownloadInfo info=downloadInfo(record.optLong("id"));boolean complete=info.status==DownloadManager.STATUS_SUCCESSFUL;String[] options=complete?new String[]{"Play offline","Share audio","Rename in library","Copy source link","Delete audio"}:new String[]{"Copy source link","Open source page","Cancel and remove"};new AlertDialog.Builder(this).setTitle(record.optString("name")).setItems(options,(d,i)->{if(complete){if(i==0 && info.uri!=null)play(info.uri.toString(),record.optString("label",record.optString("name")));else if(i==1 && info.uri!=null)share(info.uri,record.optString("name"));else if(i==2)rename("downloads",record,"id");else if(i==3)copy(record.optString("url"));else if(i==4)removeDownload(record);}else{if(i==0)copy(record.optString("url"));else if(i==1)openBrowser(record.optString("url"));else removeDownload(record);}}).show();}
    private void removeDownload(JSONObject record){new AlertDialog.Builder(this).setTitle("Remove this download?").setMessage("The queued download or downloaded file will be deleted.").setPositiveButton("Remove",(d,w)->{downloads.remove(record.optLong("id"));store.remove("downloads","id",record.optString("id"));selectTab(tab);}).setNegativeButton("Cancel",null).show();}

    private void renderLibrary(){
        LinearLayout p=pageColumn(page());heading(p,"YOUR COLLECTION","Good music. Kept.","Downloaded audio and links saved on this phone.");Spinner filter=spinner(new String[]{"Downloaded audio","Saved links","Imported MP3s"});p.addView(filter);gap(p,12);Button importButton=ghost("+ Import an MP3 from your phone");p.addView(importButton);importButton.setOnClickListener(v->importAudio());gap(p,12);LinearLayout list=column();p.addView(list);
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int index,long id){renderCollection(list,index);}});renderCollection(list,0);
    }
    private void renderCollection(LinearLayout p,int category){p.removeAllViews();String key=category==0?"downloads":category==1?"links":"imports";JSONArray items=store.list(key);int visible=0;for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null)continue;Uri uri=null;if(category==0){DownloadInfo info=downloadInfo(item.optLong("id"));if(info.status!=DownloadManager.STATUS_SUCCESSFUL || info.uri==null)continue;uri=info.uri;}else if(category==2)uri=Uri.parse(item.optString("url"));visible++;final Uri audio=uri;
        LinearLayout c=card(p);String name=item.optString("label",item.optString("name"));c.addView(text(name,16,TEXT,true));gap(c,6);c.addView(text(category==1?host(item.optString("url")):"Available offline",12,MUTED,false));gap(c,12);Button play=button(category==1?"Preview":"Play",false),more=ghost("Options");pair(c,play,more);play.setOnClickListener(v->{if(category==1)probe(item.optString("url"),false);else play(audio.toString(),name);});more.setOnClickListener(v->{if(category==0)downloadOptions(item);else if(category==1)new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Download MP3","Open source","Copy link","Rename saved link","Remove link"},(d,n)->{if(n==0)probe(item.optString("url"),true);else if(n==1)openBrowser(item.optString("url"));else if(n==2)copy(item.optString("url"));else if(n==3)rename("links",item,"url");else{store.remove("links","url",item.optString("url"));renderCollection(p,category);}}).show();else new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Share audio","Rename in library","Remove from library"},(d,n)->{if(n==0)share(audio,name);else if(n==1)rename("imports",item,"url");else{store.remove("imports","url",item.optString("url"));renderCollection(p,category);}}).show();});
    }if(visible==0)empty(p,category==1?"Save a little inspiration.":"Your collection starts here.",category==1?"Choose Save link when you find a track.":"Download a track or import an MP3 to listen offline.","Find music",()->selectTab(0));}
    private void importAudio(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(intent,IMPORT);}catch(Exception e){toast("No file picker is available.");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==IMPORT && result==RESULT_OK && data!=null && data.getData()!=null){Uri uri=data.getData();String name="Imported audio.mp3";try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null && cursor.moveToFirst())name=cursor.getString(0);}catch(Exception ignored){}String mime=getContentResolver().getType(uri);if(!name.toLowerCase(Locale.ROOT).endsWith(".mp3") && !"audio/mpeg".equals(mime) && !"audio/mp3".equals(mime)){toast("Choose an MP3 audio file.");return;}try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception e){toast("This file provider may require you to import again after restarting.");}store.put("imports",Store.object("url",uri.toString(),"name",name,"time",System.currentTimeMillis()),"url",500);selectTab(2);toast("MP3 added to Library");}}
    private void rename(String key,JSONObject item,String identity){EditText name=input("Track name",false);name.setText(item.optString("label",item.optString("name")));LinearLayout panel=column();panel.setPadding(dp(22),dp(12),dp(22),dp(12));panel.addView(name);new AlertDialog.Builder(this).setTitle("Rename in library").setView(panel).setPositiveButton("Save",(d,w)->{String title=name.getText().toString().trim();if(title.isEmpty()){toast("Name cannot be empty.");return;}try{item.put("label",title);}catch(Exception ignored){}store.put(key,item,identity,500);selectTab(tab);}).setNegativeButton("Cancel",null).show();}
    private void share(Uri uri,String name){try{Intent intent=new Intent(Intent.ACTION_SEND).setType("audio/mpeg").putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_SUBJECT,name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newUri(getContentResolver(),name,uri));startActivity(Intent.createChooser(intent,"Share MP3"));}catch(Exception e){toast("Could not share this file. Check that it still exists.");}}
    private String host(String url){try{return Uri.parse(url).getHost();}catch(Exception e){return "Audio source";}}
    private void empty(LinearLayout p,String title,String message,String action,Runnable callback){LinearLayout c=card(p);c.addView(text("♫",42,ACCENT,false));gap(c,12);c.addView(text(title,20,TEXT,true));gap(c,8);c.addView(text(message,14,MUTED,false));gap(c,20);Button b=button(action,true);c.addView(b);b.setOnClickListener(v->callback.run());}

    private void renderSettings(){
        LinearLayout p=pageColumn(page());heading(p,"MAKE IT YOURS","Your app. Your way.","Simple controls for downloads and local data.");LinearLayout c=card(p);c.addView(text("Downloads",18,TEXT,true));gap(c,12);Switch wifi=new Switch(this);wifi.setText("Download on Wi-Fi only");wifi.setTextSize(14);wifi.setTextColor(TEXT);wifi.setChecked(store.settings().getBoolean("wifi_only",false));wifi.setPadding(0,dp(8),0,dp(8));c.addView(wifi);wifi.setOnCheckedChangeListener((b,on)->store.settings().edit().putBoolean("wifi_only",on).apply());gap(c,10);label(c,"SAVE NEW DOWNLOADS IN");Spinner folder=spinner(new String[]{"Downloads / MP3Dork","Music / MP3Dork"});folder.setSelection(store.settings().getBoolean("music_folder",false)?1:0);c.addView(folder);folder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int index,long id){store.settings().edit().putBoolean("music_folder",index==1).apply();}});gap(c,10);c.addView(text("These settings apply to new downloads. Android manages retries and supported resumptions.",12,MUTED,false));
        LinearLayout local=card(p);local.addView(text("Saved on this phone",18,TEXT,true));gap(local,10);local.addView(text("Searches, saved links and download records are stored locally. No account is needed.",13,MUTED,false));gap(local,8);Button clear=ghost("Clear search history");local.addView(clear);clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear search history?").setPositiveButton("Clear",(d,w)->{store.save("history",new JSONArray());toast("History cleared");}).setNegativeButton("Cancel",null).show());Button cache=ghost("Clear browser cookies and cache");local.addView(cache);cache.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear browser data?").setMessage("Website sign-ins and browser cookies will be removed.").setPositiveButton("Clear",(d,w)->{CookieManager.getInstance().removeAllCookies(ok->toast("Browser data cleared"));CookieManager.getInstance().flush();android.webkit.WebStorage.getInstance().deleteAllData();if(browser!=null){browser.clearCache(true);browser.clearHistory();}}).setNegativeButton("Cancel",null).show());
        LinearLayout about=card(p);about.addView(text("MP3 Dork  1.0",18,TEXT,true));gap(about,8);about.addView(text("Google search tools, source browsing, background downloads and an offline audio player.",13,MUTED,false));gap(about,12);Button help=button("How to use the app",false);about.addView(help);help.setOnClickListener(v->help());
    }
    private void help(){new AlertDialog.Builder(this).setTitle("Find. Listen. Keep.").setMessage("1. Enter a song, artist or keywords and select a search preset.\n\n2. Search Google, open a source page, then tap an MP3 link. Find MP3 links lists direct links on the current page; long-press a link for more options.\n\n3. Preview, save the link or download the audio. The app verifies MP3 content before downloading.\n\n4. Listen from Library, even offline. Downloads are saved in your selected folder.\n\nGoogle may show a consent page or CAPTCHA. Complete it normally, or use Open externally. For results opened externally, share or paste the MP3 link back into this app.\n\nSome sources block downloads, require sign-in or use expiring links. The app uses the download links provided by the source.").setPositiveButton("Got it",null).show();}

    private void buildPlayer(){
        mini=column();mini.setPadding(dp(18),dp(10),dp(18),dp(6));mini.setBackground(shape(CARD,0));mini.setVisibility(View.GONE);root.addView(mini);
        LinearLayout row=row();LinearLayout words=column();miniTitle=text("Audio preview",14,TEXT,true);miniTitle.setMaxLines(1);miniTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(miniTitle);miniTime=text("0:00 / 0:00",10,MUTED,false);words.addView(miniTime);row.addView(words,weight());miniToggle=ghost("▶");miniToggle.setContentDescription("Play or pause audio");row.addView(miniToggle,new LinearLayout.LayoutParams(dp(46),dp(40)));miniToggle.setOnClickListener(v->{PlaybackService service=PlaybackService.instance;if(service!=null && !service.isLoading())PlaybackService.toggle(this);});Button stop=ghost("×");stop.setContentDescription("Stop audio");row.addView(stop,new LinearLayout.LayoutParams(dp(36),dp(40)));stop.setOnClickListener(v->{if(PlaybackService.instance!=null)PlaybackService.stop(this);mini.setVisibility(View.GONE);});mini.addView(row);
        seek=new SeekBar(this);seek.setPadding(0,0,0,0);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT));mini.addView(seek,new LinearLayout.LayoutParams(-1,dp(24)));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar bar){seeking=true;}public void onStopTrackingTouch(SeekBar bar){seeking=false;PlaybackService s=PlaybackService.instance;if(s!=null)s.seekTo(bar.getProgress());}public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){PlaybackService s=PlaybackService.instance;if(s!=null)miniTime.setText(time(value)+" / "+time(s.duration()));}}});
    }
    private String time(int value){int seconds=Math.max(0,value/1000);return String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    private void updatePlayer(){PlaybackService service=PlaybackService.instance;if(service==null){mini.setVisibility(View.GONE);return;}mini.setVisibility(View.VISIBLE);miniTitle.setText(service.title());miniToggle.setText(service.isPlaying()?"Ⅱ":"▶");miniToggle.setEnabled(!service.isLoading());miniTime.setText(service.isLoading()?"Loading audio…":time(service.position())+" / "+time(service.duration()));if(!seeking){seek.setMax(Math.max(1,service.duration()));seek.setProgress(service.position());}seek.setEnabled(service.duration()>0);}
    Store storeForTest(){return store;}
}
