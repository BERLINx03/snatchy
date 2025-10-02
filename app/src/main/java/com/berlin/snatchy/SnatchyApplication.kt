package com.berlin.snatchy

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
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
enum class FilterType {
    ALL, IMAGES, VIDEOS
}

enum class SortOrder {
    NEWEST_FIRST, OLDEST_FIRST
}
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
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var showFilterMenu by remember { mutableStateOf(false) }

        val context = LocalContext.current

    val allStatuses = if (statusResponse is StorageResponse.Success) {
        (statusResponse as StorageResponse.Success).statusList
    } else {
        emptyList()
    }
    val filteredStatuses = remember(allStatuses, filterType, sortOrder) {
        val filtered = when (filterType) {
            FilterType.ALL -> allStatuses
            FilterType.IMAGES -> allStatuses.filter {
                it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp")
            }
            FilterType.VIDEOS -> allStatuses.filter {
                it.extension.lowercase() == "mp4"
            }
        }

        when (sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.lastModified() }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.lastModified() }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedFiles.isNotEmpty()) {
                                "${selectedFiles.size} selected"
                            } else {
                                "Snatchy"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedFiles.isEmpty() && filteredStatuses.isNotEmpty()) {
                            Text(
                                text = "${filteredStatuses.size} status${if (filteredStatuses.size != 1) "es" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (filteredStatuses.isNotEmpty()) {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Badge(
                                containerColor = if (filterType != FilterType.ALL)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    Color.Transparent
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            Text(
                                text = "Filter by type",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("All (${allStatuses.size})")
                                    }
                                },
                                onClick = {
                                    filterType = FilterType.ALL
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = filterType == FilterType.ALL,
                                        onClick = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("Images (${allStatuses.count {
                                            it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp")
                                        }})")
                                    }
                                },
                                onClick = {
                                    filterType = FilterType.IMAGES
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = filterType == FilterType.IMAGES,
                                        onClick = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VideoLibrary,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("Videos (${allStatuses.count {
                                            it.extension.lowercase() == "mp4"
                                        }})")
                                    }
                                },
                                onClick = {
                                    filterType = FilterType.VIDEOS
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = filterType == FilterType.VIDEOS,
                                        onClick = null
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "Sort by",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = {
                                    sortOrder = SortOrder.NEWEST_FIRST
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = sortOrder == SortOrder.NEWEST_FIRST,
                                        onClick = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = {
                                    sortOrder = SortOrder.OLDEST_FIRST
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    RadioButton(
                                        selected = sortOrder == SortOrder.OLDEST_FIRST,
                                        onClick = null
                                    )
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                selectedFiles = if (selectedFiles.size == filteredStatuses.size) {
                                    emptySet()
                                } else {
                                    filteredStatuses.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedFiles.size == filteredStatuses.size) {
                                    Icons.Default.Clear
                                } else {
                                    Icons.Default.CheckCircle
                                },
                                contentDescription = if (selectedFiles.size == filteredStatuses.size) {
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
                ExtendedFloatingActionButton(
                    onClick = {
                        whatsappVM.downloadWhatsappStatus(selectedFiles.toList(), context)
                        selectedFiles = emptySet()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_download_24),
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "Download ${selectedFiles.size}",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        StatusList(
            innerPadding,
            viewModel = whatsappVM,
            isRefreshing = isRefreshing,
            onRefresh = {
                whatsappVM.onRefresh()
                selectedFiles = emptySet()
            },
            statuses = filteredStatuses,
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
    statuses: List<File>,
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
                    LazyVerticalGrid(GridCells.Fixed(3),contentPadding = PaddingValues(4.dp)) {
                        items(
                            items = statuses,
                            key = { status -> status.hashCode() }) { file ->
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