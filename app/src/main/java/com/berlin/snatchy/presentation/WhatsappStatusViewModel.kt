package com.berlin.snatchy.presentation

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
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


    init {
        fetchWhatsappStatuses()
        Log.d("WhatsappStatusViewModel", "files got fetched")
    }

    fun fetchWhatsappStatuses() {
        viewModelScope.launch {
            whatsappRepository.fetchWhatsappStatuses().collect {
                _statuses.value = it
            }
        }
    }

    fun downloadWhatsappStatus(statuses: List<File>, context: Context) {
        viewModelScope.launch {
            val destinationPath = File(
                Environment.getExternalStorageDirectory(),
                "snatchy"
            )
            if (!destinationPath.exists()) {
                destinationPath.mkdirs()
            }

            whatsappRepository.downloadWhatsappStatus(statuses, destinationPath.absolutePath)
                .collect {
                    when (it) {
                        is StorageResponse.Success -> {
                            // Show a success toast
                            Toast.makeText(
                                context,
                                it.message ?: "Statuses downloaded successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is StorageResponse.Failure -> {
                            // Show a failure toast
                            Toast.makeText(
                                context,
                                it.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is StorageResponse.Loading -> {
                            // Optional: Show a loading state (e.g., progress indicator)
                        }

                    }
                }
        }
    }
}