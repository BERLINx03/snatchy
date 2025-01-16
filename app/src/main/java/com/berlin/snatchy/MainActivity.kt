package com.berlin.snatchy

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

const val permission = "permission"
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val whatsappVM by viewModels<WhatsappStatusViewModel>()

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { entry ->
                when (entry.key) {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES -> {
                        if (entry.value) {
                            Log.i(permission, "Read Permission Granted")
                        } else {
                            handlePermissionDenial(entry.key)
                        }
                    }
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                        if (entry.value) {
                            Log.i(permission, "Write Permission Granted")
                        } else {
                            handlePermissionDenial(entry.key)
                        }
                    }
                }
            }
        }

    private fun handlePermissionDenial(permission: String) {
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
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // For Android 9 (P) and below, request both READ and WRITE
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            // For Android 10+ only READ is needed (Scoped Storage)
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }


    private fun requestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            Log.i(permission, "All permissions already granted")
            return
        }

        val shouldShowRationale = permissionsToRequest.any {
            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
        }

        if (shouldShowRationale) {
            AlertDialog.Builder(this)
                .setTitle("Permissions Needed")
                .setMessage("This app needs access to your storage to show and download WhatsApp statuses.")
                .setPositiveButton("Grant Permissions") { _, _ ->
                    requestMultiplePermissions.launch(permissionsToRequest)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            requestMultiplePermissions.launch(permissionsToRequest)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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