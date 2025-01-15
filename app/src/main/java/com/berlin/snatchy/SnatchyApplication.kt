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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.berlin.snatchy.domain.model.StorageResponse
import com.berlin.snatchy.presentation.WhatsappStatusViewModel
import com.berlin.snatchy.presentation.ui.StatusItem

/**
 * @author Abdallah Elsokkary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnatchyApplication(whatsappVM: WhatsappStatusViewModel) {

    val isRefreshing by whatsappVM.isRefreshing.collectAsState()

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
            viewModel = whatsappVM,
            isRefreshing = isRefreshing,
            onRefresh = whatsappVM::onRefresh
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