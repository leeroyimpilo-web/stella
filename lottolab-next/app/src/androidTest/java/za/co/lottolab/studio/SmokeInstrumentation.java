package za.co.lottolab.studio;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.*;

/** Device journeys, version-1 migration, backup/export and real native screenshots. */
public class SmokeInstrumentation extends Instrumentation {
  private MainActivity activity;
  private File folder;
  private LocalDate date;

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      Context c = getTargetContext();
      folder = new File(c.getExternalFilesDir(null), "qa");
      if (!folder.exists() && !folder.mkdirs()) throw new IOException("Cannot create QA folder");
      c.deleteDatabase("lottolab_studio.db");
      c.getSharedPreferences("lottolab_studio", Context.MODE_PRIVATE).edit().clear().commit();
      testOperatorParsing();
      date = LocalDate.now(ZoneId.of("Africa/Johannesburg"));
      while (!Game.LOTTO.scheduled(date)) date = date.minusDays(1);
      // Simulate the shipped 0.1 database and saved-entry JSON without any new fields.
      SQLiteDatabase old = c.openOrCreateDatabase("lottolab_studio.db", 0, null);
      old.execSQL(
          "CREATE TABLE draws(game TEXT NOT NULL,date TEXT NOT NULL,numbers TEXT NOT NULL,bonus"
              + " INTEGER NOT NULL,powerball INTEGER NOT NULL,PRIMARY KEY(game,date))");
      old.execSQL(
          "INSERT INTO draws VALUES(?,?,?,?,?)",
          new Object[] {"LOTTO", date.toString(), "1 2 3 4 5 6", 7, 0});
      old.setVersion(1);
      old.close();
      c.getSharedPreferences("lottolab_studio", 0)
          .edit()
          .putString(
              "saved",
              "[{\"game\":\"LOTTO\",\"numbers\":[1,2,3,4,5,6],\"powerBall\":0,\"subset\":false,\"name\":\"Upgrade"
                  + " kept my line\"}]")
          .putString("budget", "200")
          .commit();
      startApp();
      ArchiveStore store = new ArchiveStore(c);
      check(store.draws(Game.LOTTO, 0).size() == 1, "Version-1 draw was lost");
      check(store.saved().size() == 1, "Version-1 saved line was lost");
      check(store.getReadableDatabase().getVersion() == 2, "Schema was not migrated");
      store.clearImports();
      click("tab_home");
      requireText("No results downloaded yet");
      rejectText("SAMPLE DATA");
      shot("01-home-empty");
      click("home_check");
      requireText("Check a ticket");
      click("choose_numbers");
      SystemClock.sleep(250);
      setAccessible("Main numbers input", "1 2 3 4 5 6");
      shot("02-number-grid");
      clickAccessible("Use these numbers");
      click("check_ticket");
      requireText("unknown, not a losing ticket");
      Draw fixture =
          new Draw(
              Game.LOTTO,
              date,
              new int[] {1, 2, 3, 4, 5, 6},
              7,
              0,
              "https://example.com/qa-fixture",
              Instant.now().toString(),
              "imported");
      check(store.importDraws(Arrays.asList(fixture)) == 1, "Import failed");
      check(store.importDraws(Arrays.asList(fixture)) == 0, "Identical import was not idempotent");
      click("tab_home");
      requireText("Latest available result");
      shot("03-home-with-import");
      click("home_check");
      click("check_ticket");
      requireText("6 of 6 main numbers matched");
      requireText("Prize division and money are not confirmed");
      shot("04-ticket-check");
      click("tab_results");
      requireText("available draws");
      onMain(
          () ->
              ((EditText)
                      activity.findViewById(android.R.id.content).findViewWithTag("history_query"))
                  .setText("1 2"));
      requireText("1 matching records");
      shot("05-history");
      click("tab_lab");
      requireText("What would you like to do?");
      shot("06-lab-directory");
      click("group_0");
      requireText("Compare up to six lines");
      click("tool_search");
      click("search_history");
      requireText("Exact full line appeared in 1 of 1");
      click("tab_home");
      clickText("Generate numbers");
      onMain(
          () ->
              ((EditText)
                      activity.findViewById(android.R.id.content).findViewWithTag("generate_count"))
                  .setText("6"));
      click("generate_numbers");
      requireText("6 unique lines");
      shot("07-generated-lines");
      clickText("Save all favourites");
      check(store.saved().size() >= 6, "Generated lines not saved");
      clickText("Share PDF");
      SystemClock.sleep(600);
      File share = new File(c.getCacheDir(), "share");
      File[] pdfs = share.listFiles((d, n) -> n.endsWith(".pdf"));
      check(
          pdfs != null && pdfs.length > 0 && pdfs[0].length() > 500,
          "Native PDF share did not create a real file");
      copy(pdfs[0], new File(folder, "generated-lines.pdf"));
      getUiAutomation()
          .performGlobalAction(
              android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
      waitForIdleSync();
      click("tab_numbers");
      requireText("saved favourites");
      shot("08-my-numbers");
      Entry saved = store.saved().get(0);
      TicketRecord ticket = new TicketRecord("qa-ticket", saved, date, 500, true, "Snapshot test");
      store.ticket(ticket);
      store.updateSaved(saved, saved.named("Renamed favourite"));
      check(
          store.tickets().get(0).entry.name.equals("Upgrade kept my line"),
          "Favourite edit changed a dated ticket");
      check(
          store.spent(YearMonth.from(LocalDate.now(ZoneId.of("Africa/Johannesburg")))) == 500,
          "Spending was not recorded by purchase month");
      String backup = store.backup();
      store.restore(backup);
      check(store.tickets().size() == 1, "Restore duplicated ticket IDs");
      check(store.saved().size() >= 6, "Restore dropped saved lines");
      Draw corrected = new Draw(Game.LOTTO, date, new int[] {1, 2, 3, 4, 5, 8}, 7, 0);
      store.importDraws(Arrays.asList(corrected));
      check(store.revisionCount(corrected) == 1, "Correction did not retain previous record");
      store.importDraws(Arrays.asList(corrected));
      check(store.revisionCount(corrected) == 1, "Identical correction duplicated audit rows");
      int records = store.draws(Game.LOTTO, 0).size();
      try {
        FeedClient.parse(
            "{\"draws\":[{\"game\":\"LOTTO\",\"date\":\""
                + date
                + "\",\"numbers\":[1,1,2,3,4,5]}]}",
            "https://example.com/feed",
            date);
        throw new AssertionError("Invalid feed accepted");
      } catch (IllegalArgumentException expected) {
      }
      check(store.draws(Game.LOTTO, 0).size() == records, "Rejected feed changed cached rows");
      try (FileOutputStream out = new FileOutputStream(new File(folder, "backup.json"))) {
        out.write(store.backup().getBytes(StandardCharsets.UTF_8));
      }
      int savedCount = store.saved().size();
      store.close();
      restart();
      click("tab_numbers");
      ArchiveStore reloaded = new ArchiveStore(c);
      check(reloaded.saved().size() == savedCount, "Offline restart lost saved lines");
      check(reloaded.tickets().size() == 1, "Offline restart lost ticket snapshot");
      reloaded.close();
      clickText("Spending plan");
      requireText("Plan my spending");
      click("calculate_budget");
      requireText("Recorded purchases");
      shot("09-spending");
      click("tab_lab");
      click("group_1");
      click("tool_frequency");
      requireText("Number frequencies");
      shot("10-frequencies");
      // Narrow-screen and increased-font smoke checks on the actual native layout.
      shell("wm size 360x800");
      shell("wm density 160");
      shell("settings put system font_scale 1.3");
      SystemClock.sleep(500);
      restart();
      click("tab_home");
      requireText("Check my numbers");
      shot("11-narrow-large-font-home");
      click("home_check");
      click("choose_numbers");
      shot("12-narrow-large-font-grid");
      requireDialogActionVisible("Cancel");
      requireDialogActionVisible("Use these numbers");
      clickAccessible("Cancel");
      shell("wm size reset");
      shell("wm density reset");
      shell("settings put system font_scale 1.0");
      result.putString("passed", "true");
      result.putString(
          "journeys",
          "migration, empty data, number grid, ticket check, history search, subset search,"
              + " generation, saving, PDF sharing, snapshots, spending, correction audit,"
              + " backup/restore, offline restart, narrow screen, large font");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString("passed", "false");
      result.putString("error", android.util.Log.getStackTraceString(e));
      try {
        shot("failure");
      } catch (Throwable ignored) {
      }
      finish(Activity.RESULT_CANCELED, result);
    }
  }

  /** Operator response fixtures exist in device tests, never in the installed draw archive. */
  private void testOperatorParsing() throws Exception {
    JSONObject lotto = new JSONObject()
        .put("winPoolName", "LOTTO")
        .put("drawTime", "2026-10-14 21:00:00")
        .put("winNumList", new JSONArray(Arrays.asList("1", "2", "3", "4", "5", "6", "7")));
    JSONObject plus = new JSONObject()
        .put("winPoolName", "LOTTO PLUS 1")
        .put("drawTime", "2026-10-14 21:00:00")
        .put("winNumList", new JSONArray(Arrays.asList("11", "12", "13", "14", "15", "16", "17")));
    JSONObject payload = new JSONObject().put("code", 0)
        .put("data", new JSONObject().put("list", new JSONArray().put(lotto).put(plus)));
    List<Draw> results = OperatorResults.parse(payload.toString(), LocalDate.of(2026, 10, 15));
    check(results.size() == 2, "Operator response did not preserve two lotto pools");
    check(results.get(0).game == Game.LOTTO && results.get(0).bonus == 7,
        "Operator bonus ball was parsed incorrectly");
    check(results.get(1).game == Game.PLUS1 && results.get(1).bonus == 17,
        "Operator add-on pool was incorrectly mapped");
    check(results.get(0).verification.equals("feed"), "Operator results must be labelled as feed data");
    payload.getJSONObject("data").getJSONArray("list").put(lotto);
    boolean rejected = false;
    try {
      OperatorResults.parse(payload.toString(), LocalDate.of(2026, 10, 15));
    } catch (IllegalArgumentException expected) {
      rejected = true;
    }
    check(rejected, "Duplicate operator draw must reject the entire update");
    check(OperatorResults.gameForPool("DAILY LOTTO PLUS") == null,
        "Discontinued daily add-on should not be mapped to current games");
  }

  private void requireDialogActionVisible(String label) {
    AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
    check(root != null, "No accessible dialog window");
    AccessibilityNodeInfo action = findAccessible(root, label);
    check(action != null, "Dialog action missing: " + label);
    android.graphics.Rect screen = new android.graphics.Rect();
    android.graphics.Rect bounds = new android.graphics.Rect();
    root.getBoundsInScreen(screen);
    action.getBoundsInScreen(bounds);
    check(bounds.width() > 0 && bounds.height() > 0 && screen.contains(bounds),
        "Dialog action clipped or outside the visible window: " + label);
  }

  private void startApp() {
    activity =
        (MainActivity)
            startActivitySync(
                new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    waitForIdleSync();
    SystemClock.sleep(250);
  }

  private void restart() {
    onMain(() -> activity.finish());
    startApp();
  }

  private void onMain(Runnable r) {
    runOnMainSync(r);
    waitForIdleSync();
  }

  private void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  private void click(String tag) {
    onMain(
        () -> {
          View v = activity.getWindow().getDecorView().findViewWithTag(tag);
          check(v != null, "Missing control: " + tag);
          check(v.performClick(), "Cannot click: " + tag);
        });
    SystemClock.sleep(150);
  }

  private void clickText(String value) {
    onMain(
        () -> {
          View v = findText(activity.getWindow().getDecorView(), value, true);
          check(v != null, "Missing button: " + value);
          check(v.performClick(), "Cannot click: " + value);
        });
    SystemClock.sleep(150);
  }

  private View findText(View v, String text, boolean clickable) {
    if (v instanceof TextView
        && ((TextView) v).getText().toString().contains(text)
        && (!clickable || v.isClickable())) return v;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) {
        View found = findText(g.getChildAt(i), text, clickable);
        if (found != null) return found;
      }
    }
    return null;
  }

  private void requireText(String value) {
    boolean[] found = {false};
    onMain(() -> found[0] = findText(activity.getWindow().getDecorView(), value, false) != null);
    check(found[0], "Missing text: " + value);
  }

  private void rejectText(String value) {
    boolean[] found = {false};
    onMain(() -> found[0] = findText(activity.getWindow().getDecorView(), value, false) != null);
    check(!found[0], "Unexpected text: " + value);
  }

  private AccessibilityNodeInfo findAccessible(AccessibilityNodeInfo n, String value) {
    if (n == null) return null;
    if ((n.getContentDescription() != null && n.getContentDescription().toString().equals(value))
        || (n.getText() != null && n.getText().toString().equalsIgnoreCase(value))) return n;
    for (int i = 0; i < n.getChildCount(); i++) {
      AccessibilityNodeInfo found = findAccessible(n.getChild(i), value);
      if (found != null) return found;
    }
    return null;
  }

  private void setAccessible(String description, String value) {
    AccessibilityNodeInfo n =
        findAccessible(getUiAutomation().getRootInActiveWindow(), description);
    check(n != null, "Missing field: " + description);
    Bundle b = new Bundle();
    b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
    check(n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b), "Cannot type " + description);
    waitForIdleSync();
    SystemClock.sleep(150);
  }

  private void clickAccessible(String value) {
    AccessibilityNodeInfo n = findAccessible(getUiAutomation().getRootInActiveWindow(), value);
    check(n != null, "Missing accessible action: " + value);
    check(n.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Cannot click " + value);
    waitForIdleSync();
    SystemClock.sleep(250);
  }

  private void shell(String command) throws IOException {
    try (ParcelFileDescriptor fd = getUiAutomation().executeShellCommand(command);
        InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      while (in.read() != -1) {}
    }
  }

  private void copy(File from, File to) throws IOException {
    try (InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to)) {
      byte[] b = new byte[8192];
      int n;
      while ((n = in.read(b)) != -1) out.write(b, 0, n);
    }
  }

  private void shot(String name) throws IOException {
    waitForIdleSync();
    SystemClock.sleep(200);
    Bitmap b = getUiAutomation().takeScreenshot();
    if (b == null) throw new IOException("Screenshot unavailable");
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    b.recycle();
  }
}
