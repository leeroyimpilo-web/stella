package za.co.lottolab.studio;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import za.co.lottolab.studio.ConsoleArt.Icon;

import static za.co.lottolab.studio.ConsoleArt.*;

public final class MainActivity extends Activity {
    private ArchiveStore store;
    private LinearLayout root, header, navigation, content, resultArea;
    private FrameLayout body;
    private Game game=Game.LOTTO;
    private String tab="home", historyDate=null, historyQuery="", report=null, pendingExport=null;
    private int window=100, tool=0, visibleHistory=50;
    private java.util.List<Entry> draft=new ArrayList<>();
    private java.util.List<Draw> analysedDraws=new ArrayList<>();
    private java.util.List<AnalysisEngine.Result> results=new ArrayList<>();
    private final SecureRandom random=new SecureRandom();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static final int IMPORT=41,EXPORT_CSV=42,EXPORT_PDF=43;
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd MMM yyyy",Locale.ENGLISH);

    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=new ArchiveStore(this);
        try{game=Game.valueOf(store.preferences().getString("game","LOTTO"));}catch(Exception ignored){}
        if(state!=null){tab=state.getString("tab","home");historyDate=state.getString("historyDate");historyQuery=state.getString("query","");}
        draft=store.draft(game);
        getWindow().setNavigationBarColor(BG);getWindow().setStatusBarColor(BG);
        if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(new Grid(this));
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(x.left,x.top,x.right,x.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        header=vertical();root.addView(header,new LinearLayout.LayoutParams(-1,-2));
        body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        navigation=horizontal();root.addView(navigation,new LinearLayout.LayoutParams(-1,d(68)));
        setContentView(root);refresh();root.requestApplyInsets();
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("tab",tab);b.putString("historyDate",historyDate);b.putString("query",historyQuery);}
    @Override public void onBackPressed(){if(!tab.equals("home")){tab="home";refresh();}else super.onBackPressed();}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
    private int d(float x){return ConsoleArt.dp(this,x);}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,float size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);t.setLineSpacing(d(2),1);return t;}
    private void add(LinearLayout parent,View view){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=d(10);parent.addView(view,lp);}
    private void copy(LinearLayout parent,String s){add(parent,text(s,13,MUTED,false));}
    private void title(LinearLayout parent,String s){TextView v=text(s,13,GREEN,true);v.setLetterSpacing(.08f);v.setPadding(d(2),d(6),0,d(3));add(parent,v);}
    private LinearLayout panel(){LinearLayout p=vertical();p.setBackground(new Panel(this,false,false));p.setPadding(d(14),d(14),d(14),d(14));return p;}
    private TextView action(String label,boolean primary,Runnable onClick){
        TextView v=text(label,primary?15:12,primary?INK:TEXT,true);v.setGravity(Gravity.CENTER);v.setMinHeight(d(primary?52:46));v.setPadding(d(10),d(8),d(10),d(8));v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),new Panel(this,primary,false),null));v.setClickable(true);v.setFocusable(true);v.setOnClickListener(x->onClick.run());
        v.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View h,AccessibilityNodeInfo n){super.onInitializeAccessibilityNodeInfo(h,n);n.setClassName(Button.class.getName());}});return v;
    }
    private void two(LinearLayout parent,View a,View b){LinearLayout row=horizontal();LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(0,-2,1);x.setMargins(0,0,d(4),0);row.addView(a,x);LinearLayout.LayoutParams y=new LinearLayout.LayoutParams(0,-2,1);y.setMargins(d(4),0,0,0);row.addView(b,y);add(parent,row);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void alert(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Done",null).show();}
    private void invalidateReport(){report=null;results.clear();}
    private void switchGame(Game g){if(game!=g){game=g;draft=store.draft(g);store.preferences().edit().putString("game",g.name()).apply();historyQuery="";historyDate=null;invalidateReport();}refresh();}
    private String source(){return store.imported(game)?"IMPORTED DATA · OFFLINE":"SAMPLE DATA · FICTIONAL DRAWS";}

    private void refresh(){
        header.removeAllViews();LinearLayout brand=horizontal();brand.setPadding(d(10),d(5),d(10),0);
        FrameLayout menu=new FrameLayout(this);Icon menuIcon=new Icon(this,"menu",TEXT);FrameLayout.LayoutParams mil=new FrameLayout.LayoutParams(d(22),d(22),Gravity.CENTER);menu.addView(menuIcon,mil);menu.setContentDescription("Data, import and app information");menu.setOnClickListener(v->dataMenu());brand.addView(menu,new LinearLayout.LayoutParams(d(44),d(48)));
        TextView logo=text(tab.equals("home")?"LottoLab":tab.equals("history")?"DRAW HISTORY":tab.equals("lab")?"COMBINATION LAB":tab.equals("vault")?"MY NUMBERS":"DRAW PLANNER",tab.equals("home")?31:20,TEXT,true);
        if(tab.equals("home")){SpannableString s=new SpannableString("LottoLab");s.setSpan(new ForegroundColorSpan(GREEN),5,8,0);logo.setText(s);logo.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));}
        brand.addView(logo,new LinearLayout.LayoutParams(0,-2,1));Icon flask=new Icon(this,"lab",GREEN);brand.addView(flask,new LinearLayout.LayoutParams(d(29),d(29)));header.addView(brand);
        TextView status=text(source(),10,store.imported(game)?MUTED:GREEN,true);status.setLetterSpacing(.12f);status.setGravity(Gravity.CENTER);status.setPadding(d(10),0,d(10),d(10));status.setContentDescription(source()+". Design preview version 0.1.");header.addView(status);
        body.removeAllViews();ScrollView scroll=new ScrollView(this);scroll.setTag("main_scroll");scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setScrollbarFadingEnabled(true);content=vertical();content.setPadding(d(12),d(4),d(12),d(18));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));body.addView(scroll);
        renderNavigation();
        switch(tab){case "history":history();break;case "lab":lab();break;case "vault":vault();break;case "planner":planner();break;default:home();}
    }
    private void renderNavigation(){
        navigation.removeAllViews();navigation.setBackgroundColor(0xF5061B28);navigation.setPadding(d(4),d(5),d(4),d(3));
        String[] names={"Home","History","Lab","My Numbers","Planner"},keys={"home","history","lab","vault","planner"};
        for(int i=0;i<keys.length;i++){final String key=keys[i];boolean active=tab.equals(key);LinearLayout item=vertical();item.setGravity(Gravity.CENTER);item.setTag("tab_"+key);item.setContentDescription(names[i]);item.setClickable(true);item.setFocusable(true);Icon icon=new Icon(this,key,active?ORANGE:TEXT);item.addView(icon,new LinearLayout.LayoutParams(d(23),d(23)));TextView label=text(names[i],10.5f,active?ORANGE:MUTED,active);label.setGravity(Gravity.CENTER);label.setPadding(0,d(4),0,d(3));item.addView(label);View underline=new View(this);underline.setBackgroundColor(active?ORANGE:Color.TRANSPARENT);item.addView(underline,new LinearLayout.LayoutParams(d(28),d(2)));item.setOnClickListener(v->{tab=key;refresh();});navigation.addView(item,new LinearLayout.LayoutParams(0,-1,1));}
    }
    private void gameChooser(){String[] labels=new String[Game.values().length];for(int i=0;i<labels.length;i++)labels[i]=Game.values()[i].title;new AlertDialog.Builder(this).setTitle("Choose a draw game").setSingleChoiceItems(labels,game.ordinal(),(dialog,which)->{dialog.dismiss();switchGame(Game.values()[which]);}).setNegativeButton("Cancel",null).show();}
    private void gameRow(){add(content,action(game.title+"  ·  "+(game.hasPowerBall()?"5/50 + PB 1/16":game.picks+"/"+game.pool)+"  ▾",false,this::gameChooser));}
    private void home(){
        LinearLayout games=horizontal();Game[] family={Game.LOTTO,Game.POWERBALL,Game.DAILY};
        for(Game g:family){boolean active=g.hasBonus()?game.hasBonus():g==Game.DAILY?game==g:game.hasPowerBall();TextView b=action(g.shortName,active,()->switchGame(g));b.setTextSize(12);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(45),1);p.setMargins(d(2),0,d(2),0);games.addView(b,p);}add(content,games);
        TextView addon=text("ALL SIX GAMES  /  "+game.title+"  ▾",10.5f,MUTED,true);addon.setMinHeight(d(36));addon.setGravity(Gravity.CENTER);addon.setOnClickListener(v->gameChooser());add(content,addon);
        java.util.List<Draw> draws=store.draws(game,0);Draw latest=draws.get(0);LinearLayout latestPanel=panel();
        two(latestPanel,text(store.imported(game)?"LATEST IMPORTED":"LATEST DRAW",12,GREEN,true),text(store.imported(game)?"USER ARCHIVE":"●  DEMO",10,ORANGE,true));
        TextView name=text(game.title,25,TEXT,true);name.setGravity(Gravity.CENTER);add(latestPanel,name);add(latestPanel,balls(latest.numbers,latest.powerBall,latest.bonus,null));
        TextView stamp=text(DATE.format(latest.date)+"  ·  "+(store.imported(game)?"Unverified import":"Fictional sample result"),11,MUTED,false);stamp.setGravity(Gravity.CENTER);add(latestPanel,stamp);latestPanel.setOnClickListener(v->drawDetail(latest,null));add(content,latestPanel);
        TextView open=action("OPEN LOTTO LAB  ›",true,()->{tab="lab";refresh();});open.setTag("open_lab");add(content,open);
        for(Game other:new Game[]{Game.LOTTO,Game.POWERBALL,Game.DAILY})if(!(other.hasBonus()&&game.hasBonus())&&!(other.hasPowerBall()&&game.hasPowerBall())&&other!=game){LinearLayout card=panel();LinearLayout r=horizontal();TextView dot=text("●",28,other==Game.POWERBALL?Color.CYAN:ORANGE,true);r.addView(dot,new LinearLayout.LayoutParams(d(42),-2));LinearLayout detail=vertical();detail.addView(text(other.shortName,16,TEXT,true));detail.addView(text("View "+(store.imported(other)?"imported":"sample")+" results  ›",12,MUTED,false));r.addView(detail);card.addView(r);card.setOnClickListener(v->{game=other;draft=store.draft(game);invalidateReport();tab="history";refresh();});add(content,card);}
        int n=store.saved().size();add(content,action("MY NUMBERS  ·  "+n+" saved "+(n==1?"line":"lines")+"  ›",false,()->{tab="vault";refresh();}));
        copy(content,"Your numbers. Your laboratory.\nDesign build 0.1 · 18+ research tool.");
    }
    private View balls(int[] numbers,int pb,int bonus,int[] highlight){
        LinearLayout row=horizontal();int extra=pb>0?pb:bonus;int total=numbers.length+(extra>0?1:0);float screen=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;int size=d(Math.min(43,(screen-64-(total-1)*4)/total));
        for(int i=0;i<total;i++){boolean isExtra=i==numbers.length;int num=isExtra?extra:numbers[i];boolean selected=highlight==null||Arrays.stream(highlight).anyMatch(x->x==num);TextView token=text((isExtra?(pb>0?"PB":"B")+"\n":"")+String.format(Locale.ROOT,"%02d",num),isExtra?11:18,INK,true);token.setGravity(Gravity.CENTER);token.setLineSpacing(0,1);token.setMaxLines(isExtra?2:1);token.setAutoSizeTextTypeUniformWithConfiguration(10,isExtra?13:18,1,android.util.TypedValue.COMPLEX_UNIT_SP);GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{isExtra?0xFFFFC35C:selected?0xFFC7F99C:0xFFF5F5E7,isExtra?ORANGE:selected?0xFF7FDB63:0xFFCCD8D1});bg.setShape(GradientDrawable.OVAL);bg.setStroke(d(2),INK);token.setBackground(bg);token.setContentDescription((isExtra?(pb>0?"PowerBall ":"Bonus "):"Number ")+num);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,size,1);if(i>0)p.leftMargin=d(4);row.addView(token,p);}return row;
    }
    private void history(){
        TextView g=action(game.title+"  ▾",false,this::gameChooser);
        TextView dates=action(historyDate==null?"Filter date  ▾":historyDate+"  ×",false,()->{if(historyDate!=null){historyDate=null;refresh();return;}LocalDate now=LocalDate.now();new DatePickerDialog(this,(v,y,m,day)->{historyDate=LocalDate.of(y,m+1,day).toString();refresh();},now.getYear(),now.getMonthValue()-1,now.getDayOfMonth()).show();});two(content,g,dates);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(15);search.setHint("Search main numbers, e.g. 4 12");search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);search.setBackground(new Panel(this,false,false));search.setPadding(d(14),d(12),d(14),d(12));search.setText(historyQuery);add(content,search);
        LinearLayout list=vertical();content.addView(list);Runnable drawList=()->renderHistoryList(list);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){historyQuery=s.toString();visibleHistory=50;drawList.run();}public void afterTextChanged(Editable e){}});drawList.run();
        add(content,action("IMPORT DRAW CSV",false,this::startImport));
    }
    private void renderHistoryList(LinearLayout list){
        list.removeAllViews();int[] query;
        try{query=Entry.parseNumbers(historyQuery);}catch(IllegalArgumentException ex){copy(list,ex.getMessage());return;}
        java.util.List<Draw> matches=new ArrayList<>();for(Draw draw:store.draws(game,0)){if(historyDate!=null&&!draw.date.toString().equals(historyDate))continue;if(AnalysisEngine.matches(query,draw.numbers)==query.length)matches.add(draw);}
        copy(list,matches.size()+" "+(store.imported(game)?"imported":"fictional sample")+" draws · main-ball search");
        for(int i=0;i<Math.min(visibleHistory,matches.size());i++){Draw draw=matches.get(i);LinearLayout card=panel();TextView date=text(DATE.format(draw.date).toUpperCase(Locale.ENGLISH)+"  ›",14,TEXT,true);add(card,date);card.addView(balls(draw.numbers,draw.powerBall,draw.bonus,query.length==0?null:query));card.setOnClickListener(v->drawDetail(draw,null));add(list,card);}
        if(matches.isEmpty())copy(list,"No matching records in this archive window.");
        if(matches.size()>visibleHistory)add(list,action("LOAD MORE DRAWS",false,()->{visibleHistory+=50;renderHistoryList(list);}));
    }
    private void drawDetail(Draw draw,Entry entry){String msg=draw.game.title+"\n"+DATE.format(draw.date)+"\n\nMain numbers\n"+new Entry(draw.game,draw.numbers,draw.powerBall,false,"").display();if(draw.bonus>0)msg+="\nBonus: "+draw.bonus;if(entry!=null)msg+="\n\nMain matches: "+AnalysisEngine.matches(entry.numbers,draw.numbers)+(entry.powerBall>0?"\nPowerBall match: "+(entry.powerBall==draw.powerBall?"Yes":"No"):"");msg+="\n\n"+(store.imported(draw.game)?"User-imported record; verify against the operator.":"Fictional sample result for this design preview.")+"\nPrize tables are not connected in this build.";alert("Draw record",msg);}

    private void lab(){
        gameRow();copy(content,"Compare up to six lines, or research a smaller subset.");
        for(int i=0;i<draft.size();i++){
            final int index=i;Entry e=draft.get(i);LinearLayout card=panel();LinearLayout top=horizontal();TextView label=text("●  LINE "+(char)('A'+i)+(e.subset?"  / SUBSET":""),12,GREEN,true);top.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView edit=action("EDIT",false,()->editEntry(e,entry->{draft.set(index,entry);saveDraft();}));edit.setTag("edit_line_"+i);top.addView(edit,new LinearLayout.LayoutParams(d(60),d(42)));add(card,top);View numbers=balls(e.numbers,e.powerBall,0,null);numbers.setOnClickListener(v->editEntry(e,entry->{draft.set(index,entry);saveDraft();}));add(card,numbers);
            TextView save=action("SAVE LINE",false,()->saveLine(e,index));save.setTag("save_line_"+i);two(card,save,action("REMOVE",false,()->{draft.remove(index);saveDraft();}));add(content,card);
        }
        if(draft.size()<6)add(content,action("+  ADD LINE",false,()->editEntry(null,e->{draft.add(e);saveDraft();})));
        TextView range=action(window==0?"All archive draws  ▾":"Last "+window+" draws  ▾",false,()->new AlertDialog.Builder(this).setTitle("Analysis window").setItems(new String[]{"Last 20 draws","Last 50 draws","Last 100 draws","All archive draws"},(v,i)->{window=new int[]{20,50,100,0}[i];invalidateReport();refresh();}).show());
        two(content,range,action("MAIN BALLS",false,()->alert("Matching definitions","Frequencies and main-match counts exclude the LOTTO bonus ball. A selected PowerBall is checked separately. Full PowerBall matches require all five main numbers and the selected PowerBall.")));
        TextView run=action("RUN ANALYSIS",true,this::runAnalysis);run.setTag("run_analysis");add(content,run);resultArea=vertical();content.addView(resultArea);
        if(!results.isEmpty())renderResults();else copy(resultArea,"Choose your lines and run an experiment.\nSample and imported data are always labelled.");
    }
    private void saveDraft(){store.draft(game,draft);invalidateReport();refresh();}
    private interface EntryConsumer {void accept(Entry e);}
    private void editEntry(Entry existing,EntryConsumer accept){
        final Game editorGame=game;LinearLayout pane=vertical();pane.setPadding(d(15),d(8),d(15),d(12));pane.setBackgroundColor(BG);
        TextView guide=text("Choose "+editorGame.picks+" main numbers from 1–"+editorGame.pool+(editorGame.hasPowerBall()?", plus a PowerBall.":"."),13,MUTED,false);add(pane,guide);
        EditText pasted=new EditText(this);pasted.setTextColor(TEXT);pasted.setHintTextColor(MUTED);pasted.setTextSize(16);pasted.setSingleLine(true);pasted.setHint("Type or paste numbers");pasted.setInputType(android.text.InputType.TYPE_CLASS_TEXT);add(pane,pasted);
        CheckBox subset=new CheckBox(this);subset.setText("Research a subset (1–"+editorGame.picks+" numbers)");subset.setTextColor(TEXT);subset.setTextSize(12);subset.setChecked(existing!=null&&existing.subset);add(pane,subset);
        TreeSet<Integer> selected=new TreeSet<>();if(existing!=null)for(int n:existing.numbers)selected.add(n);
        GridLayout grid=new GridLayout(this);grid.setColumnCount(6);java.util.List<TextView> cells=new ArrayList<>();
        Runnable update=()->{for(int i=0;i<cells.size();i++){TextView cell=cells.get(i);boolean on=selected.contains(i+1);cell.setTextColor(on?INK:TEXT);GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.OVAL);shape.setColor(on?GREEN:0xFF123544);shape.setStroke(d(1),on?GREEN:STEEL);cell.setBackground(shape);cell.setSelected(on);cell.setContentDescription("Number "+(i+1)+(on?", selected":""));}};
        for(int n=1;n<=editorGame.pool;n++){final int value=n;TextView cell=text(Integer.toString(n),14,TEXT,true);cell.setGravity(Gravity.CENTER);cell.setTag("num_"+n);cell.setOnClickListener(v->{if(selected.contains(value))selected.remove(value);else if(selected.size()<editorGame.picks)selected.add(value);else{toast("Up to "+editorGame.picks+" main numbers.");return;}StringBuilder s=new StringBuilder();for(int x:selected)s.append(x).append(' ');pasted.setText(s.toString().trim());update.run();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec((n-1)/6),GridLayout.spec((n-1)%6,1f));lp.width=0;lp.height=d(44);lp.setMargins(d(2),d(2),d(2),d(2));grid.addView(cell,lp);cells.add(cell);}
        ScrollView numberScroll=new ScrollView(this);numberScroll.addView(grid);add(pane,numberScroll);numberScroll.getLayoutParams().height=d(235);
        EditText pb=new EditText(this);if(editorGame.hasPowerBall()){pb.setHint("PowerBall 1–16");pb.setTextColor(ORANGE);pb.setHintTextColor(MUTED);pb.setTextSize(16);pb.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);if(existing!=null&&existing.powerBall>0)pb.setText(Integer.toString(existing.powerBall));add(pane,pb);}
        if(existing!=null){StringBuilder s=new StringBuilder();for(int n:existing.numbers)s.append(n).append(' ');pasted.setText(s.toString().trim());}
        pasted.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){try{int[] values=Entry.parseNumbers(s.toString());selected.clear();for(int x:values)selected.add(x);update.run();}catch(Exception ignored){}}public void afterTextChanged(Editable e){}});
        add(pane,action("RANDOM VALID LINE",false,()->{Entry e=Entry.random(editorGame,random);subset.setChecked(false);StringBuilder s=new StringBuilder();for(int n:e.numbers)s.append(n).append(' ');pasted.setText(s.toString().trim());if(editorGame.hasPowerBall())pb.setText(Integer.toString(e.powerBall));}));update.run();
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Add a combination":"Edit combination").setView(pane).setNegativeButton("Cancel",null).setPositiveButton("Use line",null).create();dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{int power=editorGame.hasPowerBall()&&!pb.getText().toString().trim().isEmpty()?Integer.parseInt(pb.getText().toString().trim()):0;Entry e=new Entry(editorGame,Entry.parseNumbers(pasted.getText().toString()),power,subset.isChecked(),existing==null?"":existing.name);dialog.dismiss();accept.accept(e);}catch(Exception ex){pasted.setError(ex.getMessage());}}));dialog.show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void saveLine(Entry e,int index){EditText name=new EditText(this);name.setSingleLine();name.setText(e.name.isEmpty()?"Line "+(char)('A'+index):e.name);new AlertDialog.Builder(this).setTitle("Name this combination").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Save",(v,w)->{String n=name.getText().toString().trim();toast(store.save(e.named(n.isEmpty()?"My combination":n))?"Saved to My Numbers":"This line is already saved.");}).show();}

    private void runAnalysis(){
        if(draft.isEmpty()){toast("Add at least one line first.");return;}
        analysedDraws=store.draws(game,window);results.clear();for(Entry e:draft)results.add(AnalysisEngine.analyse(e,analysedDraws));tool=0;
        StringBuilder s=new StringBuilder("Game: "+game.title+"\nData: "+source()+"\nGenerated: "+ZonedDateTime.now(ZoneId.of("Africa/Johannesburg"))+"\nRule: current June 2026 onward\nDraws checked: "+analysedDraws.size()+"\nWindow: "+(window==0?"All available": "Last "+window)+"\nMain-ball frequencies exclude LOTTO bonus balls.\n\n");
        if(!analysedDraws.isEmpty())s.append("Archive span: ").append(analysedDraws.get(analysedDraws.size()-1).date).append(" to ").append(analysedDraws.get(0).date).append("\n\n");
        for(int i=0;i<results.size();i++){AnalysisEngine.Result r=results.get(i);s.append("LINE ").append((char)('A'+i)).append(r.entry.subset?" (subset)":"").append(": ").append(r.entry.display()).append("\n").append(r.entry.subset?"Contains subset: ":"Full matches: ").append(r.entry.subset?r.containsHits:r.completeHits).append(" of ").append(r.checked).append("\nBest main match: ").append(r.best).append(" of ").append(r.entry.numbers.length).append("\n");if(r.entry.powerBall>0)s.append("PowerBall matches: ").append(r.powerHits).append("\n");for(int k=0;k<r.distribution.length;k++)s.append(k).append(" main matches: ").append(r.distribution[k]).append("\n");s.append("\nMatching records (main numbers and separate PowerBall):\n");for(Draw draw:r.matching)s.append(draw.date).append(" | ").append(Arrays.toString(draw.numbers)).append(draw.powerBall>0?" PB "+draw.powerBall:"").append(" | ").append(AnalysisEngine.matches(r.entry.numbers,draw.numbers)).append(" main matches\n");s.append("\n");}
        s.append("Research observations do not increase the probability of a valid line in a fair future draw. Imported data is unverified. No prize entitlement is calculated.");report=s.toString();renderResults();
    }
    private void renderResults(){
        if(resultArea==null)return;resultArea.removeAllViews();title(resultArea,"EXPERIMENT RESULTS");copy(resultArea,analysedDraws.size()+" draws checked · "+(store.imported(game)?"your imported archive":"fictional sample archive"));
        LinearLayout tabs=horizontal();String[] labels={"Overview","Frequency","Pairs","Structure"};for(int i=0;i<labels.length;i++){final int k=i;TextView a=action(labels[i],tool==i,()->{tool=k;renderResults();});a.setTextSize(10);tabs.addView(a,new LinearLayout.LayoutParams(0,d(44),1));}add(resultArea,tabs);
        if(tool==0){for(int i=0;i<results.size();i++){AnalysisEngine.Result r=results.get(i);LinearLayout p=panel();title(p,"LINE "+(char)('A'+i)+(r.entry.subset?" / SUBSET":""));two(p,stat("Draws checked",Integer.toString(r.checked)),stat(r.entry.subset?"Contains subset":"Full matches",Integer.toString(r.entry.subset?r.containsHits:r.completeHits)));add(p,text("BEST MAIN MATCH   "+r.best+" / "+r.entry.numbers.length,14,GREEN,true));StringBuilder distribution=new StringBuilder();for(int k=0;k<r.distribution.length;k++)distribution.append(k).append(" matches: ").append(r.distribution[k]).append(k==r.distribution.length-1?"":"    ");copy(p,distribution.toString());if(r.entry.powerBall>0)copy(p,"PowerBall matched in "+r.powerHits+" / "+r.checked+" draws (separate pool).");add(p,action("VIEW MATCHING DRAWS",false,()->matchingDraws(r.entry)));add(resultArea,p);}
            if(draft.size()>1){LinearLayout p=panel();title(p,"LINE OVERLAP");for(int a=0;a<draft.size();a++)for(int b=a+1;b<draft.size();b++)copy(p,"Line "+(char)('A'+a)+" + "+(char)('A'+b)+": "+AnalysisEngine.overlap(draft.get(a),draft.get(b))+" shared main numbers"+(draft.get(a).key().equals(draft.get(b).key())?" · identical line":""));copy(p,"Overlap describes coverage, not winning strength.");add(resultArea,p);}
        }else if(tool==1){int[] freq=results.get(0).frequency;LinearLayout p=panel();title(p,"MAIN-NUMBER FREQUENCY");Histogram chart=new Histogram(this,freq);p.addView(chart,new LinearLayout.LayoutParams(-1,d(150)));copy(p,"Tap a number below to open its matching draws.\nNumber · occurrences / "+analysedDraws.size()+" draws.");for(int start=1;start<=game.pool;start+=4){LinearLayout row=horizontal();for(int n=start;n<Math.min(start+4,game.pool+1);n++){final int num=n;TextView cell=action(String.format(Locale.ROOT,"%02d · %d",n,freq[n]),false,()->matchingDraws(new Entry(game,new int[]{num},0,true,"")));cell.setTextSize(11);row.addView(cell,new LinearLayout.LayoutParams(0,d(45),1));}add(p,row);}add(resultArea,p);
        }else if(tool==2){for(int i=0;i<draft.size();i++){Entry e=draft.get(i);LinearLayout p=panel();title(p,"LINE "+(char)('A'+i)+" / PAIR SCANNER");if(e.numbers.length<2)copy(p,"Add another main number to investigate a pair.");for(int a=0;a<e.numbers.length;a++)for(int b=a+1;b<e.numbers.length;b++){int x=e.numbers[a],y=e.numbers[b],count=AnalysisEngine.pairCount(x,y,analysedDraws);add(p,action(String.format(Locale.ROOT,"%02d + %02d     %d / %d",x,y,count,analysedDraws.size()),false,()->matchingDraws(new Entry(game,new int[]{x,y},0,true,""))));}add(resultArea,p);}
        }else{for(int i=0;i<draft.size();i++){Entry e=draft.get(i);LinearLayout p=panel();title(p,"LINE "+(char)('A'+i)+" / STRUCTURE");int odd=0,sum=0,low=0,runs=0;for(int n:e.numbers){odd+=n%2;sum+=n;if(n<=game.pool/2)low++;}for(int j=1;j<e.numbers.length;j++)if(e.numbers[j]==e.numbers[j-1]+1)runs++;copy(p,"Odd / even: "+odd+" / "+(e.numbers.length-odd)+"\nLow / high: "+low+" / "+(e.numbers.length-low)+"\nNumber sum: "+sum+"\nNumber spread: "+(e.numbers[e.numbers.length-1]-e.numbers[0])+"\nAdjacent consecutive pairs: "+runs);add(resultArea,p);}copy(resultArea,"These describe a selection. They do not rank its chance of winning.");}
        add(resultArea,action("EXPORT RESEARCH PDF",false,()->{if(report==null)return;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"LottoLab_Research_"+game.name()+".pdf");startActivityForResult(i,EXPORT_PDF);}));copy(resultArea,"Historical results describe the selected archive. Every valid line has the same chance in a fair future draw.");
    }
    private LinearLayout stat(String label,String value){LinearLayout box=vertical();add(box,text(value,27,GREEN,true));box.addView(text(label,11,MUTED,false));return box;}
    private void matchingDraws(Entry e){
        java.util.List<Draw> ds=store.draws(game,window);java.util.List<Draw> matched=new ArrayList<>();for(Draw dr:ds)if(AnalysisEngine.matches(e.numbers,dr.numbers)==e.numbers.length&&(e.powerBall==0||e.powerBall==dr.powerBall))matched.add(dr);
        if(matched.isEmpty()){alert("Matching draws","0 / "+ds.size()+" draws contain all these selected numbers"+(e.powerBall>0?" and the selected PowerBall":"")+".\n\n"+source());return;}
        String[] rows=new String[Math.min(200,matched.size())];for(int j=0;j<rows.length;j++)rows[j]=DATE.format(matched.get(j).date)+"  ·  "+Arrays.toString(matched.get(j).numbers);new AlertDialog.Builder(this).setTitle(matched.size()+" / "+ds.size()+" containing draws").setItems(rows,(v,i)->drawDetail(matched.get(i),e)).setPositiveButton("Done",null).show();
    }

    private void vault(){
        gameRow();java.util.List<Entry> saved=store.saved();int shown=0;for(Entry e:saved)if(e.game==game){shown++;LinearLayout p=panel();title(p,e.name.isEmpty()?"SAVED COMBINATION":e.name.toUpperCase(Locale.ROOT));add(p,balls(e.numbers,e.powerBall,0,null));copy(p,e.game.title+(e.subset?" · Research subset":" · Complete line"));two(p,action("OPEN IN LAB",true,()->{boolean exists=false;for(Entry x:draft)if(x.key().equals(e.key()))exists=true;if(!exists&&draft.size()>=6){toast("Remove a draft line first; the lab holds six.");return;}if(!exists){draft.add(e);store.draft(game,draft);}invalidateReport();tab="lab";refresh();}),action("DELETE",false,()->new AlertDialog.Builder(this).setTitle("Delete saved combination?").setMessage(e.name+"\n"+e.display()).setNegativeButton("Cancel",null).setPositiveButton("Delete",(v,w)->{store.remove(e);refresh();}).show()));add(content,p);}
        if(shown==0){LinearLayout p=panel();title(p,"YOUR NUMBER VAULT");copy(p,"Save named combinations from the Lab. Your lines stay on this phone between app launches.");add(p,action("CREATE A COMBINATION",true,()->{tab="lab";refresh();}));add(content,p);}copy(content,"Saved lines are research notes. They are not purchased tickets.");
    }
    private void planner(){
        gameRow();int savedCount=0;for(Entry e:store.saved())if(e.game==game&&!e.subset)savedCount++;
        final int[] lines={store.preferences().getInt("plan_lines",Math.max(1,savedCount))},draws={store.preferences().getInt("plan_draws",8)};
        LinearLayout settings=panel();title(settings,"PLAN YOUR ENTRIES");TextView lineValue=text("",22,GREEN,true),drawValue=text("",22,GREEN,true);lineValue.setGravity(Gravity.CENTER);drawValue.setGravity(Gravity.CENTER);
        LinearLayout summary=panel();TextView cost=text("",32,GREEN,true),perDraw=text("",13,MUTED,false),budgetState=text("",13,TEXT,true);ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgressTintList(ColorStateList.valueOf(GREEN));EditText budget=new EditText(this);budget.setTextSize(16);budget.setTextColor(TEXT);budget.setHintTextColor(MUTED);budget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);budget.setHint("Budget in rand");budget.setText(store.preferences().getString("budget","200"));
        Runnable recalc=()->{lineValue.setText(Integer.toString(lines[0]));drawValue.setText(Integer.toString(draws[0]));long total=AnalysisEngine.cost(game,lines[0],draws[0]);cost.setText(AnalysisEngine.money(total));perDraw.setText(AnalysisEngine.money((long)game.bundleCostCents*lines[0])+" per draw · "+draws[0]+" selected draws");try{double rand=Double.parseDouble(budget.getText().toString());if(!Double.isFinite(rand)||rand<=0||rand>1000000)throw new NumberFormatException();long cap=Math.round(rand*100);boolean over=total>cap;budgetState.setText((over?"Over your limit by ":"Remaining within limit: ")+AnalysisEngine.money(Math.abs(cap-total)));budgetState.setTextColor(over?ORANGE:GREEN);progress.setProgress((int)Math.min(100,total*100.0/cap));progress.setProgressTintList(ColorStateList.valueOf(over?ORANGE:GREEN));}catch(Exception ex){budgetState.setText("Enter a positive budget to compare.");budgetState.setTextColor(MUTED);progress.setProgress(0);}};
        title(settings,"LINES");settings.addView(stepper(lineValue,()->{if(lines[0]>1)lines[0]--;recalc.run();},()->{if(lines[0]<60)lines[0]++;recalc.run();}));title(settings,"DRAW DATES");settings.addView(stepper(drawValue,()->{if(draws[0]>1)draws[0]--;recalc.run();},()->{if(draws[0]<52)draws[0]++;recalc.run();}));title(settings,"PERSONAL BUDGET (RAND)");add(settings,budget);add(content,settings);
        title(summary,"PLANNED ENTRY COST");add(summary,cost);add(summary,perDraw);add(summary,progress);add(summary,budgetState);copy(summary,game.costNote()+"\nThis is a calculation, not a ticket purchase.");add(content,summary);
        add(content,action("SAVE MY PLAN",true,()->{try{double r=Double.parseDouble(budget.getText().toString());if(!Double.isFinite(r)||r<=0||r>1000000)throw new NumberFormatException();store.preferences().edit().putInt("plan_lines",lines[0]).putInt("plan_draws",draws[0]).putString("budget",budget.getText().toString()).apply();toast("Plan saved on this phone.");}catch(Exception ex){budget.setError("Enter a positive budget up to R1,000,000.");}}));
        LinearLayout dates=panel();title(dates,"NEXT SCHEDULED DATES");LocalDate date=LocalDate.now(ZoneId.of("Africa/Johannesburg"));int count=0;while(count<4){if(game.scheduled(date)){copy(dates,date.getDayOfWeek().toString()+"  ·  "+DATE.format(date));count++;}date=date.plusDays(1);}copy(dates,"Calendar estimates in South African time. Confirm official exceptions and sales cut-offs before purchasing.");add(content,dates);
        budget.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){recalc.run();}public void afterTextChanged(Editable e){}});recalc.run();
    }
    private View stepper(TextView value,Runnable minus,Runnable plus){LinearLayout r=horizontal();r.addView(action("−",false,minus),new LinearLayout.LayoutParams(d(52),d(48)));r.addView(value,new LinearLayout.LayoutParams(0,d(48),1));r.addView(action("+",false,plus),new LinearLayout.LayoutParams(d(52),d(48)));return r;}
    private void dataMenu(){new AlertDialog.Builder(this).setTitle("LottoLab / Data & tools").setItems(new String[]{"Import draw CSV","Export this game's archive CSV","CSV format & rules","Clear imported draws","About this preview"},(v,i)->{if(i==0)startImport();if(i==1){pendingExport=CsvCodec.write(store.draws(game,0));Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.setType("text/csv");intent.putExtra(Intent.EXTRA_TITLE,"LottoLab_"+(store.imported(game)?"Imported":"FictionalSample")+"_"+game.name()+".csv");startActivityForResult(intent,EXPORT_CSV);}if(i==2)alert("CSV format",CsvCodec.HEADER+"\nLOTTO,2026-10-14,4 12 19 28 36 47,8,\nPOWERBALL,2026-10-13,4 12 19 28 47,,8\n\nSeparate main numbers with spaces, or quote a comma-separated number field. Dates use YYYY-MM-DD. Current games only; June 2026 onward.\n\nAll rows are validated before importing. Matching game/date records are replaced. Imports are user data, not verified official results.");if(i==3)new AlertDialog.Builder(this).setTitle("Clear imported draws?").setMessage("Saved combinations and your plan will remain. The sample archive becomes visible again.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(a,b)->{store.clearImports();invalidateReport();refresh();}).show();if(i==4)alert("LottoLab 0.1 / Design preview","Original Android project · Native interface\n\nSix current number-draw games, six-line research, pair counts, frequencies, saved lines, planner, CSV import and PDF export.\n\nThe bundled draws are fictional design fixtures. A live verified results feed, historical rule eras, prize claims, notifications and full system wheels are not connected in this preview. No account or internet access is required.\n\n18+. Research does not predict a fair future draw.");}).setNegativeButton("Close",null).show();}
    private void startImport(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/comma-separated-values","text/plain","application/octet-stream"});startActivityForResult(intent,IMPORT);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(request==IMPORT){toast("Validating your archive…");worker.execute(()->{try{String raw;try(InputStream in=getContentResolver().openInputStream(uri)){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){bytes.write(b,0,n);if(bytes.size()>2*1024*1024)throw new IOException("CSV files must be smaller than 2 MB.");}raw=bytes.toString(StandardCharsets.UTF_8.name());}java.util.List<Draw> parsed=CsvCodec.parse(raw,LocalDate.now(ZoneId.of("Africa/Johannesburg")));store.importDraws(parsed);runOnUiThread(()->{if(isFinishing())return;game=parsed.get(0).game;draft=store.draft(game);historyDate=null;historyQuery="";invalidateReport();tab="history";refresh();toast("Imported "+parsed.size()+" draw records.");});}catch(Exception e){runOnUiThread(()->{if(!isFinishing())alert("Import not completed",e.getMessage()+"\nNo rows from this file were saved.");});}});
        }else if(request==EXPORT_CSV||request==EXPORT_PDF){try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("Unable to open destination.");if(request==EXPORT_PDF){if(report==null)throw new IOException("Run the analysis again before exporting.");PdfReport.write(out,report);}else out.write(pendingExport.getBytes(StandardCharsets.UTF_8));toast("Saved successfully.");}catch(Exception e){alert("Could not save",e.getMessage());}}
    }
}
