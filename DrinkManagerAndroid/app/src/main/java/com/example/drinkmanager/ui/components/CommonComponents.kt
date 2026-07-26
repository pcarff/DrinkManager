package com.example.drinkmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.GlassCardBg
import com.example.drinkmanager.theme.GlassCardBorder
import com.example.drinkmanager.theme.GoldHighlight
import com.example.drinkmanager.theme.StockInStock
import com.example.drinkmanager.theme.StockLow
import com.example.drinkmanager.theme.StockOutOfStock
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (isSelected) AmberPrimary else GlassCardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SearchBarComponent(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search bottles, spirits, or notes..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AmberPrimary) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = GlassCardBg,
            unfocusedContainerColor = GlassCardBg,
            focusedBorderColor = AmberPrimary,
            unfocusedBorderColor = GlassCardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AmberPrimary
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
fun TopHeaderBar(
    isOnline: Boolean,
    bottleCount: Int,
    syncStatusMessage: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmberPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalBar, contentDescription = "Logo", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "DRINK MANAGER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) StockInStock else StockLow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline) "Server Connected" else "Offline Store Mode",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(GlassCardBg)
                .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$bottleCount Bottles",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AmberPrimary
            )
        }
    }
}

@Composable
fun StockBadge(status: String) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "in-stock", "full", "high" -> Triple(StockInStock.copy(alpha = 0.15f), StockInStock, "In Stock")
        "low", "medium" -> Triple(StockLow.copy(alpha = 0.15f), StockLow, "Low Stock")
        else -> Triple(StockOutOfStock.copy(alpha = 0.15f), StockOutOfStock, "Out of Stock")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SpecTag(label: String, value: String, color: Color = AmberPrimary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GlassCardBorder.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label: ", fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun StockLevelSelector(
    currentLevel: String,
    onLevelSelected: (String) -> Unit
) {
    val levels = listOf(
        Pair("full", "Full"),
        Pair("three-quarter", "¾"),
        Pair("half", "½"),
        Pair("quarter", "¼"),
        Pair("empty", "Empty")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassCardBg)
            .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        levels.forEach { (levelKey, levelLabel) ->
            val isSelected = normalizeStockLevel(currentLevel) == levelKey
            val btnBg = if (isSelected) AmberPrimary else Color.Transparent
            val btnText = if (isSelected) Color.Black else TextSecondary

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(btnBg)
                    .clickable { onLevelSelected(levelKey) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = levelLabel,
                    color = btnText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** Map any legacy or mixed stock level key to the canonical server key */
fun normalizeStockLevel(level: String): String {
    return when (level.lowercase()) {
        "full" -> "full"
        "high", "three-quarter", "3/4" -> "three-quarter"
        "medium", "half", "1/2" -> "half"
        "low", "quarter", "1/4", "almost-empty" -> "quarter"
        "empty" -> "empty"
        else -> "full"
    }
}

@Composable
fun QuickStockAdjuster(
    currentLevel: String,
    onLevelChange: (String) -> Unit
) {
    val levels = listOf("empty", "quarter", "half", "three-quarter", "full")
    val displayNames = mapOf("empty" to "Empty", "quarter" to "¼", "half" to "½", "three-quarter" to "¾", "full" to "Full")
    val normalized = normalizeStockLevel(currentLevel)
    val currentIndex = levels.indexOf(normalized).coerceAtLeast(0)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GlassCardBorder)
                .clickable(enabled = currentIndex > 0) {
                    if (currentIndex > 0) onLevelChange(levels[currentIndex - 1])
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease Stock", tint = if (currentIndex > 0) AmberPrimary else TextSecondary, modifier = Modifier.size(16.dp))
        }

        Text(
            text = displayNames[levels[currentIndex]] ?: "Full",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldHighlight,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GlassCardBorder)
                .clickable(enabled = currentIndex < levels.size - 1) {
                    if (currentIndex < levels.size - 1) onLevelChange(levels[currentIndex + 1])
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase Stock", tint = if (currentIndex < levels.size - 1) AmberPrimary else TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

