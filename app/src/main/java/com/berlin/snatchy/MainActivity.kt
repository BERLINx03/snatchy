package com.berlin.snatchy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.permissions.ManageStoragePermission
import com.berlin.snatchy.presentation.permissions.PermissionDialog
import com.berlin.snatchy.presentation.permissions.PermissionsViewModel
import com.berlin.snatchy.presentation.permissions.ReadPermission
import com.berlin.snatchy.presentation.permissions.WritePermission
import com.berlin.snatchy.presentation.ui.theme.SnatchyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val whatsappVM by viewModels<WhatsappStatusViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[PermissionsViewModel::class.java]
        val dialogQueue = viewModel.showDialogQueue
        setContent {
            val requestMultiplePermissions = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                whatsappVM.permissions.forEach { per ->
                    viewModel.onPermissionResult(
                        permission = per,
                        isGranted = permissions[per] == true
                    )
                }
                whatsappVM.retryFetchingStatuses()
            }
            val isDark = rememberSaveable { mutableStateOf(false) }
            dialogQueue
                .reversed()
                .forEach { permission ->
                    PermissionDialog(
                        permission = when(permission){
                            Manifest.permission.READ_EXTERNAL_STORAGE -> ReadPermission()
                            Manifest.permission.WRITE_EXTERNAL_STORAGE -> WritePermission()
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> ManageStoragePermission()
                            else -> return@forEach
                        },
                        isPermanentlyDeclined = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q &&
                                permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
                                false
                            } else {
                                !shouldShowRequestPermissionRationale(permission)
                            },
                        onDismiss = viewModel::dismissDialog,
                        onOkClick = {
                            viewModel.dismissDialog()
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q &&
                                permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
                                goToManageStorageSettings()
                            } else {
                                requestMultiplePermissions.launch(arrayOf(permission))
                            }
                        },
                        onGoToAppSettingsClick = ::goToAppSettings,
                    )
                }
            SnatchyTheme(darkTheme = isDark.value) {
                SnatchyApplication(
                    isDark = isDark,
                    onButtonClicked = { isDark.value = !isDark.value },
                    whatsappVM = whatsappVM,
                    onRequestPermission = {requestMultiplePermissions.launch(whatsappVM.permissions)}
                )
            }
        }
    }
    private fun goToManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
}

fun Context.goToAppSettings(){
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package",packageName, null)
    ).also { startActivity(it) }
}