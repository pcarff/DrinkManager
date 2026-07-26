package com.example.drinkmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.drinkmanager.model.Cocktail
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.DarkSurface
import com.example.drinkmanager.theme.GlassCardBg
import com.example.drinkmanager.theme.GlassCardBorder
import com.example.drinkmanager.theme.GoldHighlight
import com.example.drinkmanager.theme.StockInStock
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.GlassCard

@Composable
fun RecipeDetailDialog(
    cocktail: Cocktail?,
    onDismiss: () -> Unit,
    onToggleFavorite: (Cocktail) -> Unit,
    onSaveRecipe: (Cocktail) -> Unit
) {
    if (cocktail == null) return

    var isEditing by remember { mutableStateOf(false) }

    // Edit fields
    var editName by remember(cocktail) { mutableStateOf(cocktail.name) }
    var editGlass by remember(cocktail) { mutableStateOf(cocktail.glass ?: "Rocks Glass") }
    var editIngredientsText by remember(cocktail) {
        mutableStateOf(cocktail.ingredients.joinToString("\n"))
    }
    var editInstructions by remember(cocktail) { mutableStateOf(cocktail.instructions ?: "") }

    // Interactive checklist for ingredients in view mode
    val checkedIngredients = remember(cocktail) { mutableStateMapOf<Int, Boolean>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalBar,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (cocktail.isMocktail) "Mocktail Recipe" else "Cocktail Recipe",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
                            )
                            if (!isEditing) {
                                Text(
                                    text = cocktail.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onToggleFavorite(cocktail) }) {
                            Icon(
                                if (cocktail.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite Recipe",
                                tint = if (cocktail.isFavorite) GoldHighlight else TextSecondary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = GlassCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                if (isEditing) {
                    // EDIT MODE FORM
                    Text(
                        text = "Edit Recipe Details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldHighlight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Recipe Name", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editGlass,
                        onValueChange = { editGlass = it },
                        label = { Text("Glassware (e.g. Rocks Glass, Coupe)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editIngredientsText,
                        onValueChange = { editIngredientsText = it },
                        label = { Text("Ingredients (One per line)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editInstructions,
                        onValueChange = { editInstructions = it },
                        label = { Text("Instructions & Preparation", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassCardBorder)
                        ) {
                            Text("Cancel", color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val updatedIngredients = editIngredientsText
                                    .split("\n")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val updated = cocktail.copy(
                                    name = editName,
                                    glass = editGlass,
                                    ingredients = updatedIngredients,
                                    instructions = editInstructions
                                )
                                onSaveRecipe(updated)
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                        ) {
                            Text("Save Recipe", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                } else {
                    // VIEW MODE
                    if (cocktail.bottleName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Base Spirit: ", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = cocktail.bottleName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    cocktail.glass?.let { glass ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassCardBorder.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "Glassware: $glass", fontSize = 11.sp, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Ingredients",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldHighlight
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    cocktail.ingredients.forEachIndexed { idx, ing ->
                        val isChecked = checkedIngredients[idx] ?: false
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkedIngredients[idx] = !isChecked }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checkedIngredients[idx] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = StockInStock,
                                    uncheckedColor = TextSecondary
                                )
                            )
                            Text(
                                text = ing,
                                fontSize = 14.sp,
                                color = if (isChecked) TextSecondary else TextPrimary,
                                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Preparation & Serving",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cocktail.instructions ?: "No step-by-step instructions available.",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassCardBg)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AmberPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Recipe Details", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
