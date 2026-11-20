package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.model.CompanySettings
import com.example.model.Invoice
import com.example.model.Order
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object InvoicePdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        settings: CompanySettings
    ): File {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeNum = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val outFile = File(dir, "Invoice_$safeNum.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val darkPaint = Paint().apply { color = Color.parseColor("#111216") }
        val goldPaint = Paint().apply {
            color = Color.parseColor("#D99B1E")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleWhitePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitleWhitePaint = Paint().apply {
            color = Color.parseColor("#E5C068")
            textSize = 10f
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 10.5f
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }
        val goldLinePaint = Paint().apply {
            color = Color.parseColor("#D99B1E")
            strokeWidth = 2f
        }

        // Header banner
        canvas.drawRect(0f, 0f, 595f, 115f, darkPaint)
        canvas.drawText(settings.companyName.uppercase(), 36f, 44f, titleWhitePaint)
        canvas.drawText(settings.tagline, 36f, 62f, subtitleWhitePaint)
        canvas.drawText("GSTIN: ${settings.gstNumber}", 36f, 82f, subtitleWhitePaint)
        canvas.drawText("Phone: ${settings.contactNumber} | ${settings.companyEmail}", 36f, 96f, subtitleWhitePaint)

        val rightAlignPaint = Paint(goldPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightWhitePaint = Paint(subtitleWhitePaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TAX INVOICE", 559f, 44f, rightAlignPaint)
        canvas.drawText("#${invoice.invoiceNumber}", 559f, 64f, rightWhitePaint)
        canvas.drawText("Book: ${invoice.bookName}", 559f, 80f, rightWhitePaint)
        canvas.drawText("Date: ${Formatters.formatDate(invoice.createdAt)}", 559f, 96f, rightWhitePaint)

        // Company & Customer Info
        var y = 142f
        canvas.drawText("BILL FROM:", 36f, y, boldPaint)
        canvas.drawText("BILL TO:", 320f, y, boldPaint)
        y += 16f
        canvas.drawText(settings.companyName, 36f, y, bodyPaint)
        canvas.drawText(invoice.customerName, 320f, y, bodyPaint)
        y += 14f
        val companyAddr = settings.companyAddress.take(45)
        canvas.drawText(companyAddr, 36f, y, mutedPaint)
        canvas.drawText("Mobile: ${invoice.customerMobile}", 320f, y, mutedPaint)
        y += 14f
        if (invoice.customerAddress.isNotBlank()) {
            canvas.drawText(invoice.customerAddress.take(45), 320f, y, mutedPaint)
            y += 14f
        }
        if (invoice.customerGst.isNotBlank()) {
            canvas.drawText("Customer GSTIN: ${invoice.customerGst}", 320f, y, mutedPaint)
            y += 14f
        }

        y = maxOf(y + 16f, 220f)
        canvas.drawLine(36f, y, 559f, y, goldLinePaint)
        y += 20f

        // Table Header
        val headerBg = Paint().apply { color = Color.parseColor("#F9FAFB") }
        canvas.drawRect(36f, y - 14f, 559f, y + 8f, headerBg)
        canvas.drawText("#", 42f, y, boldPaint)
        canvas.drawText("Product / Description", 70f, y, boldPaint)
        canvas.drawText("Unit", 295f, y, boldPaint)
        canvas.drawText("Qty", 365f, y, boldPaint)
        canvas.drawText("Rate", 420f, y, boldPaint)
        val rightBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Amount", 553f, y, rightBold)
        y += 14f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 18f

        val rightBody = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
        val currency = settings.currencySymbol.ifBlank { "₹" }

        invoice.items.forEachIndexed { idx, item ->
            if (y > 700f) return@forEachIndexed
            canvas.drawText("${idx + 1}", 42f, y, bodyPaint)
            canvas.drawText(item.productName.take(32), 70f, y, bodyPaint)
            canvas.drawText(item.unit.take(12), 295f, y, mutedPaint)
            canvas.drawText("${item.quantity}", 365f, y, bodyPaint)
            canvas.drawText(Formatters.formatCurrency(item.unitPrice, currency), 420f, y, bodyPaint)
            canvas.drawText(Formatters.formatCurrency(item.lineTotal, currency), 553f, y, rightBody)
            y += 18f
            canvas.drawLine(36f, y - 6f, 559f, y - 6f, linePaint)
        }

        y += 14f
        canvas.drawText("Payment Status: ${invoice.paymentStatus}", 36f, y, boldPaint)
        if (invoice.notes.isNotBlank()) {
            canvas.drawText("Notes: ${invoice.notes.take(45)}", 36f, y + 16f, mutedPaint)
        }

        // Totals box
        canvas.drawText("Subtotal:", 380f, y, bodyPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.subtotal, currency), 553f, y, rightBody)
        y += 16f
        canvas.drawText("Discount:", 380f, y, bodyPaint)
        canvas.drawText("-${Formatters.formatCurrency(invoice.discount, currency)}", 553f, y, rightBody)
        y += 16f
        canvas.drawText("GST (${invoice.gstPercent}%):", 380f, y, bodyPaint)
        canvas.drawText(Formatters.formatCurrency(invoice.gstAmount, currency), 553f, y, rightBody)
        y += 10f
        canvas.drawLine(375f, y, 559f, y, goldLinePaint)
        y += 20f
        val grandBold = Paint(boldPaint).apply { textSize = 13.5f }
        val rightGrand = Paint(grandBold).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("GRAND TOTAL:", 380f, y, grandBold)
        canvas.drawText(Formatters.formatCurrency(invoice.grandTotal, currency), 553f, y, rightGrand)

        // Footer
        canvas.drawLine(36f, 780f, 559f, 780f, linePaint)
        canvas.drawText(settings.invoiceFooter.take(85), 36f, 800f, mutedPaint)
        canvas.drawText("Generated by ULTRAWRAP SOLUTION Direct Book Billing", 36f, 816f, mutedPaint)

        document.finishPage(page)
        FileOutputStream(outFile).use { fos -> document.writeTo(fos) }
        document.close()
        return outFile
    }

    fun generateSalesReportPdf(
        context: Context,
        title: String,
        dateRangeLabel: String,
        totalRevenue: Double,
        totalOrders: Int,
        totalInvoices: Int,
        productSales: List<Pair<String, Double>>,
        customerSales: List<Pair<String, Double>>,
        settings: CompanySettings
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(dir, "UltraWrap_Sales_Report.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val darkPaint = Paint().apply { color = Color.parseColor("#111216") }
        val titleWhite = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val goldSub = Paint().apply {
            color = Color.parseColor("#E5C068")
            textSize = 11f
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 10.5f
        }
        val currency = settings.currencySymbol.ifBlank { "₹" }

        canvas.drawRect(0f, 0f, 595f, 92f, darkPaint)
        canvas.drawText("${settings.companyName} — $title", 36f, 40f, titleWhite)
        canvas.drawText("Period: $dateRangeLabel | ${settings.tagline}", 36f, 64f, goldSub)

        var y = 126f
        canvas.drawText("Summary Metrics", 36f, y, boldPaint)
        y += 20f
        canvas.drawText("Total Revenue: ${Formatters.formatCurrency(totalRevenue, currency)}", 36f, y, bodyPaint)
        canvas.drawText("Total Orders: $totalOrders", 260f, y, bodyPaint)
        canvas.drawText("Direct Invoices: $totalInvoices", 410f, y, bodyPaint)

        y += 32f
        canvas.drawText("Product-wise Sales Breakdown", 36f, y, boldPaint)
        y += 18f
        productSales.take(10).forEach { (name, amount) ->
            canvas.drawText(name, 44f, y, bodyPaint)
            canvas.drawText(Formatters.formatCurrency(amount, currency), 380f, y, bodyPaint)
            y += 16f
        }

        y += 22f
        canvas.drawText("Top Customers by Revenue", 36f, y, boldPaint)
        y += 18f
        customerSales.take(10).forEach { (name, amount) ->
            canvas.drawText(name, 44f, y, bodyPaint)
            canvas.drawText(Formatters.formatCurrency(amount, currency), 380f, y, bodyPaint)
            y += 16f
        }

        document.finishPage(page)
        FileOutputStream(outFile).use { fos -> document.writeTo(fos) }
        document.close()
        return outFile
    }

    fun generateSalesReportCsv(
        context: Context,
        orders: List<Order>,
        invoices: List<Invoice>
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(dir, "UltraWrap_Sales_Export.csv")
        val sb = StringBuilder()
        sb.appendLine("Type,Reference,Date,Customer,Mobile,Status,Subtotal,Discount,Tax,Total")
        orders.forEach { o ->
            sb.appendLine(
                "Order,${o.orderNumber},${Formatters.formatDate(o.createdAt)},\"${o.customerName.replace("\"", "\"\"")}\",${o.customerMobile},${o.status},${o.subtotal},${o.discount},0,${o.totalAmount}"
            )
        }
        invoices.forEach { inv ->
            sb.appendLine(
                "DirectBill,${inv.invoiceNumber},${Formatters.formatDate(inv.createdAt)},\"${inv.customerName.replace("\"", "\"\"")}\",${inv.customerMobile},${inv.paymentStatus},${inv.subtotal},${inv.discount},${inv.gstAmount},${inv.grandTotal}"
            )
        }
        outFile.writeText(sb.toString())
        return outFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    fun encodePickedImageToDataUri(context: Context, uri: Uri): String? {
        return runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(stream)
            stream.close()
            if (original == null) return null
            val maxDim = 480
            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val width = (original.width * ratio).toInt().coerceAtLeast(1)
            val height = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, width, height, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 78, baos)
            val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        }.getOrNull()
    }
}
