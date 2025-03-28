package com.berlin.snatchy

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
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
import androidx.core.content.ContextCompat
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.theme.SnatchyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val whatsappVM by viewModels<WhatsappStatusViewModel>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("PermissionHandler", "Permission Results: $permissions")

        val allCriticalPermissionsGranted = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                permissions[Manifest.permission.READ_MEDIA_IMAGES] == true &&
                        permissions[Manifest.permission.READ_MEDIA_VIDEO] == true

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                Environment.isExternalStorageManager()

            else ->
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                        permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        }

        when {
            allCriticalPermissionsGranted -> {
                Log.i("PermissionHandler", "All critical permissions granted")
                whatsappVM.fetchWhatsappStatuses()
            }
            else -> {
                handlePermissionDenials(permissions)
            }
        }
    }

    private fun handlePermissionDenials(permissions: Map<String, Boolean>) {
        permissions.forEach { (permission, isGranted) ->
            if (!isGranted) {
                when {
                    permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                        requestAllFilesAccessPermission()
                    }
                    shouldShowRequestPermissionRationale(permission) -> {
                        showPermissionRationaleDialog(permission)
                    }
                    else -> {
                        showPermanentDenialDialog(permission)
                    }
                }
            }
        }
    }

    private fun showPermissionRationaleDialog(permission: String) {
        val rationaleMessage = when (permission) {
            Manifest.permission.READ_MEDIA_IMAGES ->
                "We need image permission to view WhatsApp image statuses."
            Manifest.permission.READ_MEDIA_VIDEO ->
                "We need video permission to view WhatsApp video statuses."
            Manifest.permission.MANAGE_EXTERNAL_STORAGE ->
                "We need storage management to save and access statuses."
            else -> "This permission is required for app functionality."
        }

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(rationaleMessage)
            .setPositiveButton("Grant Permission") { _, _ ->
                requestMissingPermissions()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showPermanentDenialDialog(permission: String) {
        val (title, message) = when (permission) {
            Manifest.permission.READ_MEDIA_IMAGES ->
                "Image Permission Denied" to "Please grant image permission in App Settings to view and save WhatsApp image statuses."

            Manifest.permission.READ_MEDIA_VIDEO ->
                "Video Permission Denied" to "Please grant video permission in App Settings to view and save WhatsApp video statuses."

            Manifest.permission.MANAGE_EXTERNAL_STORAGE ->
                "Storage Permission Denied" to "Please grant full storage access in App Settings to manage WhatsApp statuses."

            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                "Storage Permission Denied" to "Please grant storage permissions in App Settings to access and save files."

            else ->
                "Permission Permanently Denied" to "Please grant required permissions in App Settings to use all app features."
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSystemSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun openAppSystemSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun requestMissingPermissions() {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (!Environment.isExternalStorageManager()) {
                    requestAllFilesAccessPermission()
                }
                arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
            else ->
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            whatsappVM.fetchWhatsappStatuses()
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return (this).let {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // API 34
                    checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                            Environment.isExternalStorageManager()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // API 33
                    checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> { // API 30
                    Environment.isExternalStorageManager()
                }
                else -> {
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    private fun requestAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Primary method: Direct app-specific settings
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                Log.i("PermissionHandler", "Requesting all-files access via app-specific settings")
            } catch (primaryException: ActivityNotFoundException) {
                try {
                    // Fallback 1: Generic all-files access settings
                    val genericIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(genericIntent)
                    Log.w("PermissionHandler", "Fallback to generic all-files access settings")
                } catch (genericException: ActivityNotFoundException) {
                    try {
                        // Fallback 2: Open application details settings
                        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(appDetailsIntent)
                        Log.w("PermissionHandler", "Fallback to app details settings for permission")
                    } catch (settingsException: Exception) {
                        // Last resort: Show a dialog instructing manual permission
                        runOnUiThread {
                            AlertDialog.Builder(this)
                                .setTitle("Permission Required")
                                .setMessage("Please manually grant 'All Files Access' permission in device settings for this app. Navigate to: Settings > Apps > [App Name] > Permissions > Storage")
                                .setPositiveButton("Understood") { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        Log.e("PermissionHandler", "Failed to open any permission settings", settingsException)
                    }
                }
            } catch (e: Exception) {
                Log.e("PermissionHandler", "Unexpected error in permission request", e)
                // Generic error handling
                Toast.makeText(
                    this,
                    "Unable to request storage permission. Please check app settings manually.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.w("PermissionHandler", "All-files access permission not applicable for this Android version")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            when {
                hasAllRequiredPermissions() -> {
                    Log.i("AppInit", "All permissions pre-granted")
                    whatsappVM.fetchWhatsappStatuses()
                }
                else -> {
                    Log.w("AppInit", "Requesting missing permissions")
                    requestMissingPermissions()
                }
            }

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
        } catch (initException: Exception) {
            Log.e("AppInit", "Critical initialization failure", initException)
            Toast.makeText(
                this,
                "App initialization error. Please restart.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}