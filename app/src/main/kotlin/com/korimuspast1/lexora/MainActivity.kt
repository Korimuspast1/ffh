package com.korimuspast1.lexora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.korimuspast1.lexora.core.designsystem.icons.LexoraIcons
import com.korimuspast1.lexora.core.designsystem.theme.LexoraPalette
import com.korimuspast1.lexora.core.designsystem.theme.LexoraTheme
import com.korimuspast1.lexora.core.designsystem.theme.LexoraThemeValues
import com.korimuspast1.lexora.core.ui.components.LingoBadge
import com.korimuspast1.lexora.core.ui.components.LingoBottomBar
import com.korimuspast1.lexora.core.ui.components.LingoButton
import com.korimuspast1.lexora.core.ui.components.LingoCard
import com.korimuspast1.lexora.core.ui.components.LingoConnector
import com.korimuspast1.lexora.core.ui.components.LingoHeartRow
import com.korimuspast1.lexora.core.ui.components.LingoNavItem
import com.korimuspast1.lexora.core.ui.components.LingoPathNode
import com.korimuspast1.lexora.core.ui.components.LingoProgressBar
import com.korimuspast1.lexora.core.ui.components.LingoStreakFlame
import com.korimuspast1.lexora.core.ui.components.LingoTopBar
import com.korimuspast1.lexora.core.ui.components.LingoNodeStatus
import com.korimuspast1.lexora.core.ui.mascot.LexoraMascot
import com.korimuspast1.lexora.core.ui.mascot.MascotMood
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LexoraTheme {
                LexoraReleaseTrackScreen()
            }
        }
    }
}

@Composable
private fun LexoraReleaseTrackScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val navItems = listOf(
        LingoNavItem("learn", "Learn", LexoraIcons.Book),
        LingoNavItem("league", "League", LexoraIcons.Trophy),
        LingoNavItem("quests", "Quests", LexoraIcons.Star),
        LingoNavItem("shop", "Shop", LexoraIcons.Gem),
        LingoNavItem("profile", "Profile", LexoraIcons.User),
    )
    val selectedRoute = navItems[selectedTab].route

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                LexoraPalette.Primary95,
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    )
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                LingoTopBar(title = "Lexora") {
                    LingoStreakFlame(days = 1)
                    LingoHeartRow(lives = 5, total = 5)
                }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HeaderCard()
                    StageCard()
                    LearningPathPreview()
                    DesignSystemCard()
                    LingoButton(
                        text = "Продолжить разработку",
                        onClick = { selectedTab = (selectedTab + 1) % navItems.size },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = LexoraIcons.Lightning,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            LingoBottomBar(
                items = navItems,
                selectedRoute = selectedRoute,
                onSelected = { route -> selectedTab = navItems.indexOfFirst { it.route == route }.coerceAtLeast(0) },
            )
        }
    }
}

@Composable
private fun HeaderCard() {
    LingoCard(selected = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LexoraMascot(mood = MascotMood.Waving, size = 108.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nori приветствует тебя",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Это релизный трек Lexora: оригинальная архитектура, дизайн-система, маскот и базовый каркас приложения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StageCard() {
    LingoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LingoBadge(text = "2", modifier = Modifier.size(42.dp), color = LexoraThemeValues.colors.gem)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Этапы 2–3 в работе", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(text = "Design System + Nori mascot", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = LexoraIcons.Check, contentDescription = null, tint = LexoraThemeValues.colors.success)
        }
        LingoProgressBar(progress = 0.21f)
        Text(
            text = "Следом: Room, Network, Domain, DI, navigation и реальные feature-экраны до release APK.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LearningPathPreview() {
    LingoCard {
        Text(text = "Learning Path UX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LingoPathNode(status = LingoNodeStatus.Completed, label = "Start")
                LingoConnector()
                LingoPathNode(status = LingoNodeStatus.Current, label = "Lesson 1")
                LingoConnector()
                LingoPathNode(status = LingoNodeStatus.Locked, label = "Unit 2")
                LingoConnector()
                LingoPathNode(status = LingoNodeStatus.Legendary, label = "Legend")
            }
        }
    }
}

@Composable
private fun DesignSystemCard() {
    LingoCard {
        Text(text = "Собственная дизайн-система", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(
            text = "Палитры primary/secondary/tertiary/success/error/warning/neutral, светлая и тёмная темы, Material You fallback, типографика, радиусы, motion specs и кастомные ImageVector-иконки.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = LexoraIcons.Heart, contentDescription = null, tint = LexoraThemeValues.colors.heart)
            Icon(imageVector = LexoraIcons.Gem, contentDescription = null, tint = LexoraThemeValues.colors.gem)
            Icon(imageVector = LexoraIcons.Flame, contentDescription = null, tint = LexoraThemeValues.colors.streak)
            Icon(imageVector = LexoraIcons.Crown, contentDescription = null, tint = LexoraThemeValues.colors.legendary)
            Icon(imageVector = LexoraIcons.Shield, contentDescription = null, tint = Color(0xFF305CDE))
        }
    }
}
