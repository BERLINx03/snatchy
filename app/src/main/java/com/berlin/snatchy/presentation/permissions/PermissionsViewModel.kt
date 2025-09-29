package com.berlin.snatchy.presentation.permissions

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class PermissionsViewModel: ViewModel() {
    val showDialogQueue = mutableStateListOf<String>()

    fun dismissDialog() {
        showDialogQueue.removeAt(0)
    }

    fun onPermissionResult(permission: String, isGranted: Boolean) {
        if(!isGranted && !showDialogQueue.contains(permission)) {
            showDialogQueue.add(permission)
        }
    }
}