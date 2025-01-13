package com.berlin.snatchy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.berlin.snatchy.domain.model.StorageResponse
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.StatusItem
import com.berlin.snatchy.presentation.ui.theme.SnatchyTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val whatsappVM by viewModels<WhatsappStatusViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnatchyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Snatchy",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Black,
                                titleContentColor = White
                            )
                        )
                    },
                ) { innerPadding ->
                    StatusList(
                        innerPadding,
                        viewModel = whatsappVM
                    )
                }
            }
        }
    }
}


@Composable
fun StatusList(
    paddingValues: PaddingValues,
    viewModel: WhatsappStatusViewModel
) {

    val statusResponse by viewModel.statuses.collectAsState()
    when (statusResponse) {
        is StorageResponse.Loading -> {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        }

        is StorageResponse.Success -> {
            val statuses = (statusResponse as StorageResponse.Success).statusList
            LazyColumn {
                items(statuses) { file ->
                    StatusItem(status = file)
                }
            }
        }

        is StorageResponse.Failure -> {
            val errorMessage = (statusResponse as StorageResponse.Failure).message
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center
            )
        }
    }
}
