package com.berlin.snatchy.presentation

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berlin.snatchy.data.WhatsappStatusRepository
import com.berlin.snatchy.domain.model.StorageResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * @author Abdallah Elsokkary
 */
@HiltViewModel
class WhatsappStatusViewModel @Inject constructor(
    private val whatsappRepository: WhatsappStatusRepository,
    val app: Application
) : AndroidViewModel(app) {

    private val _statuses = MutableStateFlow<StorageResponse>(StorageResponse.Loading)
    val statuses = _statuses.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val permissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else { arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE) }

    init {
        if (!hasAllPermissions()) {
            _statuses.value =
                StorageResponse.Failure("The application need access to read and write status files.")
        } else {
            fetchWhatsappStatuses()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q &&
            permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                app.applicationContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasAllPermissions(): Boolean {
        return permissions.all { hasPermission(it) }
    }
    fun retryFetchingStatuses() {
        if (hasAllPermissions()) {
            fetchWhatsappStatuses()
        } else {
            _statuses.value =
                StorageResponse.Failure("The application need access to read and write status files.")
        }
    }
    fun fetchWhatsappStatuses() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _statuses.value = StorageResponse.Loading

                whatsappRepository.fetchWhatsappStatuses().collect { response ->
                    _statuses.value = response
                }
            } catch (e: Exception) {
                _statuses.value = StorageResponse.Failure("Error: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onRefresh() {
        fetchWhatsappStatuses()
    }

    fun downloadWhatsappStatus(statuses: List<File>, context: Context) {
        viewModelScope.launch {
            try {
                whatsappRepository.downloadWhatsappStatus(statuses)
                    .collect { response ->
                        when (response) {
                            is StorageResponse.Success -> {
                                Toast.makeText(
                                    context,
                                    response.message ?: "Statuses saved to gallery successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is StorageResponse.Failure -> {
                                Toast.makeText(
                                    context,
                                    "Download failed: ${response.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            is StorageResponse.Loading -> {
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("WhatsappStatusViewModel", "Download error", e)
                Toast.makeText(
                    context,
                    "Download error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}