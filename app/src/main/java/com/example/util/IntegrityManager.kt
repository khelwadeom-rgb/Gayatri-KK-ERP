package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest

object IntegrityManager {
    // Expected signature hash (Placeholder - in real app, this would be the actual release hash)
    private const val EXPECTED_SIGNATURE_HASH = "PLACEHOLDER_HASH"

    fun verifyIntegrity(context: Context): Boolean {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentHash = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
                
                // For debug/development, we might skip this or use a debug hash
                if (EXPECTED_SIGNATURE_HASH == "PLACEHOLDER_HASH") return true 
                if (currentHash == EXPECTED_SIGNATURE_HASH) return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}
