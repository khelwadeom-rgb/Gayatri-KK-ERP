package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import com.example.data.dao.AuditLogDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Database(
    entities = [
        Farmer::class,
        Product::class,
        Invoice::class,
        InvoiceItem::class,
        CreditRecord::class,
        ShopProfile::class,
        DeletedFarmerLog::class,
        DeletedProductLog::class,
        DeletedInvoiceLog::class,
        AuditLog::class,
        Supplier::class,
        PurchaseInvoice::class,
        PurchaseItem::class,
        DeletedPurchaseLog::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun farmerDao(): FarmerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun creditDao(): CreditDao
    abstract fun shopDao(): ShopDao
    abstract fun logDao(): LogDao
    abstract fun auditDao(): AuditLogDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "krushi_kendra_erp_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseSeedingCallback(scope))
                .build()
                .also { INSTANCE = it }
            }
        }
    }

    private class DatabaseSeedingCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedInitialData(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val shopDao = database.shopDao()
                    if (shopDao.getShopProfileSync() == null) {
                        shopDao.updateShopProfile(ShopProfile())
                    }
                }
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            Log.d("AppDatabase", "seedInitialData started")
            
            // 1. Seed Shop Profile
            val shopDao = db.shopDao()
            shopDao.updateShopProfile(ShopProfile())
            Log.d("AppDatabase", "Shop Profile seeded")

            // 2. Seed New Products (Old demo products removed via version bump & fallbackToDestructiveMigration)
            val productDao = db.productDao()
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            
            val products = listOf(
                Product(
                    name = "RCH-659", brandName = "Rashi", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "587397", weightValue = 475.0, weightUnit = "gm"
                ),
                Product(
                    name = "Kabaddi", brandName = "Tulsi", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "108494", weightValue = 476.0, weightUnit = "gm"
                ),
                Product(
                    name = "Moksha", brandName = "Aditya", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "LNE171911N", 
                    manufacturingDate = sdf.parse("05-05-2026")?.time ?: System.currentTimeMillis(), 
                    expiryDate = sdf.parse("02-02-2027")?.time ?: (System.currentTimeMillis() + 31536000000L),
                    weightValue = 477.0, weightUnit = "gm"
                ),
                Product(
                    name = "Asha", brandName = "Nuziveedu", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "639374", 
                    manufacturingDate = sdf.parse("16-04-2026")?.time ?: System.currentTimeMillis(), 
                    expiryDate = sdf.parse("08-01-2027")?.time ?: (System.currentTimeMillis() + 31536000000L),
                    weightValue = 478.0, weightUnit = "gm"
                ),
                Product(
                    name = "Jai Ho", brandName = "Srikar", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "S0227S", 
                    manufacturingDate = sdf.parse("08-04-2026")?.time ?: System.currentTimeMillis(), 
                    expiryDate = sdf.parse("31-12-2026")?.time ?: (System.currentTimeMillis() + 31536000000L),
                    weightValue = 479.0, weightUnit = "gm"
                ),
                Product(
                    name = "Supercot", brandName = "Prabhat", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "532360", 
                    manufacturingDate = sdf.parse("16-05-2026")?.time ?: System.currentTimeMillis(), 
                    expiryDate = sdf.parse("10-11-2026")?.time ?: (System.currentTimeMillis() + 31536000000L),
                    weightValue = 480.0, weightUnit = "gm"
                ),
                Product(
                    name = "Super Target", brandName = "Sai Bhavya", category = "Seeds", 
                    stock = 30.0, unit = "Packets", purchasePrice = 800.0, sellingPrice = 850.0, 
                    gstPercent = 0.0, batchNumber = "4251193", 
                    manufacturingDate = sdf.parse("15-05-2026")?.time ?: System.currentTimeMillis(), 
                    expiryDate = sdf.parse("25-01-2027")?.time ?: (System.currentTimeMillis() + 31536000000L),
                    weightValue = 481.0, weightUnit = "gm"
                )
            )

            products.forEach { productDao.insertProduct(it) }
            Log.d("AppDatabase", "Seeding complete. Products added: ${products.size}")
            
            // Note: Old farmers and invoices are not seeded here, effectively removing them on new install/migration.
        }
    }
}
