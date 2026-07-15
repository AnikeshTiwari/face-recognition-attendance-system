package com.example.attendanceapp
import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

fun exportAttendanceToPdf(context: Context, attendanceHistory: List<String>): File? {
    return try {
        // Create a new PDF document
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        var y = 40f
        canvas.drawText("Attendance History", 40f, y, paint)
        y += 20f
        attendanceHistory.forEach { record ->
            canvas.drawText(record, 40f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, use Scoped Storage
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "attendance_${System.currentTimeMillis()}.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Save to Downloads folder
            }

            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
                Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
            }
        } else {
            // For Android below 10, save it in the old method
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "attendance_${System.currentTimeMillis()}.pdf"
            val file = File(downloadsDir, fileName)

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            Toast.makeText(context, "PDF saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
        null // Return null or File as needed
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        null
    }
}
