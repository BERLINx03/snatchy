package com.berlin.snatchy.presentation

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berlin.snatchy.data.WhatsappStatusRepository
import com.berlin.snatchy.domain.model.StorageResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _statuses = MutableStateFlow<StorageResponse>(StorageResponse.Loading)
    val statuses = _statuses.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        if (whatsappRepository.hasRequiredPermissions(context)) {
            fetchWhatsappStatuses()
        } else {
            _statuses.value = StorageResponse.Failure("Permissions not granted")
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
        if (whatsappRepository.hasRequiredPermissions(context)) {
            fetchWhatsappStatuses()
        } else {
            _statuses.value = StorageResponse.Failure("Permissions not granted")
        }
    }

    fun downloadWhatsappStatus(statuses: List<File>, context: Context) {
        viewModelScope.launch {
            try {
                Log.d("WhatsappStatusViewModel", "Starting download for ${statuses.size} files")

                val destinationPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ""
                } else {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        .toString() + "/Snatchy"
                }

                Log.d("WhatsappStatusViewModel", "Using path: $destinationPath")

                whatsappRepository.downloadWhatsappStatus(statuses)
                    .collect { response ->
                        Log.d("WhatsappStatusViewModel", "Download response: $response")
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