package com.shakilclark.lgremote.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.shakilclark.lgremote.tv.TvTrustManager

/**
 * A Coil [ImageLoader] that trusts the TV's self-signed cert, so the TV's HTTPS icon URLs
 * (`https://<tv>:3001/resources/...`) load. Remember once per surface and pass to [AppIcon].
 */
@Composable
fun rememberTvImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember { ImageLoader.Builder(context).okHttpClient(TvTrustManager.client()).build() }
}

/**
 * An app/source icon from the TV, with a letter-mark fallback shown while loading or if the icon
 * can't be fetched — so a tile/strip never appears broken.
 */
@Composable
fun AppIcon(
    iconUrl: String?,
    fallbackText: String,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(iconUrl).crossfade(true).build(),
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = modifier,
        loading = { LetterMark(fallbackText) },
        error = { LetterMark(fallbackText) },
    )
}

@Composable
private fun LetterMark(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
