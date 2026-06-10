package com.example.gayatrikrushikendra

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.repository.ErpRepository
import com.example.data.repository.FirestoreRepository
import com.example.ui.components.AppLogo
import com.example.ui.screens.MainCabinet
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ErpViewModel
import com.example.ui.viewmodel.ErpViewModelFactory
import com.example.util.SecurityManager
import com.example.util.RootDetector
import com.example.util.IntegrityManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions granted/denied if necessary
    }

    private var backgroundTimestamp: Long = 0
    private lateinit var viewModel: ErpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed: ${e.message}", e)
        }
        
        // Screenshot Protection (PRD Requirement 11)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        checkAndRequestPermissions()
        
        // Initialize Core Room Database & Repository
        val scope = CoroutineScope(Dispatchers.IO)
        val database = try {
            AppDatabase.getDatabase(applicationContext, scope)
        } catch (e: Exception) {
            Log.e("MainActivity", "Database initialization failed: ${e.message}")
            null
        }
        
        if (database == null) {
            enableEdgeToEdge()
            setContent {
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Database Error: Unable to initialize storage. Please restart the app or clear cache.")
                    }
                }
            }
            return
        }

        val repository = ErpRepository(database)
        val firestoreRepository = try {
            val firestore = FirebaseFirestore.getInstance()
            Log.d("MainActivity", "Firestore instance retrieved successfully")
            FirestoreRepository(firestore)
        } catch (e: Exception) {
            Log.e("MainActivity", "Firestore initialization failed: ${e.message}", e)
            FirestoreRepository(null) 
        }
        val securityManager = try {
            SecurityManager(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "SecurityManager initialization failed: ${e.message}")
            // This is critical. If SecurityManager fails, we might need a fallback or show error.
            null
        }

        if (securityManager == null) {
            enableEdgeToEdge()
            setContent {
                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Security Error: Unable to initialize secure storage. This can happen if the device Keystore is corrupted. Please clear app data or restart device.")
                    }
                }
            }
            return
        }

        val networkManager = com.example.util.NetworkManager(applicationContext)
        val factory = ErpViewModelFactory(repository, firestoreRepository, securityManager, networkManager)
        
        viewModel = ViewModelProvider(this, factory)[ErpViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            var showSplash by remember { mutableStateOf(true) }
            val alpha = remember { Animatable(0f) }
            val scale = remember { Animatable(0.8f) }

            LaunchedEffect(Unit) {
                // Fade in and scale up
                launch {
                    alpha.animateTo(1f, animationSpec = tween(1000))
                }
                launch {
                    scale.animateTo(1.05f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
                }
                delay(2000) // Hold for a bit
                showSplash = false
            }

            MyApplicationTheme {
                val isRooted = RootDetector.isRooted()
                var showRootWarning by remember { mutableStateOf(isRooted) }
                val isIntegrityOk = IntegrityManager.verifyIntegrity(applicationContext)
                
                if (!isIntegrityOk) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Application integrity check failed. This app may have been modified.")
                    }
                    return@MyApplicationTheme
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen(alpha.value, scale.value, viewModel)
                    } else {
                        if (isAppLocked) {
                            LoginScreen(viewModel = viewModel)
                        } else {
                            MainCabinet(viewModel = viewModel)
                        }
                    }
                    
                    if (showRootWarning) {
                        AlertDialog(
                            onDismissRequest = { showRootWarning = false },
                            title = { Text("Security Risk Detected") },
                            text = { Text("Your device appears to be rooted. This may compromise the security of your ERP data. Proceed with caution.") },
                            confirmButton = {
                                Button(onClick = { showRootWarning = false }) {
                                    Text("I Understand")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            if (backgroundTimestamp != 0L) {
                val elapsed = System.currentTimeMillis() - backgroundTimestamp
                if (elapsed > 15 * 60 * 1000) { // 15 minutes background lock
                    viewModel.lockApp()
                }
            }
            viewModel.updateActivity() // Reset idle timer when coming back
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @Composable
    fun SplashScreen(alpha: Float, scale: Float, viewModel: ErpViewModel) {
        val shopProfile by viewModel.shopProfile.collectAsState()
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppLogo(
                    size = 180,
                    logoPath = shopProfile?.logoPath,
                    modifier = Modifier
                        .alpha(alpha)
                        .scale(scale)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = shopProfile?.shopName ?: "Gayatri Krushi Kendra",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(alpha)
                )
                Text(
                    text = if (shopProfile?.shopAddress != null) shopProfile!!.shopAddress else "मनुर बु, बोदवड, जळगाव",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(alpha),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
