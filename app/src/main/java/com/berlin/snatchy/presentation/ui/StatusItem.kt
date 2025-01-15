package com.berlin.snatchy.presentation.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import java.io.File

/**
 * @author Abdallah Elsokkary
 */

@Composable
fun StatusItem(status: File, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CardDefaults.shape)
            ){
                if (status.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif")) {
                    Image(
                        painter = rememberImagePainter(data = status),
                        contentDescription = "Status Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
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
                                .height(120.dp),
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
@Preview
@Composable
private fun PreviewStatusItem() {
}