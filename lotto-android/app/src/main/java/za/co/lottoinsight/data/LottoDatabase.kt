package za.co.lottoinsight.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import za.co.lottoinsight.model.Draw
import za.co.lottoinsight.model.Ticket
import java.time.LocalDate

class LottoDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "lotto_intelligence.db", null, 1) {
    private val appContext = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE draws (game TEXT NOT NULL, date TEXT NOT NULL, numbers TEXT NOT NULL, bonus INTEGER, verified INTEGER NOT NULL, PRIMARY KEY (game, date))")
        db.execSQL("CREATE INDEX idx_draws_date ON draws (date DESC)")
        db.execSQL("CREATE TABLE tickets (id INTEGER PRIMARY KEY AUTOINCREMENT, game TEXT NOT NULL, numbers TEXT NOT NULL, bonus INTEGER, created TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun countDraws(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM draws", null).use {
            it.moveToFirst()
            it.getInt(0)
        }

    fun loadBundledIfEmpty(): Int {
        if (countDraws() > 0) return 0
        val name = try {
            appContext.assets.open("draws.json").close()
            "draws.json"
        } catch (_: Exception) { "seed.json" }
        return appContext.assets.open(name).bufferedReader().use { importDraws(it.readText()) }
    }

    fun importDraws(text: String): Int {
        val root = JSONObject(text)
        val data = root.opt("data")
        val array = when (data) {
            is JSONArray -> data
            is JSONObject -> data.optJSONArray("recent") ?: data.optJSONArray("draws")
            else -> null
        } ?: return 0
        var imported = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val draw = parseDraw(item) ?: continue
                val values = ContentValues().apply {
                    put("game", draw.game)
                    put("date", draw.date)
                    put("numbers", draw.numbers.joinToString(","))
                    if (draw.bonus == null) putNull("bonus") else put("bonus", draw.bonus)
                    put("verified", if (draw.verified) 1 else 0)
                }
                db.insertWithOnConflict("draws", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                imported++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return imported
    }

    private fun parseDraw(item: JSONObject): Draw? {
        val game = item.optString("game")
        val date = item.optString("date")
        val nums = item.optJSONArray("numbers") ?: return null
        if (date.length != 10 || !Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) return null
        try { LocalDate.parse(date) } catch (_: Exception) { return null }
        val range = when (game) {
            "lotto", "lotto-plus-1", "lotto-plus-2", "lotto-5-max" -> 58
            "powerball", "powerball-plus" -> 50
            "daily-lotto", "daily-lotto-plus" -> 36
            else -> return null
        }
        val count = if (game.startsWith("powerball") || game.startsWith("daily")) 5 else 6
        if (nums.length() != count) return null
        val numbers = (0 until nums.length()).map { nums.optInt(it, -1) }
        if (numbers.any { it !in 1..range } || numbers.distinct().size != count) return null
        val bonus = if (item.isNull("bonusBall")) null else item.optInt("bonusBall", -1)
        val bonusRange = if (game.startsWith("powerball")) 20 else if (game.startsWith("lotto")) 58 else 0
        if (bonus != null && (bonusRange == 0 || bonus !in 1..bonusRange)) return null
        return Draw(game, date, numbers.sorted(), bonus, item.optBoolean("verified", false))
    }

    fun allDraws(): List<Draw> {
        val result = mutableListOf<Draw>()
        readableDatabase.rawQuery(
            "SELECT game, date, numbers, bonus, verified FROM draws ORDER BY date DESC, game",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += Draw(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2).split(",").mapNotNull(String::toIntOrNull),
                    if (cursor.isNull(3)) null else cursor.getInt(3),
                    cursor.getInt(4) == 1
                )
            }
        }
        return result
    }

    fun saveTickets(tickets: List<Ticket>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (ticket in tickets) {
                val values = ContentValues().apply {
                    put("game", ticket.game)
                    put("numbers", ticket.numbers.joinToString(","))
                    if (ticket.powerBall == null) putNull("bonus") else put("bonus", ticket.powerBall)
                    put("created", LocalDate.now().toString())
                }
                db.insert("tickets", null, values)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun savedTickets(): List<Ticket> {
        val items = mutableListOf<Ticket>()
        readableDatabase.rawQuery(
            "SELECT id, game, numbers, bonus, created FROM tickets ORDER BY id DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                items += Ticket(
                    c.getLong(0), c.getString(1),
                    c.getString(2).split(",").mapNotNull(String::toIntOrNull),
                    if (c.isNull(3)) null else c.getInt(3), c.getString(4)
                )
            }
        }
        return items
    }
    fun deleteTicket(id: Long) {
        writableDatabase.delete("tickets", "id=?", arrayOf(id.toString()))
    }
}
