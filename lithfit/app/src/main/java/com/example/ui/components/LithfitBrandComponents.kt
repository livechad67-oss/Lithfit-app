package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.ImageStorageHelper
import com.example.data.model.LithfitConstants
import com.example.data.model.SeedCatalogData
import com.example.ui.theme.InkBlack
import com.example.ui.theme.SyneFontFamily
import java.io.File

@Composable
fun ClastadLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    cornerRadius: Dp = 8.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_clastad_logo),
        contentDescription = "CLASTAD logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(cornerRadius))
    )
}

@Composable
fun LithfitHangerEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    Image(
        painter = painterResource(id = R.drawable.ic_lithfit_hanger_emblem),
        contentDescription = "LITHFIT hanger emblem",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(width = size * 1.65f, height = size)
    )
}

@Composable
fun LithfitLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        LithfitHangerEmblem(
            size = size * 0.44f,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LithfitPrimaryLogo(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    emblemSize: Dp = 54.dp,
    wordmarkSizeSp: Float = 26f,
    showAttribution: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LithfitHangerEmblem(
            size = emblemSize,
            tint = tint
        )
        LithfitWordmark(
            textColor = tint,
            fontSizeSp = wordmarkSizeSp,
            showAttribution = showAttribution,
            centered = true
        )
    }
}

@Composable
fun LithfitWordmark(
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSizeSp: Float = 20f,
    showAttribution: Boolean = false,
    centered: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = "LITHFIT",
            fontFamily = SyneFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp.sp,
            letterSpacing = (fontSizeSp * 0.28f).sp,
            color = textColor,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
        if (showAttribution) {
            Text(
                text = "From CLASTAD",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.72f),
                letterSpacing = 1.4.sp,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
fun ColorSwatchDot(
    colorName: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    selected: Boolean = false
) {
    val swatchColor = remember(colorName) { Color(LithfitConstants.colorHexForName(colorName)) }
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(swatchColor)
            .border(
                width = if (selected) 2.2.dp else 1.dp,
                color = ringColor,
                shape = CircleShape
            )
    )
}

@Composable
fun ColorSwatchChip(
    colorName: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.heightIn(min = 38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ColorSwatchDot(colorName = colorName, size = 14.dp, selected = selected)
            Text(
                text = colorName,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun LithfitGarmentImage(
    localImagePath: String?,
    thumbnailBase64: String?,
    imageUrl: String?,
    imageResKey: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    @Suppress("UNUSED_PARAMETER") paletteNames: List<String> = emptyList()
) {
    val context = LocalContext.current
    val file = remember(localImagePath) {
        localImagePath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }
    val decodedThumb: Bitmap? = remember(thumbnailBase64) {
        thumbnailBase64?.let { ImageStorageHelper.decodeBase64Thumbnail(it) }
    }
    val drawableId = remember(imageResKey) { SeedCatalogData.editorialDrawableForKey(imageResKey) }

    when {
        file != null -> {
            val req = remember(file) {
                ImageRequest.Builder(context)
                    .data(file)
                    .size(540)
                    .crossfade(100)
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        decodedThumb != null -> {
            Image(
                bitmap = decodedThumb.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        !imageUrl.isNullOrBlank() -> {
            val req = remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(540)
                    .crossfade(100)
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        drawableId != null -> {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        else -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun LegalDocumentDialog(
    title: String,
    bodyText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("close_legal_doc_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ReportContentDialog(
    targetTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String?) -> Unit
) {
    val reasons = listOf(
        "Copyright or trademark infringement",
        "Brand or seller impersonation",
        "Inappropriate or unsafe content",
        "Misleading product or price claim"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Content", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reporting: $targetTitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details.ifBlank { null }) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        }
    )
}

@Composable
fun LithfitShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "lithfit_shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.72f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 850),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha))
    )
}

@Composable
fun ContextualTipBanner(
    tipKey: String,
    title: String,
    body: String,
    dismissedTips: Set<String>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tipKey in dismissedTips) return
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = { onDismiss(tipKey) },
                modifier = Modifier.testTag("dismiss_tip_$tipKey")
            ) {
                Text("Got it")
            }
        }
    }
}
