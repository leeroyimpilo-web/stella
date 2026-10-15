package za.co.lottolab.studio;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

public final class ArchiveStore extends SQLiteOpenHelper {
  private final SharedPreferences prefs;

  public ArchiveStore(Context c) {
    super(c, "lottolab_studio.db", null, 1);
    prefs = c.getSharedPreferences("lottolab_studio", Context.MODE_PRIVATE);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE draws (game TEXT NOT NULL, date TEXT NOT NULL, numbers TEXT NOT NULL, bonus"
            + " INTEGER NOT NULL, powerball INTEGER NOT NULL, PRIMARY KEY(game,date))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    /* Add non-destructive migrations when schema changes. */
  }

  public boolean imported(Game game) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT 1 FROM draws WHERE game=? LIMIT 1", new String[] {game.name()})) {
      return c.moveToFirst();
    }
  }

  public List<Draw> draws(Game game, int limit) {
    List<Draw> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT date,numbers,bonus,powerball FROM draws WHERE game=? ORDER BY date DESC",
                new String[] {game.name()})) {
      while (c.moveToNext())
        out.add(
            new Draw(
                game,
                LocalDate.parse(c.getString(0)),
                Entry.parseNumbers(c.getString(1)),
                c.getInt(2),
                c.getInt(3)));
    }
    if (out.isEmpty()) out = DemoArchive.forGame(game);
    if (limit > 0 && out.size() > limit) return new ArrayList<>(out.subList(0, limit));
    return out;
  }

  public void importDraws(List<Draw> draws) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      for (Draw d : draws) {
        ContentValues v = new ContentValues();
        v.put("game", d.game.name());
        v.put("date", d.date.toString());
        StringBuilder n = new StringBuilder();
        for (int x : d.numbers) n.append(x).append(' ');
        v.put("numbers", n.toString().trim());
        v.put("bonus", d.bonus);
        v.put("powerball", d.powerBall);
        db.insertWithOnConflict("draws", null, v, SQLiteDatabase.CONFLICT_REPLACE);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void clearImports() {
    getWritableDatabase().delete("draws", null, null);
  }

  public List<Entry> saved() {
    return readEntries("saved", new ArrayList<>());
  }

  public boolean save(Entry e) {
    List<Entry> list = saved();
    for (Entry x : list) if (x.key().equals(e.key())) return false;
    list.add(e);
    writeEntries("saved", list);
    return true;
  }

  public void remove(Entry e) {
    List<Entry> list = saved();
    list.removeIf(x -> x.key().equals(e.key()));
    writeEntries("saved", list);
  }

  public List<Entry> draft(Game g) {
    List<Entry> defaults = new ArrayList<>();
    defaults.add(DemoArchive.initial(g, 0));
    defaults.add(DemoArchive.initial(g, 1));
    return readEntries("draft_" + g.name(), defaults);
  }

  public void draft(Game g, List<Entry> entries) {
    writeEntries("draft_" + g.name(), entries);
  }

  public SharedPreferences preferences() {
    return prefs;
  }

  private List<Entry> readEntries(String key, List<Entry> fallback) {
    String raw = prefs.getString(key, null);
    if (raw == null) return fallback;
    List<Entry> out = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(raw);
      for (int i = 0; i < a.length(); i++) {
        JSONObject v = a.getJSONObject(i);
        JSONArray nums = v.getJSONArray("numbers");
        int[] n = new int[nums.length()];
        for (int j = 0; j < n.length; j++) n[j] = nums.getInt(j);
        out.add(
            new Entry(
                Game.valueOf(v.getString("game")),
                n,
                v.optInt("powerBall"),
                v.optBoolean("subset"),
                v.optString("name")));
      }
      return out;
    } catch (JSONException | IllegalArgumentException e) {
      return fallback;
    }
  }

  private void writeEntries(String key, List<Entry> entries) {
    JSONArray a = new JSONArray();
    try {
      for (Entry e : entries) {
        JSONObject v = new JSONObject();
        v.put("game", e.game.name());
        JSONArray n = new JSONArray();
        for (int x : e.numbers) n.put(x);
        v.put("numbers", n);
        v.put("powerBall", e.powerBall);
        v.put("subset", e.subset);
        v.put("name", e.name);
        a.put(v);
      }
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
    prefs.edit().putString(key, a.toString()).apply();
  }
}
