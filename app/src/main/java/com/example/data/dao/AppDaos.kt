package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmers ORDER BY name ASC")
    fun getAllFarmers(): Flow<List<Farmer>>

    @Query("SELECT * FROM farmers WHERE id = :id")
    fun getFarmerById(id: Long): Flow<Farmer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: Farmer): Long

    @Update
    suspend fun updateFarmer(farmer: Farmer)

    @Delete
    suspend fun deleteFarmer(farmer: Farmer)

    @Query("UPDATE farmers SET outstandingCredit = outstandingCredit + :additionalAmount WHERE id = :farmerId")
    suspend fun updateFarmerCredit(farmerId: Long, additionalAmount: Double)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Double)

    @Query("UPDATE products SET stock = stock + :quantity WHERE id = :productId")
    suspend fun increaseStock(productId: Long, quantity: Double)

    @Query("SELECT * FROM products WHERE name = :name AND brandName = :brand AND batchNumber = :batch LIMIT 1")
    suspend fun getProductByNameBrandBatch(name: String, brand: String, batch: String): Product?
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceById(id: Long): Flow<Invoice?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItem): Long

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoice_items ORDER BY id DESC")
    fun getAllInvoiceItems(): Flow<List<InvoiceItem>>
}

@Dao
interface CreditDao {
    @Query("SELECT * FROM credit_records ORDER BY timestamp DESC")
    fun getAllCreditRecords(): Flow<List<CreditRecord>>

    @Query("SELECT * FROM credit_records WHERE farmerId = :farmerId ORDER BY timestamp DESC")
    fun getFarmerCreditRecords(farmerId: Long): Flow<List<CreditRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditRecord(record: CreditRecord): Long
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_profile WHERE id = 1")
    fun getShopProfile(): Flow<ShopProfile?>

    @Query("SELECT * FROM shop_profile WHERE id = 1")
    suspend fun getShopProfileSync(): ShopProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateShopProfile(profile: ShopProfile)
}

@Dao
interface LogDao {
    @Insert
    suspend fun insertFarmerDeletionLog(log: DeletedFarmerLog)

    @Insert
    suspend fun insertProductDeletionLog(log: DeletedProductLog)

    @Insert
    suspend fun insertInvoiceDeletionLog(log: DeletedInvoiceLog)

    @Insert
    suspend fun insertPurchaseDeletionLog(log: DeletedPurchaseLog)

    @Query("SELECT * FROM deleted_farmer_logs ORDER BY id DESC")
    fun getAllFarmerDeletionLogs(): Flow<List<DeletedFarmerLog>>

    @Query("SELECT * FROM deleted_product_logs ORDER BY id DESC")
    fun getAllProductDeletionLogs(): Flow<List<DeletedProductLog>>

    @Query("SELECT * FROM deleted_invoice_logs ORDER BY id DESC")
    fun getAllInvoiceDeletionLogs(): Flow<List<DeletedInvoiceLog>>

    @Query("SELECT * FROM deleted_purchase_logs ORDER BY id DESC")
    fun getAllPurchaseDeletionLogs(): Flow<List<DeletedPurchaseLog>>
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    fun getSupplierById(id: Long): Flow<Supplier?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("UPDATE suppliers SET outstandingBalance = outstandingBalance + :additionalAmount WHERE id = :supplierId")
    suspend fun updateSupplierBalance(supplierId: Long, additionalAmount: Double)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_invoices ORDER BY createdAt DESC")
    fun getAllPurchaseInvoices(): Flow<List<PurchaseInvoice>>

    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    fun getPurchaseInvoiceById(id: Long): Flow<PurchaseInvoice?>

    @Query("SELECT * FROM purchase_invoices WHERE id = :id")
    suspend fun getPurchaseInvoiceByIdSync(id: Long): PurchaseInvoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseInvoice(invoice: PurchaseInvoice): Long

    @Update
    suspend fun updatePurchaseInvoice(invoice: PurchaseInvoice)

    @Query("SELECT * FROM purchase_items WHERE purchaseInvoiceId = :invoiceId")
    fun getPurchaseItems(invoiceId: Long): Flow<List<PurchaseItem>>

    @Query("SELECT * FROM purchase_items WHERE purchaseInvoiceId = :invoiceId")
    suspend fun getPurchaseItemsSync(invoiceId: Long): List<PurchaseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItem(item: PurchaseItem): Long

    @Query("DELETE FROM purchase_items WHERE purchaseInvoiceId = :invoiceId")
    suspend fun deletePurchaseItemsByInvoiceId(invoiceId: Long)

    @Delete
    suspend fun deletePurchaseInvoice(invoice: PurchaseInvoice)

    @Query("SELECT * FROM purchase_items")
    fun getAllPurchaseItems(): Flow<List<PurchaseItem>>

    @Query("SELECT * FROM purchase_items")
    suspend fun getAllPurchaseItemsSync(): List<PurchaseItem>
}
