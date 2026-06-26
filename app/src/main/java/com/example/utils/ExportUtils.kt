package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.Transaction
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportUtils {

    /**
     * Generates a fully compliant modern Excel (.xlsx) file as bytes
     * without any heavy external dependencies, allowing safe, offline usage.
     */
    fun exportToXlsx(context: Context, transactions: List<Transaction>): File? {
        try {
            val headers = listOf("No", "Tanggal", "Tipe", "Kategori", "Nominal", "Catatan")
            val rows = transactions.mapIndexed { index, t ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.date))
                listOf(
                    (index + 1).toDouble(),
                    dateStr,
                    t.type,
                    t.category,
                    t.amount,
                    t.note
                )
            }

            val file = File(context.cacheDir, "MoneyFlow_Laporan_Transaksi.xlsx")
            val fos = FileOutputStream(file)
            val xlsxBytes = generateXlsxBytes(headers, rows)
            fos.write(xlsxBytes)
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun generateXlsxBytes(headers: List<String>, rows: List<List<Any>>): ByteArray {
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 3. xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="Laporan Transaksi" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 4. xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zos.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 5. xl/worksheets/sheet1.xml
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetData>""")

        // Row 1: Headers
        sb.append("""<row r="1">""")
        for (colIdx in headers.indices) {
            val ref = getColumnLabel(colIdx) + "1"
            val headerVal = escapeXml(headers[colIdx])
            sb.append("""<c r="$ref" t="inlineStr"><is><t>$headerVal</t></is></c>""")
        }
        sb.append("""</row>""")

        // Row 2+: Transactions Table Data
        for (rowIdx in rows.indices) {
            val excelRowNum = rowIdx + 2
            sb.append("""<row r="$excelRowNum">""")
            val rowData = rows[rowIdx]
            for (colIdx in rowData.indices) {
                val ref = getColumnLabel(colIdx) + excelRowNum
                val value = rowData[colIdx]
                if (value is Number) {
                    sb.append("""<c r="$ref" t="n"><v>$value</v></c>""")
                } else {
                    val strVal = escapeXml(value.toString())
                    sb.append("""<c r="$ref" t="inlineStr"><is><t>$strVal</t></is></c>""")
                }
            }
            sb.append("""</row>""")
        }

        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")

        zos.write(sb.toString().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.close()
        return bos.toByteArray()
    }

    private fun getColumnLabel(colIndex: Int): String {
        var temp = colIndex
        val sb = java.lang.StringBuilder()
        while (temp >= 0) {
            sb.insert(0, ('A'.code + (temp % 26)).toChar())
            temp = (temp / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Generates a beautiful multiple page PDF document dynamically, with summary statistics card block.
     */
    fun exportToPdf(context: Context, transactions: List<Transaction>): File? {
        try {
            val file = File(context.cacheDir, "MoneyFlow_Laporan_Keuangan.pdf")
            val outputStream = FileOutputStream(file)
            val pdfDocument = PdfDocument()

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            // Totals Calculations
            var totalIncome = 0.0
            var totalExpense = 0.0
            for (t in transactions) {
                if (t.type.equals("Pemasukan", ignoreCase = true)) {
                    totalIncome += t.amount
                } else {
                    totalExpense += t.amount
                }
            }
            val netBalance = totalIncome - totalExpense

            // Paginate logically to prevent any pixel truncation
            // We show 20 items on page 1 with broad summary header, and 28 on other pages.
            val firstPageSize = 20
            val normalPageSize = 28
            val pagesList = mutableListOf<List<Transaction>>()

            if (transactions.isEmpty()) {
                pagesList.add(emptyList())
            } else {
                var currentIdx = 0
                val firstPageItems = transactions.subList(0, Math.min(transactions.size, firstPageSize))
                pagesList.add(firstPageItems)
                currentIdx += firstPageItems.size

                while (currentIdx < transactions.size) {
                    val nextSize = Math.min(transactions.size - currentIdx, normalPageSize)
                    pagesList.add(transactions.subList(currentIdx, currentIdx + nextSize))
                    currentIdx += nextSize
                }
            }

            val totalPagesCount = pagesList.size

            // Professional Styling Colors (Material Blue Theme)
            val primaryColor = Color.rgb(25, 118, 210)  // Deep Blue
            val incomeColor = Color.rgb(46, 125, 50)     // Forest Green
            val expenseColor = Color.rgb(198, 40, 40)    // Dark Crimson
            val grayBg = Color.rgb(245, 247, 250)         // Soft slate gray
            val borderGray = Color.rgb(224, 224, 224)    // Light silver

            for (pageIdx in pagesList.indices) {
                // A4 Sheet width is 595, height is 842 points.
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val startY: Float
                if (pageIdx == 0) {
                    // Title banner
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = 18f
                    textPaint.color = primaryColor
                    canvas.drawText("LAPORAN KEUANGAN MONEYFLOW", 36f, 48f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.textSize = 10f
                    textPaint.color = Color.DKGRAY
                    val docDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("in", "ID")).format(Date())
                    canvas.drawText("Tanggal Cetak: $docDate", 36f, 65f, textPaint)

                    // Summary statistics background block
                    paint.color = grayBg
                    paint.style = Paint.Style.FILL
                    canvas.drawRoundRect(36f, 80f, 559f, 132f, 8f, 8f, paint)

                    paint.color = borderGray
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(36f, 80f, 559f, 132f, 8f, 8f, paint)

                    // 3 columns: Pemasukan, Pengeluaran, Saldo Bersih
                    textPaint.textSize = 8.5f
                    textPaint.color = Color.GRAY
                    canvas.drawText("Total Pemasukan", 50f, 98f, textPaint)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = 11f
                    textPaint.color = incomeColor
                    canvas.drawText(FormatUtils.formatRupiah(totalIncome), 50f, 116f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.textSize = 8.5f
                    textPaint.color = Color.GRAY
                    canvas.drawText("Total Pengeluaran", 210f, 98f, textPaint)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = 11f
                    textPaint.color = expenseColor
                    canvas.drawText(FormatUtils.formatRupiah(totalExpense), 210f, 116f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textPaint.textSize = 8.5f
                    textPaint.color = Color.GRAY
                    canvas.drawText("Saldo Bersih", 390f, 98f, textPaint)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = 11f
                    textPaint.color = if (netBalance >= 0) incomeColor else expenseColor
                    canvas.drawText(FormatUtils.formatRupiah(netBalance), 390f, 116f, textPaint)

                    startY = 155f
                } else {
                    // Subsequent page header
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = 11f
                    textPaint.color = primaryColor
                    canvas.drawText("LAPORAN KEUANGAN MONEYFLOW (Sambungan)", 36f, 40f, textPaint)

                    paint.color = borderGray
                    paint.strokeWidth = 1f
                    canvas.drawLine(36f, 46f, 559f, 46f, paint)

                    startY = 58f
                }

                // Table Header Row Background
                paint.color = primaryColor
                paint.style = Paint.Style.FILL
                canvas.drawRect(36f, startY, 559f, startY + 20f, paint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 9f
                textPaint.color = Color.WHITE

                // Explicit column grids matching perfect alignments
                val colXNo = 36f
                val colXDate = 68f
                val colXType = 138f
                val colXCat = 210f
                val colXAmount = 295f
                val colXNote = 395f

                canvas.drawText("No", colXNo + 5f, startY + 13f, textPaint)
                canvas.drawText("Tanggal", colXDate + 4f, startY + 13f, textPaint)
                canvas.drawText("Tipe", colXType + 4f, startY + 13f, textPaint)
                canvas.drawText("Kategori", colXCat + 4f, startY + 13f, textPaint)
                canvas.drawText("Nominal", colXAmount + 4f, startY + 13f, textPaint)
                canvas.drawText("Catatan", colXNote + 4f, startY + 13f, textPaint)

                // Fill Row Cells
                var currentY = startY + 20f
                val pageItems = pagesList[pageIdx]

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 8.5f

                for (i in pageItems.indices) {
                    val t = pageItems[i]
                    val prevCount = if (pageIdx == 0) 0 else {
                        var sum = 0
                        for (p in 0 until pageIdx) {
                            sum += pagesList[p].size
                        }
                        sum
                    }
                    val currentNo = prevCount + i + 1

                    // Alternating background shades for clear scannability
                    if (currentNo % 2 == 1) {
                        paint.style = Paint.Style.FILL
                        paint.color = Color.rgb(250, 251, 253)
                        canvas.drawRect(36f, currentY, 559f, currentY + 20f, paint)
                    }

                    // Bottom horizontal outline cell borders
                    paint.style = Paint.Style.STROKE
                    paint.color = borderGray
                    paint.strokeWidth = 0.5f
                    canvas.drawRect(36f, currentY, 559f, currentY + 20f, paint)

                    textPaint.color = Color.BLACK
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    // No
                    canvas.drawText(currentNo.toString(), colXNo + 5f, currentY + 13f, textPaint)

                    // Date
                    val dateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.date))
                    canvas.drawText(dateFormatted, colXDate + 4f, currentY + 13f, textPaint)

                    // Type color accent representation (Green = Pemasukan, Red = Pengeluaran)
                    val isIncome = t.type.equals("Pemasukan", ignoreCase = true)
                    textPaint.color = if (isIncome) incomeColor else expenseColor
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(t.type, colXType + 4f, currentY + 13f, textPaint)

                    // Category
                    textPaint.color = Color.BLACK
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(t.category, colXCat + 4f, currentY + 13f, textPaint)

                    // Nominal
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(FormatUtils.formatRupiah(t.amount), colXAmount + 4f, currentY + 13f, textPaint)

                    // Notes (trim neatly for spacing safety)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val noteStr = t.note
                    val displayNote = if (noteStr.length > 32) noteStr.substring(0, 30) + ".." else noteStr
                    canvas.drawText(displayNote, colXNote + 4f, currentY + 13f, textPaint)

                    currentY += 20f
                }

                // Page Footer (Halaman X dari Y)
                textPaint.color = Color.GRAY
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 8f
                val footerInfoStr = "Halaman ${pageIdx + 1} dari $totalPagesCount   |   MoneyFlow Laporan Digital"
                canvas.drawText(footerInfoStr, 36f, 810f, textPaint)

                pdfDocument.finishPage(page)
            }

            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
