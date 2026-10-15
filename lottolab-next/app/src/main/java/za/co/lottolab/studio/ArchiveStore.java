package za.co.lottolab.studio;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Versioned offline archive. Version 2 adds metadata; existing rows and preferences survive. */
public final class ArchiveStore extends SQLiteOpenHelper {
  private final SharedPreferences prefs;

  public ArchiveStore(Context c) {
    super(c, "lottolab_studio.db", null, 2);
    prefs = c.getSharedPreferences("lottolab_studio", Context.MODE_PRIVATE);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE draws(game TEXT NOT NULL,date TEXT NOT NULL,numbers TEXT NOT NULL,bonus"
            + " INTEGER NOT NULL,powerball INTEGER NOT NULL,source_url TEXT NOT NULL DEFAULT"
            + " '',fetched_at TEXT NOT NULL DEFAULT '',verification TEXT NOT NULL DEFAULT"
            + " 'imported',PRIMARY KEY(game,date))");
    createRevisions(db);
  }

  private void createRevisions(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS draw_revisions(id INTEGER PRIMARY KEY AUTOINCREMENT,game TEXT"
            + " NOT NULL,date TEXT NOT NULL,record TEXT NOT NULL,replaced_at TEXT NOT NULL)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
    if (oldV < 2) {
      db.execSQL("ALTER TABLE draws ADD COLUMN source_url TEXT NOT NULL DEFAULT ''");
      db.execSQL("ALTER TABLE draws ADD COLUMN fetched_at TEXT NOT NULL DEFAULT ''");
      db.execSQL("ALTER TABLE draws ADD COLUMN verification TEXT NOT NULL DEFAULT 'imported'");
      createRevisions(db);
    }
  }

  public boolean imported(Game g) {
    return !draws(g, 1).isEmpty();
  }

  public List<Draw> draws(Game g, int limit) {
    List<Draw> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT date,numbers,bonus,powerball,source_url,fetched_at,verification FROM draws"
                    + " WHERE game=? ORDER BY date DESC"
                    + (limit > 0 ? " LIMIT " + limit : ""),
                new String[] {g.name()})) {
      while (c.moveToNext())
        out.add(
            new Draw(
                g,
                LocalDate.parse(c.getString(0)),
                Entry.parseNumbers(c.getString(1)),
                c.getInt(2),
                c.getInt(3),
                c.getString(4),
                c.getString(5),
                c.getString(6)));
    }
    return out;
  }

  public int importDraws(List<Draw> draws) {
    SQLiteDatabase db = getWritableDatabase();
    int changes = 0;
    db.beginTransaction();
    try {
      for (Draw d : draws) {
        String numbers = numbers(d.numbers),
            now = d.fetchedAt.isEmpty() ? Instant.now().toString() : d.fetchedAt;
        boolean same = false;
        try (Cursor old =
            db.rawQuery(
                "SELECT numbers,bonus,powerball,source_url,verification FROM draws WHERE game=? AND"
                    + " date=?",
                new String[] {d.game.name(), d.date.toString()})) {
          if (old.moveToFirst()) {
            same =
                old.getString(0).trim().equals(numbers)
                    && old.getInt(1) == d.bonus
                    && old.getInt(2) == d.powerBall;
            if (!same) {
              ContentValues revision = new ContentValues();
              revision.put("game", d.game.name());
              revision.put("date", d.date.toString());
              revision.put(
                  "record",
                  old.getString(0)
                      + " | bonus="
                      + old.getInt(1)
                      + " | PowerBall="
                      + old.getInt(2)
                      + " | source="
                      + old.getString(3)
                      + " | "
                      + old.getString(4));
              revision.put("replaced_at", now);
              db.insertOrThrow("draw_revisions", null, revision);
            }
          }
        }
        if (same) continue;
        ContentValues v = new ContentValues();
        v.put("game", d.game.name());
        v.put("date", d.date.toString());
        v.put("numbers", numbers);
        v.put("bonus", d.bonus);
        v.put("powerball", d.powerBall);
        v.put("source_url", d.sourceUrl);
        v.put("fetched_at", now);
        v.put("verification", d.verification);
        db.insertWithOnConflict("draws", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        changes++;
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    if (changes > 0)
      prefs
          .edit()
          .putLong("archive_generation", prefs.getLong("archive_generation", 0) + 1)
          .apply();
    return changes;
  }

  static String numbers(int[] nums) {
    StringBuilder b = new StringBuilder();
    for (int n : nums) {
      if (b.length() > 0) b.append(' ');
      b.append(n);
    }
    return b.toString();
  }

  public int revisionCount(Draw d) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT COUNT(*) FROM draw_revisions WHERE game=? AND date=?",
                new String[] {d.game.name(), d.date.toString()})) {
      return c.moveToFirst() ? c.getInt(0) : 0;
    }
  }

  public void clearImports() {
    getWritableDatabase().delete("draws", null, null);
    prefs.edit().putLong("archive_generation", prefs.getLong("archive_generation", 0) + 1).apply();
  }

  public List<Entry> saved() {
    return readEntries("saved", new ArrayList<>());
  }

  public boolean save(Entry e) {
    List<Entry> a = saved();
    for (Entry x : a) if (x.key().equals(e.key())) return false;
    a.add(e);
    writeEntries("saved", a);
    return true;
  }

  public int saveAll(List<Entry> entries) {
    List<Entry> a = saved();
    Set<String> keys = new HashSet<>();
    for (Entry e : a) keys.add(e.key());
    int added = 0;
    for (Entry e : entries)
      if (keys.add(e.key())) {
        a.add(e);
        added++;
      }
    writeEntries("saved", a);
    return added;
  }

  public void updateSaved(Entry original, Entry replacement) {
    List<Entry> a = saved();
    if (!original.key().equals(replacement.key()))
      for (Entry e : a)
        if (e.key().equals(replacement.key()))
          throw new IllegalArgumentException("That combination is already saved.");
    for (int i = 0; i < a.size(); i++)
      if (a.get(i).key().equals(original.key())) a.set(i, replacement);
    writeEntries("saved", a);
  }

  public void remove(Entry e) {
    List<Entry> a = saved();
    a.removeIf(x -> x.key().equals(e.key()));
    writeEntries("saved", a);
  }

  public List<Entry> draft(Game g) {
    return readEntries("draft_" + g.name(), new ArrayList<>());
  }

  public void draft(Game g, List<Entry> a) {
    writeEntries("draft_" + g.name(), a);
  }

  public SharedPreferences preferences() {
    return prefs;
  }

  public static JSONObject entryJson(Entry e) throws JSONException {
    JSONObject v = new JSONObject();
    v.put("game", e.game.name());
    v.put("numbers", new JSONArray(e.numbers));
    v.put("powerBall", e.powerBall);
    v.put("subset", e.subset);
    v.put("name", e.name);
    v.put("ruleId", e.ruleId);
    return v;
  }

  public static Entry entryFrom(JSONObject v) throws JSONException {
    JSONArray a = v.getJSONArray("numbers");
    int[] nums = new int[a.length()];
    for (int i = 0; i < nums.length; i++) nums[i] = FeedClient.whole(a.get(i));
    return new Entry(
        Game.valueOf(v.getString("game")),
        nums,
        v.isNull("powerBall") ? 0 : FeedClient.whole(v.get("powerBall")),
        v.optBoolean("subset"),
        v.optString("name"),
        v.optString("ruleId", "2026-06"));
  }

  private List<Entry> readEntries(String key, List<Entry> fallback) {
    String raw = prefs.getString(key, null);
    if (raw == null) return fallback;
    List<Entry> a = new ArrayList<>();
    try {
      JSONArray j = new JSONArray(raw);
      for (int i = 0; i < j.length(); i++) a.add(entryFrom(j.getJSONObject(i)));
      return a;
    } catch (JSONException | IllegalArgumentException e) {
      return fallback;
    }
  }

  private void writeEntries(String key, List<Entry> entries) {
    JSONArray a = new JSONArray();
    try {
      for (Entry e : entries) a.put(entryJson(e));
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    prefs.edit().putString(key, a.toString()).apply();
  }

  public List<TicketRecord> tickets() {
    List<TicketRecord> a = new ArrayList<>();
    try {
      JSONArray j = new JSONArray(prefs.getString("tickets", "[]"));
      for (int i = 0; i < j.length(); i++) a.add(ticketFrom(j.getJSONObject(i)));
    } catch (JSONException | IllegalArgumentException ignored) {
    }
    a.sort((x, y) -> y.date.compareTo(x.date));
    return a;
  }

  private static TicketRecord ticketFrom(JSONObject v) throws JSONException {
    return new TicketRecord(
        v.getString("id"),
        entryFrom(v.getJSONObject("entry")),
        LocalDate.parse(v.getString("date")),
        FeedClient.whole(v.get("costCents")),
        v.optBoolean("purchased"),
        v.optString("note"),
        v.optBoolean("purchased")
            ? LocalDate.parse(v.optString("purchaseDate", v.getString("date")))
            : null);
  }

  private static JSONObject ticketJson(TicketRecord t) throws JSONException {
    JSONObject v = new JSONObject();
    v.put("id", t.id);
    v.put("entry", entryJson(t.entry));
    v.put("date", t.date.toString());
    v.put("costCents", t.costCents);
    v.put("purchased", t.purchased);
    v.put("note", t.note);
    if (t.purchaseDate != null) v.put("purchaseDate", t.purchaseDate.toString());
    return v;
  }

  public void ticket(TicketRecord t) {
    List<TicketRecord> a = tickets();
    a.removeIf(x -> x.id.equals(t.id));
    a.add(t);
    writeTickets(a);
  }

  public void removeTicket(String id) {
    List<TicketRecord> a = tickets();
    a.removeIf(x -> x.id.equals(id));
    writeTickets(a);
  }

  private void writeTickets(List<TicketRecord> a) {
    JSONArray j = new JSONArray();
    try {
      for (TicketRecord t : a) j.put(ticketJson(t));
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    prefs.edit().putString("tickets", j.toString()).apply();
  }

  public long spent(YearMonth month) {
    long total = 0;
    for (TicketRecord t : tickets())
      if (t.purchased && t.purchaseDate != null && YearMonth.from(t.purchaseDate).equals(month))
        total += t.costCents;
    return total;
  }

  public String backup() throws JSONException {
    JSONObject root = new JSONObject();
    root.put("format", "LottoLab");
    root.put("version", 2);
    root.put("createdAt", Instant.now().toString());
    JSONObject p = new JSONObject();
    for (Map.Entry<String, ?> e : prefs.getAll().entrySet())
      if (e.getValue() instanceof String
          || e.getValue() instanceof Number
          || e.getValue() instanceof Boolean) p.put(e.getKey(), e.getValue());
    root.put("preferences", p);
    List<Draw> all = new ArrayList<>();
    for (Game g : Game.values()) all.addAll(draws(g, 0));
    root.put("archiveCsv", CsvCodec.write(all));
    JSONArray revisions = new JSONArray();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT game,date,record,replaced_at FROM draw_revisions", null)) {
      while (c.moveToNext()) {
        JSONObject v = new JSONObject();
        v.put("game", c.getString(0));
        v.put("date", c.getString(1));
        v.put("record", c.getString(2));
        v.put("replacedAt", c.getString(3));
        revisions.put(v);
      }
    }
    root.put("revisions", revisions);
    return root.toString(2);
  }

  /** Merge restores: existing favourites and ticket IDs are retained. Validate everything first. */
  public void restore(String raw) throws JSONException {
    JSONObject root = new JSONObject(raw);
    if (!"LottoLab".equals(root.optString("format")) || FeedClient.whole(root.get("version")) != 2)
      throw new IllegalArgumentException("This is not a supported LottoLab backup.");
    JSONObject p = root.getJSONObject("preferences");
    validatePreferences(p);
    List<Entry> restoreSaved = new ArrayList<>();
    JSONArray a = new JSONArray(p.optString("saved", "[]"));
    for (int i = 0; i < a.length(); i++) restoreSaved.add(entryFrom(a.getJSONObject(i)));
    List<TicketRecord> restoreTickets = new ArrayList<>();
    a = new JSONArray(p.optString("tickets", "[]"));
    for (int i = 0; i < a.length(); i++) restoreTickets.add(ticketFrom(a.getJSONObject(i)));
    String csv = root.optString("archiveCsv");
    List<Draw> ds =
        csv.trim().equals(CsvCodec.HEADER)
            ? new ArrayList<>()
            : CsvCodec.parse(csv, LocalDate.now(ZoneId.of("Africa/Johannesburg")));
    JSONArray rev = root.optJSONArray("revisions");
    if (rev == null) rev = new JSONArray();
    for (int i = 0; i < rev.length(); i++) {
      JSONObject v = rev.getJSONObject(i);
      Game.valueOf(v.getString("game"));
      LocalDate.parse(v.getString("date"));
      v.getString("record");
      v.getString("replacedAt");
    }
    // Validate preferences used by parsers before applying any restore writes.
    for (Game g : Game.values())
      if (p.has("draft_" + g.name())) {
        a = new JSONArray(p.getString("draft_" + g.name()));
        for (int i = 0; i < a.length(); i++) entryFrom(a.getJSONObject(i));
      }
    List<Draw> newDraws = new ArrayList<>();
    Set<String> existingDraws = new HashSet<>();
    for (Game g : Game.values()) for (Draw d : draws(g, 0)) existingDraws.add(d.key());
    for (Draw d : ds) if (!existingDraws.contains(d.key())) newDraws.add(d);
    importDraws(newDraws);
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (int i = 0; i < rev.length(); i++) {
        JSONObject v = rev.getJSONObject(i);
        try (Cursor c =
            db.rawQuery(
                "SELECT 1 FROM draw_revisions WHERE game=? AND date=? AND record=? AND"
                    + " replaced_at=?",
                new String[] {
                  v.getString("game"),
                  v.getString("date"),
                  v.getString("record"),
                  v.getString("replacedAt")
                })) {
          if (c.moveToFirst()) continue;
        }
        ContentValues cv = new ContentValues();
        cv.put("game", v.getString("game"));
        cv.put("date", v.getString("date"));
        cv.put("record", v.getString("record"));
        cv.put("replaced_at", v.getString("replacedAt"));
        db.insertOrThrow("draw_revisions", null, cv);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    for (Entry e : restoreSaved) save(e);
    Set<String> ticketIds = new HashSet<>();
    for (TicketRecord t : tickets()) ticketIds.add(t.id);
    for (TicketRecord t : restoreTickets) if (ticketIds.add(t.id)) ticket(t);
    SharedPreferences.Editor edit = prefs.edit();
    Iterator<String> it = p.keys();
    while (it.hasNext()) {
      String k = it.next();
      if (k.equals("saved")
          || k.equals("tickets")
          || k.startsWith("notified_")
          || k.equals("archive_generation")
          || k.equals("last_sync")
          || k.equals("sync_error")
          || k.equals("feed_url")) continue;
      Object v = p.get(k);
      if (v instanceof String) edit.putString(k, (String) v);
      else if (v instanceof Boolean) edit.putBoolean(k, (Boolean) v);
      else if (v instanceof Number) {
        if (k.equals("last_sync")) edit.putLong(k, ((Number) v).longValue());
        else edit.putInt(k, ((Number) v).intValue());
      }
    }
    edit.apply();
  }

  private static void validatePreferences(JSONObject p) throws JSONException {
    Iterator<String> keys = p.keys();
    while (keys.hasNext()) {
      String k = keys.next();
      Object v = p.get(k);
      if (k.equals("game")) {
        if (!(v instanceof String)) throw new IllegalArgumentException("Invalid saved game.");
        Game.valueOf((String) v);
      } else if (k.equals("budget")) {
        if (!(v instanceof String)) throw new IllegalArgumentException("Invalid budget setting.");
        try {
          java.math.BigDecimal b = new java.math.BigDecimal((String) v);
          if (b.signum() < 0 || b.compareTo(new java.math.BigDecimal("1000000")) > 0)
            throw new IllegalArgumentException();
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid budget setting.");
        }
      } else if (k.equals("plan_lines") || k.equals("plan_draws")) {
        int n = FeedClient.whole(v);
        if (n < 1 || n > 1000) throw new IllegalArgumentException("Invalid plan count.");
      } else if (k.equals("notifications")
          || k.equals("background_updates")
          || k.startsWith("notify_")
          || k.startsWith("generator_advanced_")) {
        if (!(v instanceof Boolean))
          throw new IllegalArgumentException("Invalid on/off setting: " + k);
      } else if (k.startsWith("draft_")
          || (k.startsWith("generated_") && !k.startsWith("generated_options_"))) {
        if (!(v instanceof String))
          throw new IllegalArgumentException("Invalid saved number collection.");
        JSONArray a = new JSONArray((String) v);
        if (a.length() > (k.startsWith("draft_") ? 6 : 2000))
          throw new IllegalArgumentException("Too many saved collection entries.");
        for (int i = 0; i < a.length(); i++) entryFrom(a.getJSONObject(i));
      } else if (k.equals("saved")
          || k.equals("tickets")
          || k.startsWith("note_")
          || k.startsWith("locked_")
          || k.startsWith("excluded_")
          || k.startsWith("odd_")
          || k.startsWith("fixedpb_")
          || k.startsWith("gen_count_")
          || k.startsWith("wheel_")
          || k.startsWith("generated_options_")) {
        if (!(v instanceof String))
          throw new IllegalArgumentException("Invalid text setting: " + k);
      }
    }
  }
}
