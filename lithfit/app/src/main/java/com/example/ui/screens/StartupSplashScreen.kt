package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ClastadLogoMark
import com.example.ui.theme.Charcoal
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalLithfitReducedMotion
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmCream
import kotlinx.coroutines.delay

/**
 * Clean Cream & Black fast non-skippable launch screen.
 * Displays an editorial collage of coordinated cloth sets in the background
 * with a soft Warm Cream radial blend, keeping the transparent LITHFIT logo
 * front-and-center and "From CLASTAD" anchored at the bottom.
 */
@Composable
fun StartupSplashScreen(
    onFinished: () -> Unit
) {
    val reducedMotion = LocalLithfitReducedMotion.current
    val totalDurationMs = 2500f
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
            delay(80)
            onFinished()
        }
    }

    val progress = (elapsedMs / totalDurationMs).coerceIn(0f, 1f)
    val logoReveal = if (reducedMotion) 1f else ((elapsedMs - 60f) / 480f).coerceIn(0f, 1f)
    val collageReveal = if (reducedMotion) 1f else (elapsedMs / 520f).coerceIn(0f, 1f)
    val exitAlpha = if (progress > 0.92f) {
        1f - ((progress - 0.92f) / 0.08f)
    } else 1f

    val activeAlpha by animateFloatAsState(
        targetValue = if (completed) 0f else exitAlpha,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "splash_exit_fade"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else 0.94f + 0.06f * logoReveal,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "splash_logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .alpha(activeAlpha)
            .testTag("startup_splash_screen")
    ) {
        // Curated Cloth-Sets Collage Layer (Strictly Loading Screen Background)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp)
                .alpha(0.46f * collageReveal),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SplashClothSetTile(
                    resId = R.drawable.img_cloth_set_streetwear,
                    contentDescription = "Streetwear cloth set",
                    modifier = Modifier.weight(1.06f).fillMaxHeight()
                )
                SplashClothSetTile(
                    resId = R.drawable.img_cloth_set_minimalist,
                    contentDescription = "Minimalist tailored cloth set",
                    modifier = Modifier.weight(0.94f).fillMaxHeight()
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SplashClothSetTile(
                    resId = R.drawable.img_cloth_set_y2k,
                    contentDescription = "Y2K denim and sneaker cloth set",
                    modifier = Modifier.weight(0.94f).fillMaxHeight()
                )
                SplashClothSetTile(
                    resId = R.drawable.img_cloth_set_smart_casual,
                    contentDescription = "Smart casual outfit cloth set",
                    modifier = Modifier.weight(1.06f).fillMaxHeight()
                )
            }
        }

        // Soft Warm Cream radial blend so the transparent LITHFIT front logo melts into the cloth-set collage cleanly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            WarmCream.copy(alpha = 0.88f),
                            WarmCream.copy(alpha = 0.68f),
                            WarmCream.copy(alpha = 0.36f)
                        )
                    )
                )
        )

        // Blended Transparent LITHFIT Logo in Front (no box, no white card)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .scale(logoScale)
                .alpha(logoReveal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_lithfit_splash_logo),
                contentDescription = "LITHFIT",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(248.dp)
                    .height(124.dp)
                    .testTag("splash_lithfit_logo")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Your style. Your way.",
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal
            )
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = InkBlack,
                trackColor = SoftGrey,
                modifier = Modifier
                    .width(204.dp)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .testTag("splash_progress_indicator")
            )
        }

        // Centered below: "From CLASTAD" paired with Clastad's one-letter emblem
        Surface(
            color = WarmCream.copy(alpha = 0.94f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, InkBlack.copy(alpha = 0.16f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 30.dp)
                .testTag("splash_clastad_footer")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClastadLogoMark(size = 24.dp, cornerRadius = 6.dp)
                Text(
                    text = "From CLASTAD",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

@Composable
private fun SplashClothSetTile(
    resId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    )
}
