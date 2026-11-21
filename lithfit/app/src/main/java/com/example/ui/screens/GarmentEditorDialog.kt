package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.GarmentCropAspect
import com.example.core.ImageStorageHelper
import com.example.data.model.CatalogueProduct
import com.example.data.model.GarmentCategory
import com.example.data.model.LithfitConstants
import com.example.data.model.WardrobeItem
import com.example.ui.components.ColorSwatchChip
import com.example.ui.components.LithfitGarmentImage
import com.example.ui.theme.SageSuccess
import com.example.ui.theme.WarmCream
import com.example.ui.viewmodel.GarmentRecognitionState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditGarmentDialog(
    existing: WardrobeItem?,
    recognitionState: GarmentRecognitionState,
    catalogue: List<CatalogueProduct>,
    onAnalyzeUri: (Uri) -> Unit,
    onAnalyzeBitmap: ((Bitmap) -> Unit)? = null,
    onImportUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (WardrobeItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember(existing) { mutableStateOf(existing?.categoryEnum() ?: GarmentCategory.TOPS) }
    var subType by remember(existing) { mutableStateOf(existing?.subType.orEmpty()) }
    var color by remember(existing) { mutableStateOf(existing?.color.orEmpty()) }
    var pattern by remember(existing) { mutableStateOf(existing?.pattern.orEmpty()) }
    var material by remember(existing) { mutableStateOf(existing?.material.orEmpty()) }
    var texture by remember(existing) { mutableStateOf(existing?.texture.orEmpty()) }
    var finish by remember(existing) { mutableStateOf(existing?.finish.orEmpty()) }
    var style by remember(existing) { mutableStateOf(existing?.style.orEmpty()) }
    var garmentType by remember(existing) { mutableStateOf(existing?.garmentType.orEmpty()) }
    var brand by remember(existing) { mutableStateOf(existing?.brand.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var privateItem by remember(existing) { mutableStateOf(existing?.privateItem ?: true) }
    var imagePath by remember(existing) { mutableStateOf(existing?.imagePath.orEmpty()) }
    var thumbnailBase64 by remember(existing) { mutableStateOf(existing?.thumbnailBase64.orEmpty()) }
    var showUrlBox by remember { mutableStateOf(false) }
    var importUrlText by remember { mutableStateOf("") }
    var showCataloguePick by remember { mutableStateOf(false) }

    // Interactive Crop & Background Removal Studio State
    var sourceRawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropAspect by remember { mutableStateOf(GarmentCropAspect.PORTRAIT) }
    var cropZoom by remember { mutableFloatStateOf(1f) }
    var cropPanX by remember { mutableFloatStateOf(0f) }
    var cropPanY by remember { mutableFloatStateOf(0f) }
    var removeBackgroundEnabled by remember { mutableStateOf(true) }
    var bgTolerance by remember { mutableFloatStateOf(48f) }
    var studioPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bgFallbackNote by remember { mutableStateOf<String?>(null) }
    var isProcessingCutout by remember { mutableStateOf(false) }
    var cutoutAppliedOnce by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var lastAppliedImagePath by remember(existing) { mutableStateOf("") }

    suspend fun rebuildPreview(runBgRemoval: Boolean) {
        val raw = sourceRawBitmap ?: return
        isProcessingCutout = true
        bgFallbackNote = null
        val cropped = ImageStorageHelper.cropBitmap(
            source = raw,
            zoom = cropZoom,
            offsetXFrac = cropPanX,
            offsetYFrac = cropPanY,
            aspectRatio = cropAspect.ratio
        )
        if (runBgRemoval) {
            val res = ImageStorageHelper.removeBackgroundFromGarmentBitmap(
                source = cropped,
                tolerance = bgTolerance.roundToInt()
            )
            studioPreviewBitmap = res.studioBitmap
            if (res.usedFallback) {
                bgFallbackNote = "Complex backdrop kept intact so no garment details were clipped."
            }
        } else {
            studioPreviewBitmap = cropped
        }
        isProcessingCutout = false
    }

    fun stageUriForCropStudio(uri: Uri) {
        scope.launch {
            val bmp = ImageStorageHelper.loadBitmapFromUri(context, uri)
            if (bmp == null) {
                onAnalyzeUri(uri)
                return@launch
            }
            sourceRawBitmap = bmp
            cropAspect = GarmentCropAspect.PORTRAIT
            cropZoom = 1f
            cropPanX = 0f
            cropPanY = 0f
            cutoutAppliedOnce = false
            rebuildPreview(runBgRemoval = removeBackgroundEnabled)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) stageUriForCropStudio(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            stageUriForCropStudio(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (uri, _) = ImageStorageHelper.createCameraCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(cropAspect, cropZoom, cropPanX, cropPanY, removeBackgroundEnabled) {
        if (sourceRawBitmap != null) {
            rebuildPreview(runBgRemoval = removeBackgroundEnabled)
        }
    }

    LaunchedEffect(recognitionState) {
        if (recognitionState is GarmentRecognitionState.Ready) {
            val attr = recognitionState.attributes
            val isNewCapture = recognitionState.localImagePath != lastAppliedImagePath
            if (isNewCapture) {
                lastAppliedImagePath = recognitionState.localImagePath
                name = attr.name
                category = attr.category
                color = attr.color
                pattern = attr.pattern
                material = attr.material
                style = attr.style
                garmentType = attr.garmentType
                subType = attr.garmentType
            }
            imagePath = recognitionState.localImagePath
            thumbnailBase64 = recognitionState.thumbnailBase64
        } else if (recognitionState is GarmentRecognitionState.Error) {
            if (recognitionState.localImagePath.isNotBlank()) {
                imagePath = recognitionState.localImagePath
                thumbnailBase64 = recognitionState.thumbnailBase64
            }
        }
    }

    val subTypeOptions = remember(category) {
        LithfitConstants.SUBTYPES_BY_CATEGORY[category].orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Add to Wardrobe" else "Edit Garment")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (existing == null) {
                    Text(
                        "1. Select a photo -> 2. Crop & adjust -> 3. Clean studio background -> 4. Confirm & save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val (uri, _) = ImageStorageHelper.createCameraCaptureUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Camera")
                        }
                        OutlinedButton(onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery")
                        }
                        OutlinedButton(onClick = { showUrlBox = !showUrlBox }) {
                            Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("URL")
                        }
                        if (catalogue.isNotEmpty()) {
                            OutlinedButton(onClick = { showCataloguePick = !showCataloguePick }) {
                                Text("Catalogue")
                            }
                        }
                    }

                    if (showUrlBox) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = importUrlText,
                                onValueChange = { importUrlText = it },
                                label = { Text("Authorized Image URL") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        val remoteBmp = ImageStorageHelper.loadRemoteBitmap(importUrlText)
                                        if (remoteBmp != null) {
                                            sourceRawBitmap = remoteBmp
                                            cropAspect = GarmentCropAspect.PORTRAIT
                                            cropZoom = 1f
                                            cropPanX = 0f
                                            cropPanY = 0f
                                            cutoutAppliedOnce = false
                                            rebuildPreview(runBgRemoval = removeBackgroundEnabled)
                                            showUrlBox = false
                                        } else {
                                            onImportUrl(importUrlText)
                                        }
                                    }
                                },
                                enabled = importUrlText.startsWith("http")
                            ) {
                                Text("Fetch")
                            }
                        }
                    }

                    if (showCataloguePick) {
                        catalogue.take(6).forEach { prod ->
                            TextButton(onClick = {
                                name = prod.name
                                category = GarmentCategory.safeParse(prod.category)
                                color = prod.color
                                style = prod.style
                                brand = prod.sellerName
                                showCataloguePick = false
                            }) {
                                Text("${prod.name} (${prod.color})")
                            }
                        }
                    }

                    // Interactive Crop + Studio Background Removal Editor
                    if (sourceRawBitmap != null) {
                        Surface(
                            color = WarmCream,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("garment_crop_studio_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(
                                            "Crop & Studio Cutout",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    TextButton(onClick = {
                                        cropAspect = GarmentCropAspect.PORTRAIT
                                        cropZoom = 1f
                                        cropPanX = 0f
                                        cropPanY = 0f
                                    }) {
                                        Text("Reset")
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WarmCream),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val preview = studioPreviewBitmap ?: sourceRawBitmap
                                    if (preview != null) {
                                        Image(
                                            bitmap = preview.asImageBitmap(),
                                            contentDescription = "Cropped garment preview on neutral backdrop",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(176.dp)
                                        )
                                    }
                                    if (isProcessingCutout) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(GarmentCropAspect.entries) { aspect ->
                                        FilterChip(
                                            selected = cropAspect == aspect,
                                            onClick = { cropAspect = aspect },
                                            label = { Text(aspect.label) }
                                        )
                                    }
                                }

                                Text("Zoom (${String.format("%.1fx", cropZoom)})", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = cropZoom,
                                    onValueChange = { cropZoom = it },
                                    valueRange = 1f..2.5f
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pan X", style = MaterialTheme.typography.labelSmall)
                                        Slider(
                                            value = cropPanX,
                                            onValueChange = { cropPanX = it },
                                            valueRange = -1f..1f
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pan Y", style = MaterialTheme.typography.labelSmall)
                                        Slider(
                                            value = cropPanY,
                                            onValueChange = { cropPanY = it },
                                            valueRange = -1f..1f
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Remove background", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "Isolates garment on Warm Cream canvas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = removeBackgroundEnabled,
                                        onCheckedChange = { removeBackgroundEnabled = it }
                                    )
                                }

                                if (removeBackgroundEnabled) {
                                    Text("Cutout Edge Sensitivity (${bgTolerance.roundToInt()})", style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = bgTolerance,
                                        onValueChange = { bgTolerance = it },
                                        valueRange = 22f..85f,
                                        onValueChangeFinished = {
                                            scope.launch { rebuildPreview(runBgRemoval = true) }
                                        }
                                    )
                                }

                                bgFallbackNote?.let { note ->
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { scope.launch { rebuildPreview(runBgRemoval = removeBackgroundEnabled) } },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Retry")
                                    }
                                    Button(
                                        onClick = {
                                            val finalBmp = studioPreviewBitmap ?: sourceRawBitmap
                                            if (finalBmp != null) {
                                                cutoutAppliedOnce = true
                                                onAnalyzeBitmap?.invoke(finalBmp)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("apply_cutout_and_tag_button")
                                    ) {
                                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (cutoutAppliedOnce) "Re-Tag Piece" else "Use & Tag")
                                    }
                                }
                            }
                        }
                    }

                    when (recognitionState) {
                        is GarmentRecognitionState.Analyzing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("LITHFIT AI is identifying cloth type & colour...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is GarmentRecognitionState.Ready -> {
                            Surface(
                                color = SageSuccess.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = SageSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Identified: ${recognitionState.attributes.garmentType} (${recognitionState.attributes.color}) — ready to save into ${category.label}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SageSuccess
                                    )
                                }
                            }
                        }
                        is GarmentRecognitionState.Error -> {
                            Text(
                                recognitionState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        GarmentRecognitionState.Idle -> Unit
                    }
                }

                if (sourceRawBitmap == null && (imagePath.isNotBlank() || thumbnailBase64.isNotBlank())) {
                    LithfitGarmentImage(
                        localImagePath = imagePath.ifBlank { null },
                        thumbnailBase64 = thumbnailBase64.ifBlank { null },
                        imageUrl = null,
                        imageResKey = null,
                        contentDescription = "Garment preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GarmentCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (subType.isNotBlank() && subType !in LithfitConstants.SUBTYPES_BY_CATEGORY[cat].orEmpty()) {
                                    subType = ""
                                }
                            },
                            label = { Text(cat.label) }
                        )
                    }
                }

                if (subTypeOptions.isNotEmpty()) {
                    Text("Piece Type", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subTypeOptions) { piece ->
                            FilterChip(
                                selected = subType.equals(piece, ignoreCase = true),
                                onClick = {
                                    subType = piece
                                    garmentType = piece
                                    if (name.isBlank()) {
                                        name = if (color.isNotBlank()) "$color $piece" else piece
                                    }
                                },
                                label = { Text(piece) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g. Heavyweight Boxy Hoodie") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("garment_name_input")
                )

                Text("Colourway *", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.COLOR_SWATCHES) { swatch ->
                        ColorSwatchChip(
                            colorName = swatch.name,
                            selected = color.equals(swatch.name, ignoreCase = true),
                            onClick = { color = swatch.name }
                        )
                    }
                }
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Primary Colour (or custom shade) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("garment_color_input")
                )

                Text("Style Vibe", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.STYLE_CATEGORIES) { st ->
                        FilterChip(
                            selected = style.equals(st, ignoreCase = true),
                            onClick = { style = st },
                            label = { Text(st) }
                        )
                    }
                }

                Text("Pattern & Finish", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.PATTERNS) { pat ->
                        FilterChip(
                            selected = pattern.equals(pat, ignoreCase = true),
                            onClick = { pattern = pat },
                            label = { Text(pat) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LithfitConstants.TEXTURES_FINISHES) { tex ->
                        FilterChip(
                            selected = texture.equals(tex, ignoreCase = true),
                            onClick = { texture = tex },
                            label = { Text(tex) }
                        )
                    }
                }

                OutlinedTextField(
                    value = material,
                    onValueChange = { material = it },
                    label = { Text("Fabric / Material (e.g. Cotton, Denim, Fleece)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = finish,
                    onValueChange = { finish = it },
                    label = { Text("Wash / Finish (e.g. Washed, Raw, Matte)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keep item private", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = privateItem, onCheckedChange = { privateItem = it })
                }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val activePreview = studioPreviewBitmap
                    if (existing == null && imagePath.isBlank() && activePreview != null) {
                        scope.launch {
                            val processed = ImageStorageHelper.processBitmapForWardrobe(context, activePreview)
                            val bundle = processed.getOrNull()
                            val model = WardrobeItem(
                                name = name.trim(),
                                category = category.name,
                                subType = subType.trim().ifBlank { null },
                                color = color.trim(),
                                pattern = pattern.trim().ifBlank { null },
                                material = material.trim().ifBlank { null },
                                texture = texture.trim().ifBlank { null },
                                finish = finish.trim().ifBlank { null },
                                style = style.trim().ifBlank { null },
                                garmentType = garmentType.trim().ifBlank { subType.trim().ifBlank { null } },
                                brand = brand.trim().ifBlank { null },
                                notes = notes.trim().ifBlank { null },
                                imagePath = bundle?.localFilePath,
                                thumbnailBase64 = bundle?.thumbnailBase64,
                                privateItem = privateItem
                            )
                            onSave(model)
                        }
                    } else {
                        val model = (existing ?: WardrobeItem()).copy(
                            name = name.trim(),
                            category = category.name,
                            subType = subType.trim().ifBlank { null },
                            color = color.trim(),
                            pattern = pattern.trim().ifBlank { null },
                            material = material.trim().ifBlank { null },
                            texture = texture.trim().ifBlank { null },
                            finish = finish.trim().ifBlank { null },
                            style = style.trim().ifBlank { null },
                            garmentType = garmentType.trim().ifBlank { subType.trim().ifBlank { null } },
                            brand = brand.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null },
                            imagePath = imagePath.ifBlank { null },
                            thumbnailBase64 = thumbnailBase64.ifBlank { null },
                            privateItem = privateItem
                        )
                        onSave(model)
                    }
                },
                enabled = name.isNotBlank() && color.isNotBlank(),
                modifier = Modifier.testTag("save_garment_button")
            ) {
                Text("Save")
            }
        }
    )
}
