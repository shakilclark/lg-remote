package com.shakilclark.lgremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.shakilclark.lgremote.tv.TvApp
import com.shakilclark.lgremote.ui.theme.Space

/**
 * Dynamic app grid (US6 dynamic loader). Shows the TV's installed apps (in the TV's order) as icon
 * tiles; tap launches. Icons are fetched from the TV's own HTTPS resource server
 * (`https://<tv>:3001/resources/...`) via Coil over the [TvTrustManager] client (the TV's
 * self-signed cert), falling back to a letter tile while loading or if the icon can't be loaded.
 */
@Composable
fun AppGrid(
    apps: List<TvApp>,
    onLaunch: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    if (apps.isEmpty()) {
        // Distinguish "still fetching" from "the TV genuinely reported no apps" — never flash a false
        // empty during the preload (the list is fetched on connect).
        Box(modifier.fillMaxWidth().padding(Space.xl), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator()
            } else {
                Text("No apps found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    val imageLoader = rememberTvImageLoader()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 76.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Space.l),
        contentPadding = PaddingValues(bottom = Space.xxl),
    ) {
        items(apps, key = { it.id }) { app -> AppTile(app, imageLoader) { onLaunch(app) } }
    }
}

@Composable
private fun AppTile(app: TvApp, imageLoader: ImageLoader, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        Modifier
            .width(76.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { contentDescription = app.title }
            .padding(vertical = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                iconUrl = app.iconUrl,
                fallbackText = app.title,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize().padding(Space.s),
            )
        }
        Text(
            app.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
