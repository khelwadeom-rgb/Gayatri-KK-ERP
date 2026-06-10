package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.io.Serializable

@Entity(tableName = "farmers")
data class Farmer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val village: String,
    val aadhaar: String = "",
    val landArea: Double = 0.0, // in acres
    val cropType: String = "Cotton", // Cotton, Soybean, Wheat, Rice, Sugarcane, Chickpeas
    val soilType: String = "Black", // Black, Red, Sandy, Clay, Loam
    val irrigationType: String = "Drip", // Drip, Sprinkler, Flood, Rain-fed
    val notes: String = "",
    val outstandingCredit: Double = 0.0,
    val dateCreated: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brandName: String = "",
    val category: String, // Seeds, Fertilizers, Pesticides, Fungicides, Tools, Irrigation
    val stock: Double,
    val unit: String, // kg, Bags, Liters, Packets, Pieces
    val purchasePrice: Double,
    val sellingPrice: Double,
    val gstPercent: Double = 18.0, // 5%, 12%, 18%, 28%
    val batchNumber: String = "B-001",
    val manufacturingDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000), // Default 1 year from now
    val weightValue: Double = 0.0,
    val weightUnit: String = "kg", // gm, kg, ml, ltr
    val barcode: String = ""
) : Serializable {
    val marginPercent: Double
        get() = if (purchasePrice > 0) ((sellingPrice - purchasePrice) / purchasePrice) * 100.0 else 0.0
    
    val profitPerUnit: Double
        get() = sellingPrice - purchasePrice
}

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerId: Long, // 0 if walk-in customer
    val farmerName: String,
    val farmerMobile: String = "",
    val farmerVillage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdDate: String = "", // Format: dd/MM/yyyy
    val createdTime: String = "", // Format: hh:mm a
    val totalAmount: Double,
    val discount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val gstAmount: Double,
    val paidAmount: Double,
    val paymentMode: String, // Cash, UPI, Credit
    val previousBalance: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val paymentStatus: String = "UNPAID", // PAID, UNPAID
    val billingStaff: String = "Super Admin", // Super Admin, Shop Staff, Accountant
    val signature: String = "" // Hash signature for tampering detection
) : Serializable

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val brandName: String = "",
    val category: String,
    val quantity: Double,
    val unit: String,
    val weightValue: Double = 0.0,
    val weightUnit: String = "",
    val batchNumber: String = "",
    val mfgDate: Long = 0,
    val expiryDate: Long = 0,
    val sellingPrice: Double,
    val gstPercent: Double,
    val total: Double = 0.0
) : Serializable

@Entity(tableName = "credit_records")
data class CreditRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerId: Long,
    val farmerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val amount: Double, // positive means udhari added, negative means cash payment received
    val paymentMode: String = "Cash", // Cash, UPI, Card
    val description: String = ""
) : Serializable

@Entity(tableName = "shop_profile")
data class ShopProfile(
    @PrimaryKey val id: Int = 1, // Single source of truth
    val shopName: String = "Gayatri Krushi Kendra",
    val shopAddress: String = "At Post Manur (bk), Tal Bodwad, Dist Jalgaon",
    val ownerName: String = "Eknath Khelwade",
    val mobileNumber: String = "9049451733",
    val alternateNumber: String = "",
    val gstNumber: String = "27AWGPK6243H1ZF",
    val pesticideLicense: String = "LAID 1120220584 JLG",
    val seedLicense: String = "LISD 720222290 JLG",
    val cottonLicense: String = "LCSD 0720220350 JLS",
    val fertilizerLicense: String = "",
    val upiId: String = "9049451733-6@ybl",
    val qrImagePath: String? = null,
    val logoPath: String? = null,
    val invoiceLogoPath: String? = null
) : Serializable

@Entity(tableName = "deleted_farmer_logs")
data class DeletedFarmerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerName: String,
    val deletedBy: String,
    val date: String,
    val time: String,
    val reason: String
) : Serializable

@Entity(tableName = "deleted_product_logs")
data class DeletedProductLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val batchNumber: String,
    val reason: String,
    val date: String,
    val time: String
) : Serializable

@Entity(tableName = "deleted_invoice_logs")
data class DeletedInvoiceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val farmerName: String,
    val totalAmount: Double,
    val deletedBy: String,
    val date: String,
    val time: String,
    val reason: String
) : Serializable

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val user: String,
    val action: String,
    val date: String,
    val time: String,
    val device: String,
    val details: String = ""
) : Serializable

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String = "",
    val gstNumber: String = "",
    val contactPerson: String = "",
    val email: String = "",
    val outstandingBalance: Double = 0.0,
    val dateCreated: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "purchase_invoices")
data class PurchaseInvoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val invoiceNumber: String,
    val invoiceDate: String, // dd/MM/yyyy
    val gstNumber: String = "",
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "Unpaid", // Paid, Partially Paid, Unpaid
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Super Admin",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Super Admin",
    val lastModified: Long = System.currentTimeMillis(),
    val invoicePdfPath: String? = null
) : Serializable

@Entity(tableName = "purchase_items")
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseInvoiceId: Long,
    val productId: Long, // Link to unique product entry (Name+Brand+Batch)
    val productName: String,
    val brandName: String = "",
    val category: String = "",
    val batchNumber: String,
    val manufacturingDate: Long = 0,
    val expiryDate: Long = 0,
    val weightValue: Double = 0.0,
    val weightUnit: String = "",
    val quantity: Double,
    val unit: String = "", // Packets, Bags, etc.
    val purchaseRate: Double,
    val gstPercent: Double = 0.0,
    val total: Double = 0.0
) : Serializable

@Entity(tableName = "deleted_purchase_logs")
data class DeletedPurchaseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val supplierName: String,
    val totalAmount: Double,
    val deletedBy: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val date: String,
    val time: String,
    val reason: String
) : Serializable
