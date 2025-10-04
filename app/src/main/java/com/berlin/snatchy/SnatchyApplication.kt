package com.berlin.snatchy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berlin.snatchy.domain.model.StorageResponse
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.OnHoldScreen
import com.berlin.snatchy.presentation.ui.StatusList
import com.berlin.snatchy.presentation.ui.openGivenProfile
import com.berlin.snatchy.presentation.ui.util.FilterRow
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
    whatsappVM: WhatsappStatusViewModel,
    onRequestPermission: () -> Unit
) {
    val previewStatus = whatsappVM.previewStatus
    val isRefreshing by whatsappVM.isRefreshing.collectAsState()
    val statusResponse by whatsappVM.statuses.collectAsState()

    val thumbnailCache by whatsappVM.thumbnailCache.collectAsState()

    var selectedFiles by remember { mutableStateOf(setOf<File>()) }
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var showOptionsMenu by remember { mutableStateOf(false) }

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
    LaunchedEffect(allStatuses) {
        whatsappVM.preloadThumbnails(context, allStatuses)
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
                    if (selectedFiles.isNotEmpty()) {
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
                                    Icons.Default.SelectAll
                                },
                                contentDescription = if (selectedFiles.size == filteredStatuses.size) {
                                    "Deselect All"
                                } else {
                                    "Select All"
                                }
                            )
                        }
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Contact on Discord") },
                            onClick = {
                                openGivenProfile(
                                    context = context,
                                    "https://discord.com/users/543104385098579999",
                                    "com.discord"
                                )
                                showOptionsMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.discord_icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Contact on X") },
                            onClick = {
                                openGivenProfile(
                                    context = context,
                                    "https://x.com/BERLINx03",
                                    "com.twitter.android"
                                )
                                showOptionsMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.twitter),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FilterRow(
                allSelected = filterType == FilterType.ALL,
                onAllClick = { filterType = FilterType.ALL },
                imageSelected = filterType == FilterType.IMAGES,
                onImagesClick = { filterType = FilterType.IMAGES },
                isVideoSelected = filterType == FilterType.VIDEOS,
                onVideoSelected = { filterType = FilterType.VIDEOS },
                isOldSort = sortOrder == SortOrder.OLDEST_FIRST,
                onSortSelected = {
                    sortOrder = if (sortOrder == SortOrder.NEWEST_FIRST) {
                        SortOrder.OLDEST_FIRST
                    } else {
                        SortOrder.NEWEST_FIRST
                    }
                },
                allStatuses = allStatuses
            )
            StatusList(
                PaddingValues(0.dp),
                viewModel = whatsappVM,
                isRefreshing = isRefreshing,
                onRefresh = {
                    whatsappVM.onRefresh()
                    selectedFiles = emptySet()
                },
                statuses = filteredStatuses,
                selectedFiles = selectedFiles,
                thumbnailCache = thumbnailCache,
                onRequestPermission = onRequestPermission,
                onSelectedFilesChange = { selectedFiles = it },
                onPreviewStatusChange = { whatsappVM.updatePreviewStatus(it) }
            )
        }
        previewStatus?.let { status ->
            OnHoldScreen(
                status = status,
                onDismiss = { whatsappVM.updatePreviewStatus(null) }
            )
        }
    }
}