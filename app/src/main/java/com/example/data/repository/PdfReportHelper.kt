package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Child
import com.example.data.model.GlucoseReading
import com.example.data.model.TestResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for generating medical PDF reports.
 * Supports Arabic text and RTL layout.
 */
object PdfReportHelper {

    private const val PAGE_WIDTH = 595  // A4 width in points
    private const val PAGE_HEIGHT = 842  // A4 height in points
    private const val MARGIN = 40
    private const val LINE_HEIGHT = 20

    /**
     * Generate a PDF report for test results.
     */
    fun generateLabResultsPdf(
        context: Context,
        child: Child,
        results: List<TestResult>,
        filterPeriod: String,
        testFilter: String
    ): ByteArray {
        val document = PdfDocument()
        var pageNumber = 1
        var currentY = MARGIN
        var page = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var canvas = document.startPage(page).canvas

        // Title
        currentY = drawTitle(canvas, "تقرير نتائج التحاليل الطبية", currentY)
        currentY += 10

        // Child Info
        currentY = drawText(canvas, "اسم الطفل: ${child.name}", currentY, 12f)
        currentY = drawText(canvas, "العمر: ${child.ageText} | تاريخ الميلاد: ${child.birthDate}", currentY, 12f)
        currentY = drawText(canvas, "الفترة: $filterPeriod", currentY, 12f)
        if (testFilter != "الكل") {
            currentY = drawText(canvas, "نوع التحليل: $testFilter", currentY, 12f)
        }
        currentY += 15

        // Table Header
        canvas.drawRect(
            MARGIN.toFloat(),
            currentY.toFloat(),
            (PAGE_WIDTH - MARGIN).toFloat(),
            (currentY + 25).toFloat(),
            Paint().apply {
                color = android.graphics.Color.parseColor("#00796B")
                style = Paint.Style.FILL
            }
        )

        drawTableHeader(canvas, currentY)
        currentY += 25

        // Results rows
        for (result in results) {
            if (currentY + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(canvas)
                pageNumber++
                page = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                canvas = document.startPage(page).canvas
                currentY = MARGIN
            }

            // Alternate row background
            if (results.indexOf(result) % 2 == 0) {
                canvas.drawRect(
                    MARGIN.toFloat(),
                    currentY.toFloat(),
                    (PAGE_WIDTH - MARGIN).toFloat(),
                    (currentY + LINE_HEIGHT).toFloat(),
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#E0F2F1")
                        style = Paint.Style.FILL
                    }
                )
            }

            drawTableRow(canvas, currentY, result)
            currentY += LINE_HEIGHT
        }

        // Footer
        val footer = "تم إنشاء التقرير في: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date())}"
        currentY = PAGE_HEIGHT - MARGIN
        drawText(canvas, footer, currentY, 10f)

        document.finishPage(canvas)

        // Convert to ByteArray
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    /**
     * Generate a PDF report for glucose readings.
     */
    fun generateGlucosePdf(
        context: Context,
        child: Child,
        readings: List<GlucoseReading>,
        filterPeriod: String
    ): ByteArray {
        val document = PdfDocument()
        var pageNumber = 1
        var currentY = MARGIN
        var page = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var canvas = document.startPage(page).canvas

        // Title
        currentY = drawTitle(canvas, "تقرير قراءات السكر اليومية", currentY)
        currentY += 10

        // Child Info
        currentY = drawText(canvas, "اسم الطفل: ${child.name}", currentY, 12f)
        currentY = drawText(canvas, "العمر: ${child.ageText} | تاريخ الميلاد: ${child.birthDate}", currentY, 12f)
        currentY = drawText(canvas, "الفترة: $filterPeriod", currentY, 12f)
        currentY += 15

        // Summary stats
        if (readings.isNotEmpty()) {
            val average = readings.map { it.readingValue }.average()
            val minValue = readings.minOf { it.readingValue }
            val maxValue = readings.maxOf { it.readingValue }
            
            currentY = drawText(canvas, "الإحصائيات:", currentY, 12f, isBold = true)
            currentY = drawText(canvas, "المتوسط: ${String.format("%.1f", average)} mg/dL", currentY, 11f)
            currentY = drawText(canvas, "الأدنى: $minValue mg/dL | الأعلى: $maxValue mg/dL", currentY, 11f)
            currentY += 15
        }

        // Table Header
        canvas.drawRect(
            MARGIN.toFloat(),
            currentY.toFloat(),
            (PAGE_WIDTH - MARGIN).toFloat(),
            (currentY + 25).toFloat(),
            Paint().apply {
                color = android.graphics.Color.parseColor("#1565C0")
                style = Paint.Style.FILL
            }
        )

        drawGlucoseTableHeader(canvas, currentY)
        currentY += 25

        // Readings rows
        for ((index, reading) in readings.withIndex()) {
            if (currentY + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(canvas)
                pageNumber++
                page = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                canvas = document.startPage(page).canvas
                currentY = MARGIN
            }

            // Alternate row background
            if (index % 2 == 0) {
                canvas.drawRect(
                    MARGIN.toFloat(),
                    currentY.toFloat(),
                    (PAGE_WIDTH - MARGIN).toFloat(),
                    (currentY + LINE_HEIGHT).toFloat(),
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#E3F2FD")
                        style = Paint.Style.FILL
                    }
                )
            }

            drawGlucoseTableRow(canvas, currentY, reading)
            currentY += LINE_HEIGHT
        }

        // Footer
        val footer = "تم إنشاء التقرير في: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date())}"
        currentY = PAGE_HEIGHT - MARGIN
        drawText(canvas, footer, currentY, 10f)

        document.finishPage(canvas)

        // Convert to ByteArray
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    /**
     * Share a PDF report via intent.
     */
    fun sharePdf(
        context: Context,
        pdfBytes: ByteArray,
        childName: String,
        reportType: String
    ) {
        try {
            // Save PDF to cache
            val fileName = "Mawaeedna_${childName}_${reportType}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            file.writeBytes(pdfBytes)

            // Get URI using FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "com.example.mawaeedna.fileprovider",
                file
            )

            // Create share intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير طبي - $childName")
                putExtra(Intent.EXTRA_TEXT, "يرجى العثور على تقرير $reportType المرفق لـ $childName")
            }

            context.startActivity(Intent.createChooser(intent, "مشاركة التقرير"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper functions

    private fun drawTitle(canvas: Canvas, text: String, y: Int): Int {
        val paint = TextPaint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#00796B")
        }
        canvas.drawText(text, MARGIN.toFloat(), (y + 15).toFloat(), paint)
        return y + 30
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        y: Int,
        size: Float = 11f,
        isBold: Boolean = false
    ): Int {
        val paint = TextPaint().apply {
            textSize = size
            if (isBold) isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        canvas.drawText(text, MARGIN.toFloat(), (y + 12).toFloat(), paint)
        return y + LINE_HEIGHT
    }

    private fun drawTableHeader(canvas: Canvas, y: Int) {
        val paint = TextPaint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }

        val headers = listOf("التاريخ", "الاختبار", "النتيجة", "المدى الطبيعي", "الملاحظات")
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / headers.size

        for ((index, header) in headers.withIndex()) {
            val x = MARGIN + (index * colWidth) + 10
            canvas.drawText(header, x.toFloat(), (y + 15).toFloat(), paint)
        }
    }

    private fun drawTableRow(canvas: Canvas, y: Int, result: TestResult) {
        val paint = TextPaint().apply {
            textSize = 10f
            color = android.graphics.Color.BLACK
        }

        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 5

        canvas.drawText(result.testDateText, (MARGIN + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText(result.testName, (MARGIN + colWidth + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText(result.resultValue, (MARGIN + colWidth * 2 + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText(result.normalRangeText, (MARGIN + colWidth * 3 + 10).toFloat(), (y + 12).toFloat(), paint)
        
        val notes = if (result.doctorNotes.length > 15) result.doctorNotes.take(15) + "..." else result.doctorNotes
        canvas.drawText(notes, (MARGIN + colWidth * 4 + 10).toFloat(), (y + 12).toFloat(), paint)
    }

    private fun drawGlucoseTableHeader(canvas: Canvas, y: Int) {
        val paint = TextPaint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }

        val headers = listOf("التاريخ", "الوقت", "القراءة", "السياق", "ملاحظات")
        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / headers.size

        for ((index, header) in headers.withIndex()) {
            val x = MARGIN + (index * colWidth) + 10
            canvas.drawText(header, x.toFloat(), (y + 15).toFloat(), paint)
        }
    }

    private fun drawGlucoseTableRow(canvas: Canvas, y: Int, reading: GlucoseReading) {
        val paint = TextPaint().apply {
            textSize = 10f
            color = android.graphics.Color.BLACK
        }

        val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 5

        canvas.drawText(reading.dateText, (MARGIN + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText(reading.timeText, (MARGIN + colWidth + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText("${reading.readingValue}", (MARGIN + colWidth * 2 + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText(reading.mealContext.labelAr, (MARGIN + colWidth * 3 + 10).toFloat(), (y + 12).toFloat(), paint)
        
        val notes = if (reading.notes.length > 12) reading.notes.take(12) + "..." else reading.notes
        canvas.drawText(notes, (MARGIN + colWidth * 4 + 10).toFloat(), (y + 12).toFloat(), paint)
    }
}
