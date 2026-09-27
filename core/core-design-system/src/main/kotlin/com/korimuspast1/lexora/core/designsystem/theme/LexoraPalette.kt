package com.korimuspast1.lexora.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object LexoraPalette {
    val Primary10 = Color(0xFF061437)
    val Primary20 = Color(0xFF0D255B)
    val Primary30 = Color(0xFF173A8A)
    val Primary40 = Color(0xFF2450BD)
    val Primary50 = Color(0xFF305CDE)
    val Primary60 = Color(0xFF5D7EF0)
    val Primary70 = Color(0xFF8BA2FA)
    val Primary80 = Color(0xFFB8C6FF)
    val Primary90 = Color(0xFFDDE5FF)
    val Primary95 = Color(0xFFF0F4FF)

    val Secondary10 = Color(0xFF002018)
    val Secondary20 = Color(0xFF00382C)
    val Secondary30 = Color(0xFF005241)
    val Secondary40 = Color(0xFF00745D)
    val Secondary50 = Color(0xFF00A884)
    val Secondary60 = Color(0xFF25D6AA)
    val Secondary70 = Color(0xFF63EAC3)
    val Secondary80 = Color(0xFF95F5D8)
    val Secondary90 = Color(0xFFC7FFEB)
    val Secondary95 = Color(0xFFE9FFF7)

    val Tertiary10 = Color(0xFF25103B)
    val Tertiary20 = Color(0xFF3B1A61)
    val Tertiary30 = Color(0xFF563080)
    val Tertiary40 = Color(0xFF7549A8)
    val Tertiary50 = Color(0xFF9360CF)
    val Tertiary60 = Color(0xFFB07FF0)
    val Tertiary70 = Color(0xFFC8A5FA)
    val Tertiary80 = Color(0xFFE0C8FF)
    val Tertiary90 = Color(0xFFF1E6FF)
    val Tertiary95 = Color(0xFFFAF4FF)

    val Success10 = Color(0xFF03210F)
    val Success20 = Color(0xFF073B1C)
    val Success30 = Color(0xFF0F5A2C)
    val Success40 = Color(0xFF16823F)
    val Success50 = Color(0xFF20B35A)
    val Success60 = Color(0xFF47D47C)
    val Success70 = Color(0xFF72E79C)
    val Success80 = Color(0xFFA8F5C1)
    val Success90 = Color(0xFFD8FFE5)
    val Success95 = Color(0xFFEEFFF4)

    val Warning10 = Color(0xFF2A1800)
    val Warning20 = Color(0xFF4A2B00)
    val Warning30 = Color(0xFF704200)
    val Warning40 = Color(0xFFA66100)
    val Warning50 = Color(0xFFFF9800)
    val Warning60 = Color(0xFFFFB23F)
    val Warning70 = Color(0xFFFFC875)
    val Warning80 = Color(0xFFFFDDA7)
    val Warning90 = Color(0xFFFFEFD6)
    val Warning95 = Color(0xFFFFF8ED)

    val Error10 = Color(0xFF3B0508)
    val Error20 = Color(0xFF650D13)
    val Error30 = Color(0xFF921923)
    val Error40 = Color(0xFFC52D38)
    val Error50 = Color(0xFFE84D59)
    val Error60 = Color(0xFFFF7881)
    val Error70 = Color(0xFFFFA0A7)
    val Error80 = Color(0xFFFFC6CA)
    val Error90 = Color(0xFFFFE1E4)
    val Error95 = Color(0xFFFFF2F3)

    val Neutral10 = Color(0xFF10141C)
    val Neutral20 = Color(0xFF222938)
    val Neutral30 = Color(0xFF354052)
    val Neutral40 = Color(0xFF4D5A70)
    val Neutral50 = Color(0xFF68758C)
    val Neutral60 = Color(0xFF8491A6)
    val Neutral70 = Color(0xFFA5AFBF)
    val Neutral80 = Color(0xFFC6CEDA)
    val Neutral90 = Color(0xFFE5EAF1)
    val Neutral95 = Color(0xFFF4F7FB)
}

@Immutable
data class LexoraExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val legendary: Color,
    val onLegendary: Color,
    val gold: Color,
    val gem: Color,
    val heart: Color,
    val streak: Color,
)

val LightLexoraExtendedColors = LexoraExtendedColors(
    success = LexoraPalette.Success50,
    onSuccess = Color.White,
    successContainer = LexoraPalette.Success90,
    onSuccessContainer = LexoraPalette.Success20,
    warning = LexoraPalette.Warning50,
    onWarning = LexoraPalette.Warning10,
    warningContainer = LexoraPalette.Warning90,
    onWarningContainer = LexoraPalette.Warning20,
    legendary = LexoraPalette.Tertiary50,
    onLegendary = Color.White,
    gold = Color(0xFFFFC441),
    gem = Color(0xFF13C9FF),
    heart = LexoraPalette.Error50,
    streak = Color(0xFFFF6D1A),
)

val DarkLexoraExtendedColors = LexoraExtendedColors(
    success = LexoraPalette.Success60,
    onSuccess = LexoraPalette.Success10,
    successContainer = LexoraPalette.Success20,
    onSuccessContainer = LexoraPalette.Success90,
    warning = LexoraPalette.Warning60,
    onWarning = LexoraPalette.Warning10,
    warningContainer = LexoraPalette.Warning20,
    onWarningContainer = LexoraPalette.Warning90,
    legendary = LexoraPalette.Tertiary70,
    onLegendary = LexoraPalette.Tertiary10,
    gold = Color(0xFFFFD66E),
    gem = Color(0xFF7CE5FF),
    heart = LexoraPalette.Error60,
    streak = Color(0xFFFF9A52),
)

val LocalLexoraExtendedColors = staticCompositionLocalOf { LightLexoraExtendedColors }
