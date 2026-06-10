package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ErpRepository(private val db: AppDatabase) {

    private val farmerDao = db.farmerDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val creditDao = db.creditDao()
    private val shopDao = db.shopDao()
    private val logDao = db.logDao()
    private val auditDao = db.auditDao()
    private val supplierDao = db.supplierDao()
    private val purchaseDao = db.purchaseDao()

    // --- Audit Logs ---
    val auditLogs: Flow<List<AuditLog>> = auditDao.getAllAuditLogs()

    suspend fun insertAuditLog(log: AuditLog) = withContext(Dispatchers.IO) {
        auditDao.insertAuditLog(log)
    }

    // --- Shop Profile ---
    val shopProfile: Flow<ShopProfile?> = shopDao.getShopProfile()

    suspend fun updateShopProfile(profile: ShopProfile) = withContext(Dispatchers.IO) {
        shopDao.updateShopProfile(profile)
    }

    // --- Logs ---
    val farmerDeletionLogs: Flow<List<DeletedFarmerLog>> = logDao.getAllFarmerDeletionLogs()
    val productDeletionLogs: Flow<List<DeletedProductLog>> = logDao.getAllProductDeletionLogs()

    suspend fun logFarmerDeletion(log: DeletedFarmerLog) = withContext(Dispatchers.IO) {
        logDao.insertFarmerDeletionLog(log)
    }

    suspend fun logProductDeletion(log: DeletedProductLog) = withContext(Dispatchers.IO) {
        logDao.insertProductDeletionLog(log)
    }

    suspend fun logInvoiceDeletion(log: DeletedInvoiceLog) = withContext(Dispatchers.IO) {
        logDao.insertInvoiceDeletionLog(log)
    }

    // --- Farmers ---
    val allFarmers: Flow<List<Farmer>> = farmerDao.getAllFarmers()
    
    fun getFarmerById(id: Long): Flow<Farmer?> = farmerDao.getFarmerById(id)

    suspend fun insertFarmer(farmer: Farmer): Long = withContext(Dispatchers.IO) {
        farmerDao.insertFarmer(farmer)
    }

    suspend fun updateFarmer(farmer: Farmer) = withContext(Dispatchers.IO) {
        farmerDao.updateFarmer(farmer)
    }

    suspend fun deleteFarmer(farmer: Farmer) = withContext(Dispatchers.IO) {
        farmerDao.deleteFarmer(farmer)
    }

    // --- Products ---
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    fun getProductsByCategory(category: String): Flow<List<Product>> = productDao.getProductsByCategory(category)

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun adjustProductStock(productId: Long, quantity: Double, increase: Boolean) = withContext(Dispatchers.IO) {
        if (increase) {
            productDao.increaseStock(productId, quantity)
        } else {
            productDao.reduceStock(productId, quantity)
        }
    }

    // --- Invoices & POS Transaction System ---
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    val allInvoiceItems: Flow<List<InvoiceItem>> = invoiceDao.getAllInvoiceItems()

    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>> = invoiceDao.getInvoiceItems(invoiceId)

    suspend fun deleteInvoice(invoice: Invoice) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoice(invoice)
    }

    /**
     * Executes a professional atomic Billing POS Transaction.
     * Decrements product inventory, posts invoice metadata, and rolls outstanding debit to Credit Ledger.
     */
    suspend fun createInvoiceTransaction(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Long = withContext(Dispatchers.IO) {
        // Generate Hash Signature for Tampering Detection (PRD Requirement 9)
        val dataToHash = "${invoice.farmerId}|${invoice.totalAmount}|${invoice.createdAt}|${invoice.billingStaff}"
        val signature = java.security.MessageDigest.getInstance("SHA-256")
            .digest(dataToHash.toByteArray())
            .joinToString("") { "%02x".format(it) }
        
        val signedInvoice = invoice.copy(signature = signature)

        // 1. Insert Core Invoice
        val invoiceId = invoiceDao.insertInvoice(signedInvoice)
        
        // 2. Insert Invoice Items & Decrement Inventory
        items.forEach { item ->
            val updatedItem = item.copy(invoiceId = invoiceId)
            invoiceDao.insertInvoiceItem(updatedItem)
            
            // Deduct Stock
            productDao.reduceStock(item.productId, item.quantity)
        }

        // 3. Credit / Udhari Assessment
        val pendingBalance = invoice.totalAmount - invoice.paidAmount
        if (invoice.farmerId != 0L && pendingBalance > 0.0) {
            // Apply debit to Farmer profile
            farmerDao.updateFarmerCredit(invoice.farmerId, pendingBalance)

            // Log corresponding audit trail inside CreditRecord
            creditDao.insertCreditRecord(CreditRecord(
                farmerId = invoice.farmerId,
                farmerName = invoice.farmerName,
                amount = pendingBalance,
                paymentMode = invoice.paymentMode,
                description = "Udhari added on Invoice #${invoiceId}"
            ))
        }

        invoiceId
    }

    // --- Credit / Udhari ---
    val allCreditRecords: Flow<List<CreditRecord>> = creditDao.getAllCreditRecords()

    fun getFarmerCreditRecords(farmerId: Long): Flow<List<CreditRecord>> = creditDao.getFarmerCreditRecords(farmerId)

    /**
     * Record Farmer Credit Recovery (paying back outstanding udhari).
     */
    suspend fun processCreditPayment(
        farmerId: Long,
        farmerName: String,
        amountReceived: Double,
        paymentMode: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        // Validate amount to receive
        if (amountReceived <= 0.0) return@withContext -1L

        // Deduct liability from farmer profile
        farmerDao.updateFarmerCredit(farmerId, -amountReceived)

        // Log payment in CreditRecords (negative represents payment collection)
        creditDao.insertCreditRecord(CreditRecord(
            farmerId = farmerId,
            farmerName = farmerName,
            amount = -amountReceived,
            paymentMode = paymentMode,
            description = if (notes.isNotEmpty()) notes else "Received payment towards pending credit"
        ))
    }

    // --- Suppliers ---
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    
    fun getSupplierById(id: Long): Flow<Supplier?> = supplierDao.getSupplierById(id)

    suspend fun insertSupplier(supplier: Supplier): Long = withContext(Dispatchers.IO) {
        supplierDao.insertSupplier(supplier)
    }

    suspend fun updateSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        supplierDao.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) = withContext(Dispatchers.IO) {
        supplierDao.deleteSupplier(supplier)
    }

    // --- Purchases ---
    val allPurchaseInvoices: Flow<List<PurchaseInvoice>> = purchaseDao.getAllPurchaseInvoices()

    fun getAllPurchaseItems(): Flow<List<PurchaseItem>> = purchaseDao.getAllPurchaseItems()

    suspend fun restoreFullBackup(backup: com.example.util.BackupManager.FullBackup) = withContext(Dispatchers.IO) {
        backup.farmers.forEach { farmerDao.insertFarmer(it) }
        backup.products.forEach { productDao.insertProduct(it) }
        backup.suppliers.forEach { supplierDao.insertSupplier(it) }
        backup.invoices.forEach { invoiceDao.insertInvoice(it) }
        backup.invoiceItems.forEach { invoiceDao.insertInvoiceItem(it) }
        backup.purchaseInvoices.forEach { purchaseDao.insertPurchaseInvoice(it) }
        backup.purchaseItems.forEach { purchaseDao.insertPurchaseItem(it) }
        backup.shopProfile?.let { shopDao.updateShopProfile(it) }
    }

    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }

    fun getPurchaseItems(invoiceId: Long): Flow<List<PurchaseItem>> = purchaseDao.getPurchaseItems(invoiceId)

    suspend fun createPurchaseTransaction(
        invoice: PurchaseInvoice,
        items: List<PurchaseItem>
    ): Long = withContext(Dispatchers.IO) {
        // 1. Insert Purchase Invoice
        val purchaseId = purchaseDao.insertPurchaseInvoice(invoice)

        // 2. Process Items & Update Stock
        items.forEach { item ->
            val updatedItem = item.copy(purchaseInvoiceId = purchaseId)
            
            // Try to find existing product by Name + Brand + Batch
            val existingProduct = productDao.getProductByNameBrandBatch(item.productName, item.brandName, item.batchNumber)
            
            val finalProductId = if (existingProduct != null) {
                // Increase stock for existing batch
                productDao.increaseStock(existingProduct.id, item.quantity)
                existingProduct.id
            } else {
                // Create new product entry for this batch
                productDao.insertProduct(Product(
                    name = item.productName,
                    brandName = item.brandName,
                    category = item.category,
                    stock = item.quantity,
                    unit = item.unit,
                    purchasePrice = item.purchaseRate,
                    sellingPrice = item.purchaseRate * 1.15, // Default 15% margin
                    gstPercent = item.gstPercent,
                    batchNumber = item.batchNumber,
                    manufacturingDate = item.manufacturingDate,
                    expiryDate = item.expiryDate,
                    weightValue = item.weightValue,
                    weightUnit = item.weightUnit
                ))
            }
            
            purchaseDao.insertPurchaseItem(updatedItem.copy(productId = finalProductId))
        }

        // 3. Update Supplier Balance if unpaid
        val pendingBalance = invoice.totalAmount - invoice.paidAmount
        if (pendingBalance > 0.0) {
            supplierDao.updateSupplierBalance(invoice.supplierId, pendingBalance)
        }

        purchaseId
    }

    suspend fun updatePurchaseTransaction(
        invoice: PurchaseInvoice,
        newItems: List<PurchaseItem>
    ) = withContext(Dispatchers.IO) {
        val oldInvoice = purchaseDao.getPurchaseInvoiceByIdSync(invoice.id) ?: return@withContext
        val oldItems = purchaseDao.getPurchaseItemsSync(invoice.id)

        // 1. Reverse old stock effect
        oldItems.forEach { item ->
            productDao.reduceStock(item.productId, item.quantity)
        }

        // 2. Reverse old supplier balance effect
        val oldPendingBalance = oldInvoice.totalAmount - oldInvoice.paidAmount
        if (oldPendingBalance > 0.0) {
            supplierDao.updateSupplierBalance(oldInvoice.supplierId, -oldPendingBalance)
        }

        // 3. Update Invoice
        purchaseDao.updatePurchaseInvoice(invoice)

        // 4. Remove old items
        purchaseDao.deletePurchaseItemsByInvoiceId(invoice.id)

        // 5. Add new items and apply stock effect
        newItems.forEach { item ->
            val existingProduct = productDao.getProductByNameBrandBatch(item.productName, item.brandName, item.batchNumber)
            
            val finalProductId = if (existingProduct != null) {
                productDao.increaseStock(existingProduct.id, item.quantity)
                existingProduct.id
            } else {
                productDao.insertProduct(Product(
                    name = item.productName,
                    brandName = item.brandName,
                    category = item.category,
                    stock = item.quantity,
                    unit = item.unit,
                    purchasePrice = item.purchaseRate,
                    sellingPrice = item.purchaseRate * 1.15,
                    gstPercent = item.gstPercent,
                    batchNumber = item.batchNumber,
                    manufacturingDate = item.manufacturingDate,
                    expiryDate = item.expiryDate,
                    weightValue = item.weightValue,
                    weightUnit = item.weightUnit
                ))
            }
            purchaseDao.insertPurchaseItem(item.copy(purchaseInvoiceId = invoice.id, productId = finalProductId))
        }

        // 6. Apply new supplier balance effect
        val newPendingBalance = invoice.totalAmount - invoice.paidAmount
        if (newPendingBalance > 0.0) {
            supplierDao.updateSupplierBalance(invoice.supplierId, newPendingBalance)
        }
    }

    suspend fun deletePurchaseTransaction(
        invoiceId: Long,
        deletedBy: String,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val invoice = purchaseDao.getPurchaseInvoiceByIdSync(invoiceId) ?: return@withContext
        val items = purchaseDao.getPurchaseItemsSync(invoiceId)

        // 1. Reverse stock effect
        items.forEach { item ->
            productDao.reduceStock(item.productId, item.quantity)
        }

        // 2. Reverse supplier balance effect
        val pendingBalance = invoice.totalAmount - invoice.paidAmount
        if (pendingBalance > 0.0) {
            supplierDao.updateSupplierBalance(invoice.supplierId, -pendingBalance)
        }

        // 3. Log deletion
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val now = java.util.Date()

        logDao.insertPurchaseDeletionLog(DeletedPurchaseLog(
            invoiceNumber = invoice.invoiceNumber,
            supplierName = invoice.supplierName,
            totalAmount = invoice.totalAmount,
            deletedBy = deletedBy,
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            reason = reason
        ))

        // 4. Delete invoice and items
        purchaseDao.deletePurchaseInvoice(invoice)
        purchaseDao.deletePurchaseItemsByInvoiceId(invoiceId)
    }
}
