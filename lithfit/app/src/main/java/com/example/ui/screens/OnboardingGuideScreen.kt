package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LithfitWordmark

data class GuideStepInfo(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val tip: String,
    val icon: ImageVector,
    val previewRes: Int
)

val lithfitGuideSteps = listOf(
    GuideStepInfo(
        stepNumber = 1,
        title = "Discovering Fashion",
        subtitle = "Browse curated streetwear, Y2K, old money, grunge, Afro-inspired fits, and Style Radar listings sorted for For You, Men's, Women's, Gender-Neutral, Teen, and Adult edits.",
        tip = "Use visual search with any outfit photo to find matching silhouettes.",
        icon = Icons.Outlined.Explore,
        previewRes = R.drawable.img_editorial_diverse_trio
    ),
    GuideStepInfo(
        stepNumber = 2,
        title = "Setting Preferences",
        subtitle = "Tune your favourite aesthetics, go-to colours, weather city, and under-18 privacy settings in Profile anytime.",
        tip = "LITHFIT AI reads your style preferences so every outfit recommendation feels naturally yours.",
        icon = Icons.Outlined.Tune,
        previewRes = R.drawable.img_hero_minimalist
    ),
    GuideStepInfo(
        stepNumber = 3,
        title = "Using the AI Stylist",
        subtitle = "Ask quick fashion questions like \"What shoes go with cargo trousers?\" for instant concise answers, or ask for full occasion outfits built around clothes you already own.",
        tip = "Tap Regenerate, Swap 1 Piece, or Visual Preview right from any outfit card.",
        icon = Icons.Outlined.AutoAwesome,
        previewRes = R.drawable.img_editorial_streetwear
    ),
    GuideStepInfo(
        stepNumber = 4,
        title = "Creating a Wardrobe",
        subtitle = "Upload garment photos from your gallery or camera—LITHFIT automatically tags category, colour, fabric, and style. Add an optional private full-body styling photo stored solely on your device.",
        tip = "Nothing you upload is shared or rated.",
        icon = Icons.Outlined.Checkroom,
        previewRes = R.drawable.img_editorial_y2k_vintage
    ),
    GuideStepInfo(
        stepNumber = 5,
        title = "Designing an Avatar",
        subtitle = "Open Avatar Studio inside My Wardrobe to customize skin tone, hairstyle, facial expression, glasses, shoes, and dress your avatar directly with items from your closet.",
        tip = "Save your avatar persona to keep lookbook previews cohesive.",
        icon = Icons.Outlined.FaceRetouchingNatural,
        previewRes = R.drawable.img_editorial_afro_fashion
    ),
    GuideStepInfo(
        stepNumber = 6,
        title = "Saving & Exporting Outfits",
        subtitle = "Mix pieces in Outfit Studio, rename your favourite combinations, generate visual outfit previews, and export generated lookbook images directly to your phone's Photos gallery.",
        tip = "Like or flag outfits so LITHFIT learns your taste over time.",
        icon = Icons.Outlined.BookmarkAdded,
        previewRes = R.drawable.img_editorial_grunge_teen
    )
)

@Composable
fun OnboardingGuideScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val step = lithfitGuideSteps[currentIndex]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LithfitWordmark(fontSizeSp = 17f, showAttribution = true)
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("guide_skip_button")
                ) {
                    Text("Skip Guide")
                }
            }

            // Animated Step Body
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                modifier = Modifier.weight(1f),
                label = "guide_step"
            ) { activeStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = activeStep.previewRes),
                                contentDescription = activeStep.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = activeStep.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Step ${activeStep.stepNumber} of ${lithfitGuideSteps.size}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = activeStep.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = activeStep.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tip: ${activeStep.tip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Progress dots & Back/Next CTA
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    lithfitGuideSteps.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (idx == currentIndex) 22.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == currentIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentIndex > 0) {
                        OutlinedButton(
                            onClick = { currentIndex -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            if (currentIndex < lithfitGuideSteps.lastIndex) {
                                currentIndex += 1
                            } else {
                                onFinish()
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentIndex > 0) 1.6f else 1f)
                            .height(50.dp)
                            .testTag("guide_next_button")
                    ) {
                        Text(if (currentIndex < lithfitGuideSteps.lastIndex) "Next Step" else "Start Styling")
                    }
                }
            }
        }
    }
}
