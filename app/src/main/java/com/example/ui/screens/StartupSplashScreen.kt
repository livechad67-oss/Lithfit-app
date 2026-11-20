package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ClastadLogoMark
import com.example.ui.components.LithfitWordmark
import com.example.ui.theme.BrandPink
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class SplashEditorialScene(
    val imageRes: Int,
    val secondaryImageRes: Int,
    val cultureTag: String,
    val caption: String
)

private val splashEditorialScenes = listOf(
    SplashEditorialScene(
        imageRes = R.drawable.img_editorial_afro_fashion,
        secondaryImageRes = R.drawable.img_editorial_streetwear,
        cultureTag = "AFRO-INSPIRED • STREETWEAR",
        caption = "Expressive silhouettes across cultures"
    ),
    SplashEditorialScene(
        imageRes = R.drawable.img_editorial_y2k_vintage,
        secondaryImageRes = R.drawable.img_editorial_grunge_teen,
        cultureTag = "Y2K • GRUNGE • TEEN EDITION",
        caption = "Vintage layering with fresh attitude"
    ),
    SplashEditorialScene(
        imageRes = R.drawable.img_editorial_diverse_trio,
        secondaryImageRes = R.drawable.img_editorial_smart_casual,
        cultureTag = "OLD MONEY • CONTEMPORARY",
        caption = "Tailored clean fits for any occasion"
    ),
    SplashEditorialScene(
        imageRes = R.drawable.img_hero_minimalist,
        secondaryImageRes = R.drawable.img_editorial_afro_fashion,
        cultureTag = "MINIMALIST • TECHWEAR • CASUAL",
        caption = "Smart styling tuned to your wardrobe"
    )
)

/**
 * Full, non-skippable editorial launch experience showcasing LITHFIT and CLASTAD
 * over layered studio fashion photography without pixelated character animations.
 */
@Composable
fun StartupSplashScreen(
    onFinished: () -> Unit
) {
    val totalDurationMs = 4000f
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startNanos = System.nanoTime()
        while (elapsedMs < totalDurationMs) {
            withFrameMillis {
                elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000f).coerceAtMost(totalDurationMs)
            }
        }
        if (!completed) {
            completed = true
            delay(160)
            onFinished()
        }
    }

    val progress = (elapsedMs / totalDurationMs).coerceIn(0f, 1f)
    val sceneDuration = totalDurationMs / splashEditorialScenes.size
    val sceneIdx = (elapsedMs / sceneDuration).toInt().coerceIn(0, splashEditorialScenes.lastIndex)
    val scene = splashEditorialScenes[sceneIdx]

    val logoReveal = ((elapsedMs - 200f) / 700f).coerceIn(0f, 1f)
    val exitAlpha = if (progress > 0.92f) {
        1f - ((progress - 0.92f) / 0.08f)
    } else 1f

    val activeAlpha by animateFloatAsState(
        targetValue = if (completed) 0f else exitAlpha,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "splash_fade"
    )
    val logoScale by animateFloatAsState(
        targetValue = 0.90f + 0.10f * logoReveal,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "splash_logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141518))
            .alpha(activeAlpha)
    ) {
        val kenBurnsScale = 1.04f + 0.05f * sin(elapsedMs * 0.0009f)
        val driftX = (cos(elapsedMs * 0.0011f) * 10f).roundToInt()

        AnimatedContent(
            targetState = scene,
            transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(450)) },
            label = "splash_backdrop_scene"
        ) { activeScene ->
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = activeScene.imageRes),
                    contentDescription = activeScene.cultureTag,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(kenBurnsScale)
                        .alpha(0.46f)
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    color = Color(0xFF1E2025),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 66.dp)
                        .offset { IntOffset(driftX / 2, (-driftX / 3)) }
                        .size(width = 114.dp, height = 152.dp)
                        .alpha(0.84f)
                ) {
                    Image(
                        painter = painterResource(id = activeScene.secondaryImageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Dark editorial gradient scrim for crisp magazine contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88121316),
                            Color(0x4D121316),
                            Color(0xCC121316)
                        )
                    )
                )
        )

        // Top culture edition pill (no skip button — splash finishes smoothly)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BrandPink.copy(alpha = 0.5f))
            ) {
                Text(
                    text = scene.cultureTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandPink,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Center Brand Showcase with attached opening LITHFIT wordmark logo + CLASTAD emblem
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .scale(logoScale)
                .alpha(logoReveal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_lithfit_opening_logo),
                contentDescription = "LITHFIT opening logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(268.dp)
                    .height(166.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            LithfitWordmark(
                textColor = Color(0xFFFAF8F5),
                fontSizeSp = 15f,
                showAttribution = false,
                centered = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClastadLogoMark(size = 22.dp, cornerRadius = 6.dp)
                Text(
                    text = "From CLASTAD",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFAF8F5).copy(alpha = 0.85f),
                    letterSpacing = 1.8.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your Style. Your World.",
                style = MaterialTheme.typography.titleSmall,
                color = BrandPink,
                letterSpacing = 0.6.sp
            )
        }

        // Bottom loading status caption & full progress indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = scene.caption,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFAF8F5).copy(alpha = 0.85f)
            )
            LinearProgressIndicator(
                progress = { progress },
                color = BrandPink,
                trackColor = Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }
    }
}
