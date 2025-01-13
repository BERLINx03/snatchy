package com.berlin.snatchy.domain.model

import java.io.File

/**
 * @author Abdallah Elsokkary
 */
sealed class StorageResponse {
    data class Success(val statusList: List<File>,val message: String? = null) : StorageResponse()
    data class Failure(val message: String) : StorageResponse()
    data object Loading : StorageResponse()
}