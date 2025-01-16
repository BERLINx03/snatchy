package com.berlin.snatchy.presentation

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
    private val whatsappRepository: WhatsappStatusRepository
) : ViewModel() {

    private val _statuses = MutableStateFlow<StorageResponse>(StorageResponse.Loading)
    val statuses = _statuses.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        fetchWhatsappStatuses()
        Log.d("WhatsappStatusViewModel", "files got fetched")
    }

    private fun fetchWhatsappStatuses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _statuses.value = StorageResponse.Loading
            whatsappRepository.fetchWhatsappStatuses().collect {
                _statuses.value = it
            }
            _isRefreshing.value = false
        }
    }

    fun onRefresh() {
        fetchWhatsappStatuses()
    }

    fun downloadWhatsappStatus(statuses: List<File>, context: Context) {
        viewModelScope.launch {
            val destinationPath = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Snatchy"
            )
            if (!destinationPath.exists()) {
                destinationPath.mkdirs()
            }

            whatsappRepository.downloadWhatsappStatus(statuses, destinationPath.absolutePath)
                .collect {
                    when (it) {
                        is StorageResponse.Success -> {
                            Toast.makeText(
                                context,
                                it.message ?: "Statuses downloaded successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is StorageResponse.Failure -> {
                            Toast.makeText(
                                context,
                                it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is StorageResponse.Loading -> {
                            // loading state progress indicator
                        }

                    }
                }
        }
    }
}