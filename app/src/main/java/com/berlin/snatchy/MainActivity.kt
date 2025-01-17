package com.berlin.snatchy

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.theme.SnatchyTheme
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val whatsappVM by viewModels<WhatsappStatusViewModel>()
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d("MainActivity", "Permission results: $permissions")
            permissions.entries.forEach { entry ->
                when (entry.key) {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES -> {
                        if (entry.value) {
                            Log.i("permission", "Read Permission Granted")
                        } else {
                            handlePermissionDenial(entry.key)
                        }
                    }
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                        if (entry.value) {
                            Log.i("permission", "Write Permission Granted")
                        } else {
                            handlePermissionDenial(entry.key)
                        }
                    }
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                        if (entry.value) {
                            Log.i("permission", "Manage External Storage Permission Granted")
                        } else {
                            handlePermissionDenial(entry.key)
                        }
                    }
                }
            }

            // Check permissions after they are granted and trigger fetch statuses if all are granted
            if (hasRequiredPermissions(this)) {
                Log.d("MainActivity", "All required permissions are granted.")
                whatsappVM.fetchWhatsappStatuses()
            } else {
                Log.e("MainActivity", "Not all required permissions are granted.")
            }
        }

    private fun handlePermissionDenial(permission: String) {
        Log.w("MainActivity", "Permission denied: $permission")
        if (shouldShowRequestPermissionRationale(permission)) {
            Toast.makeText(
                this,
                "Permission denied. Some features may not work properly.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Permission permanently denied. Enable it in app settings.",
                Toast.LENGTH_LONG
            ).show()

            // Call requestAllFilesAccessPermission if MANAGE_EXTERNAL_STORAGE is denied
            if (permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Log.d("MainActivity", "Launching intent to manage all files access permission.")
                requestAllFilesAccessPermission()
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            else -> {
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun requestPermissions() {
        Log.d("MainActivity", "Requesting permissions...")
        val requiredPermissions = getRequiredPermissions()
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            Log.i("MainActivity", "All permissions already granted")
            if (hasRequiredPermissions(this)) {
                whatsappVM.fetchWhatsappStatuses()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    Log.d("MainActivity", "MANAGE_EXTERNAL_STORAGE not granted. Redirecting to settings.")
                    requestAllFilesAccessPermission()
                } else {
                    whatsappVM.fetchWhatsappStatuses()
                }
            }
            return
        }

        Log.d("MainActivity", "Permissions to request: ${permissionsToRequest.joinToString(", ")}")

        val shouldShowRationale = permissionsToRequest.any {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }

        if (shouldShowRationale) {
            Log.d("MainActivity", "Showing rationale dialog for permissions.")
            AlertDialog.Builder(this)
                .setTitle("Permissions Needed")
                .setMessage("This app needs access to your storage to show and download WhatsApp statuses.")
                .setPositiveButton("Grant Permissions") { _, _ ->
                    Log.d("MainActivity", "User clicked 'Grant Permissions'. Launching permission request.")
                    requestMultiplePermissions.launch(permissionsToRequest)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    Log.d("MainActivity", "User clicked 'Cancel'. Dismissing dialog.")
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            Log.d("MainActivity", "Launching permission request without rationale.")
            requestMultiplePermissions.launch(permissionsToRequest)
        }
    }

    private fun requestAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${applicationContext.packageName}")
                    }
                    Log.d("MainActivity", "Launching intent to manage all files access permission.")
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error requesting permissions: ${e.message}")
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called.")
        requestPermissions()
        setContent {
            val isDark = remember { mutableStateOf(false) }
            SnatchyTheme(darkTheme = isDark.value) {
                SnatchyApplication(
                    isDark = isDark,
                    onButtonClicked = { isDark.value = !isDark.value },
                    whatsappVM
                )
            }
        }
    }
}