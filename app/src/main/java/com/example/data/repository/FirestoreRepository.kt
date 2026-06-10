package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val firestore: FirebaseFirestore?) {

    private val shopsCollection = firestore?.collection("shops")
    private val farmersCollection = firestore?.collection("farmers")
    private val productsCollection = firestore?.collection("products")
    private val invoicesCollection = firestore?.collection("invoices")
    private val creditRecordsCollection = firestore?.collection("credit_records")
    private val auditLogsCollection = firestore?.collection("audit_logs")
    private val deletionLogsCollection = firestore?.collection("deletion_logs")
    private val suppliersCollection = firestore?.collection("suppliers")
    private val purchasesCollection = firestore?.collection("purchases")

    // --- Connection Test ---
    suspend fun testConnection() {
        val firestoreInstance = firestore ?: throw Exception("Firestore not initialized")
        // Try a simple read to verify connectivity and permissions
        try {
            shopsCollection?.document("connectivity_test")?.get()?.await()
            Log.d("FirestoreRepository", "Firestore connection test successful")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Firestore connection test failed: ${e.message}", e)
            throw e
        }
    }

    // --- Shop Profile ---
    fun getShopProfile(shopId: String = "default"): Flow<ShopProfile?> {
        val collection = shopsCollection ?: return kotlinx.coroutines.flow.flowOf(null)
        return collection.document(shopId).snapshots().map { it.toObject<ShopProfile>() }
    }

    suspend fun updateShopProfile(profile: ShopProfile, shopId: String = "default") {
        shopsCollection?.document(shopId)?.set(profile)?.await()
    }

    // --- Farmers ---
    val allFarmers: Flow<List<Farmer>> = if (farmersCollection != null) {
        farmersCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Farmer::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertFarmer(farmer: Farmer) {
        val collection = farmersCollection ?: return
        val docRef = if (farmer.id == 0L) collection.document() else collection.document(farmer.id.toString())
        val finalFarmer = if (farmer.id == 0L) {
            // Firestore doesn't use Long IDs the same way, but for compatibility we might need them.
            // Better to use document IDs as strings.
            farmer.copy(id = System.currentTimeMillis()) 
        } else farmer
        docRef.set(finalFarmer).await()
    }

    suspend fun deleteFarmer(farmer: Farmer) {
        farmersCollection?.document(farmer.id.toString())?.delete()?.await()
    }

    // --- Products ---
    val allProducts: Flow<List<Product>> = if (productsCollection != null) {
        productsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Product::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertProduct(product: Product) {
        val collection = productsCollection ?: return
        val id = if (product.id == 0L) System.currentTimeMillis() else product.id
        collection.document(id.toString()).set(product.copy(id = id)).await()
    }

    suspend fun deleteProduct(product: Product) {
        productsCollection?.document(product.id.toString())?.delete()?.await()
    }

    // --- Invoices ---
    val allInvoices: Flow<List<Invoice>> = if (invoicesCollection != null) {
        invoicesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Invoice::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertInvoice(invoice: Invoice): String {
        val collection = invoicesCollection ?: return ""
        val docRef = collection.document()
        docRef.set(invoice).await()
        return docRef.id
    }

    // --- Credit Records ---
    val allCreditRecords: Flow<List<CreditRecord>> = if (creditRecordsCollection != null) {
        creditRecordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(CreditRecord::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertCreditRecord(record: CreditRecord) {
        creditRecordsCollection?.document()?.set(record)?.await()
    }

    // --- Audit Logs ---
    val auditLogs: Flow<List<AuditLog>> = if (auditLogsCollection != null) {
        auditLogsCollection
            .orderBy("id", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(AuditLog::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertAuditLog(log: AuditLog) {
        auditLogsCollection?.document()?.set(log)?.await()
    }

    // --- Suppliers ---
    val allSuppliers: Flow<List<Supplier>> = if (suppliersCollection != null) {
        suppliersCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Supplier::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertSupplier(supplier: Supplier) {
        val collection = suppliersCollection ?: return
        val id = if (supplier.id == 0L) System.currentTimeMillis() else supplier.id
        collection.document(id.toString()).set(supplier.copy(id = id)).await()
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        suppliersCollection?.document(supplier.id.toString())?.delete()?.await()
    }

    // --- Purchases ---
    val allPurchases: Flow<List<PurchaseInvoice>> = if (purchasesCollection != null) {
        purchasesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(PurchaseInvoice::class.java) }
    } else kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertPurchase(invoice: PurchaseInvoice) {
        val collection = purchasesCollection ?: return
        val id = if (invoice.id == 0L) System.currentTimeMillis() else invoice.id
        collection.document(id.toString()).set(invoice.copy(id = id)).await()
    }
}
