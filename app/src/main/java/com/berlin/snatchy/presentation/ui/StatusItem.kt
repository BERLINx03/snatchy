package com.berlin.snatchy.presentation.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.File

/**
 * @author Abdallah Elsokkary
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusItem(
    status: File,
    isSelected: Boolean,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val timeLeft = remember(status.lastModified()) {
        calculateTimeLeft(status.lastModified())
    }
    val isVideo = status.extension.lowercase() == "mp4"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (status.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp")) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(ctx)
                                .data(status)
                                .memoryCacheKey(status.absolutePath)
                                .diskCacheKey(status.absolutePath)
                                .size(400)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Status Image",
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (isVideo) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = timeLeft,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isVideo) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "VIDEO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
private fun calculateTimeLeft(lastModified: Long): String {
    val expiryTime = lastModified + (24 * 60 * 60 * 1000)
    val remainingTime = expiryTime - System.currentTimeMillis()

    return when {
        remainingTime <= 0 -> "Expired"
        remainingTime < 60 * 60 * 1000 -> "${remainingTime / (60 * 1000)}m left"
        else -> "${remainingTime / (60 * 60 * 1000)}h left"
    }
}