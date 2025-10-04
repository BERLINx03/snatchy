package com.berlin.snatchy.presentation.ui.util

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun FilterRow(
    modifier: Modifier = Modifier,
    allSelected: Boolean,
    onAllClick: () -> Unit,
    imageSelected: Boolean,
    onImagesClick: () -> Unit,
    isVideoSelected: Boolean,
    onVideoSelected: () -> Unit,
    isOldSort: Boolean,
    onSortSelected: () -> Unit,
    allStatuses: List<File>,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = allSelected,
            onClick = onAllClick,
            label = { Text("All (${allStatuses.size})") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = imageSelected,
            onClick = onImagesClick,
            label = {
                Text(
                    "Images (${
                        allStatuses.count {
                            it.extension.lowercase() in listOf(
                                "jpg",
                                "jpeg",
                                "png",
                                "gif",
                                "webp"
                            )
                        }
                    })"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = isVideoSelected,
            onClick = onVideoSelected,
            label = {
                Text(
                    "Videos (${
                        allStatuses.count {
                            it.extension.lowercase() == "mp4"
                        }
                    })"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        FilterChip(
            selected = isOldSort,
            onClick = onSortSelected,
            label = {
                Text(if (!isOldSort) "Newest" else "Oldest")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}