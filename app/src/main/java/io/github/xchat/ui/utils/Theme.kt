package io.github.xchat.ui.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import io.github.xchat.ui.utils.theme.SeedResolver
import io.github.xchat.ui.utils.theme.ThemeSettings
import io.github.xchat.ui.utils.theme.darkScheme
import io.github.xchat.ui.utils.theme.lightScheme
import io.github.xchat.utils.HostInfo
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * Theme for Xchat UI injected INTO WeChat. Wraps content in BOTH a Material 3
 * [MaterialExpressiveTheme] and a miuix [MiuixTheme] so components from either design system share
 * one accent while living inside the WeChat process.
 *
 * The seed is [SeedResolver.injectedSeed]: WeChat green by default, or the user's custom color when
 * they opted it into WeChat ([ThemeSettings.applyToWechat]). This is read once when the composition
 * enters — it does NOT re-theme live (the user must restart WeChat for a change to apply).
 */
@Composable
fun InjectedUiTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalConfiguration provides HostInfo.application.resources.configuration
    ) {
        val dark = darkTheme ?: isSystemInDarkTheme()
        val applyCustom = ThemeSettings.applyToWechat && ThemeSettings.customColor
        val seed = SeedResolver.injectedSeed(HostInfo.application, dark)

        // ---- Material 3 ----
        val materialScheme = if (!applyCustom) {
            if (dark) darkScheme else lightScheme
        } else {
            SeedResolver.materialScheme(seed, dark)
        }

        // ---- miuix ----
        val controller = if (!applyCustom) {
            ThemeController(
                colorSchemeMode = if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light,
                isDark = dark,
            )
        } else {
            ThemeController(
                colorSchemeMode = if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight,
                keyColor = Color(seed),
                colorSpec = ThemeSettings.effectiveColorSpec.miuix,
                paletteStyle = ThemeSettings.paletteStyle.miuix,
                isDark = dark,
            )
        }

        MaterialExpressiveTheme(
            colorScheme = materialScheme,
            motionScheme = MotionScheme.expressive(),
        ) {
            MiuixTheme(controller = controller) {
                CompositionLocalProvider(
                    LocalContentColor provides MiuixTheme.colorScheme.onBackground,
                ) {
                    content()
                }
            }
        }
    }
}
