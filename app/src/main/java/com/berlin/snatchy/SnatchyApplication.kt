package com.berlin.snatchy

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berlin.snatchy.domain.model.StorageResponse
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.FailedScreen
import com.berlin.snatchy.presentation.ui.StatusItem
import java.io.File

/**
 * @author Abdallah Elsokkary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnatchyApplication(
    isDark: MutableState<Boolean>,
    onButtonClicked: () -> Unit,
    whatsappVM: WhatsappStatusViewModel,
    onRequestPermission: () -> Unit
) {

    val isRefreshing by whatsappVM.isRefreshing.collectAsState()
    val statusResponse by whatsappVM.statuses.collectAsState()

    var selectedFiles by remember { mutableStateOf(setOf<File>()) }

    val context = LocalContext.current

    val allStatuses = if (statusResponse is StorageResponse.Success) {
        (statusResponse as StorageResponse.Success).statusList
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedFiles.isNotEmpty()) {
                            "${selectedFiles.size} selected"
                        } else {
                            "Snatchy"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (allStatuses.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedFiles = if (selectedFiles.size == allStatuses.size) {
                                    emptySet()
                                } else {
                                    allStatuses.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedFiles.size == allStatuses.size) {
                                    Icons.Default.Clear
                                } else {
                                    Icons.Default.CheckCircle
                                },
                                contentDescription = if (selectedFiles.size == allStatuses.size) {
                                    "Deselect All"
                                } else {
                                    "Select All"
                                }
                            )
                        }
                    }

                    IconButton(onClick = onButtonClicked) {
                        Icon(
                            painter = if (isDark.value)
                                painterResource(R.drawable.baseline_light_mode_24)
                            else
                                painterResource(R.drawable.baseline_dark_mode_24),
                            contentDescription = "Switch dark or light mode Button"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        whatsappVM.downloadWhatsappStatus(selectedFiles.toList(), context)
                        selectedFiles = emptySet()
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_download_24),
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        StatusList(
            innerPadding,
            viewModel = whatsappVM,
            isRefreshing = isRefreshing,
            onRefresh = whatsappVM::onRefresh,
            selectedFiles = selectedFiles,
            onRequestPermission = onRequestPermission,
            onSelectedFilesChange = { selectedFiles = it }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusList(
    paddingValues: PaddingValues,
    viewModel: WhatsappStatusViewModel,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedFiles: Set<File>,
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
                    val statuses = (statusResponse as StorageResponse.Success).statusList
                    LazyVerticalGrid(GridCells.Fixed(3)) {
                        items(
                            items = statuses,
                            key = { status ->
                            status.hashCode()
                        }) { file ->
                            StatusItem(
                                status = file,
                                isSelected = selectedFiles.contains(file),
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

//from documentation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCustomIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pullToRefreshIndicator(
            state = state,
            isRefreshing = isRefreshing,
            containerColor = PullToRefreshDefaults.containerColor,
            threshold = PositionalThreshold
        ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isRefreshing,
            animationSpec = tween(durationMillis = 100),
            modifier = Modifier.align(Alignment.Center)
        ) { refreshing ->
            if (refreshing) {
                CircularProgressIndicator(Modifier.size(16.dp))
            } else {
                val distanceFraction = { state.distanceFraction.coerceIn(0f, 1f) }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            val progress = distanceFraction()
                            this.alpha = progress
                            this.scaleX = progress
                            this.scaleY = progress
                        }
                )
            }
        }
    }
}