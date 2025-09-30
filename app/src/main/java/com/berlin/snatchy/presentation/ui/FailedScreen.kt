package com.berlin.snatchy.presentation.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

@Composable
fun FailedScreen(
    errorMessage: String,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNoStatusesError = errorMessage == "No supported statuses found"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isNoStatusesError) {
                Icons.Default.Search
            } else {
                Icons.Default.Warning
            },
            contentDescription = if (isNoStatusesError) "No statuses" else "Error",
            tint = if (isNoStatusesError) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isNoStatusesError) {
                "No Statuses Found"
            } else {
                "Permission Required"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )


        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pull down to refresh",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isNoStatusesError) {
                "We couldn't find any WhatsApp statuses. This might be due to:\n\n• WhatsApp not installed\n• No recent statuses available\n• App compatibility issue\n\nNeed help? Join our Discord!"
            } else {
                errorMessage
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = if (isNoStatusesError) {
                {
                    openGivenProfile(
                        context = context,
                        "https://discord.gg/XZD9xjmbCU",
                        "com.discord"
                    )
                }
            } else {
                onRequestPermissions
            },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (errorMessage == "No supported statuses found") {
                    Color(0xFF5865F2)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            if (errorMessage == "No supported statuses found") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Get Help on Discord",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Grant Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun openGivenProfile(context: Context, profileUrl: String, packageName: String) {
    val pm = context.packageManager
    val isInstalled = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    if (isInstalled) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = profileUrl.toUri()
            setPackage(packageName)
        }
        context.startActivity(intent)
    } else {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = profileUrl.toUri()
        }
        context.startActivity(intent)
    }
}