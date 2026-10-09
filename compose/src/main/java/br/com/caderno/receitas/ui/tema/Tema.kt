package br.com.caderno.receitas.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Paleta "cozinha" (terracota, manjericão e mostarda). As combinações texto/fundo foram
 * escolhidas com contraste de pelo menos 4,5:1 (nível AA), nos temas claro e escuro.
 * Não usamos cores dinâmicas (Material You) para manter o contraste sob controle.
 */

private val Claro = lightColorScheme(
    primary = Color(0xFFB23A12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF2F6B3A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB3F0BB),
    onSecondaryContainer = Color(0xFF002107),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA0),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF2B1A12),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF2B1A12),
    surfaceVariant = Color(0xFFF5DED4),
    onSurfaceVariant = Color(0xFF53433D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1E8),
    surfaceContainer = Color(0xFFFBEBE1),
    surfaceContainerHigh = Color(0xFFF5E5DB),
    surfaceContainerHighest = Color(0xFFEFDFD5),
    outline = Color(0xFF85736C),
    outlineVariant = Color(0xFFD8C2B8),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFFFFB59C),
    onPrimary = Color(0xFF5C1A04),
    primaryContainer = Color(0xFF7C2709),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF98D5A0),
    onSecondary = Color(0xFF003914),
    secondaryContainer = Color(0xFF155227),
    onSecondaryContainer = Color(0xFFB3F0BB),
    tertiary = Color(0xFFF0C048),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA0),
    background = Color(0xFF1E1612),
    onBackground = Color(0xFFF2DFD6),
    surface = Color(0xFF1E1612),
    onSurface = Color(0xFFF2DFD6),
    surfaceVariant = Color(0xFF53433D),
    onSurfaceVariant = Color(0xFFD8C2B8),
    surfaceContainerLowest = Color(0xFF180F0C),
    surfaceContainerLow = Color(0xFF261E1A),
    surfaceContainer = Color(0xFF2B221E),
    surfaceContainerHigh = Color(0xFF362C28),
    surfaceContainerHighest = Color(0xFF413733),
    outline = Color(0xFFA08D85),
    outlineVariant = Color(0xFF53433D),
)

@Composable
fun TemaCaderno(
    escuro: Boolean = isSystemInDarkTheme(),
    conteudo: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (escuro) Escuro else Claro,
        content = conteudo,
    )
}
