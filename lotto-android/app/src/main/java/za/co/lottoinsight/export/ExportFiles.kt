package za.co.lottoinsight.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ExportFiles {
    private const val YELLOW = 0xFFFFDA00.toInt()
    private const val DARK = 0xFF17191D.toInt()

    fun sharePdf(context: Context, title: String, rows: List<String>) {
        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var pageNumber = 1
        var index = 0
        try {
            do {
                val page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                )
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                paint.color = DARK
                paint.textSize = 23f
                paint.isFakeBoldText = true
                canvas.drawText(title.take(34), 35f, 55f, paint)
                paint.isFakeBoldText = false
                paint.textSize = 12f
                canvas.drawText("Lotto Intelligence · Unofficial selections and analysis", 35f, 80f, paint)
                paint.color = YELLOW
                canvas.drawRect(35f, 95f, 560f, 102f, paint)
                paint.textSize = 14f
                paint.color = DARK
                var y = 133f
                while (index < rows.size && y < 780f) {
                    canvas.drawText(rows[index].take(75), 35f, y, paint)
                    index++
                    y += 29f
                }
                paint.textSize = 10f
                canvas.drawText("18+ · Not a lottery ticket · Play responsibly", 35f, 818f, paint)
                pdf.finishPage(page)
                pageNumber++
            } while (index < rows.size)
            val file = outputFile(context, "pdf")
            FileOutputStream(file).use { pdf.writeTo(it) }
            share(context, file, "application/pdf")
        } finally { pdf.close() }
    }

    fun shareJpeg(context: Context, title: String, rows: List<String>) {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(DARK)
            paint.color = YELLOW
            paint.textSize = 66f
            paint.isFakeBoldText = true
            canvas.drawText(title.take(23), 65f, 130f, paint)
            paint.isFakeBoldText = false
            paint.color = Color.WHITE
            paint.textSize = 27f
            canvas.drawText("LOTTO INTELLIGENCE", 65f, 192f, paint)
            paint.color = YELLOW
            canvas.drawRect(65f, 225f, 1015f, 237f, paint)
            var y = 305f
            paint.color = Color.WHITE
            paint.textSize = 33f
            val maxRows = 33
            rows.take(maxRows).forEach { row ->
                canvas.drawText(row.take(48), 65f, y, paint)
                y += 43f
            }
            if (rows.size > maxRows) {
                paint.textSize = 25f
                canvas.drawText("Showing first $maxRows of ${rows.size} rows", 65f, y + 20f, paint)
            }
            paint.color = YELLOW
            paint.textSize = 24f
            canvas.drawText("18+ · Unofficial · Not a purchased ticket", 65f, 1815f, paint)
            paint.color = Color.LTGRAY
            paint.textSize = 21f
            canvas.drawText("Results: BetTip (bettip.co.za). Verify with operator.", 65f, 1860f, paint)
            val file = outputFile(context, "jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
            share(context, file, "image/jpeg")
        } finally { bitmap.recycle() }
    }

    private fun outputFile(context: Context, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(dir, "lotto-${System.currentTimeMillis()}.$extension")
    }

    private fun share(context: Context, file: File, type: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = type
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Lotto Intelligence"))
    }
}
