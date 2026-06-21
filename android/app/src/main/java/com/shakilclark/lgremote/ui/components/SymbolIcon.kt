package com.shakilclark.lgremote.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shakilclark.lgremote.R

/**
 * An icon rendered from the Material Symbols Rounded variable font (design-system §7), using the
 * **FILL** axis for active/selected state ([filled] = 1, otherwise 0). The font is subset to the
 * glyphs in [MaterialSymbols] (~5 KB). Drop-in replacement for `Icon(Icons.Rounded.*)`: pass a
 * codepoint from [MaterialSymbols], a [contentDescription] (null = decorative), and a [size].
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun SymbolIcon(
    symbol: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
) {
    val family = remember(filled) {
        FontFamily(
            Font(
                R.font.material_symbols_rounded,
                variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", if (filled) 1f else 0f)),
            ),
        )
    }
    Text(
        text = symbol,
        modifier = modifier.clearAndSetSemantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        color = tint,
        fontFamily = family,
        fontSize = size.value.sp,
    )
}

/**
 * Material Symbols Rounded codepoints for the glyphs in the subset font (res/font). Built from Int
 * codepoints (not unicode escapes) so the source stays plain ASCII.
 */
object MaterialSymbols {
    private fun cp(code: Int) = String(Character.toChars(code))
    val ArrowBack = cp(0xE5C4)
    val VolumeOff = cp(0xE04F)
    val VolumeUp = cp(0xE050)
    val FastForward = cp(0xE01F)
    val FastRewind = cp(0xE020)
    val Home = cp(0xE9B2)
    val KeyboardArrowDown = cp(0xE313)
    val KeyboardArrowUp = cp(0xE316)
    val Mic = cp(0xE31D)
    val Pause = cp(0xE034)
    val PlayArrow = cp(0xE037)
    val PowerSettingsNew = cp(0xF8C7)
    val Settings = cp(0xE8B8)
    val Speaker = cp(0xE32D)
    val Stop = cp(0xE047)
    val Sync = cp(0xE627)
    val Tv = cp(0xE63B)
    val KeyboardArrowRight = cp(0xE315)
    val Palette = cp(0xE40A)
    val Tune = cp(0xE429)
    val Info = cp(0xE88E)
    val OpenInNew = cp(0xE89E)
    val Description = cp(0xE873)
    val Check = cp(0xE668)
}
