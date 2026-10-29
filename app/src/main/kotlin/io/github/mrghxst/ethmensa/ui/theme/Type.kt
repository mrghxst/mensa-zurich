// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.mrghxst.ethmensa.R

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
    Font(R.font.geist_extrabold, FontWeight.ExtraBold),
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun geist(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0, style: FontStyle = FontStyle.Normal) =
    TextStyle(
        fontFamily = Geist,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        fontStyle = style,
        letterSpacing = tracking.em,
        lineHeightStyle = Trim,
    )

/** Type roles from docs/port-spec.md §9.2. Colors are applied at the call site. */
@Immutable
data class EthTypography(
    val display: TextStyle = geist(44, 48, FontWeight.ExtraBold, -0.03),
    val largeTitle: TextStyle = geist(32, 38, FontWeight.SemiBold, -0.02),
    val screenTitle: TextStyle = geist(18, 24, FontWeight.SemiBold, -0.01),
    val sectionLabel: TextStyle = geist(12, 16, FontWeight.Medium, 0.06),
    val rowTitle: TextStyle = geist(17, 22, FontWeight.Medium, -0.01),
    val mealTitle: TextStyle = geist(16, 22, FontWeight.SemiBold),
    val mealName: TextStyle = geist(15, 20, FontWeight.SemiBold),
    val body: TextStyle = geist(15, 21, FontWeight.Normal),
    val caption: TextStyle = geist(13, 18, FontWeight.Normal, style = FontStyle.Italic),
    val label: TextStyle = geist(14, 20, FontWeight.Medium),
    val mono: TextStyle = TextStyle(
        fontFamily = GeistMono,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    ),
)

val LocalEthTypography = staticCompositionLocalOf { EthTypography() }

/** Material typography mapped onto Geist so stock components match. */
fun materialTypography(): Typography {
    val base = Typography()
    fun TextStyle.geist() = copy(fontFamily = Geist)
    return Typography(
        displayLarge = base.displayLarge.geist(),
        displayMedium = base.displayMedium.geist(),
        displaySmall = base.displaySmall.geist(),
        headlineLarge = base.headlineLarge.geist(),
        headlineMedium = base.headlineMedium.geist(),
        headlineSmall = base.headlineSmall.geist(),
        titleLarge = base.titleLarge.geist(),
        titleMedium = base.titleMedium.geist(),
        titleSmall = base.titleSmall.geist(),
        bodyLarge = base.bodyLarge.geist(),
        bodyMedium = base.bodyMedium.geist(),
        bodySmall = base.bodySmall.geist(),
        labelLarge = base.labelLarge.geist(),
        labelMedium = base.labelMedium.geist(),
        labelSmall = base.labelSmall.geist(),
    )
}
