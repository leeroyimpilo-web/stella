package za.co.lottoinsight.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object DrawSync {
    private const val URL_DRAWS = "https://bettip.co.za/api/v1/lotto/draws.json"
    private const val LAST_SYNC = "last_sync"

    suspend fun update(context: Context, db: LottoDatabase, force: Boolean = false): Int =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("lotto_sync", Context.MODE_PRIVATE)
            if (!force && System.currentTimeMillis() - prefs.getLong(LAST_SYNC, 0) < 6 * 60 * 60 * 1000L) {
                return@withContext 0
            }
            val connection = URL(URL_DRAWS).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 40000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "LottoIntelligenceAndroid/0.1 (+https://bettip.co.za/)")
            try {
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                if (payload.length > 30_000_000) error("Archive too large")
                val updated = db.importDraws(payload)
                if (updated == 0) error("No valid draw rows found")
                prefs.edit().putLong(LAST_SYNC, System.currentTimeMillis()).apply()
                updated
            } finally { connection.disconnect() }
        }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work = PeriodicWorkRequestBuilder<DrawSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "lotto_draw_updates", ExistingPeriodicWorkPolicy.KEEP, work
        )
    }
}

class DrawSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        DrawSync.update(applicationContext, LottoDatabase(applicationContext))
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
