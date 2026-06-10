package com.example.util

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.print.PrintAttributes
import android.webkit.WebView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object BillPrintManager {

    fun printPdf(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = object : PrintDocumentAdapter() {
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

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (destination == null) {
                    callback.onWriteFailed("Destination is null")
                    return
                }

                var input: FileInputStream? = null
                var output: FileOutputStream? = null

                try {
                    input = FileInputStream(file)
                    output = FileOutputStream(destination.fileDescriptor)

                    val buffer = ByteArray(1024)
                    var size: Int
                    while (input.read(buffer).also { size = it } >= 0 && cancellationSignal?.isCanceled == false) {
                        output.write(buffer, 0, size)
                    }

                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                    } else {
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    try {
                        input?.close()
                        output?.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }

        printManager.print(jobName, printAdapter, null)
    }
}
