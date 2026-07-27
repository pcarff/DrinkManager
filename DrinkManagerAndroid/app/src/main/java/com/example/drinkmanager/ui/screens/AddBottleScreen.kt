package com.example.drinkmanager.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.DarkBackground
import com.example.drinkmanager.theme.DarkSurface
import com.example.drinkmanager.theme.GlassCardBorder
import com.example.drinkmanager.theme.GoldHighlight
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.GlassCard
import java.io.File

data class BottleFormData(
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val subCategory: String = "",
    val proof: String = "",
    val abvPercent: String = "",
    val volume: String = "",
    val notes: String = "",
    val photoFilename: String = "",
    val cocktailsRaw: String = "",
    val mocktailRaw: String = ""
)

enum class AddBottleState {
    CAMERA_PROMPT,
    ANALYZING,
    REVIEW_FORM
}

@Composable
fun AddBottleScreen(
    onDismiss: () -> Unit,
    onAnalyzeImage: (String, (BottleFormData?) -> Unit) -> Unit,
    onSaveBottle: (BottleFormData) -> Unit,
    existingBottles: List<com.example.drinkmanager.model.Bottle> = emptyList(),
    isAnalyzing: Boolean = false
) {
    val context = LocalContext.current
    var screenState by remember { mutableStateOf(AddBottleState.CAMERA_PROMPT) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var formData by remember { mutableStateOf(BottleFormData()) }
    var analysisError by remember { mutableStateOf<String?>(null) }

    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = currentPhotoUri
        if (success && uri != null) {
            capturedImageUri = uri
            // Read and base64-encode the image
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    screenState = AddBottleState.ANALYZING
                    analysisError = null

                    onAnalyzeImage(imageBase64!!) { result ->
                        if (result != null) {
                            formData = result
                            screenState = AddBottleState.REVIEW_FORM
                        } else {
                            analysisError = "Failed to analyze image. Please try again or fill in manually."
                            screenState = AddBottleState.REVIEW_FORM
                        }
                    }
                }
            } catch (e: Exception) {
                analysisError = "Error reading photo: ${e.message}"
                screenState = AddBottleState.REVIEW_FORM
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && currentPhotoUri != null) {
            cameraLauncher.launch(currentPhotoUri!!)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "bottle_images")
        dir.mkdirs()
        val tempFile = File(dir, "bottle_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        currentPhotoUri = uri

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
                Text(
                    text = "📸 Scan Bottle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (screenState == AddBottleState.REVIEW_FORM) {
                    IconButton(onClick = {
                        if (formData.name.isNotBlank()) {
                            onSaveBottle(formData)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = AmberPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            HorizontalDivider(color = GlassCardBorder)

            when (screenState) {
                AddBottleState.CAMERA_PROMPT -> {
                    // Initial state — prompt to take photo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = AmberPrimary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Scan a Bottle Label",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Take a photo of the bottle label and AI will automatically extract the details",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { launchCamera() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberPrimary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Camera", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                AddBottleState.ANALYZING -> {
                    // Loading state while Gemini analyzes
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Show captured photo thumbnail
                            if (capturedImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(capturedImageUri),
                                    contentDescription = "Captured bottle",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            CircularProgressIndicator(
                                color = AmberPrimary,
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analyzing bottle label...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldHighlight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI is reading the label to extract details",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                AddBottleState.REVIEW_FORM -> {
                    // Check for duplicate/similar bottle in real-time
                    val matchingBottle = remember(formData.name, existingBottles) {
                        if (formData.name.isBlank()) null
                        else {
                            val norm = { s: String -> s.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim() }
                            val target = norm(formData.name)
                            val targetWords = target.split(" ").filter { it.length > 2 }
                            existingBottles.find { b ->
                                val bNorm = norm(b.name)
                                if (bNorm == target) true
                                else {
                                    val bWords = bNorm.split(" ").filter { it.length > 2 }
                                    val common = targetWords.filter { bWords.contains(it) }
                                    val minLen = minOf(targetWords.size, bWords.size)
                                    val overlap = if (minLen > 0) common.size.toDouble() / minLen else 0.0
                                    (overlap >= 0.6 && common.size >= 2) || (target.length > 8 && bNorm.contains(target)) || (bNorm.length > 8 && target.contains(bNorm))
                                }
                            }
                        }
                    }

                    // Review and edit form
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Error banner if analysis failed
                        if (analysisError != null) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "⚠️ $analysisError",
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Duplicate warning banner if similar bottle exists
                        if (matchingBottle != null) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚠️ Similar Bottle Already In Bar",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldHighlight
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"${matchingBottle.name}\" is already in your inventory list.",
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Photo preview + retake
                        if (capturedImageUri != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(capturedImageUri),
                                    contentDescription = "Captured bottle",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Photo captured ✓", fontSize = 13.sp, color = GoldHighlight, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { launchCamera() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GlassCardBorder,
                                            contentColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Retake Photo", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = "Review & Edit Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldHighlight
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val textFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedLabelColor = AmberPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = AmberPrimary
                        )

                        // Name
                        OutlinedTextField(
                            value = formData.name,
                            onValueChange = { formData = formData.copy(name = it) },
                            label = { Text("Bottle Name *") },
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Brand
                        OutlinedTextField(
                            value = formData.brand,
                            onValueChange = { formData = formData.copy(brand = it) },
                            label = { Text("Brand / Distillery") },
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Category + SubCategory side by side
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formData.category,
                                onValueChange = { formData = formData.copy(category = it) },
                                label = { Text("Category") },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = formData.subCategory,
                                onValueChange = { formData = formData.copy(subCategory = it) },
                                label = { Text("Sub-Category") },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Proof + ABV + Volume row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formData.proof,
                                onValueChange = { formData = formData.copy(proof = it) },
                                label = { Text("Proof") },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = formData.abvPercent,
                                onValueChange = { formData = formData.copy(abvPercent = it) },
                                label = { Text("ABV %") },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = formData.volume,
                                onValueChange = { formData = formData.copy(volume = it) },
                                label = { Text("Volume") },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Notes
                        OutlinedTextField(
                            value = formData.notes,
                            onValueChange = { formData = formData.copy(notes = it) },
                            label = { Text("Tasting Notes / Description") },
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Save button
                        Button(
                            onClick = {
                                if (formData.name.isNotBlank()) {
                                    onSaveBottle(formData)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = formData.name.isNotBlank()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Inventory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
