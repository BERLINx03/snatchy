package com.berlin.snatchy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.theme.SnatchyTheme
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat

const val permission = "permission"
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val whatsappVM by viewModels<WhatsappStatusViewModel>()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(permission, "Granted")
            } else {
                Log.i(permission, "Denied")
                if (shouldShowRequestPermissionRationale(getRequiredPermission())) {
                    Toast.makeText(
                        this,
                        "Permission denied. Cannot show WhatsApp statuses.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // "Don't ask again" case
                    Toast.makeText(
                        this,
                        "Permission permanently denied. Enable it in app settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }


    private fun requestPermission() {
        val requiredPermission = getRequiredPermission()
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                //already granted
                Log.i(permission, "Already granted")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                //UI for showing why the permission is needed
                AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("This app needs access to your storage to show and download WhatsApp statuses.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }

            else -> {
                requestPermissionLauncher.launch(
                    requiredPermission
                )
                // Additional check for "Don't ask again" cases
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        requiredPermission
                    )
                ) {
                    Toast.makeText(
                        this,
                        "Permission permanently denied. Enable it in app settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission()
        enableEdgeToEdge()
        setContent {
            SnatchyTheme {
                SnatchyApplication(whatsappVM)
            }
        }
    }
}