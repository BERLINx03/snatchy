package com.berlin.snatchy.presentation.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

/**
 * @author Abdallah Elsokkary
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusItem(status: File, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CardDefaults.shape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CardDefaults.shape
                    )
            ){
                if (status.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif")) {
                    Image(
                        painter = rememberAsyncImagePainter(model = status),
                        contentDescription = "Status Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .scale(if (isSelected) 1.05f else 1f),
                        contentScale = ContentScale.Crop
                    )
                } else if (status.extension.lowercase() == "mp4") {
                    val videoFrame = remember { getVideoFrame(status) }
                    videoFrame?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Video Frame",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .scale(if (isSelected) 1.05f else 1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

fun getVideoFrame(file: File): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        frame
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}