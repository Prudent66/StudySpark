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
        department: String,
        courseTitle: String,
        timeAllowed: String,
        examDate: String,
        examTitle: String,
        instructions: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size in points (72 DPI)
        
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        paint.color = Color.BLACK
        
        // --- Define Text Paints for different elements ---
        val schoolPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 18f; isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val deptPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 13f; isAntiAlias = true
        }
        val coursePaint = TextPaint().apply {
            color = Color.BLACK; textSize = 14f; isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val examTitlePaint = TextPaint().apply {
            color = Color.BLACK; textSize = 13f; isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
        }
        val metaPaint = TextPaint().apply {
            color = Color.DKGRAY; textSize = 12f; isAntiAlias = true
        }
        val instructionsPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 11.5f; isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
        }
        val questionPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 12.5f; isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val optionPaint = TextPaint().apply {
            color = Color.BLACK; textSize = 12f; isAntiAlias = true
        }

        val margin = 50f
        val pageWidth = 595f
        val pageHeight = 842f
        val contentWidth = pageWidth - (margin * 2)
        var yPos = margin

        // --- Helper Functions ---
        fun drawCentered(text: String, paintToUse: TextPaint) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paintToUse, contentWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.1f)
                .build()
            canvas.save()
            canvas.translate(margin, yPos)
            layout.draw(canvas)
            canvas.restore()
            yPos += layout.height + 6f
        }

        // --- 1. Draw Header ---
        drawCentered(schoolName.uppercase(), schoolPaint)
        drawCentered(department, deptPaint)
        drawCentered(courseTitle, coursePaint)
        drawCentered(examTitle, examTitlePaint)
        
        yPos += 10f
        
        // Draw Time Allowed (Left) and Date (Right)
        canvas.drawText(timeAllowed, margin, yPos, metaPaint)
        val dateWidth = metaPaint.measureText(examDate)
        canvas.drawText(examDate, pageWidth - margin - dateWidth, yPos, metaPaint)
        
        yPos += 15f
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, paint) // Divider line
        yPos += 15f

        // Draw Instructions
        if (instructions.isNotBlank()) {
            val instLayout = StaticLayout.Builder.obtain(instructions, 0, instructions.length, instructionsPaint, contentWidth.toInt())
                .setLineSpacing(0f, 1.2f)
                .build()
            canvas.save()
            canvas.translate(margin, yPos)
            instLayout.draw(canvas)
            canvas.restore()
            yPos += instLayout.height + 15f
        }

        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, paint) // Another divider
        yPos += 20f

        // --- 2. Draw Questions ---
        questions.forEachIndexed { index, question ->
            // Format the question text with "Q1." at the beginning
            val questionText = "Q${index + 1}. ${question.text}"
            
            val qLayout = StaticLayout.Builder.obtain(questionText, 0, questionText.length, questionPaint, contentWidth.toInt())
                .setLineSpacing(0f, 1.2f)
                .build()

            // Check if we need a new page (leave room for at least one option)
            if (yPos + qLayout.height + 60 > pageHeight - margin) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin
            }

            canvas.save()
            canvas.translate(margin, yPos)
            qLayout.draw(canvas)
            canvas.restore()
            yPos += qLayout.height + 12f

            // Draw Options (for Objectives)
            if (question.questionType == "Objectives" && question.options.isNotEmpty() && question.options[0].isNotBlank()) {
                val labels = listOf("A", "B", "C", "D")
                question.options.forEachIndexed { optIndex, option ->
                    val optText = "${labels[optIndex]}. $option"
                    val optLayout = StaticLayout.Builder.obtain(optText, 0, optText.length, optionPaint, contentWidth.toInt() - 20)
                        .setLineSpacing(0f, 1.1f)
                        .build()
                    
                    // Check page break for options
                    if (yPos + optLayout.height > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = margin
                    }

                    canvas.save()
                    canvas.translate(margin + 15, yPos) // Indent options slightly
                    optLayout.draw(canvas)
                    canvas.restore()
                    yPos += optLayout.height + 6f
                }
            }
            yPos += 20f // Space between questions
        }

        pdfDocument.finishPage(page)

        // --- 3. Save File ---
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
