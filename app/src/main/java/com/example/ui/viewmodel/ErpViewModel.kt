package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.api.GeminiClient
import com.example.data.model.*
import com.example.data.repository.ErpRepository
import com.example.util.SecurityManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*

class ErpViewModel(
    private val repository: ErpRepository,
    private val firestoreRepository: com.example.data.repository.FirestoreRepository,
    private val securityManager: com.example.util.SecurityManager,
    private val networkManager: com.example.util.NetworkManager
) : ViewModel() {

    private val auth by lazy {
        try {
            val instance = FirebaseAuth.getInstance()
            Log.d("ErpViewModel", "FirebaseAuth initialized successfully")
            instance
        } catch (e: Exception) {
            Log.e("ErpViewModel", "FirebaseAuth initialization failed: ${e.message}", e)
            null
        }
    }

    // --- Connection Status ---
    val isNetworkAvailable = networkManager.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val firebaseStatus = MutableStateFlow("Disconnected")
    val connectionError = MutableStateFlow<String?>(null)

    // --- Cloud Sync Mode ---
    val isCloudSyncEnabled = MutableStateFlow(securityManager.isCloudSyncEnabled())

    // --- Security & Application Lock ---
    val isAppLocked = MutableStateFlow(true)
    val loginCooldownSeconds = MutableStateFlow(0)
    val loginErrorMessage = MutableStateFlow("")
    private var lastActivityTime = System.currentTimeMillis()

    init {
        checkLockStatus()
        monitorFirebaseStatus()
    }

    private fun monitorFirebaseStatus() {
        viewModelScope.launch {
            combine(isNetworkAvailable, isCloudSyncEnabled) { connected, sync ->
                connected to sync
            }.collect { (connected, sync) ->
                if (!connected) {
                    firebaseStatus.value = "Network Unavailable"
                    connectionError.value = "No internet connection detected."
                } else if (sync) {
                    checkFirebaseConnection()
                } else {
                    firebaseStatus.value = "Local Mode Active"
                    connectionError.value = null
                }
            }
        }
    }

    private suspend fun checkFirebaseConnection() {
        val authClient = auth
        if (authClient == null) {
            firebaseStatus.value = "Firebase Error"
            connectionError.value = "Firebase Authentication is not available."
            return
        }

        if (authClient.currentUser == null) {
            firebaseStatus.value = "Auth Required"
            connectionError.value = "Please login to cloud to enable synchronization."
            return
        }

        try {
            // Test Firestore Connectivity
            firebaseStatus.value = "Connecting..."
            firestoreRepository.testConnection()
            firebaseStatus.value = "Connected"
            connectionError.value = null
            Log.d("ErpViewModel", "Firebase Cloud Sync fully connected and verified")
        } catch (e: Exception) {
            Log.e("ErpViewModel", "Firestore connection test failed", e)
            firebaseStatus.value = "Firestore Error"
            connectionError.value = when {
                e.message?.contains("permission-denied") == true -> "Permission Denied: Check Firestore Security Rules."
                e.message?.contains("unavailable") == true -> "Firestore service is currently unavailable."
                else -> "Cloud Connection Failed: ${e.localizedMessage}"
            }
        }
    }

    private fun checkLockStatus() {
        viewModelScope.launch {
            while (true) {
                val remaining = securityManager.getRemainingLockTime()
                loginCooldownSeconds.value = (remaining / 1000).toInt()
                if (remaining <= 0 && loginErrorMessage.value == "Too many failed attempts. Try again later.") {
                    loginErrorMessage.value = ""
                }
                delay(1000)
            }
        }
    }

    fun unlockApp(enteredPin: String) {
        if (loginCooldownSeconds.value > 0) return

        if (securityManager.validatePin(enteredPin)) {
            isAppLocked.value = false
            loginErrorMessage.value = ""
            updateActivity()
        } else {
            val remaining = securityManager.getRemainingLockTime()
            if (remaining > 0) {
                loginErrorMessage.value = "Too many failed attempts. Try again later."
            } else {
                loginErrorMessage.value = "Incorrect PIN."
            }
        }
    }

    fun resetPinViaMaster(enteredMasterPin: String) {
        if (securityManager.validatePin(enteredMasterPin)) {
             isAppLocked.value = false
             loginErrorMessage.value = ""
             updateActivity()
        }
    }

    fun resetPinViaOwnerName(name: String) {
        if (securityManager.validateOwnerName(name)) {
            isAppLocked.value = false
            loginErrorMessage.value = ""
            updateActivity()
        } else {
            loginErrorMessage.value = "Incorrect Owner Name."
        }
    }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun checkIdleLock() {
        val idleTime = System.currentTimeMillis() - lastActivityTime
        if (idleTime > 5 * 60 * 1000) { // 5 minutes
            lockApp()
        }
    }

    fun lockApp() {
        isAppLocked.value = true
    }

    private fun logAction(action: String, details: String = "") {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val now = Calendar.getInstance(tz).time
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tz }.format(now)

            val auditLog = AuditLog(
                user = currentUserRole.value,
                action = action,
                date = dateStr,
                time = timeStr,
                device = android.os.Build.MODEL,
                details = details
            )
            repository.insertAuditLog(auditLog)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.insertAuditLog(auditLog)
            }
        }
    }

    fun syncDataToCloud() {
        viewModelScope.launch {
            // Migration logic: Push all local data to cloud
            farmers.value.forEach { firestoreRepository.insertFarmer(it) }
            products.value.forEach { firestoreRepository.insertProduct(it) }
            invoices.value.forEach { firestoreRepository.insertInvoice(it) }
            suppliers.value.forEach { firestoreRepository.insertSupplier(it) }
            purchaseInvoices.value.forEach { firestoreRepository.insertPurchase(it) }
            logAction("Manual Data Migration", "Pushed all local records to cloud")
        }
    }

    fun updateAppPin(current: String, new: String): Boolean {
        if (securityManager.validatePin(current)) {
            val success = securityManager.setPin(new)
            if (success) {
                logAction("Security Settings Changed", "App PIN updated")
            }
            return success
        }
        return false
    }

    // --- Authentication & Session User Role ---
    val currentUserRole = MutableStateFlow("Super Admin") // Super Admin, Shop Staff, Accountant
    val isAuthenticated = MutableStateFlow(true)

    // --- Localization ---
    val selectedLanguage = MutableStateFlow("English") // English, Hindi, Marathi

    // --- Shop Profile & Developer Panel ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val shopProfile: StateFlow<ShopProfile?> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.getShopProfile() else repository.shopProfile
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isDeveloperAuthenticated = MutableStateFlow(false)
    val isDeveloperModeUnlocked = MutableStateFlow(securityManager.isDeveloperModeEnabled())
    val developerSessionTimeoutMinutes = 15

    fun unlockDeveloperMode() {
        securityManager.setDeveloperModeEnabled(true)
        isDeveloperModeUnlocked.value = true
    }

    fun loginAsDeveloper(pass: String): Boolean {
        if (securityManager.validateDeveloperPassword(pass)) {
            isDeveloperAuthenticated.value = true
            logAction("Developer Login", "Direct Access")
            return true
        }
        return false
    }
    
    // Biometrics
    val isBiometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled())
    
    fun toggleBiometric(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        isBiometricEnabled.value = enabled
    }

    fun updateShopProfile(profile: ShopProfile) {
        viewModelScope.launch {
            repository.updateShopProfile(profile)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.updateShopProfile(profile)
            }
        }
    }

    fun saveImageToInternalStorage(context: android.content.Context, uri: android.net.Uri, fileName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = java.io.File(context.filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ErpViewModel", "Error saving image", e)
            null
        }
    }

    // --- UI Database Streams ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val farmers: StateFlow<List<Farmer>> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.allFarmers else repository.allFarmers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.allProducts else repository.allProducts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val invoices: StateFlow<List<Invoice>> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.allInvoices else repository.allInvoices
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val suppliers: StateFlow<List<Supplier>> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.allSuppliers else repository.allSuppliers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val purchaseInvoices: StateFlow<List<PurchaseInvoice>> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.allPurchases else repository.allPurchaseInvoices
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchaseItems: StateFlow<List<PurchaseItem>> = repository.getAllPurchaseItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val creditRecords: StateFlow<List<CreditRecord>> = isCloudSyncEnabled.flatMapLatest { enabled ->
        if (enabled) firestoreRepository.allCreditRecords else repository.allCreditRecords
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoiceItems: StateFlow<List<InvoiceItem>> = repository.allInvoiceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    fun restoreFromBackup(backup: com.example.util.BackupManager.FullBackup) {
        viewModelScope.launch {
            repository.restoreFullBackup(backup)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLocalData()
        }
    }
        
    fun toggleCloudSync(enabled: Boolean) {
        securityManager.setCloudSyncEnabled(enabled)
        isCloudSyncEnabled.value = enabled
        logAction("Cloud Sync ${if (enabled) "Enabled" else "Disabled"}")
    }

    fun loginToCloud(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val authClient = auth
            if (authClient == null) {
                val err = "Firebase Auth component initialization failed."
                onError(err)
                Log.e("ErpViewModel", err)
                return@launch
            }

            if (!networkManager.isConnected.first()) {
                onError("No internet connection.")
                return@launch
            }

            try {
                Log.d("ErpViewModel", "Attempting cloud login for: $email")
                authClient.signInWithEmailAndPassword(email, pass).await()
                Log.d("ErpViewModel", "Cloud login successful for: ${authClient.currentUser?.uid}")
                toggleCloudSync(true)
                checkFirebaseConnection()
                onSuccess()
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "User account does not exist."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid password."
                    is com.google.firebase.auth.FirebaseAuthException -> "Authentication provider error."
                    else -> e.localizedMessage ?: "Cloud Login Failed"
                }
                Log.e("ErpViewModel", "Cloud Login Error: $errorMessage", e)
                onError(errorMessage)
            }
        }
    }

    fun logoutFromCloud() {
        auth?.signOut()
        toggleCloudSync(false)
        Log.d("ErpViewModel", "Signed out from Firebase Cloud")
    }

    // --- Deletion Logs ---
    val farmerDeletionLogs: StateFlow<List<DeletedFarmerLog>> = repository.farmerDeletionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val productDeletionLogs: StateFlow<List<DeletedProductLog>> = repository.productDeletionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteFarmerWithReason(farmer: Farmer, reason: String) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val now = Calendar.getInstance(tz).time
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tz }.format(now)

            repository.logFarmerDeletion(DeletedFarmerLog(
                farmerName = farmer.name,
                deletedBy = currentUserRole.value,
                date = dateStr,
                time = timeStr,
                reason = reason
            ))
            logAction("Farmer Deleted", "Farmer: ${farmer.name}, Reason: $reason")
            repository.deleteFarmer(farmer)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.deleteFarmer(farmer)
            }
        }
    }

    fun deleteProductWithReason(product: Product, reason: String) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val now = Calendar.getInstance(tz).time
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tz }.format(now)

            repository.logProductDeletion(DeletedProductLog(
                productName = product.name,
                batchNumber = product.batchNumber,
                reason = reason,
                date = dateStr,
                time = timeStr
            ))
            logAction("Product Deleted", "Product: ${product.name}, Reason: $reason")
            repository.deleteProduct(product)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.deleteProduct(product)
            }
        }
    }

    fun deleteInvoiceWithReason(invoice: Invoice, reason: String) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val now = Calendar.getInstance(tz).time
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tz }.format(now)

            repository.logInvoiceDeletion(DeletedInvoiceLog(
                invoiceId = invoice.id,
                farmerName = invoice.farmerName,
                totalAmount = invoice.totalAmount,
                deletedBy = currentUserRole.value,
                date = dateStr,
                time = timeStr,
                reason = reason
            ))
            logAction("Bill Deleted", "Invoice ID: ${invoice.id}, Reason: $reason")
            repository.deleteInvoice(invoice)
            // If we add deleteInvoice to FirestoreRepository, we should call it here too
        }
    }

    // --- POS Basket State ---
    val selectedFarmerForBilling = MutableStateFlow<Farmer?>(null)
    private val _basket = MutableStateFlow<Map<Product, Double>>(emptyMap())
    val basket: StateFlow<Map<Product, Double>> = _basket.asStateFlow()

    val posDiscount = MutableStateFlow(0.0)
    val posPaymentMode = MutableStateFlow("Cash") // Cash, UPI, Card, Credit

    // Computed POS aggregates
    val basketSubtotal = basket.map { items ->
        items.entries.sumOf { it.key.sellingPrice * it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val basketGstAmount = basket.map { items ->
        items.entries.sumOf {
            val gstPercent = it.key.gstPercent.coerceAtLeast(0.0)
            val netPrice = it.key.sellingPrice / (1.0 + (gstPercent / 100.0))
            val gstPart = it.key.sellingPrice - netPrice
            gstPart * it.value
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val basketTotalAmount = combine(basketSubtotal, posDiscount) { sub, disc ->
        (sub - disc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Gemini Recommender State ---
    val isRecommendationLoading = MutableStateFlow(false)
    val responseRecommendation = MutableStateFlow("")

    // --- Navigation Helper ---
    val currentRoute = MutableStateFlow("dashboard") // dashboard, billing, farmers_stock, purchase_suppliers, udhari, settings

    // --- POS Billing Basket Operations ---
    fun setFarmerForBilling(farmer: Farmer?) {
        selectedFarmerForBilling.value = farmer
        // If farmer has credit outstanding, auto suggest Credit mode as option
        if (farmer != null && farmer.outstandingCredit > 0) {
            posPaymentMode.value = "Credit"
        }
    }

    fun addProductToBasket(product: Product) {
        val current = _basket.value.toMutableMap()
        val qty = current[product] ?: 0.0
        if (product.stock > qty) {
            current[product] = qty + 1.0
            _basket.value = current
        }
    }

    fun subtractProductFromBasket(product: Product) {
        val current = _basket.value.toMutableMap()
        val qty = current[product] ?: return
        if (qty <= 1.0) {
            current.remove(product)
        } else {
            current[product] = qty - 1.0
        }
        _basket.value = current
    }

    fun setProductBasketQuantity(product: Product, quantity: Double) {
        val current =_basket.value.toMutableMap()
        if (quantity <= 0.0) {
            current.remove(product)
        } else {
            val finalQty = quantity.coerceAtMost(product.stock)
            current[product] = finalQty
        }
        _basket.value = current
    }

    fun clearPOSBillingBasket() {
        _basket.value = emptyMap()
        posDiscount.value = 0.0
        selectedFarmerForBilling.value = null
        posPaymentMode.value = "Cash"
    }

    /**
     * Executes the checkout of active basket.
     */
    fun checkoutBasket(paidAmount: Double, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            Log.d("ErpViewModel", "checkoutBasket started with paidAmount: $paidAmount")
            val activeItems = _basket.value
            if (activeItems.isEmpty()) {
                Log.e("ErpViewModel", "checkoutBasket: Basket is empty")
                return@launch
            }

            val farmer = selectedFarmerForBilling.value
            val grossAmount = basketTotalAmount.value
            
            // Precise GST Calculations
            var totalTaxableAmount = 0.0
            var totalGst = 0.0
            
            activeItems.forEach { (prod, qty) ->
                val itemTotal = prod.sellingPrice * qty
                val gstPercent = prod.gstPercent.coerceAtLeast(0.0)
                val netPrice = prod.sellingPrice / (1.0 + (gstPercent / 100.0))
                val gstPart = prod.sellingPrice - netPrice
                totalTaxableAmount += netPrice * qty
                totalGst += gstPart * qty
            }

            val finalPaid = if (posPaymentMode.value == "Credit") 0.0 else paidAmount

            val tz = TimeZone.getTimeZone("Asia/Kolkata")
            val now = Calendar.getInstance(tz).time
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = tz }.format(now)

            val remainingBalance = (farmer?.outstandingCredit ?: 0.0) + grossAmount - finalPaid
            val paymentStatus = when {
                remainingBalance <= 0.1 && posPaymentMode.value != "Credit" -> "PAID"
                finalPaid > 0.0 -> "PARTIALLY PAID"
                else -> "UNPAID"
            }

            val invoice = Invoice(
                farmerId = farmer?.id ?: 0L,
                farmerName = farmer?.name ?: "Walk-in Farmer",
                farmerMobile = farmer?.mobile ?: "",
                farmerVillage = farmer?.village ?: "",
                createdAt = System.currentTimeMillis(),
                createdDate = dateStr,
                createdTime = timeStr,
                totalAmount = grossAmount,
                discount = posDiscount.value,
                taxableAmount = totalTaxableAmount,
                cgst = totalGst / 2.0,
                sgst = totalGst / 2.0,
                gstAmount = totalGst,
                paidAmount = finalPaid,
                paymentMode = posPaymentMode.value,
                previousBalance = farmer?.outstandingCredit ?: 0.0,
                remainingBalance = remainingBalance,
                paymentStatus = paymentStatus,
                billingStaff = currentUserRole.value
            )

            Log.d("ErpViewModel", "checkoutBasket: Creating invoice for ${invoice.farmerName}")

            val invoiceItems = activeItems.map { (prod, qty) ->
                InvoiceItem(
                    invoiceId = 0L, // will be written by repo Transaction
                    productId = prod.id,
                    productName = prod.name,
                    brandName = prod.brandName,
                    category = prod.category,
                    quantity = qty,
                    unit = prod.unit,
                    weightValue = prod.weightValue,
                    weightUnit = prod.weightUnit,
                    batchNumber = prod.batchNumber,
                    mfgDate = prod.manufacturingDate,
                    expiryDate = prod.expiryDate,
                    sellingPrice = prod.sellingPrice,
                    gstPercent = prod.gstPercent,
                    total = prod.sellingPrice * qty
                )
            }

            try {
                val savedInvoiceId = repository.createInvoiceTransaction(invoice, invoiceItems)
                Log.d("ErpViewModel", "checkoutBasket: Transaction successful, ID: $savedInvoiceId")
                logAction("Bill Generated", "Invoice ID: $savedInvoiceId, Farmer: ${invoice.farmerName}")
                
                if (isCloudSyncEnabled.value) {
                    firestoreRepository.insertInvoice(invoice.copy(id = savedInvoiceId))
                    // Also sync the farmer's updated credit if it changed
                    selectedFarmerForBilling.value?.let { 
                        val updatedFarmer = it.copy(outstandingCredit = invoice.remainingBalance)
                        firestoreRepository.insertFarmer(updatedFarmer)
                    }
                }

                clearPOSBillingBasket()
                onComplete(savedInvoiceId)
            } catch (e: Exception) {
                Log.e("ErpViewModel", "checkoutBasket: Transaction failed", e)
            }
        }
    }

    // --- DB Mutations ---
    fun addFarmer(name: String, mobile: String, village: String, aadhaar: String, landArea: Double, crop: String, soil: String, irrigation: String, notes: String) {
        viewModelScope.launch {
            val farmer = Farmer(
                name = name, mobile = mobile, village = village, aadhaar = aadhaar,
                landArea = landArea, cropType = crop, soilType = soil, irrigationType = irrigation, notes = notes
            )
            repository.insertFarmer(farmer)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.insertFarmer(farmer)
            }
        }
    }

    fun updateFarmer(farmer: Farmer) {
        viewModelScope.launch {
            repository.updateFarmer(farmer)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.insertFarmer(farmer)
            }
        }
    }

    fun deleteFarmer(farmer: Farmer) {
        viewModelScope.launch {
            repository.deleteFarmer(farmer)
        }
    }

    fun payFarmerCredit(farmerId: Long, farmerName: String, amount: Double, paymentMode: String, notes: String) {
        viewModelScope.launch {
            repository.processCreditPayment(farmerId, farmerName, amount, paymentMode, notes)
        }
    }

    fun addProduct(
        name: String,
        brandName: String,
        category: String,
        stock: Double,
        unit: String,
        purchase: Double,
        selling: Double,
        gst: Double,
        batch: String,
        mfgDate: Long,
        expiryDate: Long,
        weightValue: Double,
        weightUnit: String,
        barcode: String
    ) {
        viewModelScope.launch {
            val product = Product(
                name = name, brandName = brandName, category = category, stock = stock, unit = unit,
                purchasePrice = purchase, sellingPrice = selling, gstPercent = gst,
                batchNumber = batch, manufacturingDate = mfgDate, expiryDate = expiryDate,
                weightValue = weightValue, weightUnit = weightUnit, barcode = barcode
            )
            repository.insertProduct(product)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.insertProduct(product)
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.insertProduct(product)
            }
        }
    }

    fun adjustProductStock(productId: Long, quantity: Double, increase: Boolean) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, quantity, increase)
            // For stock adjustment, we might need to get the full product to sync to cloud
            // Simplified: if cloud sync is on, we'll need a better way or just re-sync the product
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            if (isCloudSyncEnabled.value) {
                firestoreRepository.deleteProduct(product)
            }
        }
    }

    // --- Suppliers ---
    fun addSupplier(name: String, mobile: String, address: String, gst: String, contact: String, email: String) {
        viewModelScope.launch {
            val supplier = Supplier(name = name, mobile = mobile, address = address, gstNumber = gst, contactPerson = contact, email = email)
            repository.insertSupplier(supplier)
            if (isCloudSyncEnabled.value) firestoreRepository.insertSupplier(supplier)
            logAction("Supplier Added", "Supplier: $name")
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.updateSupplier(supplier)
            if (isCloudSyncEnabled.value) firestoreRepository.insertSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            if (isCloudSyncEnabled.value) firestoreRepository.deleteSupplier(supplier)
            logAction("Supplier Deleted", "Supplier: ${supplier.name}")
        }
    }

    // --- Purchases ---
    fun getPurchaseItems(purchaseId: Long): Flow<List<PurchaseItem>> = repository.getPurchaseItems(purchaseId)

    fun addPurchaseInvoice(invoice: PurchaseInvoice, items: List<PurchaseItem>) {
        viewModelScope.launch {
            try {
                val savedId = repository.createPurchaseTransaction(invoice, items)
                if (isCloudSyncEnabled.value) {
                    firestoreRepository.insertPurchase(invoice.copy(id = savedId))
                }
                logAction("Purchase Invoice Added", "Invoice: ${invoice.invoiceNumber}, Supplier: ${invoice.supplierName}")
            } catch (e: Exception) {
                Log.e("ErpViewModel", "Failed to add purchase invoice", e)
            }
        }
    }

    fun updatePurchaseInvoice(invoice: PurchaseInvoice, items: List<PurchaseItem>) {
        viewModelScope.launch {
            try {
                repository.updatePurchaseTransaction(invoice, items)
                if (isCloudSyncEnabled.value) {
                    firestoreRepository.insertPurchase(invoice)
                }
                logAction("Purchase Invoice Edited", "Invoice: ${invoice.invoiceNumber}, Supplier: ${invoice.supplierName}")
            } catch (e: Exception) {
                Log.e("ErpViewModel", "Failed to edit purchase invoice", e)
            }
        }
    }

    fun deletePurchaseInvoiceWithReason(invoiceId: Long, reason: String) {
        viewModelScope.launch {
            try {
                val user = "Super Admin"
                repository.deletePurchaseTransaction(invoiceId, user, reason)
                // Firestore delete logic could be added here if needed
                logAction("Purchase Invoice Deleted", "Invoice ID: $invoiceId, Reason: $reason")
            } catch (e: Exception) {
                Log.e("ErpViewModel", "Failed to delete purchase invoice", e)
            }
        }
    }

    // --- AI Smart Recommendations ---
    fun getCropConsultation(farmer: Farmer, queryText: String) {
        viewModelScope.launch {
            isRecommendationLoading.value = true
            responseRecommendation.value = "Consulting Gayatri Krushi Kendra Smart AI Advisor for cotton, soybean and local crop diagnostics... Please wait."
            val response = GeminiClient.getAgricultureAdvice(
                cropType = farmer.cropType,
                soilType = farmer.soilType,
                irrigationType = farmer.irrigationType,
                queryPrompt = queryText
            )
            responseRecommendation.value = response
            isRecommendationLoading.value = false
        }
    }

    // --- Simple Language Dictionary mapping for instantaneous multilingual translation ---
    fun t(key: String): String {
        val lang = selectedLanguage.value
        val labelMap = translations[key] ?: return key
        return labelMap[lang] ?: labelMap["English"] ?: key
    }

    companion object {
        private val translations = mapOf(
            "dashboard" to mapOf("English" to "Dashboard", "Hindi" to "डैशबोर्ड", "Marathi" to "डॅशबोर्ड"),
            "billing" to mapOf("English" to "Billing", "Hindi" to "बिलिंग", "Marathi" to "बिलिंग"),
            "farmers" to mapOf("English" to "Farmers", "Hindi" to "किसान", "Marathi" to "शेतकरी"),
            "stock" to mapOf("English" to "Stock", "Hindi" to "स्टॉक माल", "Marathi" to "स्टॉक माल"),
            "history" to mapOf("English" to "History", "Hindi" to "इतिहास", "Marathi" to "इतिहास"),
            "udhari" to mapOf("English" to "Udhari", "Hindi" to "उधारी", "Marathi" to "उधारी"),
            "settings" to mapOf("English" to "Settings", "Hindi" to "सेटिंग्स", "Marathi" to "सेटिंग्ज"),
            "farmers_stock" to mapOf("English" to "Farmers & Stock", "Hindi" to "किसान और स्टॉक", "Marathi" to "शेतकरी आणि स्टॉक"),
            "purchase_suppliers" to mapOf("English" to "Purchase & Suppliers", "Hindi" to "खरीद और विक्रेता", "Marathi" to "खरेदी आणि पुरवठादार"),
            
            // Stats
            "today_sales" to mapOf("English" to "Today sales", "Hindi" to "आज की बिक्री", "Marathi" to "आजची विक्री"),
            "monthly_rev" to mapOf("English" to "Monthly Revenue", "Hindi" to "मासिक राजस्व", "Marathi" to "मासिक महसूल"),
            "pending_credits" to mapOf("English" to "Pending Udhari", "Hindi" to "कुल उधार राशि", "Marathi" to "एकूण उधारी"),
            "low_stock" to mapOf("English" to "Stock Alerts", "Hindi" to "कम स्टॉक आइटम", "Marathi" to "कमी शिल्लक माल"),
            "farmers_count" to mapOf("English" to "Registered Farmers", "Hindi" to "पंजीकृत किसान", "Marathi" to "नोंदणीकृत शेतकरी"),
            
            // Buttons
            "add_farmer" to mapOf("English" to "Add Farmer", "Hindi" to "नया किसान जोड़ें", "Marathi" to "नवीन शेतकरी जोडा"),
            "add_product" to mapOf("English" to "New Product", "Hindi" to "नया उत्पाद जोड़ें", "Marathi" to "नवीन उत्पादन जोडा"),
            "checkout" to mapOf("English" to "Checkout Bill", "Hindi" to "बिल का भुगतान करें", "Marathi" to "बिल पूर्ण करा"),
            "print" to mapOf("English" to "Print Receipt", "Hindi" to "रसीद प्रिंट करें", "Marathi" to "पावती प्रिंट करा"),
            "search" to mapOf("English" to "Search", "Hindi" to "खोजें", "Marathi" to "शोधा")
        )
    }
}

class ErpViewModelFactory(
    private val repository: ErpRepository,
    private val firestoreRepository: com.example.data.repository.FirestoreRepository,
    private val securityManager: SecurityManager,
    private val networkManager: com.example.util.NetworkManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ErpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ErpViewModel(repository, firestoreRepository, securityManager, networkManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
