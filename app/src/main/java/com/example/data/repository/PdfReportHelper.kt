package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
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
 */
object PdfReportHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40
    private const val LINE_HEIGHT = 20
    private const val GLUCOSE_CHART_HEIGHT = 190

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
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        currentY = drawTitle(canvas, "تقرير نتائج التحاليل الطبية", currentY)
        currentY += 10
        currentY = drawText(canvas, "اسم الطفل: ${child.name}", currentY, 12f)
        currentY = drawText(canvas, "العمر: ${child.ageText} | تاريخ الميلاد: ${child.birthDate}", currentY, 12f)
        currentY = drawText(canvas, "الفترة: $filterPeriod", currentY, 12f)
        if (testFilter != "الكل") {
            currentY = drawText(canvas, "نوع التحليل: $testFilter", currentY, 12f)
        }
        currentY += 15

        canvas.drawRect(
            MARGIN.toFloat(), currentY.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (currentY + 25).toFloat(),
            Paint().apply {
                color = android.graphics.Color.parseColor("#00796B")
                style = Paint.Style.FILL
            }
        )
        drawTableHeader(canvas, currentY)
        currentY += 25

        for ((index, result) in results.withIndex()) {
            if (currentY + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                currentY = MARGIN
            }

            if (index % 2 == 0) {
                canvas.drawRect(
                    MARGIN.toFloat(), currentY.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (currentY + LINE_HEIGHT).toFloat(),
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#E0F2F1")
                        style = Paint.Style.FILL
                    }
                )
            }
            drawTableRow(canvas, currentY, result)
            currentY += LINE_HEIGHT
        }

        val footer = "تم إنشاء التقرير في: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date())}"
        drawText(canvas, footer, PAGE_HEIGHT - MARGIN, 10f)
        document.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    /**
     * Generate a PDF report for glucose readings including the same trend chart
     * shown in the application, followed by the detailed readings table.
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
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        currentY = drawTitle(canvas, "تقرير قراءات السكر اليومية", currentY)
        currentY += 10
        currentY = drawText(canvas, "اسم الطفل: ${child.name}", currentY, 12f)
        currentY = drawText(canvas, "العمر: ${child.ageText} | تاريخ الميلاد: ${child.birthDate}", currentY, 12f)
        currentY = drawText(canvas, "الفترة: $filterPeriod", currentY, 12f)
        currentY += 10

        if (readings.isNotEmpty()) {
            val average = readings.map { it.readingValue }.average()
            val minValue = readings.minOf { it.readingValue }
            val maxValue = readings.maxOf { it.readingValue }

            currentY = drawText(canvas, "الإحصائيات:", currentY, 12f, isBold = true)
            currentY = drawText(canvas, "عدد القراءات: ${readings.size}", currentY, 11f)
            currentY = drawText(canvas, "المتوسط: ${String.format(Locale.US, "%.1f", average)} mg/dL", currentY, 11f)
            currentY = drawText(canvas, "الأدنى: $minValue mg/dL | الأعلى: $maxValue mg/dL", currentY, 11f)
            currentY += 8

            // Keep the complete chart together on one page.
            if (currentY + GLUCOSE_CHART_HEIGHT + 35 > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                currentY = MARGIN
            }

            currentY = drawText(canvas, "الرسم البياني للقراءات", currentY, 12f, isBold = true)
            drawGlucoseChart(canvas, readings, currentY, GLUCOSE_CHART_HEIGHT)
            currentY += GLUCOSE_CHART_HEIGHT + 15
        }

        if (currentY + 25 + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            currentY = MARGIN
        }

        canvas.drawRect(
            MARGIN.toFloat(), currentY.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (currentY + 25).toFloat(),
            Paint().apply {
                color = android.graphics.Color.parseColor("#1565C0")
                style = Paint.Style.FILL
            }
        )
        drawGlucoseTableHeader(canvas, currentY)
        currentY += 25

        for ((index, reading) in readings.withIndex()) {
            if (currentY + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                currentY = MARGIN

                // Repeat the table header on every continuation page.
                canvas.drawRect(
                    MARGIN.toFloat(), currentY.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (currentY + 25).toFloat(),
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#1565C0")
                        style = Paint.Style.FILL
                    }
                )
                drawGlucoseTableHeader(canvas, currentY)
                currentY += 25
            }

            if (index % 2 == 0) {
                canvas.drawRect(
                    MARGIN.toFloat(), currentY.toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (currentY + LINE_HEIGHT).toFloat(),
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#E3F2FD")
                        style = Paint.Style.FILL
                    }
                )
            }

            drawGlucoseTableRow(canvas, currentY, reading)
            currentY += LINE_HEIGHT
        }

        val footer = "تم إنشاء التقرير في: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date())}"
        drawText(canvas, footer, PAGE_HEIGHT - MARGIN, 10f)
        document.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    fun sharePdf(
        context: Context,
        pdfBytes: ByteArray,
        childName: String,
        reportType: String
    ) {
        try {
            val fileName = "Mawaeedna_${childName}_${reportType}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            file.writeBytes(pdfBytes)

            val uri = FileProvider.getUriForFile(
                context,
                "com.example.mawaeedna.fileprovider",
                file
            )

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

    private fun drawGlucoseChart(canvas: Canvas, readings: List<GlucoseReading>, top: Int, chartHeight: Int) {
        if (readings.isEmpty()) return

        val sorted = readings.sortedBy { it.createdAt }
        val left = MARGIN + 38f
        val right = PAGE_WIDTH - MARGIN - 10f
        val chartTop = top + 18f
        val chartBottom = top + chartHeight - 28f
        val graphWidth = right - left
        val graphHeight = chartBottom - chartTop

        val minValue = sorted.minOf { it.readingValue }
        val maxValue = sorted.maxOf { it.readingValue }
        val minY = maxOf(0, minOf(60, minValue - 10))
        val maxY = maxOf(200, maxValue + 20)
        val yRange = (maxY - minY).coerceAtLeast(1).toFloat()

        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(left, chartTop, right, chartBottom, borderPaint)

        val gridPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        val axisTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 8f
            textAlign = Paint.Align.RIGHT
        }

        listOf(70, 110, 150, 190).filter { it in minY..maxY }.forEach { value ->
            val ratio = (value - minY).toFloat() / yRange
            val y = chartBottom - ratio * graphHeight
            canvas.drawLine(left, y, right, y, gridPaint)
            canvas.drawText(value.toString(), left - 6f, y + 3f, axisTextPaint)
        }

        val minX = sorted.first().createdAt
        val maxX = sorted.last().createdAt
        val xRange = (maxX - minX).takeIf { it != 0L }?.toFloat() ?: 1f

        val points = sorted.map { reading ->
            val x = if (maxX == minX) {
                left + graphWidth / 2f
            } else {
                left + ((reading.createdAt - minX).toFloat() / xRange) * graphWidth
            }
            val y = chartBottom - ((reading.readingValue - minY).toFloat() / yRange) * graphHeight
            Triple(x, y, reading)
        }

        if (points.size > 1) {
            val fillPath = Path().apply {
                moveTo(points.first().first, points.first().second)
                points.drop(1).forEach { lineTo(it.first, it.second) }
                lineTo(points.last().first, chartBottom)
                lineTo(points.first().first, chartBottom)
                close()
            }
            val fillPaint = Paint().apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, chartTop, 0f, chartBottom,
                    android.graphics.Color.argb(55, 0, 168, 150),
                    android.graphics.Color.argb(0, 0, 168, 150),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawPath(fillPath, fillPaint)

            val linePath = Path().apply {
                moveTo(points.first().first, points.first().second)
                points.drop(1).forEach { lineTo(it.first, it.second) }
            }
            canvas.drawPath(linePath, Paint().apply {
                color = android.graphics.Color.parseColor("#00A896")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            })
        }

        val valuePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#004D40")
            textSize = 8f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val dotPaint = Paint().apply { color = android.graphics.Color.parseColor("#00796B") }
        val innerDotPaint = Paint().apply { color = android.graphics.Color.WHITE }

        points.forEach { (x, y, reading) ->
            canvas.drawCircle(x, y, 4.5f, dotPaint)
            canvas.drawCircle(x, y, 2f, innerDotPaint)
            canvas.drawText(reading.readingValue.toString(), x, y - 7f, valuePaint)
        }

        val dateIndices = when {
            sorted.size == 1 -> listOf(0)
            sorted.size == 2 -> listOf(0, 1)
            else -> listOf(0, sorted.size / 2, sorted.size - 1)
        }.distinct()
        val datePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val dateFormat = SimpleDateFormat("d MMM", Locale("ar"))
        dateIndices.forEach { index ->
            val point = points[index]
            val label = try {
                dateFormat.format(Date(sorted[index].createdAt))
            } catch (e: Exception) {
                sorted[index].dateText
            }
            canvas.drawText(label, point.first, chartBottom + 14f, datePaint)
        }
    }

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
        canvas.drawText(reading.readingValue.toString(), (MARGIN + colWidth * 2 + 10).toFloat(), (y + 12).toFloat(), paint)
        canvas.drawText(reading.mealContext.labelAr, (MARGIN + colWidth * 3 + 10).toFloat(), (y + 12).toFloat(), paint)
        val notes = if (reading.notes.length > 12) reading.notes.take(12) + "..." else reading.notes
        canvas.drawText(notes, (MARGIN + colWidth * 4 + 10).toFloat(), (y + 12).toFloat(), paint)
    }
}
