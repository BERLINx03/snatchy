package com.berlin.snatchy.presentation.ui


import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berlin.snatchy.domain.model.StorageResponse
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.util.MyCustomIndicator
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusList(
    paddingValues: PaddingValues,
    viewModel: WhatsappStatusViewModel,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    statuses: List<File>,
    selectedFiles: Set<File>,
    thumbnailCache: Map<String, Bitmap?>,
    onRequestPermission: () -> Unit,
    onSelectedFilesChange: (Set<File>) -> Unit
) {
    val statusResponse by viewModel.statuses.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            MyCustomIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullToRefreshState
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (statusResponse) {
                is StorageResponse.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(80.dp))
                    }
                }

                is StorageResponse.Success -> {
                    LazyVerticalGrid(GridCells.Fixed(3),contentPadding = PaddingValues(4.dp)) {
                        items(
                            items = statuses,
                            key = { status -> status.absolutePath },
                            contentType = { it.extension.lowercase() }) { file ->
                            StatusItem(
                                status = file,
                                isSelected = selectedFiles.contains(file),
                                thumbnail = thumbnailCache[file.absolutePath],
                                onClick = {
                                    onSelectedFilesChange(
                                        if (file in selectedFiles) {
                                            selectedFiles - file
                                        } else {
                                            selectedFiles + file
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                is StorageResponse.Failure -> {
                    val errorMessage = (statusResponse as StorageResponse.Failure).message
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            FailedScreen(
                                errorMessage = errorMessage,
                                onRequestPermissions = onRequestPermission,
                            )
                        }
                    }
                }
            }
        }
    }
}