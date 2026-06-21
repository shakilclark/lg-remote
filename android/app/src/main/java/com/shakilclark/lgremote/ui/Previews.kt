package com.shakilclark.lgremote.ui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview for the design-iteration inner loop (audit P2): render a surface in **light + dark** on
 * the Ultraviolet fallback at the real device width (411dp), so theme/colour regressions show up in
 * Android Studio at a glance without a hand-written preview per mode. Apply `@ThemePreviews` to a
 * surface's preview composable instead of a single `@Preview`.
 */
@Preview(name = "Light", showBackground = true, widthDp = 411)
@Preview(name = "Dark", showBackground = true, widthDp = 411, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews
