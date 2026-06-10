package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.*
import com.example.ui.viewmodel.ErpViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {
    
    @JsonClass(generateAdapter = true)
    data class FullBackup(
        val farmers: List<Farmer>,
        val products: List<Product>,
        val suppliers: List<Supplier>,
        val invoices: List<Invoice>,
        val invoiceItems: List<InvoiceItem>,
        val purchaseInvoices: List<PurchaseInvoice>,
        val purchaseItems: List<PurchaseItem>,
        val shopProfile: ShopProfile?
    )

    suspend fun createBackup(context: Context, viewModel: ErpViewModel): File? = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(FullBackup::class.java)
            
            val backup = FullBackup(
                farmers = viewModel.farmers.value,
                products = viewModel.products.value,
                suppliers = viewModel.suppliers.value,
                invoices = viewModel.invoices.value,
                invoiceItems = viewModel.allInvoiceItems.value,
                purchaseInvoices = viewModel.purchaseInvoices.value,
                purchaseItems = viewModel.allPurchaseItems.value,
                shopProfile = viewModel.shopProfile.value
            )
            
            val json = adapter.toJson(backup)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(context.cacheDir, "GKK_Backup_$timestamp.json")
            
            FileOutputStream(file).use { 
                it.write(json.toByteArray())
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreBackup(context: Context, uri: Uri, viewModel: ErpViewModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(FullBackup::class.java)
            
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@withContext false
            val backup = adapter.fromJson(json) ?: return@withContext false
            
            // Logic to restore into DB via ViewModel/Repository
            viewModel.restoreFromBackup(backup)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
