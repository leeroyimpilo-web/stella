package za.co.lottolab.studio;

import static za.co.lottolab.studio.ConsoleArt.*;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Four familiar destinations, with one task per tool screen. All controls are native. */
public final class MainActivity extends Activity {
  private ArchiveStore store;
  private long viewedRevision;
  private LinearLayout root, toolbar, navigation, content, output;
  PopupWindow drawer; // Package-visible for native navigation verification.
  private int chapterIndex = 0;
  private Game game = Game.LOTTO;
  private String tab = "home",
      page = "",
      group = "",
      ruleId = "2026-06",
      query = "",
      dateFrom = "",
      dateTo = "";
  private int window = 100, visible = 30;
  private Entry working;
  private List<Entry> draft = new ArrayList<>(), generated = new ArrayList<>();
  private LocalDate checkDate;
  private final Deque<String[]> back = new ArrayDeque<>();
  private final SecureRandom random = new SecureRandom();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private String report = "",
      csv = "",
      pendingText = "",
      pendingReport = "",
      pendingKind = "",
      pendingName = "";
  private static final int IMPORT = 41, SAVE = 42, RESTORE = 43, NOTIFY = 44;
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

  private static LocalDate today() {
    return LocalDate.now(ZoneId.of("Africa/Johannesburg"));
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    store = new ArchiveStore(this);
    try {
      game = Game.valueOf(store.preferences().getString("game", "LOTTO"));
    } catch (Exception ignored) {
    }
    ruleId = RuleSet.current(game).id;
    draft = store.draft(game);
    loadGenerated();
    checkDate = defaultDate();
    if (state != null) {
      tab = state.getString("tab", "home");
      page = state.getString("page", "");
      group = state.getString("group", "");
      ruleId = state.getString("ruleId", ruleId);
      window = state.getInt("window", 100);
      chapterIndex = state.getInt("chapterIndex", 0);
      query = state.getString("query", "");
      dateFrom = state.getString("from", "");
      dateTo = state.getString("to", "");
      report = restoreStateText(state.getString("reportFile", ""));
      csv = restoreStateText(state.getString("csvFile", ""));
      pendingText = restoreStateText(state.getString("pendingTextFile", ""));
      pendingReport = restoreStateText(state.getString("pendingReportFile", ""));
      pendingKind = state.getString("pendingKind", "");
      pendingName = state.getString("pendingName", "");
      try {
        checkDate = LocalDate.parse(state.getString("checkDate", checkDate.toString()));
        if (state.containsKey("working"))
          working = ArchiveStore.entryFrom(new JSONObject(state.getString("working")));
      } catch (Exception ignored) {
      }
      String[] routes = state.getStringArray("back");
      if (routes != null) for (String route : routes) back.addLast(route.split("\\|", -1));
    }
    consumeIntent(getIntent());
    if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    root = column();
    root.setBackgroundColor(BG);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets x =
                insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(x.left, x.top, x.right, x.bottom);
          } else
            v.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
          return insets;
        });
    toolbar = row();
    root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
    ScrollView scroll = new ScrollView(this);
    scroll.setTag("main_scroll");
    scroll.setFillViewport(true);
    content = column();
    content.setPadding(dp(18), dp(8), dp(18), dp(22));
    scroll.addView(content);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    navigation = row();
    root.addView(navigation, new LinearLayout.LayoutParams(-1, -2));
    setContentView(root);
    render();
    root.requestApplyInsets();
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::goBack);
    viewedRevision = store.preferences().getLong("archive_generation", 0);
    ResultUpdates.schedule(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (store != null && root != null) {
      long revision = store.preferences().getLong("archive_generation", 0);
      if (revision != viewedRevision) {
        viewedRevision = revision;
        report = csv = "";
        render();
      }
    }
  }

  // Large exports remain in private cache rather than Android's size-limited state Bundle.
  private String saveStateText(String value) {
    if (value.isEmpty()) return "";
    String name = "state_" + UUID.randomUUID() + ".txt";
    try (OutputStream out = new FileOutputStream(new File(getCacheDir(), name))) {
      out.write(value.getBytes(StandardCharsets.UTF_8));
      return name;
    } catch (IOException ignored) {
      return "";
    }
  }

  private String restoreStateText(String name) {
    if (!name.matches("state_[A-Za-z0-9-]+\\.txt")) return "";
    try (InputStream in = new FileInputStream(new File(getCacheDir(), name))) {
      return FeedClient.read(in, 20 * 1024 * 1024);
    } catch (IOException ignored) {
      return "";
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    consumeIntent(intent);
    render();
  }

  private void consumeIntent(Intent intent) {
    if (intent == null || !intent.hasExtra("game")) return;
    try {
      game = Game.valueOf(intent.getStringExtra("game"));
      checkDate = LocalDate.parse(intent.getStringExtra("date"));
      ruleId = RuleSet.forDate(game, checkDate).id;
      tab = "results";
      page = "";
      dateFrom = dateTo = checkDate.toString();
      draft = store.draft(game);
    } catch (Exception ignored) {
    }
    intent.removeExtra("game");
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putString("tab", tab);
    b.putString("page", page);
    b.putString("group", group);
    b.putString("ruleId", ruleId);
    b.putInt("window", window);
    b.putInt("chapterIndex", chapterIndex);
    b.putString("query", query);
    b.putString("from", dateFrom);
    b.putString("to", dateTo);
    b.putString("checkDate", checkDate.toString());
    b.putString("reportFile", saveStateText(report));
    b.putString("csvFile", saveStateText(csv));
    b.putString("pendingTextFile", saveStateText(pendingText));
    b.putString("pendingReportFile", saveStateText(pendingReport));
    b.putString("pendingKind", pendingKind);
    b.putString("pendingName", pendingName);
    try {
      if (working != null) b.putString("working", ArchiveStore.entryJson(working).toString());
    } catch (JSONException ignored) {
    }
    List<String> routes = new ArrayList<>();
    for (String[] r : back) routes.add(String.join("|", r));
    b.putStringArray("back", routes.toArray(new String[0]));
  }

  @Override
  @android.annotation.SuppressLint("GestureBackNavigation")
  public void onBackPressed() {
    if (Build.VERSION.SDK_INT < 33) goBack();
  }

  @Override
  protected void onDestroy() {
    if (drawer != null) drawer.dismiss();
    worker.shutdown();
    store.close();
    super.onDestroy();
  }

  private void goBack() {
    hideKeyboard();
    if (!back.isEmpty()) {
      String[] r = back.pop();
      tab = r[0];
      page = r[1];
      group = r[2];
      render();
    } else if (!page.isEmpty()) {
      page = "";
      render();
    } else if (!tab.equals("home")) {
      tab = "home";
      render();
    } else finish();
  }

  private void go(String destination, String tool) {
    if (drawer != null) drawer.dismiss();
    back.push(new String[] {tab, page, group});
    tab = destination;
    page = tool;
    report = "";
    csv = "";
    hideKeyboard();
    render();
  }

  private void tab(String key) {
    if (drawer != null) drawer.dismiss();
    back.clear();
    tab = key;
    page = "";
    query = "";
    dateFrom = dateTo = "";
    visible = 30;
    hideKeyboard();
    render();
  }

  private int dp(float n) {
    return ConsoleArt.dp(this, n);
  }

  private LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  private LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  private TextView text(String s, float size, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
    t.setLineSpacing(dp(2), 1);
    t.setFontFeatureSettings("tnum");
    return t;
  }

  private void add(LinearLayout p, View v) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(12);
    p.addView(v, lp);
  }

  private void note(LinearLayout p, String s) {
    add(p, text(s, 14, MUTED, false));
  }

  private GradientDrawable shape(int color, int border, float radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    if (border != 0) d.setStroke(dp(1), border);
    return d;
  }

  private LinearLayout card() {
    LinearLayout p = column();
    p.setPadding(dp(16), dp(14), dp(16), dp(14));
    p.setBackground(shape(0xFF102E3C, 0xFF245160, 16));
    return p;
  }

  private TextView button(String label, boolean primary, Runnable run) {
    TextView b = text(label, 15, primary ? INK : TEXT, true);
    b.setGravity(Gravity.CENTER);
    b.setPadding(dp(12), dp(13), dp(12), dp(13));
    b.setMinHeight(dp(50));
    b.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(0x22FFFFFF),
            shape(primary ? ORANGE : 0xFF163E4D, primary ? 0 : 0xFF356070, 12),
            null));
    b.setOnClickListener(v -> run.run());
    b.setClickable(true);
    b.setFocusable(true);
    b.setAccessibilityDelegate(
        new View.AccessibilityDelegate() {
          @Override
          public void onInitializeAccessibilityNodeInfo(View h, AccessibilityNodeInfo n) {
            super.onInitializeAccessibilityNodeInfo(h, n);
            n.setClassName(Button.class.getName());
          }
        });
    return b;
  }

  private void primary(LinearLayout p, String title, String tag, Runnable action) {
    TextView b = button(title, true, action);
    b.setTag(tag);
    add(p, b);
  }

  private void two(LinearLayout p, View a, View b) {
    LinearLayout r = row();
    LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, -2, 1);
    l.rightMargin = dp(5);
    r.addView(a, l);
    l = new LinearLayout.LayoutParams(0, -2, 1);
    l.leftMargin = dp(5);
    r.addView(b, l);
    add(p, r);
  }

  private EditText input(LinearLayout p, String label, String hint, String value, boolean numeric) {
    add(p, text(label, 14, TEXT, true));
    EditText e = new EditText(this);
    e.setSingleLine(true);
    e.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(4000)});
    e.setTextColor(TEXT);
    e.setHintTextColor(MUTED);
    e.setTextSize(16);
    e.setMinHeight(dp(52));
    e.setHint(hint);
    e.setText(value);
    e.setInputType(
        numeric
            ? android.text.InputType.TYPE_CLASS_NUMBER
            : android.text.InputType.TYPE_CLASS_TEXT);
    add(p, e);
    return e;
  }

  private void hideKeyboard() {
    View v = getCurrentFocus();
    if (v != null)
      ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
          .hideSoftInputFromWindow(v.getWindowToken(), 0);
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  private void alert(String title, String message) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("Done", null)
        .show();
  }

  private void heading(String title, String explanation) {
    add(content, text(title, 26, TEXT, true));
    if (!explanation.isEmpty()) note(content, explanation);
  }

  private String pageTitle() {
    switch (page) {
      case "check":
        return "Check a ticket";
      case "search":
        return "Search my combination";
      case "compare":
        return "Compare my lines";
      case "generate":
        return "Generate numbers";
      case "wheel":
        return "Build a full system";
      case "coverage":
        return "Check line coverage";
      case "odds":
        return "Explore the odds";
      case "budget":
        return "Plan my spending";
      case "simulate":
        return "Explore randomness";
      case "frequency":
        return "Number frequencies";
      case "hotcold":
        return "Hot and cold numbers";
      case "gaps":
        return "When numbers appeared";
      case "pairs":
        return "Pairs and triples";
      case "patterns":
        return "Draw patterns";
      case "settings":
        return "Settings & data";
      case "tickets":
        return "My dated tickets";
      case "library":
        return "User guide";
      case "faq":
        return "Questions & answers";
      case "chapter":
        return "User manual";
      case "group":
        return group;
      default:
        return tab.equals("results")
            ? "Results"
            : tab.equals("lab") ? "Lotto Lab" : tab.equals("numbers") ? "My Numbers" : "LottoLab";
    }
  }

  private void render() {
    if (isFinishing() || isDestroyed()) return;
    toolbar.removeAllViews();
    toolbar.setPadding(dp(10), dp(4), dp(10), 0);
    TextView openMenu = button("☰", false, this::showDrawer);
    openMenu.setTag("menu_toggle");
    openMenu.setContentDescription("Open the LottoLab side menu");
    toolbar.addView(openMenu, new LinearLayout.LayoutParams(dp(52), -2));
    if (!page.isEmpty()) {
      TextView backButton = button("‹", false, this::goBack);
      backButton.setContentDescription("Back");
      toolbar.addView(backButton, new LinearLayout.LayoutParams(dp(48), -2));
    }
    TextView brand = text(pageTitle(), page.isEmpty() ? 23 : 19, TEXT, true);
    brand.setPadding(dp(8), 0, 0, 0);
    brand.setSingleLine(true);
    brand.setEllipsize(android.text.TextUtils.TruncateAt.END);
    toolbar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    // The drawer contains all destinations. Removing duplicate bottom tabs
    // leaves more screen space for a single clear task.
    navigation.setVisibility(View.GONE);
    content.removeAllViews();
    output = null;
    if (!page.isEmpty() && !page.equals("group")) heading(pageTitle(), simpleTip(page));
    switch (page) {
      case "group":
        toolGroup();
        return;
      case "check":
        checker();
        return;
      case "search":
        searchTool();
        return;
      case "compare":
        compare();
        return;
      case "generate":
        generator();
        return;
      case "wheel":
        wheel();
        return;
      case "coverage":
        coverage();
        return;
      case "frequency":
      case "hotcold":
      case "gaps":
      case "pairs":
      case "patterns":
        statistics();
        return;
      case "odds":
        odds();
        return;
      case "budget":
        budget();
        return;
      case "simulate":
        simulation();
        return;
      case "settings":
        settings();
        return;
      case "tickets":
        tickets();
        return;
      case "library":
        userLibrary();
        return;
      case "chapter":
        userChapter();
        return;
      case "faq":
        userFaq();
        return;
    }
    if (tab.equals("results")) history();
    else if (tab.equals("lab")) lab();
    else if (tab.equals("numbers")) numbers();
    else home();
  }

  /** All features live in one accessible side drawer. Nothing requires an account. */
  private void showDrawer() {
    hideKeyboard();
    if (drawer != null && drawer.isShowing()) {
      drawer.dismiss();
      return;
    }
    LinearLayout panel = column();
    panel.setPadding(dp(16), dp(30), dp(16), dp(20));
    panel.setBackgroundColor(0xFF071F2D);
    add(panel, text("LOT TOLAB".replace(" ", ""), 14, GREEN, true));
    add(panel, text("Pick a task", 25, TEXT, true));
    note(panel, "Everything in one place. Tap to open.");
    drawerSection(panel, "START HERE");
    drawerOption(panel, "Home dashboard", "menu_home", () -> tab("home"));
    drawerOption(panel, "Latest results", "menu_results", () -> tab("results"));
    drawerOption(panel, "My saved numbers", "menu_numbers", () -> tab("numbers"));
    drawerSection(panel, "MY NUMBERS");
    drawerOption(panel, "Check a ticket", "menu_check", () -> go("lab", "check"));
    drawerOption(panel, "Pick numbers", "menu_generate", () -> go("lab", "generate"));
    drawerOption(panel, "Find my numbers in old draws", "menu_search", () -> go("lab", "search"));
    drawerOption(panel, "Compare my lines", "menu_compare", () -> go("lab", "compare"));
    drawerOption(panel, "My dated tickets", "menu_tickets", () -> go("numbers", "tickets"));
    drawerSection(panel, "EXPLORE PAST RESULTS");
    drawerOption(panel, "All research tools", "menu_lab", () -> tab("lab"));
    drawerOption(panel, "Most and least seen", "menu_hotcold", () -> go("lab", "hotcold"));
    drawerOption(panel, "How often numbers appeared", "menu_frequency", () -> go("lab", "frequency"));
    drawerOption(panel, "When numbers last appeared", "menu_gaps", () -> go("lab", "gaps"));
    drawerOption(panel, "Numbers that appeared together", "menu_pairs", () -> go("lab", "pairs"));
    drawerOption(panel, "Draw patterns", "menu_patterns", () -> go("lab", "patterns"));
    drawerSection(panel, "MORE TOOLS");
    drawerOption(panel, "Full number systems", "menu_wheel", () -> go("lab", "wheel"));
    drawerOption(panel, "Check repeated lines", "menu_coverage", () -> go("lab", "coverage"));
    drawerOption(panel, "Understand the chances", "menu_odds", () -> go("lab", "odds"));
    drawerOption(panel, "Plan my spending", "menu_budget", () -> go("numbers", "budget"));
    drawerOption(panel, "Learn about randomness", "menu_simulate", () -> go("lab", "simulate"));
    drawerSection(panel, "HELP AND DATA");
    drawerOption(panel, "User manual library", "menu_library", () -> go("home", "library"));
    drawerOption(panel, "Frequently asked questions", "menu_faq", () -> go("home", "faq"));
    drawerOption(panel, "Update results and settings", "menu_settings", () -> go("home", "settings"));
    note(panel, "18+ only. Past draws cannot predict future winning numbers.");
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.addView(panel);
    scroll.setBackgroundColor(0xFF071F2D);
    int width = Math.min(dp(340), getResources().getDisplayMetrics().widthPixels - dp(36));
    drawer = new PopupWindow(scroll, width, -1, true);
    drawer.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF071F2D));
    drawer.setOutsideTouchable(true);
    drawer.setElevation(dp(20));
    drawer.setOnDismissListener(() -> drawer = null);
    drawer.showAtLocation(root, Gravity.START | Gravity.TOP, 0, 0);
  }

  private void drawerSection(LinearLayout parent, String title) {
    TextView t = text(title, 12, GREEN, true);
    t.setPadding(dp(4), dp(16), 0, dp(3));
    add(parent, t);
  }

  private void drawerOption(LinearLayout parent, String label, String tag, Runnable route) {
    TextView b = button(label, false, route);
    b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    b.setContentDescription(label);
    b.setTag(tag);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(6);
    parent.addView(b, lp);
  }

  private static String simpleTip(String key) {
    switch (key) {
      case "check": return "Enter your numbers and choose the draw you want to check.";
      case "generate": return "Get a random set of numbers, then save it if you like.";
      case "search": return "Find out whether your numbers appeared in the saved draw history.";
      case "compare": return "Look at up to six saved lines side by side.";
      case "frequency": return "How often did each number show up in these past draws?";
      case "hotcold": return "Most and least seen in the selected past draws—not future tips.";
      case "gaps": return "See the last time each number appeared.";
      case "pairs": return "Find which numbers appeared together in past draws.";
      case "patterns": return "Explore simple facts about each saved draw.";
      case "wheel": return "See all complete lines from a larger set before considering the cost.";
      case "coverage": return "Find repeated lines and shared numbers.";
      case "odds": return "Understand your mathematical chance, not a prediction.";
      case "budget": return "Work out a limit before spending anything.";
      case "simulate": return "An experiment with imaginary draws, not actual results.";
      case "library": return "Short lessons. Everything here works offline.";
      case "faq": return "Tap a question to read its answer.";
      case "chapter": return "A clear, step-by-step explanation.";
      case "settings": return "Refresh results, manage downloaded data and backups.";
      default: return "";
    }
  }

  private void userLibrary() {
    note(content, "Start with any topic. No internet or sign-in needed.");
    for (int i = 0; i < HelpLibrary.CHAPTERS.length; i++) {
      final int chapter = i;
      String[] item = HelpLibrary.CHAPTERS[i];
      LinearLayout panel = card();
      TextView open = button((i + 1) + ". " + item[0] + "  ›", false,
          () -> openChapter(chapter));
      open.setTag("chapter_" + i);
      add(panel, open);
      note(panel, item[1]);
      add(content, panel);
    }
    primary(content, "Questions & answers", "library_faq", () -> go("home", "faq"));
  }

  private void openChapter(int index) {
    chapterIndex = index;
    go("home", "chapter");
  }

  private void userChapter() {
    if (chapterIndex < 0 || chapterIndex >= HelpLibrary.CHAPTERS.length) {
      note(content, "Choose a topic in the User manual library.");
      return;
    }
    String[] topic = HelpLibrary.CHAPTERS[chapterIndex];
    add(content, text(topic[0], 22, GREEN, true));
    LinearLayout panel = card();
    note(panel, topic[2]);
    add(content, panel);
    if (chapterIndex + 1 < HelpLibrary.CHAPTERS.length)
      primary(content, "Next: " + HelpLibrary.CHAPTERS[chapterIndex + 1][0],
          "chapter_next", () -> openChapter(chapterIndex + 1));
    add(content, button("Back to all topics", false, () -> go("home", "library")));
  }

  private void userFaq() {
    note(content, "Simple answers using no internet. For help with a tool, read the User manual.");
    for (int i = 0; i < HelpLibrary.FAQ.length; i++) {
      final int index = i;
      TextView b = button(HelpLibrary.FAQ[i][0] + "  ›", false,
          () -> alert(HelpLibrary.FAQ[index][0], HelpLibrary.FAQ[index][1]));
      b.setTag("faq_" + i);
      add(content, b);
    }
    primary(content, "Open the user manual", "faq_manual", () -> go("home", "library"));
  }

  private void gameRow() {
    TextView b = button(game.title + "  ▾", false, this::chooseGame);
    b.setTag("choose_game");
    add(content, b);
  }

  private void chooseGame() {
    List<Game> games = Arrays.asList(Game.values());
    String[] labels = new String[games.size()];
    for (int i = 0; i < labels.length; i++)
      labels[i] = games.get(i).title + (games.get(i).active() ? "" : " · archive only");
    new AlertDialog.Builder(this)
        .setTitle("Choose a game")
        .setSingleChoiceItems(
            labels,
            game.ordinal(),
            (d, i) -> {
              d.dismiss();
              switchGame(games.get(i));
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void switchGame(Game g) {
    if (game != g) {
      game = g;
      store.preferences().edit().putString("game", g.name()).apply();
      draft = store.draft(g);
      working = null;
      generated.clear();
      loadGenerated();
      ruleId = RuleSet.current(g).id;
      checkDate = defaultDate();
      query = "";
      dateFrom = dateTo = "";
      report = csv = "";
    }
    render();
  }

  private LocalDate defaultDate() {
    List<Draw> ds = store.draws(game, 1);
    if (!ds.isEmpty()) return ds.get(0).date;
    LocalDate d = game.active() ? today() : RuleSet.CURRENT_START.minusDays(1);
    while (!game.scheduled(d)) d = d.minusDays(1);
    return d;
  }

  private void periodRow() {
    RuleSet r = RuleSet.forId(game, ruleId);
    two(
        content,
        button(
            (r.id.equals("2026-06") ? "Current rules" : "Sep 2025–May 2026") + " ▾",
            false,
            () -> {
              String[] options =
                  game.active() && game != Game.MAX5 && game != Game.XTRA
                      ? new String[] {
                        "Current rules · from June 2026", "Older rules · Sep 2025–May 2026"
                      }
                      : new String[] {
                        game.active()
                            ? "Current rules · from June 2026"
                            : "Older rules · Sep 2025–May 2026"
                      };
              new AlertDialog.Builder(this)
                  .setTitle("Choose a rule period")
                  .setItems(
                      options,
                      (d, i) -> {
                        ruleId = (options[i].startsWith("Current") ? "2026-06" : "2025-09");
                        dateFrom = dateTo = "";
                        report = csv = "";
                        render();
                      })
                  .show();
            }),
        button(
            (window == 0 ? "All available" : "Last " + window) + " ▾",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Draws to examine")
                    .setItems(
                        new String[] {
                          "Last 20", "Last 50", "Last 100", "All available in this era"
                        },
                        (d, i) -> {
                          window = new int[] {20, 50, 100, 0}[i];
                          render();
                        })
                    .show()));
    note(content, r.description() + ". Older and current rules are analysed separately.");
  }

  private List<Draw> selectedDraws() {
    List<Draw> a = ToolEngine.eligible(game, ruleId, store.draws(game, 0), 0);
    if (!dateFrom.isEmpty()) a.removeIf(d -> d.date.isBefore(LocalDate.parse(dateFrom)));
    if (!dateTo.isEmpty()) a.removeIf(d -> d.date.isAfter(LocalDate.parse(dateTo)));
    return window > 0 && a.size() > window ? new ArrayList<>(a.subList(0, window)) : a;
  }

  private String sourceLabel(Draw d) {
    return d.verification.equals("feed")
        ? "Downloaded result · confirm with operator"
        : d.verification.equals("bundled archive")
            ? "Saved offline history · confirm with operator"
            : "Imported / entered · confirm with operator";
  }

  private String coverageText(List<Draw> ds) {
    if (ds.isEmpty())
      return "No draw records available for this game and period. Coverage is unknown.";
    int gaps = ToolEngine.missingScheduled(game, ds);
    return ds.size()
        + " available draws · "
        + DATE.format(ds.get(ds.size() - 1).date)
        + " to "
        + DATE.format(ds.get(0).date)
        + "\n"
        + gaps
        + " scheduled dates absent within this span; exceptions are not independently verified."
        + " These are imported records, not a certified complete archive.";
  }

  private boolean requireData(List<Draw> ds, LinearLayout p) {
    note(p, coverageText(ds));
    if (!ds.isEmpty()) return true;
    add(p, button("Add draw results", false, () -> go(tab, "settings")));
    return false;
  }

  private void home() {
    heading("Welcome to LottoLab", "One simple step at a time. Choose a game to begin.");
    gameRow();
    List<Draw> history = store.draws(game, 0);
    SmartInsights insight = SmartInsights.analyze(game, history, 30);
    LinearLayout stats = card();
    add(stats, text("AI Lotto Stats", 22, GREEN, true));
    note(stats, "Smart facts from past results stored on your phone. These are explainable"
        + " counts—not AI predictions or tips for winning.");
    if (insight == null) {
      note(stats, "No stored draws for this game. Update results when online or add a published draw.");
    } else {
      add(stats, text(insight.drawCount + " recent available draws", 19, TEXT, true));
      note(stats, insight.firstDate + " to " + insight.lastDate
          + " · same game and number rules only");
      LinearLayout p = row();
      LinearLayout most = card();
      add(most, text("MOST SEEN", 12, MUTED, true));
      add(most, text(String.format(Locale.ROOT, "%02d", insight.mostNumber), 30, GREEN, true));
      note(most, insight.mostCount + " of " + insight.drawCount + " draws");
      LinearLayout least = card();
      add(least, text("LEAST SEEN", 12, MUTED, true));
      add(least, text(String.format(Locale.ROOT, "%02d", insight.leastNumber), 30, ORANGE, true));
      note(least, insight.leastCount + " of " + insight.drawCount + " draws");
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      lp.rightMargin = dp(4);
      p.addView(most, lp);
      lp = new LinearLayout.LayoutParams(0, -2, 1);
      lp.leftMargin = dp(4);
      p.addView(least, lp);
      add(stats, p);
      note(stats, "Last stored draw: " + insight.lastOdd + " odd, " + insight.lastEven
          + " even numbers · total " + insight.lastTotal
          + (insight.repeatsFromPrevious < 0 ? "" : " · " + insight.repeatsFromPrevious
              + " repeated from the previous available draw"));
      add(stats, button("Understand these stats  ›", false, () -> go("lab", "hotcold")));
      add(stats, button("Explore more number patterns  ›", false,
          () -> go("lab", "patterns")));
    }
    add(content, stats);

    LinearLayout latest = card();
    if (history.isEmpty()) {
      add(latest, text("Latest result not stored yet", 19, TEXT, true));
      note(latest, "The app can still make random number selections. Connect to the internet"
          + " for updates, or enter published numbers.");
      add(latest, button("Manage draw results", false, () -> go("home", "settings")));
    } else {
      Draw d = history.get(0);
      add(latest, text("Latest saved result", 18, TEXT, true));
      add(latest, text(DATE.format(d.date), 16, GREEN, true));
      add(latest, balls(d.numbers, d.powerBall, d.bonus, null));
      note(latest, sourceLabel(d));
      add(latest, button("See this result  ›", false, () -> drawDetail(d, null)));
    }
    add(content, latest);

    add(content, text("What would you like to do?", 22, TEXT, true));
    primary(content, "Check my numbers", "home_check", () -> go("lab", "check"));
    add(content, button("Generate numbers", false, () -> go("lab", "generate")));
    add(content, button("See past results", false, () -> tab("results")));
    add(content, button("My saved numbers", false, () -> tab("numbers")));

    LinearLayout tip = card();
    add(tip, text("Need a hand?", 18, GREEN, true));
    note(tip, "The easy-to-follow guide explains every feature, with answers to common questions.");
    add(tip, button("Read the user manual  ›", false, () -> go("home", "library")));
    add(tip, button("Frequently asked questions  ›", false, () -> go("home", "faq")));
    add(content, tip);
    note(content, "18+. Past results do not change the chance of a future fair draw.");
  }

  private View balls(int[] numbers, int pb, int bonus, int[] matched) {
    LinearLayout all = column();
    GridLayout grid = new GridLayout(this);
    grid.setColumnCount(3);
    int cols = numbers.length <= 5 ? 3 : 3;
    for (int i = 0; i < numbers.length; i++) {
      int n = numbers[i];
      boolean hit = matched != null && Arrays.stream(matched).anyMatch(x -> x == n);
      TextView t =
          text(
              String.format(Locale.ROOT, "%02d", n) + (hit ? " ✓" : ""),
              21,
              hit ? INK : TEXT,
              true);
      t.setGravity(Gravity.CENTER);
      t.setMinHeight(dp(49));
      t.setBackground(shape(hit ? GREEN : 0xFF204755, hit ? GREEN : 0xFF3B6974, 24));
      t.setContentDescription("Main number " + n + (hit ? ", matched" : ""));
      GridLayout.LayoutParams lp =
          new GridLayout.LayoutParams(GridLayout.spec(i / cols), GridLayout.spec(i % cols, 1f));
      lp.width = 0;
      lp.height = -2;
      lp.setMargins(dp(3), dp(3), dp(3), dp(3));
      grid.addView(t, lp);
    }
    all.addView(grid, new LinearLayout.LayoutParams(-1, -2));
    if (pb > 0 || bonus > 0) {
      TextView extra =
          text(
              (pb > 0
                  ? "PowerBall  " + String.format(Locale.ROOT, "%02d", pb)
                  : "Bonus  " + String.format(Locale.ROOT, "%02d", bonus)),
              16,
              ORANGE,
              true);
      extra.setPadding(dp(7), dp(9), 0, dp(4));
      all.addView(extra);
    }
    return all;
  }

  private void toolCard(String name, String description, String destination, String tag) {
    LinearLayout p = card();
    TextView b = button(name + "  ›", false, () -> go("lab", destination));
    b.setTag(tag);
    add(p, b);
    note(p, description);
    add(content, p);
  }

  private void lab() {
    heading(
        "What would you like to do?", "Start with a task. Advanced options stay inside each tool.");
    String[] names = {"Check my numbers", "Explore past draws", "Build my numbers", "Plan my play"},
        desc =
            {
              "Check a ticket, search a combination or compare up to six lines.",
              "See frequencies, last appearances, pairs and draw patterns.",
              "Generate unique lines, build a full system and inspect coverage.",
              "Understand the odds, estimate costs and set a spending limit."
            };
    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      LinearLayout p = card();
      TextView b =
          button(
              name + "  ›",
              false,
              () -> {
                group = name;
                go("lab", "group");
              });
      b.setTag("group_" + i);
      add(p, b);
      note(p, desc[i]);
      add(content, p);
    }
  }

  private void toolGroup() {
    heading(group, "Choose one tool to get started.");
    switch (group) {
      case "Check my numbers":
        toolCard(
            "Check a ticket", "Compare a full line with a specific draw.", "check", "tool_check");
        toolCard(
            "Search my combination",
            "Find draws containing a number, subset or full line.",
            "search",
            "tool_search");
        toolCard(
            "Compare up to six lines",
            "See each line’s history and matching dates separately.",
            "compare",
            "tool_compare");
        add(content, button("Track my dated tickets", false, () -> go("numbers", "tickets")));
        break;
      case "Explore past draws":
        toolCard(
            "Number frequencies",
            "Count how often each main number appeared.",
            "frequency",
            "tool_frequency");
        toolCard(
            "Hot and cold numbers",
            "Compare most and least frequent numbers in one period.",
            "hotcold",
            "tool_hotcold");
        toolCard(
            "Last seen & draw gaps",
            "Find the most recent appearance of each number.",
            "gaps",
            "tool_gaps");
        toolCard(
            "Pairs and triples",
            "Count draws containing chosen pairs and groups of three.",
            "pairs",
            "tool_pairs");
        toolCard(
            "Draw patterns",
            "Inspect sums, balance, consecutive numbers and repeats.",
            "patterns",
            "tool_patterns");
        break;
      case "Build my numbers":
        toolCard(
            "Generate numbers",
            "Quick Pick or personalise locked and excluded numbers.",
            "generate",
            "tool_generate");
        toolCard(
            "Build a full system",
            "Expand your chosen pool into every complete line.",
            "wheel",
            "tool_wheel");
        toolCard(
            "Check line coverage",
            "Inspect duplicate lines and shared numbers.",
            "coverage",
            "tool_coverage");
        break;
      default:
        toolCard(
            "Explore the odds",
            "See exact probabilities for possible match categories.",
            "odds",
            "tool_odds");
        toolCard(
            "Plan my spending",
            "Calculate entry costs and compare with a budget.",
            "budget",
            "tool_budget");
        toolCard(
            "Explore randomness",
            "Simulate independent draws and inspect natural variation.",
            "simulate",
            "tool_simulate");
    }
  }

  private interface EntryConsumer {
    void accept(Entry e);
  }

  private void editEntry(Entry existing, boolean allowSubset, String era, EntryConsumer accept) {
    Game editorGame = game;
    RuleSet r = RuleSet.forId(editorGame, era);
    LinearLayout pane = column();
    pane.setPadding(dp(18), dp(8), dp(18), dp(8));
    pane.setBackgroundColor(BG);
    note(
        pane,
        "Choose "
            + r.picks
            + " main numbers from 1–"
            + r.pool
            + (r.powerPool > 0 ? ", plus a separate PowerBall." : "."));
    EditText paste =
        input(
            pane,
            "Your main numbers",
            "Tap below, or paste 1 2 3…",
            existing == null ? "" : ArchiveStore.numbers(existing.numbers),
            false);
    paste.setTag("editor_paste");
    paste.setContentDescription("Main numbers input");
    TextView count = text("0 selected", 14, GREEN, true);
    count.setTag("selection_count");
    add(pane, count);
    CheckBox subset = new CheckBox(this);
    subset.setText("Research fewer than " + r.picks + " main numbers");
    subset.setTextColor(TEXT);
    subset.setTextSize(14);
    subset.setMinHeight(dp(48));
    subset.setChecked(existing != null && existing.subset);
    if (allowSubset) add(pane, subset);
    GridLayout grid = new GridLayout(this);
    grid.setColumnCount(5);
    List<TextView> cells = new ArrayList<>();
    Set<Integer> chosen = new TreeSet<>();
    Runnable update =
        () -> {
          try {
            int[] nums = Entry.parseNumbers(paste.getText().toString());
            chosen.clear();
            for (int n : nums) chosen.add(n);
            count.setText(nums.length + " of " + r.picks + " main numbers selected");
            for (int i = 0; i < cells.size(); i++) {
              boolean on = chosen.contains(i + 1);
              TextView c = cells.get(i);
              c.setTextColor(on ? INK : TEXT);
              c.setBackground(shape(on ? GREEN : 0xFF1B4050, on ? GREEN : 0xFF376373, 24));
              c.setSelected(on);
              c.setContentDescription("Number " + (i + 1) + (on ? ", selected" : ""));
            }
          } catch (IllegalArgumentException e) {
            count.setText(e.getMessage());
          }
        };
    for (int n = 1; n <= r.pool; n++) {
      int value = n;
      TextView cell = text(Integer.toString(n), 16, TEXT, true);
      cell.setGravity(Gravity.CENTER);
      cell.setMinHeight(dp(48));
      cell.setTag("num_" + n);
      cell.setFocusable(true);
      cell.setOnClickListener(
          v -> {
            if (chosen.contains(value)) chosen.remove(value);
            else if (chosen.size() < r.picks) chosen.add(value);
            else {
              paste.setError("Choose at most " + r.picks + " main numbers.");
              return;
            }
            paste.setText(
                ArchiveStore.numbers(chosen.stream().mapToInt(Integer::intValue).toArray()));
            update.run();
          });
      GridLayout.LayoutParams lp =
          new GridLayout.LayoutParams(
              GridLayout.spec((n - 1) / 5), GridLayout.spec((n - 1) % 5, 1f));
      lp.width = 0;
      lp.height = -2;
      lp.setMargins(dp(2), dp(3), dp(2), dp(3));
      grid.addView(cell, lp);
      cells.add(cell);
    }
    add(pane, grid);
    EditText pb = new EditText(this);
    if (r.powerPool > 0) {
      pb =
          input(
              pane,
              "Separate PowerBall",
              "1–" + r.powerPool + (allowSubset ? "; blank for main-only research" : ""),
              existing != null && existing.powerBall > 0
                  ? Integer.toString(existing.powerBall)
                  : "",
              true);
      pb.setTag("editor_powerball");
      pb.setContentDescription("PowerBall input");
    }
    final EditText power = pb;
    TextView error = text("", 14, ORANGE, false);
    error.setTag("editor_error");
    add(pane, error);
    add(
        pane,
        button(
            "Choose a random full line",
            false,
            () -> {
              Entry e =
                  ToolEngine.generate(editorGame, r.id, 1, new int[0], new int[0], 0, -1, random)
                      .get(0);
              paste.setText(ArchiveStore.numbers(e.numbers));
              if (r.powerPool > 0) power.setText(Integer.toString(e.powerBall));
              subset.setChecked(false);
              update.run();
            }));
    paste.addTextChangedListener(watcher(update));
    update.run();
    ScrollView scroll = new ScrollView(this);
    scroll.addView(pane);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(existing == null ? "Choose your numbers" : "Edit your numbers")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Use numbers", null)
            .create();
    dialog.setOnShowListener(
        v -> {
          android.view.Window window = dialog.getWindow();
          // Fit inside visible display bounds, including system bars and large-font devices.
          android.graphics.Rect visible = new android.graphics.Rect();
          window.getDecorView().getWindowVisibleDisplayFrame(visible);
          int height = visible.height() > 0
              ? visible.height() : getResources().getDisplayMetrics().heightPixels;
          window.setSoftInputMode(
              android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                  | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
          window.setLayout(-1, (int) (height * .80f));
          TextView use = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
          use.setTag("editor_use");
          use.setOnClickListener(
              b -> {
                try {
                  int p =
                      power.getText().toString().trim().isEmpty()
                          ? 0
                          : Integer.parseInt(power.getText().toString());
                  Entry e =
                      new Entry(
                          editorGame,
                          Entry.parseNumbers(paste.getText().toString()),
                          p,
                          allowSubset && subset.isChecked(),
                          existing == null ? "" : existing.name,
                          r.id);
                  accept.accept(e);
                  dialog.dismiss();
                  hideKeyboard();
                } catch (Exception e) {
                  error.setText(
                      e.getMessage() == null ? "Please check your numbers." : e.getMessage());
                  paste.setError("Review the selection below.");
                }
              });
        });
    dialog.show();
  }

  private TextWatcher watcher(Runnable update) {
    return new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      public void onTextChanged(CharSequence s, int start, int before, int count) {
        update.run();
      }

      public void afterTextChanged(Editable e) {}
    };
  }

  private void datePicker(LocalDate value, java.util.function.Consumer<LocalDate> accept) {
    DatePickerDialog d =
        new DatePickerDialog(
            this,
            (v, y, m, day) -> accept.accept(LocalDate.of(y, m + 1, day)),
            value.getYear(),
            value.getMonthValue() - 1,
            value.getDayOfMonth());
    d.show();
  }

  private void selectionCard(boolean subset) {
    LinearLayout p = card();
    add(p, text("Your numbers", 17, TEXT, true));
    if (working == null || working.game != game) {
      note(p, "Choose your own numbers using the number grid.");
    } else {
      add(p, balls(working.numbers, working.powerBall, 0, null));
      note(
          p,
          (working.subset ? "Research subset" : "Complete line")
              + " · "
              + RuleSet.forId(game, working.ruleId).description());
    }
    TextView b =
        button(
            working == null ? "Choose numbers" : "Edit numbers",
            false,
            () ->
                editEntry(
                    working,
                    subset,
                    ruleId,
                    e -> {
                      working = e;
                      render();
                    }));
    b.setTag("choose_numbers");
    add(p, b);
    add(content, p);
  }

  private void checker() {
    note(content, "Compare your full line with the published result for one draw.");
    gameRow();
    add(
        content,
        button(
            "Draw date: " + DATE.format(checkDate) + "  ▾",
            false,
            () ->
                datePicker(
                    checkDate,
                    d -> {
                      try {
                        ruleId = RuleSet.forDate(game, d).id;
                        checkDate = d;
                        report = "";
                        render();
                      } catch (Exception e) {
                        alert("Date unavailable", e.getMessage());
                      }
                    })));
    note(content, RuleSet.forDate(game, checkDate).description() + " for this date.");
    selectionCard(false);
    primary(content, "Check ticket", "check_ticket", this::runCheck);
    output = column();
    add(content, output);
    if (!report.isEmpty()) note(output, report);
  }

  private void runCheck() {
    output.removeAllViews();
    try {
      if (working == null) throw new IllegalArgumentException("Choose your numbers first.");
      Entry e =
          new Entry(
              game,
              working.numbers,
              working.powerBall,
              false,
              working.name,
              RuleSet.forDate(game, checkDate).id);
      Draw draw = null;
      for (Draw d : store.draws(game, 0))
        if (d.date.equals(checkDate)) {
          draw = d;
          break;
        }
      if (draw == null) {
        note(
            output,
            "No result is available for "
                + DATE.format(checkDate)
                + ". This is unknown, not a losing ticket.");
        add(output, button("Enter published draw numbers", false, () -> addDraw(checkDate)));
        return;
      }
      working = e;
      int matches = AnalysisEngine.matches(e.numbers, draw.numbers);
      boolean bonus = draw.bonus > 0 && Arrays.binarySearch(e.numbers, draw.bonus) >= 0;
      LinearLayout p = card();
      add(p, text(matches + " of " + game.picks + " main numbers matched", 23, GREEN, true));
      add(p, balls(draw.numbers, draw.powerBall, draw.bonus, e.numbers));
      String extra =
          game.hasPowerBall()
              ? "PowerBall matched: " + (e.powerBall == draw.powerBall ? "Yes" : "No")
              : game.hasBonus()
                  ? "Bonus matched: "
                      + (draw.bonus == 0 ? "Unknown — bonus not supplied" : bonus ? "Yes" : "No")
                  : "No separate ball in this game.";
      note(p, extra);
      note(p, sourceLabel(draw));
      note(
          p,
          "Prize division and money are not confirmed by this build. Check the official draw before"
              + " making a claim.");
      add(output, p);
      Draw result = draw;
      report =
          reportHeader("Ticket check", Collections.singletonList(draw))
              + "\nYour line: "
              + e.display()
              + "\nMain matches: "
              + matches
              + " / "
              + game.picks
              + "\n"
              + extra
              + "\nPublished main numbers: "
              + ArchiveStore.numbers(draw.numbers)
              + "\nBonus: "
              + draw.bonus
              + "; PowerBall: "
              + draw.powerBall
              + "\n"
              + sourceDescription(draw)
              + "\nPrize amount: unavailable.";
      add(output, button("View source & draw details", false, () -> drawDetail(result, e)));
      add(output, button("Save as a dated ticket", false, () -> ticketDialog(e, checkDate)));
      exportButtons(output);
    } catch (Exception ex) {
      note(output, ex.getMessage() == null ? "Check your selection." : ex.getMessage());
    }
  }

  private void searchTool() {
    note(
        content,
        "Find draws containing your selected main numbers. For example, search a pair or a complete"
            + " line.");
    gameRow();
    periodRow();
    selectionCard(true);
    primary(
        content,
        "Search available history",
        "search_history",
        () -> {
          output.removeAllViews();
          List<Draw> ds = selectedDraws();
          if (!requireData(ds, output)) return;
          try {
            if (working == null)
              throw new IllegalArgumentException("Choose numbers to search first.");
            Entry e =
                new Entry(
                    game, working.numbers, working.powerBall, working.subset, working.name, ruleId);
            AnalysisEngine.Result r = AnalysisEngine.analyse(e, ds);
            int count = e.subset ? r.containsHits : r.completeHits;
            note(
                output,
                (e.subset
                        ? "All selected main numbers appeared together in "
                        : "Exact full line appeared in ")
                    + count
                    + " of "
                    + r.checked
                    + " available draws.");
            if (e.subset && e.powerBall > 0)
              note(
                  output,
                  "This subset count concerns main numbers. Your selected PowerBall is not part of"
                      + " this count.");
            List<Draw> found = new ArrayList<>();
            for (Draw d : ds)
              if (AnalysisEngine.matches(e.numbers, d.numbers) == e.numbers.length
                  && (e.subset || e.powerBall == 0 || e.powerBall == d.powerBall)) found.add(d);
            for (Draw d : found.subList(0, Math.min(30, found.size()))) drawCard(output, d, e);
            if (found.size() > 30)
              note(
                  output,
                  "Showing the first 30 matching dates; the CSV contains all matching records.");
            report =
                reportHeader("Combination search", ds)
                    + "\nLine: "
                    + e.display()
                    + "\nDefinition: "
                    + (e.subset
                        ? "contains all chosen main numbers"
                        : "exact complete entry, including selected PowerBall")
                    + "\nMatches: "
                    + count
                    + " of "
                    + r.checked
                    + "\n";
            for (Draw d : found) report += d.date + " " + ArchiveStore.numbers(d.numbers) + "\n";
            csv = CsvCodec.write(found);
            exportButtons(output);
          } catch (Exception ex) {
            note(output, ex.getMessage());
          }
        });
    output = column();
    add(content, output);
  }

  private void compare() {
    note(
        content,
        "Add up to six lines. Each line is checked separately against the selected available"
            + " history.");
    gameRow();
    periodRow();
    for (int i = 0; i < draft.size(); i++) {
      int index = i;
      Entry e = draft.get(i);
      LinearLayout p = card();
      add(p, text("Line " + (char) ('A' + i) + (e.subset ? " · subset" : ""), 17, GREEN, true));
      add(p, balls(e.numbers, e.powerBall, 0, null));
      two(
          p,
          button(
              "Edit",
              false,
              () ->
                  editEntry(
                      e,
                      true,
                      ruleId,
                      x -> {
                        draft.set(index, x);
                        saveDraft();
                      })),
          button(
              "Remove",
              false,
              () -> {
                draft.remove(index);
                saveDraft();
              }));
      add(p, button("Save favourite", false, () -> saveFavourite(e)));
      add(content, p);
    }
    if (draft.size() < 6) {
      TextView b =
          button(
              "+ Add a line",
              false,
              () ->
                  editEntry(
                      null,
                      true,
                      ruleId,
                      e -> {
                        draft.add(e);
                        saveDraft();
                      }));
      b.setTag("add_compare_line");
      add(content, b);
    }
    primary(
        content,
        "Compare these lines",
        "compare_lines",
        () -> {
          output.removeAllViews();
          List<Draw> ds = selectedDraws();
          if (!requireData(ds, output)) return;
          if (draft.isEmpty()) {
            note(output, "Add at least one line first.");
            return;
          }
          report = reportHeader("Line comparison", ds) + "\n";
          csv = "line,game,numbers,powerball,definition,eligible_draws,matches,best_main_match\n";
          for (int i = 0; i < draft.size(); i++) {
            Entry e = draft.get(i);
            AnalysisEngine.Result r = AnalysisEngine.analyse(e, ds);
            LinearLayout p = card();
            add(p, text("Line " + (char) ('A' + i), 20, GREEN, true));
            if (r.checked == 0) {
              note(p, "No eligible draws. This line belongs to a different rule period.");
              add(output, p);
              continue;
            }
            int hits = e.subset ? r.containsHits : r.completeHits;
            String summary =
                (e.subset ? "Contains the main-number subset: " : "Exact full line: ")
                    + hits
                    + " of "
                    + r.checked
                    + " draws\nBest main match: "
                    + r.best
                    + " of "
                    + e.numbers.length;
            note(p, summary);
            StringBuilder dist = new StringBuilder("Main-match counts\n");
            for (int m = r.distribution.length - 1; m >= 0; m--)
              dist.append(m).append(" matched: ").append(r.distribution[m]).append('\n');
            note(p, dist.toString());
            List<Draw> best = new ArrayList<>();
            for (Draw d : ds)
              if (AnalysisEngine.matches(e.numbers, d.numbers) == r.best) best.add(d);
            add(
                p,
                button(
                    "See best-match dates",
                    false,
                    () -> showDraws("Best main-match dates", best, e)));
            add(output, p);
            report +=
                "Line "
                    + (char) ('A' + i)
                    + ": "
                    + e.display()
                    + "\n"
                    + summary
                    + "\n"
                    + dist
                    + "\n";
            for (Draw d : best) report += "Best date: " + d.date + "\n";
            csv +=
                (char) ('A' + i)
                    + ","
                    + game.name()
                    + ","
                    + CsvCodec.quote(ArchiveStore.numbers(e.numbers))
                    + ","
                    + e.powerBall
                    + ","
                    + (e.subset ? "main subset" : "exact full line")
                    + ","
                    + r.checked
                    + ","
                    + hits
                    + ","
                    + r.best
                    + "\n";
          }
          exportButtons(output);
        });
    output = column();
    add(content, output);
  }

  private void saveDraft() {
    store.draft(game, draft);
    report = csv = "";
    render();
  }

  private void history() {
    heading(
        "Browse the draw archive",
        "Search published numbers and inspect where each record came from.");
    gameRow();
    periodRow();
    EditText search = input(content, "Main numbers to find", "Optional: 4 12", query, false);
    search.setTag("history_query");
    add(
        content,
        button(
            dateFrom.isEmpty()
                ? "Filter by date  ▾"
                : dateFrom + (dateTo.equals(dateFrom) ? "" : " to " + dateTo) + "  ▾",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Filter draw dates")
                    .setItems(
                        new String[] {"One date", "Custom date range", "Clear date filter"},
                        (d, i) -> {
                          if (i == 2) {
                            dateFrom = dateTo = "";
                            render();
                          } else if (i == 0)
                            datePicker(
                                defaultDate(),
                                x -> {
                                  dateFrom = dateTo = x.toString();
                                  try {
                                    ruleId = RuleSet.forDate(game, x).id;
                                    render();
                                  } catch (Exception ex) {
                                    dateFrom = dateTo = "";
                                    alert("Date unavailable", ex.getMessage());
                                  }
                                });
                          else
                            datePicker(
                                RuleSet.forId(game, ruleId).start,
                                x ->
                                    datePicker(
                                        today(),
                                        y -> {
                                          if (y.isBefore(x)) {
                                            alert(
                                                "Check dates",
                                                "The end date must be on or after the start date.");
                                            return;
                                          }
                                          dateFrom = x.toString();
                                          dateTo = y.toString();
                                          render();
                                        }));
                        })
                    .show()));
    LinearLayout list = column();
    add(content, list);
    Runnable update =
        () -> {
          query = search.getText().toString();
          list.removeAllViews();
          List<Draw> ds = selectedDraws();
          if (!requireData(ds, list)) return;
          try {
            int[] nums = Entry.parseNumbers(query);
            Set<Integer> seen = new HashSet<>();
            for (int n : nums)
              if (n < 1 || n > RuleSet.forId(game, ruleId).pool || !seen.add(n))
                throw new IllegalArgumentException(
                    "Use distinct numbers within the selected rule range.");
            List<Draw> found = new ArrayList<>();
            for (Draw d : ds)
              if (AnalysisEngine.matches(nums, d.numbers) == nums.length) found.add(d);
            note(
                list,
                found.size()
                    + " matching records in this window. Search excludes bonus and PowerBall.");
            for (Draw d : found.subList(0, Math.min(visible, found.size())))
              drawCard(list, d, null);
            if (found.size() > visible)
              add(
                  list,
                  button(
                      "Show more draws",
                      false,
                      () -> {
                        visible += 30;
                        search.setText(query);
                      }));
            if (found.isEmpty())
              note(
                  list,
                  "No matching records in this available window. The archive may be incomplete.");
          } catch (Exception ex) {
            search.setError(ex.getMessage());
          }
        };
    search.addTextChangedListener(watcher(update));
    update.run();
  }

  private void drawCard(LinearLayout parent, Draw d, Entry e) {
    LinearLayout p = card();
    add(p, text(DATE.format(d.date), 17, TEXT, true));
    add(p, balls(d.numbers, d.powerBall, d.bonus, e == null ? null : e.numbers));
    note(p, sourceLabel(d));
    add(p, button("Draw details  ›", false, () -> drawDetail(d, e)));
    add(parent, p);
  }

  private String sourceDescription(Draw d) {
    return sourceLabel(d)
        + "\nSource: "
        + (d.sourceUrl.isEmpty() ? "not supplied" : d.sourceUrl)
        + "\nImported/downloaded: "
        + (d.fetchedAt.isEmpty() ? "not recorded" : d.fetchedAt)
        + "\nRule period: "
        + d.ruleId;
  }

  private void drawDetail(Draw d, Entry e) {
    String msg =
        d.game.title
            + "\n"
            + DATE.format(d.date)
            + "\n\nMain numbers: "
            + ArchiveStore.numbers(d.numbers)
            + (d.bonus > 0 ? "\nBonus: " + d.bonus : "")
            + (d.powerBall > 0 ? "\nPowerBall: " + d.powerBall : "")
            + "\n\n"
            + sourceDescription(d)
            + "\nArchived corrections: "
            + store.revisionCount(d)
            + "\n\nPrize divisions and payouts are not connected.";
    new AlertDialog.Builder(this)
        .setTitle("Draw record")
        .setMessage(msg)
        .setPositiveButton(
            "Check my numbers",
            (v, i) -> {
              game = d.game;
              checkDate = d.date;
              ruleId = d.ruleId;
              if (e != null) working = e;
              go("lab", "check");
            })
        .setNeutralButton(
            "Open source",
            (v, i) ->
                openUrl(d.sourceUrl.isEmpty() ? "https://www.nationallottery.co.za/" : d.sourceUrl))
        .setNegativeButton("Close", null)
        .show();
  }

  private void showDraws(String title, List<Draw> ds, Entry e) {
    if (ds.isEmpty()) {
      alert(title, "No matching records in this available history.");
      return;
    }
    String[] labels = new String[ds.size()];
    for (int i = 0; i < labels.length; i++)
      labels[i] = DATE.format(ds.get(i).date) + " · " + ArchiveStore.numbers(ds.get(i).numbers);
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(labels, (v, i) -> drawDetail(ds.get(i), e))
        .setNegativeButton("Close", null)
        .show();
  }

  private void statistics() {
    note(
        content,
        "These tools describe the available archive. They do not forecast future numbers.");
    gameRow();
    periodRow();
    List<Draw> ds = selectedDraws();
    if (!requireData(ds, content)) return;
    RuleSet rules = RuleSet.forId(game, ruleId);
    report = reportHeader(pageTitle(), ds) + "\n";
    if (page.equals("pairs")) {
      note(
          content,
          "Choose 2–"
              + game.picks
              + " numbers to inspect their pairs and triples. Extra balls are excluded.");
      selectionCard(true);
      primary(
          content,
          "Count pairs & triples",
          "count_pairs",
          () -> {
            output.removeAllViews();
            try {
              if (working == null || working.numbers.length < 2)
                throw new IllegalArgumentException("Choose at least two main numbers.");
              new Entry(game, working.numbers, 0, true, "", ruleId);
              report = reportHeader("Pairs and triples", ds) + "\n";
              csv = "group,count,draws,percentage\n";
              int[] a = working.numbers;
              for (int size = 2; size <= 3; size++) {
                add(output, text(size == 2 ? "Pairs" : "Triples", 20, GREEN, true));
                for (int i = 0; i < a.length; i++)
                  for (int j = i + 1; j < a.length; j++) {
                    if (size == 2) subsetRow(output, new int[] {a[i], a[j]}, ds);
                    else
                      for (int k = j + 1; k < a.length; k++)
                        subsetRow(output, new int[] {a[i], a[j], a[k]}, ds);
                  }
              }
              exportButtons(output);
            } catch (Exception e) {
              note(output, e.getMessage());
            }
          });
      output = column();
      add(content, output);
      return;
    }
    if (page.equals("patterns")) {
      csv = "date,sum,odd,even,low,high,consecutive_pairs,repeated_from_previous_available\n";
      double sums = 0;
      int[] oddCounts = new int[game.picks + 1];
      for (int i = 0; i < ds.size(); i++) {
        Draw d = ds.get(i);
        int sum = 0, odd = 0, low = 0, consecutive = 0;
        for (int j = 0; j < d.numbers.length; j++) {
          int n = d.numbers[j];
          sum += n;
          if (n % 2 != 0) odd++;
          if (n <= rules.pool / 2) low++;
          if (j > 0 && n == d.numbers[j - 1] + 1) consecutive++;
        }
        int repeat =
            i + 1 < ds.size() ? AnalysisEngine.matches(d.numbers, ds.get(i + 1).numbers) : -1;
        sums += sum;
        oddCounts[odd]++;
        String desc =
            d.date
                + " · Sum "
                + sum
                + " · Odd/even "
                + odd
                + "/"
                + (game.picks - odd)
                + " · Low/high "
                + low
                + "/"
                + (game.picks - low)
                + " · Consecutive pairs "
                + consecutive
                + " · Repeats "
                + (repeat < 0 ? "unknown" : repeat);
        report += desc + "\n";
        csv +=
            d.date
                + ","
                + sum
                + ","
                + odd
                + ","
                + (game.picks - odd)
                + ","
                + low
                + ","
                + (game.picks - low)
                + ","
                + consecutive
                + ","
                + (repeat < 0 ? "" : repeat)
                + "\n";
      }
      LinearLayout p = card();
      add(
          p,
          text(
              "Average draw sum: " + String.format(Locale.ENGLISH, "%.1f", sums / ds.size()),
              20,
              GREEN,
              true));
      for (int i = 0; i < oddCounts.length; i++)
        note(
            p,
            i
                + " odd + "
                + (game.picks - i)
                + " even: "
                + ToolEngine.percent(oddCounts[i], ds.size()));
      note(
          p,
          "Low numbers: 1–"
              + (rules.pool / 2)
              + ". Repeats compare adjacent available records, which may have gaps.");
      add(content, p);
      for (Draw d : ds.subList(0, Math.min(20, ds.size())))
        add(
            content,
            button(DATE.format(d.date) + " · inspect draw", false, () -> drawDetail(d, null)));
      exportButtons(content);
      return;
    }
    int[] freq = new int[rules.pool + 1], pb = new int[rules.powerPool + 1];
    for (Draw d : ds) {
      for (int n : d.numbers) freq[n]++;
      if (d.powerBall > 0) pb[d.powerBall]++;
    }
    csv = "number,main_appearances,eligible_draws,percentage,last_seen,draws_since_last_seen\n";
    List<Integer> ordered = new ArrayList<>();
    for (int n = 1; n <= rules.pool; n++) ordered.add(n);
    if (page.equals("hotcold")) {
      ordered.sort(
          (a, b) -> freq[a] == freq[b] ? Integer.compare(a, b) : Integer.compare(freq[b], freq[a]));
      add(content, text("Most frequent in this window", 20, GREEN, true));
      for (int n : ordered.subList(0, Math.min(10, ordered.size())))
        frequencyRow(content, n, freq[n], ds, false);
      add(content, text("Least frequent in this window", 20, GREEN, true));
      List<Integer> cold = new ArrayList<>(ordered);
      cold.sort(
          (a, b) -> freq[a] == freq[b] ? Integer.compare(a, b) : Integer.compare(freq[a], freq[b]));
      for (int n : cold.subList(0, Math.min(10, cold.size())))
        frequencyRow(content, n, freq[n], ds, false);
      note(
          content,
          "Ties are shown by ascending number. Frequency ranks give no future winning advantage.");
    } else for (int n : ordered) frequencyRow(content, n, freq[n], ds, page.equals("gaps"));
    // The export always contains all numbers, even when the screen shows only hot/cold leaders.
    for (int n = 1; n <= rules.pool; n++) {
      int gap = AnalysisEngine.gap(n, ds);
      String last = gap < 0 ? "not seen" : ds.get(gap).date.toString();
      report +=
          "Number "
              + n
              + ": "
              + freq[n]
              + " of "
              + ds.size()
              + "; last seen "
              + last
              + "; draws since "
              + (gap < 0 ? "unknown" : gap)
              + "\n";
      csv +=
          n
              + ","
              + freq[n]
              + ","
              + ds.size()
              + ","
              + String.format(Locale.ROOT, "%.6f", 100.0 * freq[n] / ds.size())
              + ","
              + last
              + ","
              + (gap < 0 ? "" : gap)
              + "\n";
    }
    if (game.hasPowerBall()) {
      LinearLayout p = card();
      add(p, text("Separate PowerBall frequencies", 19, ORANGE, true));
      for (int n = 1; n < pb.length; n++)
        note(p, "PowerBall " + n + ": " + ToolEngine.percent(pb[n], ds.size()));
      add(content, p);
      report += "\nSeparate PowerBall frequencies\n";
      for (int n = 1; n < pb.length; n++) report += n + ": " + pb[n] + " of " + ds.size() + "\n";
    }
    exportButtons(content);
  }

  private void frequencyRow(LinearLayout parent, int n, int count, List<Draw> ds, boolean gaps) {
    LinearLayout p = card();
    add(p, text(String.format(Locale.ROOT, "%02d", n), 21, GREEN, true));
    if (gaps) {
      int gap = AnalysisEngine.gap(n, ds);
      note(
          p,
          gap < 0
              ? "Not seen in this available window. Its true gap is unknown."
              : "Last seen "
                  + DATE.format(ds.get(gap).date)
                  + " · "
                  + gap
                  + " available draws since.");
    } else note(p, ToolEngine.percent(count, ds.size()));
    add(
        p,
        button(
            "See appearances",
            false,
            () -> {
              List<Draw> found = new ArrayList<>();
              for (Draw d : ds) if (Arrays.binarySearch(d.numbers, n) >= 0) found.add(d);
              showDraws("Number " + n + " · appearances", found, null);
            }));
    add(parent, p);
  }

  private void subsetRow(LinearLayout p, int[] nums, List<Draw> ds) {
    int n = ToolEngine.subsetCount(nums, ds);
    String label = ArchiveStore.numbers(nums) + ": " + ToolEngine.percent(n, ds.size());
    add(
        p,
        button(
            label,
            false,
            () -> {
              List<Draw> found = new ArrayList<>();
              for (Draw d : ds)
                if (AnalysisEngine.matches(nums, d.numbers) == nums.length) found.add(d);
              showDraws(ArchiveStore.numbers(nums), found, null);
            }));
    report += label + "\n";
    csv +=
        CsvCodec.quote(ArchiveStore.numbers(nums))
            + ","
            + n
            + ","
            + ds.size()
            + ","
            + String.format(Locale.ROOT, "%.6f", 100.0 * n / ds.size())
            + "\n";
  }

  private void generator() {
    note(
        content,
        "Create valid, unique lines. The default Quick Pick is random; preferences do not improve a"
            + " line’s odds.");
    gameRow();
    if (!game.active()) {
      note(content, "This game is archive-only. Select a current game to generate new lines.");
      return;
    }
    LinearLayout p = card();
    EditText count =
        input(
            p,
            "How many lines?",
            "1–100",
            store.preferences().getString("gen_count_" + game.name(), "1"),
            true);
    count.setTag("generate_count");
    CheckBox advanced = new CheckBox(this);
    advanced.setText("Advanced options");
    advanced.setChecked(store.preferences().getBoolean("generator_advanced_" + game.name(), false));
    advanced.setTextColor(TEXT);
    advanced.setMinHeight(dp(48));
    add(p, advanced);
    LinearLayout controls = column();
    controls.setVisibility(advanced.isChecked() ? View.VISIBLE : View.GONE);
    add(p, controls);
    EditText
        locked =
            input(
                controls,
                "Locked main numbers",
                "Optional: 4 12",
                store.preferences().getString("locked_" + game.name(), ""),
                false),
        excluded =
            input(
                controls,
                "Excluded main numbers",
                "Optional: 7 19",
                store.preferences().getString("excluded_" + game.name(), ""),
                false),
        odd =
            input(
                controls,
                "Number of odd main numbers",
                "Blank = unrestricted",
                store.preferences().getString("odd_" + game.name(), ""),
                true),
        power = new EditText(this);
    if (game.hasPowerBall())
      power =
          input(
              controls,
              "Locked PowerBall",
              "Blank = random",
              store.preferences().getString("fixedpb_" + game.name(), ""),
              true);
    final EditText fixed = power;
    advanced.setOnCheckedChangeListener(
        (v, on) -> {
          controls.setVisibility(on ? View.VISIBLE : View.GONE);
          store.preferences().edit().putBoolean("generator_advanced_" + game.name(), on).apply();
        });
    for (EditText e : new EditText[] {count, locked, excluded, odd, fixed})
      e.addTextChangedListener(
          watcher(
              () ->
                  store
                      .preferences()
                      .edit()
                      .putString("gen_count_" + game.name(), count.getText().toString())
                      .putString("locked_" + game.name(), locked.getText().toString())
                      .putString("excluded_" + game.name(), excluded.getText().toString())
                      .putString("odd_" + game.name(), odd.getText().toString())
                      .putString("fixedpb_" + game.name(), fixed.getText().toString())
                      .apply()));
    TextView error = text("", 14, ORANGE, false);
    add(p, error);
    primary(
        p,
        "Generate my numbers",
        "generate_numbers",
        () -> {
          try {
            int c = Integer.parseInt(count.getText().toString());
            boolean use = advanced.isChecked();
            generated =
                ToolEngine.generate(
                    game,
                    "2026-06",
                    c,
                    use ? Entry.parseNumbers(locked.getText().toString()) : new int[0],
                    use ? Entry.parseNumbers(excluded.getText().toString()) : new int[0],
                    use && !fixed.getText().toString().trim().isEmpty()
                        ? Integer.parseInt(fixed.getText().toString())
                        : 0,
                    use && !odd.getText().toString().trim().isEmpty()
                        ? Integer.parseInt(odd.getText().toString())
                        : -1,
                    random);
            store
                .preferences()
                .edit()
                .putString(
                    "generated_options_" + game.name(),
                    use
                        ? "Preferences: locked "
                            + locked.getText()
                            + "; excluded "
                            + excluded.getText()
                            + "; odd count "
                            + odd.getText()
                            + "; fixed PB "
                            + fixed.getText()
                        : "Uniform Quick Pick; no filters")
                .apply();
            saveGenerated();
            visible = 30;
            hideKeyboard();
            render();
          } catch (Exception ex) {
            error.setText(
                ex instanceof NumberFormatException
                    ? "Enter a valid line count and whole-number options."
                    : ex.getMessage());
          }
        });
    add(content, p);
    renderGenerated();
  }

  private void saveGenerated() {
    JSONArray a = new JSONArray();
    try {
      for (Entry e : generated) a.put(ArchiveStore.entryJson(e));
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    store.preferences().edit().putString("generated_" + game.name(), a.toString()).apply();
  }

  private void loadGenerated() {
    generated = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(store.preferences().getString("generated_" + game.name(), "[]"));
      for (int i = 0; i < a.length(); i++)
        generated.add(ArchiveStore.entryFrom(a.getJSONObject(i)));
    } catch (Exception ignored) {
    }
  }

  private void renderGenerated() {
    if (generated.isEmpty()) return;
    add(content, text(generated.size() + " unique lines", 21, GREEN, true));
    String options = store.preferences().getString("generated_options_" + game.name(), "");
    note(content, options);
    report =
        "LottoLab / Generated numbers\nGame: "
            + game.title
            + "\nRules: "
            + RuleSet.current(game).description()
            + "\nGenerated/exported: "
            + Instant.now()
            + "\n"
            + options
            + "\nThese are selections, not purchased tickets or predictions.\n\n";
    csv = "line,game,numbers,powerball\n";
    for (int i = 0; i < generated.size(); i++) {
      Entry e = generated.get(i);
      report += (i + 1) + ". " + e.display() + "\n";
      csv +=
          (i + 1)
              + ","
              + game.name()
              + ","
              + CsvCodec.quote(ArchiveStore.numbers(e.numbers))
              + ","
              + (e.powerBall > 0 ? e.powerBall : "")
              + "\n";
    }
    two(
        content,
        button(
            "Save all favourites",
            false,
            () -> {
              int count = store.saveAll(generated);
              toast("Saved " + count + " new lines. Existing duplicates were kept once.");
            }),
        button(
            "Use first six in Lab",
            false,
            () -> {
              draft = new ArrayList<>(generated.subList(0, Math.min(6, generated.size())));
              store.draft(game, draft);
              go("lab", "compare");
            }));
    exportButtons(content);
    for (int i = 0; i < Math.min(visible, generated.size()); i++) {
      Entry e = generated.get(i);
      LinearLayout p = card();
      add(p, text("Line " + (i + 1), 16, GREEN, true));
      add(p, balls(e.numbers, e.powerBall, 0, null));
      two(
          p,
          button("Save favourite", false, () -> saveFavourite(e)),
          button("Track a draw", false, () -> ticketDialog(e, defaultDate())));
      add(content, p);
    }
    if (generated.size() > visible)
      add(
          content,
          button(
              "Show more lines",
              false,
              () -> {
                visible += 30;
                render();
              }));
  }

  private void wheel() {
    note(
        content,
        "A full system creates every complete combination from your selected pool. Preview its size"
            + " and cost first.");
    gameRow();
    if (!game.active()) {
      note(content, "Select a current game to build a new full system.");
      return;
    }
    LinearLayout p = card();
    EditText
        pool =
            input(
                p,
                "Your main-number pool",
                "At least " + game.picks + " distinct numbers",
                store.preferences().getString("wheel_pool_" + game.name(), ""),
                false),
        power = new EditText(this);
    if (game.hasPowerBall())
      power =
          input(
              p,
              "Separate PowerBall pool",
              "Example: 3 8",
              store.preferences().getString("wheel_pb_" + game.name(), ""),
              false);
    final EditText pb = power;
    TextView error = text("", 14, ORANGE, false);
    add(p, error);
    primary(
        p,
        "Preview my system",
        "preview_wheel",
        () -> {
          output.removeAllViews();
          try {
            int[] nums = Entry.parseNumbers(pool.getText().toString()),
                powers =
                    game.hasPowerBall() ? Entry.parseNumbers(pb.getText().toString()) : new int[0];
            Set<Integer> s = new HashSet<>();
            for (int n : nums)
              if (n < 1 || n > game.pool || !s.add(n))
                throw new IllegalArgumentException(
                    "Use distinct main numbers from 1–" + game.pool + ".");
            s.clear();
            for (int n : powers)
              if (n < 1 || n > game.powerPool || !s.add(n))
                throw new IllegalArgumentException(
                    "Use distinct PowerBalls from their separate pool.");
            long lines = ToolEngine.wheelCount(game, nums.length, powers.length);
            store
                .preferences()
                .edit()
                .putString("wheel_pool_" + game.name(), pool.getText().toString())
                .putString("wheel_pb_" + game.name(), pb.getText().toString())
                .apply();
            note(
                output,
                lines
                    + " distinct lines\n"
                    + AnalysisEngine.money(lines * game.bundleCostCents)
                    + " for one draw, including required base games.\n"
                    + game.costNote());
            note(
                output,
                "Coverage: every "
                    + game.picks
                    + "-number subset of your pool"
                    + (game.hasPowerBall() ? ", combined with every selected PowerBall" : "")
                    + ". The jackpot combination is covered only if all winning main numbers are in"
                    + " your pool"
                    + (game.hasPowerBall()
                        ? " and the winning PowerBall is in its separate pool"
                        : "")
                    + ". This does not guarantee a win.");
            if (lines > 2000) {
              note(
                  output,
                  "Reduce the pool to 2,000 lines or fewer to generate and export on this phone.");
              return;
            }
            add(
                output,
                button(
                    "Generate these " + lines + " lines",
                    true,
                    () -> {
                      generated = ToolEngine.wheel(game, nums, powers);
                      store
                          .preferences()
                          .edit()
                          .putString(
                              "generated_options_" + game.name(),
                              "Full system from main pool "
                                  + ArchiveStore.numbers(nums)
                                  + (game.hasPowerBall()
                                      ? "; PowerBall pool " + ArchiveStore.numbers(powers)
                                      : ""))
                          .apply();
                      saveGenerated();
                      visible = 30;
                      render();
                    }));
          } catch (Exception e) {
            error.setText(e.getMessage());
          }
        });
    add(content, p);
    output = column();
    add(content, output);
    renderGenerated();
  }

  private void coverage() {
    note(
        content,
        "Coverage measures how your lines differ. Overlap is a structural measure, not a"
            + " prediction.");
    gameRow();
    periodRow();
    String[] choices = {"My comparison lines", "My generated lines", "My saved favourites"};
    add(
        content,
        button(
            "Choose lines to inspect  ▾",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Line collection")
                    .setItems(
                        choices,
                        (v, i) -> {
                          List<Entry> input = i == 0 ? draft : i == 1 ? generated : store.saved();
                          List<Entry> a = new ArrayList<>();
                          for (Entry e : input)
                            if (e.game == game && !e.subset && e.ruleId.equals(ruleId)) a.add(e);
                          output.removeAllViews();
                          if (a.isEmpty()) {
                            note(
                                output,
                                "No complete lines in this game and rule period. Add lines in the"
                                    + " generator or comparison tool first.");
                            return;
                          }
                          Set<String> unique = new HashSet<>(),
                              pairs = new HashSet<>(),
                              triples = new HashSet<>();
                          Set<Integer> numbers = new TreeSet<>();
                          for (Entry e : a) {
                            unique.add(e.key());
                            for (int n : e.numbers) numbers.add(n);
                            for (int x = 0; x < e.numbers.length; x++)
                              for (int y = x + 1; y < e.numbers.length; y++) {
                                pairs.add(e.numbers[x] + ":" + e.numbers[y]);
                                for (int z = y + 1; z < e.numbers.length; z++)
                                  triples.add(
                                      e.numbers[x] + ":" + e.numbers[y] + ":" + e.numbers[z]);
                              }
                          }
                          long space =
                              AnalysisEngine.choose(RuleSet.forId(game, ruleId).pool, game.picks)
                                  * (game.hasPowerBall()
                                      ? RuleSet.forId(game, ruleId).powerPool
                                      : 1);
                          String summary =
                              a.size()
                                  + " complete lines\n"
                                  + unique.size()
                                  + " distinct entries\n"
                                  + (a.size() - unique.size())
                                  + " duplicate entries\n"
                                  + numbers.size()
                                  + " distinct main numbers\n"
                                  + pairs.size()
                                  + " main-number pairs\n"
                                  + triples.size()
                                  + " main-number triples\n\n"
                                  + "Jackpot outcome coverage for one draw: "
                                  + unique.size()
                                  + " of "
                                  + String.format(Locale.ENGLISH, "%,d", space)
                                  + " possible complete outcomes.";
                          note(output, summary);
                          report =
                              "LottoLab / Line coverage\nGame: "
                                  + game.title
                                  + "\nRules: "
                                  + ruleId
                                  + "\n"
                                  + summary
                                  + "\n\n";
                          for (int x = 0; x < Math.min(a.size(), 10); x++)
                            for (int y = x + 1; y < Math.min(a.size(), 10); y++) {
                              String line =
                                  "Line "
                                      + (x + 1)
                                      + " & "
                                      + (y + 1)
                                      + ": "
                                      + AnalysisEngine.overlap(a.get(x), a.get(y))
                                      + " shared main numbers";
                              note(output, line);
                              report += line + "\n";
                            }
                          note(
                              output,
                              "Overlap shown for the first 10 lines. Repeated purchases of an"
                                  + " identical entry do not add distinct jackpot outcomes.");
                          exportButtons(output);
                        })
                    .show()));
    output = column();
    add(content, output);
  }

  private void odds() {
    note(content, "Understand one line’s chances using the rules for the selected period.");
    gameRow();
    periodRow();
    RuleSet r = RuleSet.forId(game, ruleId);
    long total = AnalysisEngine.choose(r.pool, r.picks) * (r.powerPool > 0 ? r.powerPool : 1);
    LinearLayout p = card();
    add(p, text("Jackpot: 1 in " + String.format(Locale.ENGLISH, "%,d", total), 23, GREEN, true));
    note(
        p,
        r.description()
            + "\n"
            + "Each distinct, valid complete line has this same jackpot probability in a fair"
            + " draw.");
    add(content, p);
    add(content, text("Possible match categories", 21, TEXT, true));
    note(
        content,
        "These are exact match probabilities. A category is not necessarily a paying prize tier."
            + " Current and historical prize tables are not connected.");
    report =
        "LottoLab / Odds explorer\nGame: "
            + game.title
            + "\nRule period: "
            + r.id
            + "\nJackpot odds: 1 in "
            + total
            + "\nMatch categories, not a verified prize table.\n\n";
    csv = "main_matches,extra_ball_matches,probability,odds_one_in\n";
    for (int m = r.picks; m >= 0; m--) {
      for (int extra = 0; extra < (game.hasBonus() || game.hasPowerBall() ? 2 : 1); extra++) {
        double probability = ToolEngine.categoryProbability(game, r, m, extra == 1);
        if (probability <= 0) continue;
        String label =
            m
                + " main"
                + (game.hasPowerBall()
                    ? (extra == 1 ? " + PowerBall" : "; no PowerBall")
                    : game.hasBonus() ? (extra == 1 ? " + bonus" : "; no bonus") : "");
        LinearLayout c = card();
        add(c, text(label, 17, TEXT, true));
        note(c, ToolEngine.odds(probability));
        add(content, c);
        report += label + ": " + ToolEngine.odds(probability) + "\n";
        csv +=
            m
                + ","
                + extra
                + ","
                + String.format(Locale.ROOT, "%.12f", probability)
                + ","
                + String.format(Locale.ROOT, "%.4f", 1 / probability)
                + "\n";
      }
    }
    add(content, button("How these odds work", false, this::maths));
    exportButtons(content);
  }

  private void maths() {
    alert(
        "What the maths can tell you",
        "Numbers are sampled without replacement from the main pool. A PowerBall is selected"
            + " independently from its separate pool. A LOTTO bonus is drawn after the main"
            + " numbers.\n\n"
            + "Historical counts describe only the available records. Missing records mean unknown"
            + " coverage. Hot, cold and long-gap numbers do not become more likely in the next"
            + " independent draw.\n\n"
            + "Different complete entries cover different jackpot outcomes. Duplicate entries cover"
            + " the same outcome. Lower-tier outcomes can overlap between lines.\n\n"
            + "Full-system coverage is conditional on the winning numbers lying inside the chosen"
            + " pool.");
  }

  private static long cents(String text) {
    try {
      BigDecimal r = new BigDecimal(text.trim());
      if (r.scale() > 2 || r.signum() < 0 || r.compareTo(new BigDecimal("1000000")) > 0)
        throw new IllegalArgumentException();
      return r.movePointRight(2).longValueExact();
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Enter a rand amount from R0 to R1,000,000 with at most two decimal places.");
    }
  }

  private void budget() {
    note(
        content,
        "Estimate an additional plan and compare it with a monthly spending limit. Only tickets"
            + " marked purchased count as recorded spending.");
    gameRow();
    if (!game.active()) {
      note(
          content,
          "Historical prices are shown with dated tickets. Select a current game to plan future"
              + " entries.");
      return;
    }
    LinearLayout p = card();
    EditText
        lines =
            input(
                p,
                "Lines per draw",
                "1–1000",
                Integer.toString(store.preferences().getInt("plan_lines", 1)),
                true),
        draws =
            input(
                p,
                "Number of draws",
                "1–1000",
                Integer.toString(store.preferences().getInt("plan_draws", 1)),
                true),
        limit =
            input(
                p,
                "Monthly spending limit (rand)",
                "Example: 200.00",
                store.preferences().getString("budget", "200"),
                false);
    limit.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    TextView error = text("", 14, ORANGE, false);
    add(p, error);
    primary(
        p,
        "Calculate & save my plan",
        "calculate_budget",
        () -> {
          try {
            int l = Integer.parseInt(lines.getText().toString()),
                d = Integer.parseInt(draws.getText().toString());
            if (l < 1 || l > 1000 || d < 1 || d > 1000)
              throw new IllegalArgumentException("Use 1–1,000 lines and 1–1,000 draws.");
            long cap = cents(limit.getText().toString()),
                cost = AnalysisEngine.cost(game, l, d),
                spent = store.spent(YearMonth.from(today()));
            store
                .preferences()
                .edit()
                .putInt("plan_lines", l)
                .putInt("plan_draws", d)
                .putString("budget", limit.getText().toString())
                .apply();
            output.removeAllViews();
            String s =
                "Additional plan: "
                    + AnalysisEngine.money(cost)
                    + "\n"
                    + l
                    + " lines × "
                    + d
                    + " draws\n"
                    + AnalysisEngine.money((long) l * game.bundleCostCents)
                    + " per draw\n\nRecorded purchases in "
                    + YearMonth.from(today())
                    + ": "
                    + AnalysisEngine.money(spent)
                    + "\nMonthly limit: "
                    + AnalysisEngine.money(cap)
                    + "\nRemaining after recorded purchases: "
                    + AnalysisEngine.money(Math.max(0, cap - spent))
                    + "\n"
                    + (cost + spent > cap
                        ? "This additional plan exceeds the remaining limit by "
                            + AnalysisEngine.money(cost + spent - cap)
                        : "This additional plan fits within your remaining limit.");
            note(output, s);
            note(
                output,
                game.costNote()
                    + " Prices exclude channel fees. The projection assumes all planned entries are"
                    + " additional purchases within this budget month. No ticket is bought or"
                    + " spending recorded here.");
            report =
                "LottoLab / Spending plan\nGame: "
                    + game.title
                    + "\nCreated: "
                    + today()
                    + "\n"
                    + s
                    + "\n"
                    + game.costNote()
                    + "\n"
                    + "Source:"
                    + " https://www.absa.co.za/self-service/digital-banking-services/anytime-anywhere-banking-transactions/lotto-and-powerball/\n";
            exportButtons(output);
            hideKeyboard();
          } catch (Exception e) {
            error.setText(
                e instanceof NumberFormatException
                    ? "Enter whole-number line and draw counts."
                    : e.getMessage());
          }
        });
    add(content, p);
    output = column();
    add(content, output);
    add(content, button("View recorded tickets", false, () -> go("numbers", "tickets")));
  }

  private void simulation() {
    note(
        content,
        "Generate imaginary independent draws and see how streaks and gaps happen naturally. This"
            + " is not a forecast or historical data.");
    gameRow();
    if (!game.active()) {
      note(content, "Select a current game for this simulation.");
      return;
    }
    LinearLayout p = card();
    EditText target = input(p, "Main number to follow", "1–" + game.pool, "1", true),
        count = input(p, "Simulated draws", "100–10000", "1000", true);
    TextView error = text("", 14, ORANGE, false);
    add(p, error);
    primary(
        p,
        "Run simulation",
        "run_simulation",
        () -> {
          try {
            int number = Integer.parseInt(target.getText().toString()),
                n = Integer.parseInt(count.getText().toString());
            if (number < 1 || number > game.pool || n < 100 || n > 10000)
              throw new IllegalArgumentException(
                  "Choose a valid main number and 100–10,000 simulated draws.");
            Game simGame = game;
            note(output, "Running an imaginary experiment…");
            worker.execute(
                () -> {
                  int hits = 0, gap = 0, longest = 0;
                  for (int i = 0; i < n; i++) {
                    Entry e = Entry.random(simGame, random);
                    if (Arrays.binarySearch(e.numbers, number) >= 0) {
                      hits++;
                      gap = 0;
                    } else {
                      gap++;
                      longest = Math.max(longest, gap);
                    }
                  }
                  String s =
                      "SIMULATED DATA — NOT REAL RESULTS\n\n"
                          + n
                          + " imaginary "
                          + simGame.title
                          + " draws\nNumber followed: "
                          + number
                          + "\nAppeared in "
                          + hits
                          + " draws\nExpected average: "
                          + String.format(
                              Locale.ENGLISH, "%.1f", n * simGame.picks / (double) simGame.pool)
                          + " draws\nLongest observed run without it: "
                          + longest
                          + " draws\n\n"
                          + "A different run will give different counts. A long gap does not make a"
                          + " number due.";
                  post(
                      () -> {
                        if (!page.equals("simulate") || game != simGame) return;
                        output.removeAllViews();
                        note(output, s);
                        report =
                            "LottoLab / Randomness simulation\nGenerated: "
                                + Instant.now()
                                + "\n"
                                + s;
                        exportButtons(output);
                      });
                });
          } catch (Exception e) {
            error.setText(
                e instanceof NumberFormatException
                    ? "Enter whole numbers in both fields."
                    : e.getMessage());
          }
        });
    add(content, p);
    output = column();
    add(content, output);
  }

  private void saveFavourite(Entry e) {
    LinearLayout p = column();
    p.setPadding(dp(20), dp(8), dp(20), 0);
    EditText name = input(p, "Name", "Optional: Weekend line", e.name, false),
        notes =
            input(
                p,
                "Notes",
                "Optional personal note",
                store.preferences().getString("note_" + e.key(), ""),
                false);
    new AlertDialog.Builder(this)
        .setTitle("Save a favourite")
        .setView(p)
        .setNegativeButton("Cancel", null)
        .setPositiveButton(
            "Save",
            (v, i) -> {
              Entry saved = e.named(name.getText().toString().trim());
              boolean added = store.save(saved);
              if (!added) store.updateSaved(e, saved);
              store
                  .preferences()
                  .edit()
                  .putString("note_" + saved.key(), notes.getText().toString())
                  .apply();
              toast(added ? "Saved in My Numbers." : "Saved line updated; duplicates kept once.");
              if (tab.equals("numbers") && page.isEmpty()) render();
            })
        .show();
  }

  private void numbers() {
    heading(
        "Keep your numbers organised",
        "Favourites are reusable lines. Dated tickets track a particular draw.");
    gameRow();
    two(
        content,
        button("My dated tickets", false, () -> go("numbers", "tickets")),
        button("Spending plan", false, () -> go("numbers", "budget")));
    List<Entry> saved = new ArrayList<>();
    for (Entry e : store.saved()) if (e.game == game) saved.add(e);
    add(content, text(saved.size() + " saved favourites", 21, GREEN, true));
    if (saved.isEmpty()) {
      note(
          content,
          "You have no saved lines for this game. Generate a line or choose your own numbers to"
              + " start.");
      primary(content, "Create my first line", "create_first_line", () -> go("lab", "generate"));
      return;
    }
    add(
        content,
        button(
            "Export saved lines",
            false,
            () -> {
              report =
                  "LottoLab / Saved lines\nGame: "
                      + game.title
                      + "\nExported: "
                      + Instant.now()
                      + "\nThese are favourites, not purchased tickets.\n\n";
              csv = "name,game,rule_period,numbers,powerball,subset,note\n";
              for (Entry e : saved) {
                String note = store.preferences().getString("note_" + e.key(), "");
                report +=
                    (e.name.isEmpty() ? "Saved line" : e.name)
                        + "\n"
                        + e.display()
                        + "\nRules: "
                        + e.ruleId
                        + "\n"
                        + note
                        + "\n\n";
                csv +=
                    CsvCodec.quote(e.name)
                        + ","
                        + e.game.name()
                        + ","
                        + e.ruleId
                        + ","
                        + CsvCodec.quote(ArchiveStore.numbers(e.numbers))
                        + ","
                        + e.powerBall
                        + ","
                        + e.subset
                        + ","
                        + CsvCodec.quote(note)
                        + "\n";
              }
              exportDialog();
            }));
    for (int i = 0; i < Math.min(visible, saved.size()); i++) {
      Entry e = saved.get(i);
      LinearLayout p = card();
      add(p, text(e.name.isEmpty() ? "Saved line " + (i + 1) : e.name, 19, TEXT, true));
      add(p, balls(e.numbers, e.powerBall, 0, null));
      note(p, (e.subset ? "Research subset · " : "") + RuleSet.forId(game, e.ruleId).description());
      String note = store.preferences().getString("note_" + e.key(), "");
      if (!note.isEmpty()) note(p, note);
      add(
          p,
          button(
              "Use or edit this line  ▾",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle(e.name.isEmpty() ? "Saved line" : e.name)
                      .setItems(
                          new String[] {
                            "Check a dated draw",
                            "Research this line",
                            "Add to six-line comparison",
                            "Track a draw",
                            "Edit name / notes",
                            "Edit numbers",
                            "Delete favourite"
                          },
                          (v, which) -> {
                            if (which == 0) {
                              working = e;
                              ruleId = e.ruleId;
                              checkDate = dateForEntry(e);
                              go("lab", "check");
                            }
                            if (which == 1) {
                              working = e;
                              ruleId = e.ruleId;
                              go("lab", "search");
                            }
                            if (which == 2) {
                              if (draft.size() >= 6) {
                                toast(
                                    "Your comparison already has six lines. Remove one in the Lab"
                                        + " first.");
                                return;
                              }
                              draft.add(e);
                              store.draft(game, draft);
                              ruleId = e.ruleId;
                              go("lab", "compare");
                            }
                            if (which == 3) ticketDialog(e, dateForEntry(e));
                            if (which == 4) saveFavourite(e);
                            if (which == 5)
                              editEntry(
                                  e,
                                  true,
                                  e.ruleId,
                                  x -> {
                                    store.updateSaved(e, x);
                                    render();
                                  });
                            if (which == 6)
                              new AlertDialog.Builder(this)
                                  .setTitle("Delete this favourite?")
                                  .setMessage("Dated ticket snapshots remain unchanged.")
                                  .setNegativeButton("Cancel", null)
                                  .setPositiveButton(
                                      "Delete",
                                      (a, b) -> {
                                        store.remove(e);
                                        render();
                                      })
                                  .show();
                          })
                      .show()));
      add(content, p);
    }
    if (saved.size() > visible)
      add(
          content,
          button(
              "Show more saved lines",
              false,
              () -> {
                visible += 30;
                render();
              }));
  }

  private LocalDate dateForEntry(Entry e) {
    List<Draw> ds = ToolEngine.eligible(e.game, e.ruleId, store.draws(e.game, 0), 1);
    if (!ds.isEmpty()) return ds.get(0).date;
    LocalDate d = e.ruleId.equals("2025-09") ? RuleSet.CURRENT_START.minusDays(1) : today();
    while (!e.game.scheduled(d)) d = d.minusDays(1);
    return d;
  }

  private void ticketDialog(Entry e, LocalDate initial) {
    if (e.subset) {
      alert(
          "A complete line is needed",
          "A research subset cannot be recorded as a ticket. Choose a full line first.");
      return;
    }
    LinearLayout p = column();
    p.setPadding(dp(18), dp(8), dp(18), 0);
    LocalDate[] date = {initial};
    TextView dateButton =
        button(
            "Draw: " + DATE.format(date[0]) + "  ▾",
            false,
            () ->
                datePicker(
                    date[0],
                    d -> {
                      date[0] = d;
                      ticketDateLabel(p, d);
                    }));
    dateButton.setTag("ticket_date");
    add(p, dateButton);
    note(p, e.game.title + "\n" + e.display());
    CheckBox paid = new CheckBox(this);
    paid.setText("I purchased this entry");
    paid.setTextColor(TEXT);
    paid.setMinHeight(dp(48));
    add(p, paid);
    EditText cost =
        input(
            p,
            "Amount paid / planned (rand)",
            "Record a bundled purchase only once",
            String.format(Locale.ROOT, "%.2f", RuleSet.forId(e.game, e.ruleId).bundleCents / 100.0),
            false);
    cost.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    EditText note = input(p, "Ticket note", "Optional reference or reminder", "", false);
    note(
        p,
        "Purchased entries count in the month you record the purchase. Related tracking entries can"
            + " use R0 to avoid counting the same bundle twice.");
    TextView error = text("", 14, ORANGE, false);
    add(p, error);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(p);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle("Save a dated ticket")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save ticket", null)
            .create();
    d.setOnShowListener(
        v -> {
          d.getButton(AlertDialog.BUTTON_POSITIVE)
              .setOnClickListener(
                  x -> {
                    try {
                      TicketRecord t =
                          new TicketRecord(
                              UUID.randomUUID().toString(),
                              e,
                              date[0],
                              cents(cost.getText().toString()),
                              paid.isChecked(),
                              note.getText().toString());
                      store.ticket(t);
                      d.dismiss();
                      toast("Dated ticket saved. Its numbers are kept as a snapshot.");
                    } catch (Exception ex) {
                      error.setText(ex.getMessage());
                    }
                  });
        });
    d.show();
  }

  private void ticketDateLabel(LinearLayout p, LocalDate d) {
    TextView b = p.findViewWithTag("ticket_date");
    if (b != null) b.setText("Draw: " + DATE.format(d) + "  ▾");
  }

  private void tickets() {
    note(
        content,
        "Selections are stored against a specific draw date. Editing a favourite does not change"
            + " these tickets.");
    List<TicketRecord> all = store.tickets();
    if (all.isEmpty()) {
      note(content, "No dated tickets yet. Save one from a favourite or a generated line.");
      primary(content, "Open my favourites", "open_favourites", () -> tab("numbers"));
      return;
    }
    for (TicketRecord t : all) {
      LinearLayout p = card();
      add(p, text(t.entry.game.shortName + " · " + DATE.format(t.date), 18, TEXT, true));
      add(p, balls(t.entry.numbers, t.entry.powerBall, 0, null));
      note(
          p,
          (t.purchased ? "Purchased · " : "Planned · ")
              + AnalysisEngine.money(t.costCents)
              + (t.purchased ? " · recorded " + t.purchaseDate : "")
              + (t.note.isEmpty() ? "" : "\n" + t.note));
      Draw found = null;
      for (Draw d : store.draws(t.entry.game, 0))
        if (d.date.equals(t.date)) {
          found = d;
          break;
        }
      if (found == null) note(p, "Result unavailable. Your outcome is unknown.");
      else {
        int m = AnalysisEngine.matches(t.entry.numbers, found.numbers);
        note(
            p,
            m
                + " of "
                + t.entry.game.picks
                + " main numbers matched"
                + (t.entry.game.hasPowerBall()
                    ? " · PowerBall "
                        + (t.entry.powerBall == found.powerBall ? "matched" : "not matched")
                    : "")
                + "\n"
                + sourceLabel(found));
        Draw d = found;
        add(p, button("View this draw", false, () -> drawDetail(d, t.entry)));
      }
      if (!t.purchased)
        add(
            p,
            button(
                "Mark purchased",
                false,
                () ->
                    new AlertDialog.Builder(this)
                        .setTitle("Record purchase?")
                        .setMessage(
                            "Add "
                                + AnalysisEngine.money(t.costCents)
                                + " to recorded spending for this month? Record a bundle only"
                                + " once.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton(
                            "Record",
                            (v, i) -> {
                              store.ticket(
                                  new TicketRecord(
                                      t.id, t.entry, t.date, t.costCents, true, t.note));
                              render();
                            })
                        .show()));
      add(
          p,
          button(
              "Delete ticket",
              false,
              () ->
                  new AlertDialog.Builder(this)
                      .setTitle("Delete this ticket?")
                      .setMessage("Purchased tickets will also be removed from recorded spending.")
                      .setNegativeButton("Cancel", null)
                      .setPositiveButton(
                          "Delete",
                          (v, i) -> {
                            store.removeTicket(t.id);
                            render();
                          })
                      .show()));
      add(content, p);
    }
  }

  private void settings() {
    note(content, "Your numbers stay on this phone. Basic tools need no account.");
    LinearLayout data = card();
    add(data, text("Draw results", 20, GREEN, true));
    List<Draw> ds = store.draws(game, 0);
    note(data, game.title + "\n" + coverageText(ds));
    note(
        data,
        "Saved history works offline. Automatic updates use an optional public web feed, which"
            + " may be unavailable. You can always enter a draw or import an archive.");
    add(
        data,
        button("Enter a published draw", false, () -> datePicker(defaultDate(), this::addDraw)));
    add(data, button("Import draw CSV", false, () -> openDocument(IMPORT)));
    add(
        data,
        button(
            "Open official results website",
            false,
            () -> openUrl("https://www.nationallottery.co.za/")));
    add(
        data,
        button(
            "Export this game’s archive CSV",
            false,
            () -> {
              if (ds.isEmpty()) {
                alert("Nothing to export", "Import or enter draw results first.");
                return;
              }
              exportFile("csv", CsvCodec.write(ds), "LottoLab_Archive_" + game.name() + ".csv");
            }));
    add(
        data,
        button(
            "CSV format & supported history",
            false,
            () ->
                alert(
                    "Importing a draw archive",
                    CsvCodec.HEADER
                        + "\n\n"
                        + "Required columns: game, date, numbers. Bonus, powerball, source_url and"
                        + " fetched_at are optional columns; PowerBall values are required for"
                        + " PowerBall results. Use YYYY-MM-DD dates and space-separated main"
                        + " numbers.\n\n"
                        + "History supported from 21 September 2025. Current and older rule periods"
                        + " stay separate. Use LOTTO PLUS 2, POWERBALL PLUS and DAILY LOTTO PLUS"
                        + " for historical add-on draws.\n\n"
                        + "Every row is validated before saving. Re-imported identical records are"
                        + " ignored. Corrections retain the previous numbers. Your saved lines are"
                        + " not removed.\n\n"
                        + "Imports: up to 2 MB and 10,000 rows. Sources remain unverified"
                        + " imports.")));
    add(content, data);
    LinearLayout feed = card();
    add(feed, text("Optional automatic updates", 20, TEXT, true));
    long last = store.preferences().getLong("last_sync", 0);
    note(
        feed,
        last == 0
            ? "No automatic refresh has completed."
            : "Last successful refresh: " + Instant.ofEpochMilli(last));
    String failed = store.preferences().getString("sync_error", "");
    if (!failed.isEmpty())
      note(feed, "Last update failed: " + failed + "\nCached records have been kept.");
    CheckBox advanced = new CheckBox(this);
    advanced.setText("Configure an approved data source");
    advanced.setTextColor(TEXT);
    advanced.setMinHeight(dp(48));
    add(feed, advanced);
    LinearLayout f = column();
    f.setVisibility(View.GONE);
    add(feed, f);
    EditText url =
        input(
            f,
            "HTTPS results-feed address",
            "An approved JSON feed URL",
            store.preferences().getString("feed_url", ""),
            false);
    url.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    note(
        f,
        "Use a source you have permission to access. Setting a URL does not make its data official"
            + " or independently verified.");
    add(
        f,
        button(
            "Save data source",
            false,
            () -> {
              try {
                String value = url.getText().toString().trim();
                if (!value.isEmpty()) {
                  java.net.URL u = new java.net.URL(value);
                  if (!u.getProtocol().equals("https") || u.getHost().isEmpty())
                    throw new IllegalArgumentException();
                }
                store.preferences().edit().putString("feed_url", value).apply();
                ResultUpdates.schedule(this);
                toast("Data-source setting saved.");
              } catch (Exception e) {
                url.setError("Enter a valid HTTPS address or leave blank.");
              }
            }));
    advanced.setOnCheckedChangeListener((v, on) -> f.setVisibility(on ? View.VISIBLE : View.GONE));
    note(feed, "Optional: use the National Lottery website's public results service. "
        + "Its undocumented interface may change; downloaded records are not independent prize verification.");
    add(feed, button("Connect National Lottery results", false, () ->
        new AlertDialog.Builder(this)
            .setTitle("Use public lottery results?")
            .setMessage("This connects to the National Lottery website's public results endpoint. "
                + "It is not a guaranteed or supported API and may become unavailable. "
                + "Any existing custom feed address will be replaced. Saved tickets and imported history stay intact. "
                + "Always confirm numbers and prizes with the operator.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Connect and refresh", (dialog, which) -> {
              store.preferences().edit().putString("feed_url", OperatorResults.ENDPOINT).apply();
              ResultUpdates.schedule(this);
              refreshFeed();
            }).show()));
    add(feed, button("Refresh results", false, this::refreshFeed));
    CheckBox background =
        check(
            "Check periodically in the background",
            store.preferences().getBoolean("background_updates", false));
    add(feed, background);
    background.setOnCheckedChangeListener(
        (v, on) -> {
          store.preferences().edit().putBoolean("background_updates", on).apply();
          ResultUpdates.schedule(this);
          if (on && store.preferences().getString("feed_url", "").isEmpty())
            toast("Configure an approved data source to enable updates.");
        });
    note(
        feed,
        "Android schedules periodic checks when a source is configured. Connection and battery"
            + " settings affect update timing.");
    add(content, feed);
    LinearLayout notifications = card();
    add(notifications, text("Result notifications", 20, TEXT, true));
    CheckBox enable =
        check(
            "Notify me about selected games and dated tickets",
            store.preferences().getBoolean("notifications", false));
    add(notifications, enable);
    enable.setOnCheckedChangeListener(
        (v, on) -> {
          store.preferences().edit().putBoolean("notifications", on).apply();
          if (on
              && Build.VERSION.SDK_INT >= 33
              && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                  != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, NOTIFY);
        });
    for (Game g : Game.current()) {
      CheckBox c = check(g.title, store.preferences().getBoolean("notify_" + g.name(), false));
      c.setOnCheckedChangeListener(
          (v, on) -> store.preferences().edit().putBoolean("notify_" + g.name(), on).apply());
      add(notifications, c);
    }
    note(
        notifications,
        "Notifications follow incoming result updates. They identify feed/imported records and do"
            + " not confirm prize claims.");
    add(content, notifications);
    LinearLayout backup = card();
    add(backup, text("Backup & restore", 20, TEXT, true));
    note(
        backup,
        "Export your saved lines, dated tickets, notes, settings and draw archive to a file you"
            + " control. Restore merges records and preserves existing favourites. Keep backups"
            + " private.");
    add(
        backup,
        button(
            "Save a backup file",
            false,
            () -> {
              try {
                exportFile("json", store.backup(), "LottoLab_Backup_" + today() + ".json");
              } catch (Exception e) {
                alert("Backup unavailable", e.getMessage());
              }
            }));
    add(backup, button("Restore a backup", false, () -> openDocument(RESTORE)));
    add(content, backup);
    LinearLayout about = card();
    add(about, text("LottoLab 0.5", 20, GREEN, true));
    note(
        about,
        "Native Android · Easy side menu · Offline history and help\n\n"
            + "Current number-draw games and selected historical eras are supported. Sports pools"
            + " are outside this number-draw build. An optional public National Lottery website"
            + " results adapter can be connected; it is not independently verified and may"
            + " become unavailable. Verified prize payouts are not connected.\n\n"
            + "18+. Historical analysis cannot predict a fair future draw.");
    add(
        about,
        button(
            "View current game rules",
            false,
            () ->
                openUrl(
                    "https://personal.nedbank.co.za/legal/terms-and-conditions/lottery-games-terms-and-conditions.html")));
    add(content, about);
  }

  private CheckBox check(String label, boolean value) {
    CheckBox c = new CheckBox(this);
    c.setText(label);
    c.setTextColor(TEXT);
    c.setTextSize(14);
    c.setMinHeight(dp(48));
    c.setChecked(value);
    return c;
  }

  private void addDraw(LocalDate date) {
    try {
      if (date.isAfter(today()))
        throw new IllegalArgumentException("A published result cannot have a future date.");
      RuleSet r = RuleSet.forDate(game, date);
      Game selected = game;
      editEntry(
          null,
          false,
          r.id,
          e -> {
            LinearLayout p = column();
            p.setPadding(dp(18), dp(8), dp(18), 0);
            note(p, selected.title + " · " + DATE.format(date) + "\n" + e.display());
            EditText bonus = new EditText(this);
            if (selected.hasBonus())
              bonus = input(p, "Published bonus number", "Optional, 1–" + r.pool, "", true);
            EditText source = input(p, "Source link", "Optional HTTPS link to the draw", "", false);
            final EditText bonusInput = bonus;
            TextView error = text("", 14, ORANGE, false);
            add(p, error);
            AlertDialog d =
                new AlertDialog.Builder(this)
                    .setTitle("Save published draw numbers")
                    .setView(p)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Save draw", null)
                    .create();
            d.setOnShowListener(
                v ->
                    d.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(
                            x -> {
                              try {
                                String url = source.getText().toString().trim();
                                if (!url.isEmpty() && !url.startsWith("https://"))
                                  throw new IllegalArgumentException("Use an HTTPS source link.");
                                int b =
                                    bonusInput.getText().toString().trim().isEmpty()
                                        ? 0
                                        : Integer.parseInt(bonusInput.getText().toString());
                                Draw draw =
                                    new Draw(
                                        selected,
                                        date,
                                        e.numbers,
                                        b,
                                        e.powerBall,
                                        url,
                                        Instant.now().toString(),
                                        "imported");
                                store.importDraws(Collections.singletonList(draw));
                                ResultUpdates.notifyAvailable(this, store);
                                d.dismiss();
                                toast("Draw saved as a manually entered, unverified record.");
                                render();
                              } catch (Exception ex) {
                                error.setText(
                                    ex instanceof NumberFormatException
                                        ? "Enter a whole bonus number."
                                        : ex.getMessage());
                              }
                            }));
            d.show();
          });
    } catch (Exception ex) {
      alert("Draw date unavailable", ex.getMessage());
    }
  }

  private void refreshFeed() {
    String url = store.preferences().getString("feed_url", "");
    if (url.isEmpty()) {
      alert(
          "No source connected",
          "No approved automatic source is configured. Enter published numbers or import a CSV"
              + " archive. An approved feed can be configured under automatic updates.");
      return;
    }
    ProgressDialog progress = new ProgressDialog(this);
    progress.setMessage("Checking for results…");
    progress.setCancelable(false);
    progress.show();
    worker.execute(
        () -> {
          ArchiveStore task = new ArchiveStore(getApplicationContext());
          try {
            List<Draw> draws = FeedClient.fetch(url);
            int changed = task.importDraws(draws);
            task.preferences()
                .edit()
                .putLong("last_sync", System.currentTimeMillis())
                .putString("sync_error", "")
                .apply();
            ResultUpdates.notifyAvailable(getApplicationContext(), task);
            post(
                () -> {
                  progress.dismiss();
                  render();
                  toast(changed + " new or corrected records saved.");
                });
          } catch (Exception e) {
            task.preferences().edit().putString("sync_error", e.getMessage()).apply();
            post(
                () -> {
                  progress.dismiss();
                  alert(
                      "Update unavailable",
                      e.getMessage() + "\nYour saved lines and cached results are kept.");
                });
          } finally {
            task.close();
          }
        });
  }

  private void openUrl(String value) {
    try {
      Uri uri = Uri.parse(value);
      if (!"https".equals(uri.getScheme()))
        throw new IllegalArgumentException("Use an HTTPS source link.");
      startActivity(new Intent(Intent.ACTION_VIEW, uri));
    } catch (Exception e) {
      alert(
          "Could not open link",
          e.getMessage() == null ? "No browser is available." : e.getMessage());
    }
  }

  private void post(Runnable action) {
    runOnUiThread(
        () -> {
          if (!isFinishing() && !isDestroyed()) action.run();
        });
  }

  private String reportHeader(String title, List<Draw> draws) {
    String s =
        "LottoLab / "
            + title
            + "\nGame: "
            + game.title
            + "\nRule period: "
            + ruleId
            + "\nExported: "
            + Instant.now()
            + " (dates shown in Africa/Johannesburg)\n"
            + coverageText(draws)
            + "\n"
            + "Historical patterns do not predict a fair future draw.\n"
            + "Sources are imported / entered, not independently verified.\n";
    Set<String> sources = new TreeSet<>();
    for (Draw d : draws) if (!d.sourceUrl.isEmpty()) sources.add(d.sourceUrl);
    for (String source : sources) s += "Source: " + source + "\n";
    return s;
  }

  private void exportButtons(LinearLayout p) {
    if (report.isEmpty()) return;
    LinearLayout exports = card();
    add(exports, text("Keep this result", 17, TEXT, true));
    two(
        exports,
        button(
            "Save PDF",
            false,
            () -> exportFile("pdf", report, "LottoLab_" + game.name() + "_" + today() + ".pdf")),
        button("Share PDF", false, () -> shareFile(true)));
    if (!csv.isEmpty())
      two(
          exports,
          button(
              "Save CSV",
              false,
              () -> exportFile("csv", csv, "LottoLab_" + game.name() + "_" + today() + ".csv")),
          button("Share CSV", false, () -> shareFile(false)));
    add(p, exports);
  }

  private void exportDialog() {
    new AlertDialog.Builder(this)
        .setTitle("Export saved lines")
        .setItems(
            new String[] {"Save PDF", "Share PDF", "Save CSV", "Share CSV"},
            (v, i) -> {
              if (i == 0) exportFile("pdf", report, "LottoLab_Saved_" + game.name() + ".pdf");
              if (i == 1) shareFile(true);
              if (i == 2) exportFile("csv", csv, "LottoLab_Saved_" + game.name() + ".csv");
              if (i == 3) shareFile(false);
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void exportFile(String kind, String value, String name) {
    pendingKind = kind;
    pendingName = name;
    if (kind.equals("pdf")) pendingReport = value;
    else pendingText = value;
    Intent i =
        new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(
                kind.equals("pdf")
                    ? "application/pdf"
                    : kind.equals("json") ? "application/json" : "text/csv")
            .putExtra(Intent.EXTRA_TITLE, name);
    try {
      startActivityForResult(i, SAVE);
    } catch (Exception e) {
      alert("Could not open save picker", e.getMessage());
    }
  }

  private void shareFile(boolean pdf) {
    try {
      File directory = new File(getCacheDir(), "share");
      if (!directory.exists() && !directory.mkdirs())
        throw new IOException("Could not prepare a share file.");
      File f =
          new File(
              directory,
              "LottoLab_"
                  + game.name()
                  + "_"
                  + System.currentTimeMillis()
                  + (pdf ? ".pdf" : ".csv"));
      try (OutputStream out = new FileOutputStream(f)) {
        if (pdf) PdfReport.write(out, report);
        else out.write(csv.getBytes(StandardCharsets.UTF_8));
      }
      Uri uri =
          new Uri.Builder()
              .scheme("content")
              .authority("za.co.lottolab.studio.share")
              .appendPath(f.getName())
              .build();
      Intent i =
          new Intent(Intent.ACTION_SEND)
              .setType(pdf ? "application/pdf" : "text/csv")
              .putExtra(Intent.EXTRA_STREAM, uri)
              .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      i.setClipData(ClipData.newRawUri("LottoLab export", uri));
      startActivity(Intent.createChooser(i, "Share LottoLab export"));
    } catch (Exception e) {
      alert("Could not share", e.getMessage());
    }
  }

  private void openDocument(int request) {
    Intent i =
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*");
    i.putExtra(
        Intent.EXTRA_MIME_TYPES,
        request == RESTORE
            ? new String[] {"application/json", "text/plain", "application/octet-stream"}
            : new String[] {
              "text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream"
            });
    try {
      startActivityForResult(i, request);
    } catch (Exception e) {
      alert("Could not open file picker", e.getMessage());
    }
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    if (request == SAVE) {
      String kind = pendingKind, value = kind.equals("pdf") ? pendingReport : pendingText;
      try (OutputStream out = getContentResolver().openOutputStream(uri)) {
        if (out == null) throw new IOException("The chosen destination could not be opened.");
        if (kind.equals("pdf")) PdfReport.write(out, value);
        else out.write(value.getBytes(StandardCharsets.UTF_8));
        toast("File saved.");
      } catch (Exception e) {
        alert("Could not save file", e.getMessage());
      }
      return;
    }
    if (request == IMPORT || request == RESTORE) {
      toast("Validating the file…");
      worker.execute(
          () -> {
            ArchiveStore task = new ArchiveStore(getApplicationContext());
            try {
              String raw;
              try (InputStream in = getContentResolver().openInputStream(uri)) {
                raw = FeedClient.read(in, request == RESTORE ? 20 * 1024 * 1024 : 2 * 1024 * 1024);
              }
              String message;
              if (request == RESTORE) {
                task.restore(raw);
                message = "Backup merged. Existing favourites and dated tickets were kept.";
              } else {
                List<Draw> draws = CsvCodec.parse(raw, today());
                int changed = task.importDraws(draws);
                message = changed + " new or corrected draws imported. Sources remain unverified.";
                ResultUpdates.notifyAvailable(getApplicationContext(), task);
              }
              post(
                  () -> {
                    report = csv = "";
                    draft = store.draft(game);
                    loadGenerated();
                    render();
                    toast(message);
                  });
            } catch (Exception e) {
              post(
                  () ->
                      alert(
                          request == RESTORE ? "Restore not completed" : "Import not completed",
                          e.getMessage() + "\nCheck the file format before trying again."));
            } finally {
              task.close();
            }
          });
    }
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    if (request == NOTIFY
        && (results.length == 0
            || results[0] != android.content.pm.PackageManager.PERMISSION_GRANTED)) {
      store.preferences().edit().putBoolean("notifications", false).apply();
      toast("Notifications remain off. You can enable them later in Android settings.");
      render();
    }
  }
}
