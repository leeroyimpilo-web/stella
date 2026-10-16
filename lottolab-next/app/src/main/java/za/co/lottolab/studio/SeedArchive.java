package za.co.lottolab.studio;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Insert vetted build-time history only when missing. Never replace a user's newer records. */
final class SeedArchive {
  static final int VERSION = 1;
  private SeedArchive() {}

  static int install(Context context, SQLiteDatabase db) throws Exception {
    String contents;
    try (InputStream in = context.getAssets().open("seed_draws.csv")) {
      contents = FeedClient.read(in, 4 * 1024 * 1024);
    }
    List<Draw> draws = CsvCodec.parse(contents, LocalDate.now(ZoneId.of("Africa/Johannesburg")));
    if (draws.size() < 500)
      throw new IllegalArgumentException("Bundled archive is incomplete (" + draws.size() + ").");

    int inserted = 0;
    db.beginTransaction();
    try {
      for (Draw draw : draws) {
        ContentValues row = new ContentValues();
        row.put("game", draw.game.name());
        row.put("date", draw.date.toString());
        row.put("numbers", ArchiveStore.numbers(draw.numbers));
        row.put("bonus", draw.bonus);
        row.put("powerball", draw.powerBall);
        row.put("source_url", draw.sourceUrl);
        row.put("fetched_at", draw.fetchedAt);
        row.put("verification", "bundled archive");
        if (db.insertWithOnConflict("draws", null, row, SQLiteDatabase.CONFLICT_IGNORE) != -1)
          inserted++;
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return inserted;
  }
}
