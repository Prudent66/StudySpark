package com.studyspark.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.studyspark.app.domain.model.DraftQuestion
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateQuizPdf(
        context: Context,
        questions: List<DraftQuestion>,
        schoolName: String,
        examTitle: String,
        courseCode: String,
        instructions: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size in points
        
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        val textPaint = TextPaint()
        textPaint.color = Color.BLACK
        textPaint.textSize = 12f
        textPaint.isAntiAlias = true

        val boldPaint = TextPaint(textPaint)
        boldPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        
        val margin = 50f
        val contentWidth = 595 - (margin * 2)
        var yPos = margin

        // 1. Draw Header
        val headerLayout = StaticLayout.Builder.obtain(schoolName, 0, schoolName.length, boldPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(margin, yPos)
        headerLayout.draw(canvas)
        canvas.restore()
        yPos += headerLayout.height + 10f

        val titleLayout = StaticLayout.Builder.obtain(examTitle, 0, examTitle.length, boldPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(margin, yPos)
        titleLayout.draw(canvas)
        canvas.restore()
        yPos += titleLayout.height + 5f

        val courseLayout = StaticLayout.Builder.obtain(courseCode, 0, courseCode.length, textPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(margin, yPos)
        courseLayout.draw(canvas)
        canvas.restore()
        yPos += courseLayout.height + 15f

        val instLayout = StaticLayout.Builder.obtain(instructions, 0, instructions.length, textPaint, contentWidth.toInt())
            .build()
        canvas.save()
        canvas.translate(margin, yPos)
        instLayout.draw(canvas)
        canvas.restore()
        yPos += instLayout.height + 20f

        canvas.drawLine(margin, yPos, 595 - margin, yPos, paint)
        yPos += 20f

        // 2. Draw Questions
        questions.forEachIndexed { index, question ->
            val questionText = "Question ${index + 1}: ${question.text}"
            val qLayout = StaticLayout.Builder.obtain(questionText, 0, questionText.length, boldPaint, contentWidth.toInt())
                .build()

            // Check if we need a new page
            if (yPos + qLayout.height + 100 > 842 - margin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin
            }

            canvas.save()
            canvas.translate(margin, yPos)
            qLayout.draw(canvas)
            canvas.restore()
            yPos += qLayout.height + 10f

            if (question.options.isNotEmpty() && question.options[0] != "See Explanation") {
                val labels = listOf("A", "B", "C", "D")
                question.options.forEachIndexed { optIndex, option ->
                    val optText = "${labels[optIndex]}. $option"
                    val optLayout = StaticLayout.Builder.obtain(optText, 0, optText.length, textPaint, contentWidth.toInt() - 20)
                        .build()
                    
                    if (yPos + optLayout.height > 842 - margin) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = margin
                    }

                    canvas.save()
                    canvas.translate(margin + 20, yPos)
                    optLayout.draw(canvas)
                    canvas.restore()
                    yPos += optLayout.height + 5f
                }
            }
            yPos += 15f
        }

        pdfDocument.finishPage(page)

        // 3. Save File
        val directory = File(context.getExternalFilesDir(null), "StudySpark_PDFs")
        if (!directory.exists()) directory.mkdirs()
        
        val fileName = "Quiz_${System.currentTimeMillis()}.pdf"
        val file = File(directory, fileName)

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        return file
    }
}
