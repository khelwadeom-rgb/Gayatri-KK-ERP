package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.PurchaseInvoice
import com.example.data.model.PurchaseItem
import com.example.data.model.Farmer
import com.example.data.model.Product
import com.example.data.model.ShopProfile
import com.example.gayatrikrushikendra.R
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

object BillShareManager {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points

    private fun getDirectory(context: Context, subDir: String): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(root, "GayatriKrushiKendra/$subDir")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateFarmerDirectoryPdf(
        context: Context,
        farmers: List<Farmer>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val margin = 40f
        var currentY = 50f

        // --- 1. Header ---
        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, margin, currentY, paint)
        currentY += 25

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(shop.shopAddress, margin, currentY, paint)
        currentY += 15
        canvas.drawText("Owner: ${shop.ownerName} | Mob: ${shop.mobileNumber}", margin, currentY, paint)
        currentY += 25

        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val now = Calendar.getInstance(tz).time
        val dateTimeStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).apply { timeZone = tz }.format(now)
        canvas.drawText("Generated on: $dateTimeStr", margin, currentY, paint)
        currentY += 20

        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 30

        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FARMER DIRECTORY", (PAGE_WIDTH - paint.measureText("FARMER DIRECTORY")) / 2, currentY, paint)
        currentY += 30

        // --- 2. Table Header ---
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val col1 = margin
        val col2 = margin + 150f
        val col3 = margin + 280f
        val col4 = PAGE_WIDTH - margin - 80f

        canvas.drawText("Farmer Name", col1, currentY, paint)
        canvas.drawText("Village", col2, currentY, paint)
        canvas.drawText("Mobile", col3, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Udhari (₹)", PAGE_WIDTH - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        // --- 3. Table Rows ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var totalOutstanding = 0.0
        
        for (farmer in farmers) {
            canvas.drawText(farmer.name, col1, currentY, paint)
            canvas.drawText(farmer.village, col2, currentY, paint)
            canvas.drawText(farmer.mobile, col3, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(farmer.outstandingCredit.format(2), PAGE_WIDTH - margin, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            
            totalOutstanding += farmer.outstandingCredit
            currentY += 20

            if (currentY > PAGE_HEIGHT - 100) {
                // Potential page break logic could go here
            }
        }

        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 25

        // --- 4. Summary & Footer ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Farmers: ${farmers.size}", margin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total Outstanding: ₹${totalOutstanding.format(2)}", PAGE_WIDTH - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        
        currentY = PAGE_HEIGHT - 50f
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val footerText = "Generated by ${shop.shopName} ERP"
        canvas.drawText(footerText, (PAGE_WIDTH - paint.measureText(footerText)) / 2, currentY, paint)

        pdfDocument.finishPage(page)

        val file = File(getDirectory(context, "Reports"), "Farmer_Directory.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    fun generateStockDirectoryPdf(
        context: Context,
        products: List<Product>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val margin = 40f
        var currentY = 50f

        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, margin, currentY, paint)
        currentY += 25

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("STOCK DIRECTORY", margin, currentY, paint)
        currentY += 20
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 30

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Product Name", margin, currentY, paint)
        canvas.drawText("Brand", margin + 180f, currentY, paint)
        canvas.drawText("Stock", margin + 280f, currentY, paint)
        canvas.drawText("Batch", margin + 340f, currentY, paint)
        canvas.drawText("Expiry", margin + 420f, currentY, paint)
        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (prod in products) {
            canvas.drawText(prod.name, margin, currentY, paint)
            canvas.drawText(prod.brandName, margin + 180f, currentY, paint)
            canvas.drawText("${prod.stock} ${prod.unit}", margin + 280f, currentY, paint)
            canvas.drawText(prod.batchNumber, margin + 340f, currentY, paint)
            canvas.drawText(formatDate(prod.expiryDate), margin + 420f, currentY, paint)
            currentY += 15
            if (currentY > PAGE_HEIGHT - 50) break
        }

        pdfDocument.finishPage(page)
        val file = File(getDirectory(context, "Reports"), "Stock_Report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    }

    fun generateGstReportPdf(
        context: Context,
        invoices: List<Invoice>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val margin = 40f
        var currentY = 50f

        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, margin, currentY, paint)
        currentY += 25
        canvas.drawText("GST SALES REPORT", margin, currentY, paint)
        currentY += 20
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 30

        var totalTaxable = 0.0
        var totalCgst = 0.0
        var totalSgst = 0.0
        var totalNet = 0.0

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date", margin, currentY, paint)
        canvas.drawText("Invoice #", margin + 70f, currentY, paint)
        canvas.drawText("Taxable", margin + 150f, currentY, paint)
        canvas.drawText("CGST", margin + 220f, currentY, paint)
        canvas.drawText("SGST", margin + 290f, currentY, paint)
        canvas.drawText("Net Total", margin + 360f, currentY, paint)
        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (inv in invoices) {
            canvas.drawText(inv.createdDate, margin, currentY, paint)
            canvas.drawText(inv.id.toString(), margin + 70f, currentY, paint)
            canvas.drawText(inv.taxableAmount.format(2), margin + 150f, currentY, paint)
            canvas.drawText(inv.cgst.format(2), margin + 220f, currentY, paint)
            canvas.drawText(inv.sgst.format(2), margin + 290f, currentY, paint)
            canvas.drawText(inv.totalAmount.format(2), margin + 360f, currentY, paint)
            
            totalTaxable += inv.taxableAmount
            totalCgst += inv.cgst
            totalSgst += inv.sgst
            totalNet += inv.totalAmount
            currentY += 15
        }

        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTALS", margin, currentY, paint)
        canvas.drawText(totalTaxable.format(2), margin + 150f, currentY, paint)
        canvas.drawText(totalCgst.format(2), margin + 220f, currentY, paint)
        canvas.drawText(totalSgst.format(2), margin + 290f, currentY, paint)
        canvas.drawText(totalNet.format(2), margin + 360f, currentY, paint)

        pdfDocument.finishPage(page)
        val file = File(getDirectory(context, "Reports"), "GST_Report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    }

    fun generateSalesReportPdf(
        context: Context,
        invoices: List<Invoice>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val margin = 40f
        var currentY = 50f

        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, margin, currentY, paint)
        currentY += 25
        canvas.drawText("SALES SUMMARY REPORT", margin, currentY, paint)
        currentY += 20
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 30

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date", margin, currentY, paint)
        canvas.drawText("Farmer", margin + 70f, currentY, paint)
        canvas.drawText("Mode", margin + 250f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total (₹)", PAGE_WIDTH - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var totalSales = 0.0
        for (inv in invoices) {
            canvas.drawText(inv.createdDate, margin, currentY, paint)
            canvas.drawText(inv.farmerName, margin + 70f, currentY, paint)
            canvas.drawText(inv.paymentMode, margin + 250f, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(inv.totalAmount.format(2), PAGE_WIDTH - margin, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            
            totalSales += inv.totalAmount
            currentY += 15
            if (currentY > PAGE_HEIGHT - 50) break
        }

        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL GROSS SALES", margin, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${totalSales.format(2)}", PAGE_WIDTH - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT

        pdfDocument.finishPage(page)
        val file = File(getDirectory(context, "Reports"), "Sales_Summary.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    }

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        val margin = 40f
        var currentY = 40f

        // --- PAID Watermark (Large, Center) ---
        if (invoice.remainingBalance <= 0.1 && invoice.paymentMode != "Credit") {
            val watermarkPaint = Paint().apply {
                color = Color.parseColor("#4CAF50") // Green
                alpha = 40 // Very faint
                textSize = 120f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.save()
            canvas.rotate(-45f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
            canvas.drawText("PAID", PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, watermarkPaint)
            canvas.restore()
        }

        // --- 1. Top Border ---
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(margin, 30f, PAGE_WIDTH - margin, PAGE_HEIGHT - 30f, paint)
        paint.style = Paint.Style.FILL

        // --- PAID Stamp (Small, Bottom Right) ---
        if (invoice.remainingBalance <= 0.1 && invoice.paymentMode != "Credit") {
            val stampPaint = Paint().apply {
                color = Color.parseColor("#0F6B3E")
                alpha = 220 
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val stampX = PAGE_WIDTH - margin - 100f
            val stampY = PAGE_HEIGHT - 200f
            
            canvas.save()
            canvas.rotate(-15f, stampX, stampY)
            
            // Draw border first
            stampPaint.style = Paint.Style.STROKE
            stampPaint.strokeWidth = 3f
            val text = "PAID"
            val rect = Rect()
            stampPaint.getTextBounds(text, 0, text.length, rect)
            canvas.drawRect(
                stampX - (rect.width() / 2f) - 15,
                stampY - rect.height() - 15,
                stampX + (rect.width() / 2f) + 15,
                stampY + 15,
                stampPaint
            )
            
            // Draw "PAID" text
            stampPaint.style = Paint.Style.FILL
            canvas.drawText(text, stampX, stampY, stampPaint)

            canvas.restore()
        }

        // --- 2. Header ---
        val logoSize = 85
        val logoBitmap = if (shop.invoiceLogoPath != null) {
            try {
                val original = BitmapFactory.decodeFile(shop.invoiceLogoPath)
                if (original != null) Bitmap.createScaledBitmap(original, logoSize, logoSize, true)
                else vectorToBitmap(context, R.drawable.gkk_logo, logoSize, logoSize)
            } catch (e: Exception) {
                vectorToBitmap(context, R.drawable.gkk_logo, logoSize, logoSize)
            }
        } else {
            vectorToBitmap(context, R.drawable.gkk_logo, logoSize, logoSize)
        }
        logoBitmap?.let {
            canvas.drawBitmap(it, margin + 5, currentY, paint)
        }

        // Center Info (Business Name & Address)
        paint.color = Color.parseColor("#1B5E20") // Deep Green
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val shopName = shop.shopName.uppercase()
        val headerInfoX = margin + logoSize + 15
        canvas.drawText(shopName, headerInfoX, currentY + 20, paint)

        paint.color = Color.BLACK
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(shop.shopAddress, headerInfoX, currentY + 35, paint)
        canvas.drawText("Pro: ${shop.ownerName}", headerInfoX, currentY + 50, paint)
        canvas.drawText("Mob: ${shop.mobileNumber}", headerInfoX, currentY + 65, paint)

        // Right Info (License Numbers)
        paint.textAlign = Paint.Align.RIGHT
        val headerRightX = PAGE_WIDTH - margin - 5
        
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GSTIN: ${shop.gstNumber}", headerRightX, currentY + 15, paint)
        
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Pesticide License No: ${shop.pesticideLicense}", headerRightX, currentY + 32, paint)
        canvas.drawText("Seeds License No: ${shop.seedLicense}", headerRightX, currentY + 47, paint)
        canvas.drawText("Cotton License No: ${shop.cottonLicense}", headerRightX, currentY + 62, paint)
        
        paint.textAlign = Paint.Align.LEFT // Reset alignment

        currentY += 85
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        
        // --- TAX INVOICE Title ---
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val title = "TAX INVOICE"
        canvas.drawText(title, (PAGE_WIDTH - paint.measureText(title)) / 2, currentY + 20, paint)
        
        currentY += 30
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        // --- 3. Farmer & Invoice Info Grid ---
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Farmer: ${invoice.farmerName}", margin + 10, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Invoice No: #${invoice.id}", PAGE_WIDTH - margin - 10, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 15

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Village: ${invoice.farmerVillage}", margin + 10, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Date: ${invoice.createdDate}", PAGE_WIDTH - margin - 10, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 15

        canvas.drawText("Mobile: ${invoice.farmerMobile}", margin + 10, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Time: ${invoice.createdTime}", PAGE_WIDTH - margin - 10, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 15

        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        // --- 4. Table Header ---
        val colWidths = floatArrayOf(25f, 120f, 60f, 45f, 40f, 40f, 25f, 35f, 40f, 35f, 50f)
        val headers = arrayOf("Sr", "Product Name", "Brand", "Batch", "MFG", "Exp", "Qty", "Wgt", "Rate", "GST%", "Total")
        val alignments = arrayOf(Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.CENTER, Paint.Align.RIGHT)
        
        val tableTop = currentY
        val tableWidth = PAGE_WIDTH - 2 * margin
        
        // Draw Header Background
        paint.color = Color.LTGRAY
        canvas.drawRect(margin, currentY, margin + tableWidth, currentY + 20f, paint)
        paint.color = Color.BLACK
        
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var tempX = margin
        for (i in headers.indices) {
            // Draw Header Text
            val textX = when (alignments[i]) {
                Paint.Align.LEFT -> tempX + 5f
                Paint.Align.RIGHT -> tempX + colWidths[i] - 5f
                else -> tempX + colWidths[i] / 2f
            }
            paint.textAlign = alignments[i]
            canvas.drawText(headers[i], textX, currentY + 14f, paint)
            
            // Draw Vertical Line (Left)
            paint.style = Paint.Style.STROKE
            canvas.drawLine(tempX, currentY, tempX, currentY + 20f, paint)
            paint.style = Paint.Style.FILL
            
            tempX += colWidths[i]
        }
        // Last Vertical Line (Right)
        canvas.drawLine(margin + tableWidth, currentY, margin + tableWidth, currentY + 20f, paint)
        
        // Draw Horizontal Lines (Top & Bottom of Header)
        canvas.drawLine(margin, currentY, margin + tableWidth, currentY, paint)
        canvas.drawLine(margin, currentY + 20f, margin + tableWidth, currentY + 20f, paint)
        
        currentY += 20f

        // --- 5. Table Content ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for ((index, item) in items.withIndex()) {
            val rowHeight = 18f
            
            // Alternating Background
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F5F5F5") // Very Light Grey
                canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, paint)
                paint.color = Color.BLACK
            }
            
            tempX = margin
            val rowData = arrayOf(
                (index + 1).toString(),
                item.productName,
                item.brandName,
                item.batchNumber,
                formatDate(item.mfgDate),
                formatDate(item.expiryDate),
                item.quantity.toString(),
                "${item.weightValue.format()} ${item.weightUnit}",
                item.sellingPrice.format(2),
                "${item.gstPercent}%",
                (item.sellingPrice * item.quantity).format(2)
            )

            for (i in rowData.indices) {
                paint.textAlign = alignments[i]
                val text = rowData[i]
                val availableWidth = colWidths[i] - 6
                
                val truncated = if (paint.measureText(text) > availableWidth) {
                    text.substring(0, paint.breakText(text, true, availableWidth, null))
                } else text
                
                val textX = when (alignments[i]) {
                    Paint.Align.LEFT -> tempX + 4f
                    Paint.Align.RIGHT -> tempX + colWidths[i] - 4f
                    else -> tempX + colWidths[i] / 2f
                }
                
                canvas.drawText(truncated, textX, currentY + 13f, paint)
                
                // Draw Vertical Line
                paint.style = Paint.Style.STROKE
                canvas.drawLine(tempX, currentY, tempX, currentY + rowHeight, paint)
                paint.style = Paint.Style.FILL
                
                tempX += colWidths[i]
            }
            // Last Vertical Line
            canvas.drawLine(margin + tableWidth, currentY, margin + tableWidth, currentY + rowHeight, paint)
            
            // Draw Bottom Border of Row
            canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, paint)
            
            currentY += rowHeight
        }

        // Fill remaining space with empty grid lines up to minimum table height
        val minTableBottom = 400f
        while (currentY < minTableBottom) {
            val rowHeight = 18f
            tempX = margin
            for (i in colWidths.indices) {
                canvas.drawLine(tempX, currentY, tempX, currentY + rowHeight, paint)
                tempX += colWidths[i]
            }
            canvas.drawLine(margin + tableWidth, currentY, margin + tableWidth, currentY + rowHeight, paint)
            canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, paint)
            currentY += rowHeight
        }
        
        currentY += 20f

        // --- 6. Summary & QR ---
        // QR Code (Left)
        val qrBitmap = if (shop.qrImagePath != null) {
            try {
                val original = BitmapFactory.decodeFile(shop.qrImagePath)
                if (original != null) Bitmap.createScaledBitmap(original, 100, 100, true)
                else {
                    val upiUri = "upi://pay?pa=${shop.upiId}&pn=${shop.shopName.replace(" ", "%20")}&am=${invoice.totalAmount}&cu=INR"
                    QrCodeGenerator.generate(upiUri, 100)
                }
            } catch (e: Exception) {
                val upiUri = "upi://pay?pa=${shop.upiId}&pn=${shop.shopName.replace(" ", "%20")}&am=${invoice.totalAmount}&cu=INR"
                QrCodeGenerator.generate(upiUri, 100)
            }
        } else {
            val upiUri = "upi://pay?pa=${shop.upiId}&pn=${shop.shopName.replace(" ", "%20")}&am=${invoice.totalAmount}&cu=INR"
            QrCodeGenerator.generate(upiUri, 100)
        }

        qrBitmap?.let {
            canvas.drawBitmap(it, margin + 10, currentY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 10f
            canvas.drawText("Scan & Pay", margin + 30, currentY + 105, paint)
            
            // Add UPI ID below QR code
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            canvas.drawText("UPI ID: ${shop.upiId}", margin + 15, currentY + 95, paint)
        }

        // Summary (Right) - Bordered Section
        val summaryX = PAGE_WIDTH - margin - 180f
        val summaryWidth = 180f
        var summaryY = currentY
        
        paint.style = Paint.Style.STROKE
        canvas.drawRect(summaryX, summaryY, summaryX + summaryWidth, summaryY + 80f, paint)
        paint.style = Paint.Style.FILL
        
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        canvas.drawText("Taxable Amount:", summaryX + 5, summaryY + 15, paint)
        canvas.drawText("CGST:", summaryX + 5, summaryY + 30, paint)
        canvas.drawText("SGST:", summaryX + 5, summaryY + 45, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${invoice.taxableAmount.format(2)}", summaryX + summaryWidth - 5, summaryY + 15, paint)
        canvas.drawText("₹${invoice.cgst.format(2)}", summaryX + summaryWidth - 5, summaryY + 30, paint)
        canvas.drawText("₹${invoice.sgst.format(2)}", summaryX + summaryWidth - 5, summaryY + 45, paint)
        
        canvas.drawLine(summaryX, summaryY + 55, summaryX + summaryWidth, summaryY + 55, paint)
        
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#1B5E20")
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Grand Total:", summaryX + 5, summaryY + 72, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹${invoice.totalAmount.format(2)}", summaryX + summaryWidth - 5, summaryY + 72, paint)
        paint.color = Color.BLACK
        
        currentY += 120f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Amount in Words: ${NumberToWords.convert(invoice.totalAmount)}", margin + 10, currentY, paint)

        // --- 7. Udhari Balance Section ---
        if (invoice.paymentMode == "Udhari") {
            currentY += 30
            paint.style = Paint.Style.STROKE
            canvas.drawRect(margin + 10, currentY - 15, PAGE_WIDTH - margin - 10, currentY + 50, paint)
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CREDIT BALANCE INFO", margin + 20, currentY, paint)
            currentY += 15
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Previous Balance: ₹${invoice.previousBalance.format(2)}", margin + 20, currentY, paint)
            canvas.drawText("Current Bill: ₹${invoice.totalAmount.format(2)}", margin + 200, currentY, paint)
            currentY += 15
            canvas.drawText("Paid Amount: ₹${invoice.paidAmount.format(2)}", margin + 20, currentY, paint)
            paint.color = Color.RED
            canvas.drawText("Total Remaining: ₹${invoice.remainingBalance.format(2)}", margin + 200, currentY, paint)
            paint.color = Color.BLACK
        }

        // --- 8. Marathi Footer & Thank You ---
        currentY = PAGE_HEIGHT - 100f
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 25
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Thank You For Visiting!", PAGE_WIDTH / 2f, currentY, paint)
        currentY += 18
        canvas.drawText(shop.shopName, PAGE_WIDTH / 2f, currentY, paint)
        currentY += 20
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("This Bill is Generated by computer | GKK ERP", PAGE_WIDTH / 2f, currentY, paint)
        pdfDocument.finishPage(page)

        val fileName = "Invoice_${String.format("%04d", invoice.id)}.pdf"
        val file = File(getDirectory(context, "Bills"), fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    private fun vectorToBitmap(context: Context, drawableId: Int, width: Int, height: Int): Bitmap? {
        val drawable = context.getDrawable(drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "-"
        return java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.gkk.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report/Invoice"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareOnWhatsapp(context: Context, file: File, phoneNumber: String = "") {
        try {
            val authority = "${context.packageName}.gkk.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                // If phone number is provided, we can try to target WhatsApp directly
                if (phoneNumber.isNotEmpty()) {
                    val targetNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
                    // Note: Direct message with attachment is tricky on WhatsApp without Accessibility or Business API
                    // but we can at least open the share sheet with WhatsApp preferred or pre-fill text.
                }
                `package` = "com.whatsapp"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to regular share if WhatsApp is not installed
            sharePdf(context, file)
        }
    }

    fun viewPdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.gkk.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer found or error opening: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareApp(context: Context) {
        try {
            val sourceFile = File(context.applicationInfo.sourceDir)
            val destFile = File(context.cacheDir, "GAYATRI_ERP_v1.apk")
            
            // Copy current APK to cache folder for external sharing
            sourceFile.copyTo(destFile, overwrite = true)

            val authority = "${context.packageName}.gkk.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, destFile)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Gayatri Krushi Kendra ERP Installation")
                putExtra(Intent.EXTRA_TEXT, "GAYATRI KRUSHI KENDRA ERP\nProfessional Agri-Business Management Solution.\n\nClick to install the application directly.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ERP App via"))
        } catch (e: Exception) {
            Log.e("BillShareManager", "App share failed: ${e.message}")
            Toast.makeText(context, "Failed to prepare installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generatePurchaseInvoicePdf(
        context: Context,
        invoice: PurchaseInvoice,
        items: List<PurchaseItem>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val margin = 40f
        var currentY = 50f

        // --- Header ---
        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, margin, currentY, paint)
        currentY += 25

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("PURCHASE INVOICE", margin, currentY, paint)
        currentY += 20
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 30

        // --- Supplier & Invoice Info ---
        paint.textSize = 10f
        canvas.drawText("Supplier: ${invoice.supplierName}", margin, currentY, paint)
        canvas.drawText("Invoice #: ${invoice.invoiceNumber}", margin + 300f, currentY, paint)
        currentY += 15
        canvas.drawText("Date: ${invoice.invoiceDate}", margin + 300f, currentY, paint)
        currentY += 25

        // --- Table Header ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Product", margin, currentY, paint)
        canvas.drawText("Qty", margin + 250f, currentY, paint)
        canvas.drawText("Rate", margin + 350f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Total", PAGE_WIDTH - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        // --- Items ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (item in items) {
            canvas.drawText(item.productName, margin, currentY, paint)
            canvas.drawText("${item.quantity} ${item.unit}", margin + 250f, currentY, paint)
            canvas.drawText("₹${item.purchaseRate.format()}", margin + 350f, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹${item.total.format()}", PAGE_WIDTH - margin, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            currentY += 15
        }

        currentY += 15
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 25

        // --- Summary ---
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Grand Total: ₹${invoice.totalAmount.format()}", PAGE_WIDTH - margin, currentY, paint)
        currentY += 15
        canvas.drawText("Status: ${invoice.paymentStatus}", PAGE_WIDTH - margin, currentY, paint)

        pdfDocument.finishPage(page)
        val file = File(getDirectory(context, "Purchases"), "Purchase_${invoice.invoiceNumber}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    fun generatePurchaseSummaryPdf(
        context: Context,
        purchases: List<PurchaseInvoice>,
        shop: ShopProfile
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val margin = 40f
        var currentY = 50f

        // Header
        paint.color = Color.parseColor("#1B5E20")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(shop.shopName, margin, currentY, paint)
        currentY += 25
        paint.color = Color.BLACK
        paint.textSize = 12f
        canvas.drawText("Purchase Summary Report", margin, currentY, paint)
        currentY += 30

        // Table
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date", margin, currentY, paint)
        canvas.drawText("Invoice", margin + 100f, currentY, paint)
        canvas.drawText("Supplier", margin + 200f, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", PAGE_WIDTH - margin, currentY, paint)
        paint.textAlign = Paint.Align.LEFT
        currentY += 10
        canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, paint)
        currentY += 20

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for (p in purchases) {
            canvas.drawText(p.invoiceDate, margin, currentY, paint)
            canvas.drawText(p.invoiceNumber, margin + 100f, currentY, paint)
            canvas.drawText(p.supplierName, margin + 200f, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹${p.totalAmount.toInt()}", PAGE_WIDTH - margin, currentY, paint)
            paint.textAlign = Paint.Align.LEFT
            currentY += 15
        }

        pdfDocument.finishPage(page)
        val file = File(getDirectory(context, "Reports"), "Purchase_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }
}

private class TextPaint : Paint(ANTI_ALIAS_FLAG)
