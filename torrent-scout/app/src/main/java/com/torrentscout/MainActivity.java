package com.torrentscout;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 16, 29);
    private static final int CARD = Color.rgb(19, 31, 49);
    private static final int PANEL = Color.rgb(25, 40, 61);
    private static final int BORDER = Color.rgb(45, 66, 85);
    private static final int PRIMARY = Color.rgb(67, 218, 229);
    private static final int TEXT = Color.rgb(236, 247, 251);
    private static final int MUTED = Color.rgb(158, 180, 197);
    private static final String[] CATEGORIES = {"All", "Video", "Audio", "Software", "Books", "Games", "Other"};
    private static final String[] SORTS = {"Relevance", "Seeders", "Size"};

    private final ExecutorService pool = Executors.newFixedThreadPool(5);
    private final List<TorrentItem> results = new ArrayList<>();
    private SettingsStore store;
    private LinearLayout body;
    private LinearLayout cards;
    private EditText searchInput;
    private TextView status;
    private String query = "";
    private String category = "All";
    private String sort = "Relevance";
    private boolean savedMode = false;
    private boolean settingsMode = false;
    private int searchId = 0;
    private int pending = 0;
    private final List<String> errors = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new SettingsStore(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        showSearch();
    }

    @Override protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }

    private int dp(float n) {
        return (int) (getResources().getDisplayMetrics().density * n + 0.5f);
    }

    private GradientDrawable background(int color, int radius, int outline) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (outline != 0) d.setStroke(dp(1), outline);
        return d;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(true);
        return t;
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

    private void gap(LinearLayout parent, int dp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    private void add(LinearLayout to, View child) {
        to.addView(child, new LinearLayout.LayoutParams(-1, -2));
    }

    private void marginAdd(LinearLayout to, View child, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        p.bottomMargin = dp(bottom);
        to.addView(child, p);
    }

    private TextView button(String label, boolean highlighted, Runnable action) {
        TextView t = text(label, 12, highlighted ? BG : TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(12), dp(14), dp(12));
        t.setBackground(background(highlighted ? PRIMARY : PANEL, 12, highlighted ? 0 : BORDER));
        t.setClickable(true);
        t.setOnClickListener(v -> action.run());
        return t;
    }

    private void buttonInRow(LinearLayout r, TextView b, int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.leftMargin = dp(left);
        r.addView(b, p);
    }

    private void setupPage() {
        LinearLayout outer = column();
        outer.setBackgroundColor(BG);
        outer.setPadding(dp(16), dp(14), dp(16), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        body = column();
        body.setPadding(0, dp(6), 0, dp(18));
        scroll.addView(body);
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(outer);
        outer.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0;
            int bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            outer.setPadding(dp(16), top + dp(10), dp(16), bottom + dp(10));
            return insets;
        });
        outer.requestApplyInsets();
    }

    private void header(String secondary) {
        LinearLayout h = row();
        TextView monogram = text("TS", 14, BG, true);
        monogram.setGravity(Gravity.CENTER);
        monogram.setBackground(background(PRIMARY, 13, 0));
        h.addView(monogram, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout title = column();
        TextView app = text("TORRENTSCOUT", 18, TEXT, true);
        TextView small = text(secondary, 11, MUTED, false);
        add(title, app);
        add(title, small);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.leftMargin = dp(11);
        h.addView(title, tp);
        add(body, h);
        gap(body, 22);
    }

    private EditText input(String hint, int type) {
        EditText t = new EditText(this);
        t.setTextSize(15);
        t.setTextColor(TEXT);
        t.setHintTextColor(MUTED);
        t.setSingleLine(true);
        t.setHint(hint);
        t.setInputType(type);
        t.setPadding(dp(14), dp(13), dp(14), dp(13));
        t.setBackground(background(CARD, 12, BORDER));
        return t;
    }

    private void showSearch() {
        settingsMode = false;
        setupPage();
        header("SEARCH • DISCOVER • OPEN");
        TextView tagline = text("Find torrents across your connected sources.", 16, TEXT, false);
        add(body, tagline);
        gap(body, 15);

        LinearLayout searchRow = row();
        searchInput = input("Search any torrent...", InputType.TYPE_CLASS_TEXT);
        searchInput.setText(query);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { startSearch(); return true; }
            return false;
        });
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0, -2, 1f));
        buttonInRow(searchRow, button("SEARCH", true, this::startSearch), 8);
        add(body, searchRow);
        gap(body, 12);

        LinearLayout actions = row();
        actions.addView(button("SOURCES  " + (store.indexers().size() + 1), false, this::showSources),
            new LinearLayout.LayoutParams(0, -2, 1f));
        buttonInRow(actions, button(savedMode ? "ALL RESULTS" : "SAVED", savedMode, () -> {
            savedMode = !savedMode;
            showSearch();
        }), 8);
        add(body, actions);
        gap(body, 20);

        add(body, text("FILE CATEGORY", 11, MUTED, true));
        gap(body, 8);
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = row();
        for (String c : CATEGORIES) {
            buttonInRow(chips, button(c, category.equals(c), () -> {
                category = c;
                showSearch();
            }), c.equals("All") ? 0 : 6);
        }
        horizontal.addView(chips);
        add(body, horizontal);

        gap(body, 18);
        add(body, text("SORT RESULTS", 11, MUTED, true));
        gap(body, 8);
        LinearLayout sorts = row();
        for (String s : SORTS) {
            TextView b = button(s, sort.equals(s), () -> {
                sort = s;
                showSearch();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.rightMargin = dp(6);
            sorts.addView(b, p);
        }
        add(body, sorts);
        gap(body, 17);

        status = text("", 12, MUTED, false);
        add(body, status);
        cards = column();
        marginAdd(body, cards, 8, 0);
        display();
        gap(body, 28);
        add(body, text("Use torrents only for material you are authorised to download. Results are supplied by independent indexers.", 11, MUTED, false));
    }

    private void startSearch() {
        String requested = searchInput.getText().toString().trim();
        if (requested.isEmpty()) { toast("Enter a search term"); return; }
        if (requested.length() > 120) { toast("Search is too long"); return; }
        query = requested;
        savedMode = false;
        searchId++;
        final int id = searchId;
        results.clear();
        errors.clear();
        List<SettingsStore.Indexer> indexers = store.indexers();
        pending = 1 + indexers.size();
        display();
        pool.execute(() -> {
            try { deliver(id, SearchService.archive(requested), null); }
            catch (Exception e) { deliver(id, Collections.emptyList(), "Internet Archive: " + e.getMessage()); }
        });
        for (SettingsStore.Indexer indexer : indexers) {
            pool.execute(() -> {
                try { deliver(id, SearchService.torznab(indexer, requested), null); }
                catch (Exception e) { deliver(id, Collections.emptyList(), indexer.name + ": " + e.getMessage()); }
            });
        }
    }

    private void deliver(int id, List<TorrentItem> found, String error) {
        runOnUiThread(() -> {
            if (id != searchId || settingsMode) return;
            results.addAll(found);
            pending--;
            if (error != null) errors.add(error);
            display();
        });
    }

    private List<TorrentItem> visible() {
        LinkedHashMap<String,TorrentItem> unique = new LinkedHashMap<>();
        List<TorrentItem> list = savedMode ? store.favorites() : results;
        for (TorrentItem t : list) {
            if (!"All".equals(category) && !category.equals(t.category)) continue;
            TorrentItem existing = unique.get(t.key());
            if (existing == null || t.seeders > existing.seeders) unique.put(t.key(), t);
        }
        List<TorrentItem> out = new ArrayList<>(unique.values());
        if ("Seeders".equals(sort)) out.sort((a,b) -> Integer.compare(b.seeders, a.seeders));
        if ("Size".equals(sort)) out.sort((a,b) -> Long.compare(b.size, a.size));
        return out;
    }

    private void display() {
        if (cards == null || status == null) return;
        cards.removeAllViews();
        List<TorrentItem> items = visible();
        String prefix = savedMode ? "SAVED TORRENTS" : (pending > 0 ? "SEARCHING " + pending + " SOURCE(S)" : "SEARCH RESULTS");
        status.setText(prefix + "  •  " + items.size() + " FOUND");
        if (!savedMode && query.isEmpty() && pending == 0) {
            intro();
            return;
        }
        if (items.isEmpty()) {
            LinearLayout c = tile();
            add(c, text(pending > 0 ? "Checking search sources..." : "No matching torrents found", 16, TEXT, true));
            gap(c, 7);
            add(c, text(pending > 0 ? "Available results will appear here." :
                "Try a broader term, select All, or add more indexers under Sources.", 13, MUTED, false));
            add(cards, c);
        } else {
            int cap = Math.min(items.size(), 100);
            for (int i = 0; i < cap; i++) renderResult(items.get(i));
        }
        if (!errors.isEmpty() && pending == 0) {
            marginAdd(cards, text("Some sources were unavailable: " + String.join("  •  ", errors), 11, MUTED, false), 13, 0);
        }
    }

    private void intro() {
        LinearLayout c = tile();
        add(c, text("One search. Multiple sources.", 18, TEXT, true));
        gap(c, 8);
        add(c, text("Explore Linux distributions, public-domain media, open datasets and files of all types. Add Torznab indexers to widen your search.", 13, MUTED, false));
        gap(c, 16);
        TextView quick = text("TRY A SEARCH", 11, PRIMARY, true);
        add(c, quick);
        gap(c, 10);
        LinearLayout r = row();
        buttonInRow(r, button("Ubuntu", false, () -> quickSearch("ubuntu")), 0);
        buttonInRow(r, button("Open films", false, () -> quickSearch("public domain film")), 8);
        add(c, r);
        add(cards, c);
    }

    private void quickSearch(String word) {
        query = word;
        searchInput.setText(word);
        startSearch();
    }

    private LinearLayout tile() {
        LinearLayout c = column();
        c.setPadding(dp(15), dp(15), dp(15), dp(15));
        c.setBackground(background(CARD, 16, BORDER));
        return c;
    }

    private void renderResult(TorrentItem t) {
        LinearLayout c = tile();
        LinearLayout top = row();
        TextView kind = text(t.category.toUpperCase(Locale.ROOT), 10, PRIMARY, true);
        top.addView(kind, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView source = text(t.source, 10, MUTED, false);
        top.addView(source);
        add(c, top);
        gap(c, 8);
        TextView title = text(t.title, 15, TEXT, true);
        title.setMaxLines(3);
        add(c, title);
        gap(c, 7);
        String meta = t.sizeText() + "   •   " + (t.seeders < 0 ? "Seeders unknown" : t.seeders + " seeders");
        add(c, text(meta, 12, MUTED, false));
        gap(c, 13);
        LinearLayout actions = row();
        TextView open = button(t.link.toLowerCase(Locale.ROOT).startsWith("magnet:") ? "OPEN MAGNET" : "GET TORRENT", true, () -> openTorrent(t));
        actions.addView(open, new LinearLayout.LayoutParams(0, -2, 1f));
        buttonInRow(actions, button("COPY", false, () -> copy(t.link)), 6);
        buttonInRow(actions, button(store.saved(t) ? "★" : "☆", false, () -> {
            store.toggle(t);
            display();
        }), 6);
        add(c, actions);
        if (!t.details.isEmpty() && t.details.startsWith("https://")) {
            gap(c, 9);
            TextView details = text("View source details  ›", 12, PRIMARY, false);
            details.setOnClickListener(v -> browse(t.details));
            add(c, details);
        }
        marginAdd(cards, c, 0, 9);
    }

    private void openTorrent(TorrentItem t) {
        if (t.link.startsWith("magnet:?")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(t.link));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(Intent.createChooser(intent, "Open in torrent client"));
            } catch (ActivityNotFoundException e) {
                new AlertDialog.Builder(this).setTitle("No torrent client found")
                    .setMessage("Install tTorrent or another app that can open magnet links. You can copy the link instead.")
                    .setPositiveButton("Copy magnet", (dialog, which) -> copy(t.link))
                    .setNegativeButton("Close", null).show();
            }
        } else if (t.link.startsWith("https://") || t.link.startsWith("http://")) {
            try {
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(t.link));
                String name = t.title.replaceAll("[^a-zA-Z0-9 _.-]", "_").trim();
                if (name.length() > 54) name = name.substring(0, 54);
                if (name.isEmpty()) name = "torrent";
                req.setTitle(t.title);
                req.setDescription("Torrent metadata download");
                req.setMimeType("application/x-bittorrent");
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "TorrentScout-" + System.currentTimeMillis() + "-" + name + ".torrent");
                dm.enqueue(req);
                toast("Torrent download sent to Downloads");
            } catch (Exception e) {
                toast("Could not download torrent: " + e.getMessage());
            }
        } else toast("This result has no compatible link");
    }

    private void copy(String s) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Torrent link", s));
        toast("Link copied");
    }

    private void browse(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast("No browser available"); }
    }

    private void showSources() {
        settingsMode = true;
        searchId++;
        pending = 0;
        setupPage();
        header("SOURCE CONNECTIONS");
        add(body, button("← BACK TO SEARCH", false, this::showSearch));
        gap(body, 20);
        add(body, text("Your search sources", 23, TEXT, true));
        gap(body, 6);
        add(body, text("Internet Archive is included automatically. Add any compatible Torznab provider to search more torrent categories.", 13, MUTED, false));
        gap(body, 15);
        LinearLayout archive = tile();
        add(archive, text("●  Internet Archive", 15, TEXT, true));
        gap(archive, 5);
        add(archive, text("Built in  •  Public collections with torrent metadata", 12, PRIMARY, false));
        add(body, archive);
        List<SettingsStore.Indexer> indexers = store.indexers();
        for (int i = 0; i < indexers.size(); i++) {
            final int index = i;
            SettingsStore.Indexer ix = indexers.get(i);
            LinearLayout c = tile();
            add(c, text(ix.name, 16, TEXT, true));
            gap(c, 4);
            add(c, text(ix.url, 11, MUTED, false));
            gap(c, 9);
            add(c, button("REMOVE SOURCE", false, () ->
                new AlertDialog.Builder(this).setTitle("Remove " + ix.name + "?")
                    .setPositiveButton("Remove", (d, w) -> {
                        store.removeIndexer(index);
                        showSources();
                    }).setNegativeButton("Cancel", null).show()));
            marginAdd(body, c, 8, 0);
        }
        gap(body, 22);
        add(body, text("ADD TORZNAB INDEXER", 13, PRIMARY, true));
        gap(body, 10);
        EditText name = input("Source name (e.g. My Prowlarr)", InputType.TYPE_CLASS_TEXT);
        add(body, name);
        gap(body, 9);
        EditText url = input("HTTPS Torznab API endpoint", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        add(body, url);
        gap(body, 9);
        EditText key = input("API key (if required)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        add(body, key);
        gap(body, 11);
        add(body, button("ADD SOURCE", true, () -> {
            String n = name.getText().toString().trim();
            String u = url.getText().toString().trim();
            String k = key.getText().toString().trim();
            if (n.isEmpty() || !SearchService.isValidEndpoint(u)) {
                toast("Enter a name and a valid HTTPS or local network URL");
                return;
            }
            if (n.length() > 50 || k.length() > 300) { toast("Source name or key is too long"); return; }
            for (SettingsStore.Indexer ix : store.indexers()) {
                if (ix.url.equalsIgnoreCase(u)) { toast("This source is already added"); return; }
            }
            store.addIndexer(new SettingsStore.Indexer(n, u, k));
            toast("Source added");
            showSources();
        }));
        gap(body, 13);
        add(body, text("Tip: Prowlarr and Jackett can supply Torznab endpoints. API keys are saved locally on your phone. Avoid entering private keys for untrusted HTTP sites.", 12, MUTED, false));
    }

    @Override public void onBackPressed() {
        if (settingsMode) showSearch();
        else if (savedMode) { savedMode = false; showSearch(); }
        else super.onBackPressed();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
