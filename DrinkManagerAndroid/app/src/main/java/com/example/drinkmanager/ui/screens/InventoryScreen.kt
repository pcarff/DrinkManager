package com.example.drinkmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkmanager.model.Bottle
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.GlassCardBg
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.GlassCard
import com.example.drinkmanager.ui.components.QuickStockAdjuster
import com.example.drinkmanager.ui.components.SearchBarComponent
import com.example.drinkmanager.ui.components.StockBadge

@Composable
fun InventoryListPane(
    bottles: List<Bottle>,
    selectedBottle: Bottle?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedStockFilter: String,
    onStockFilterChange: (String) -> Unit,
    onSelectBottle: (Bottle) -> Unit,
    onUpdateStockLevel: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Bourbon", "Rye", "Scotch", "Tequila", "Gin", "Rum", "Liqueur", "Bitters")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        SearchBarComponent(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Search bar inventory (${bottles.size} bottles)..."
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) AmberPrimary else GlassCardBg)
                        .clickable { onCategoryChange(cat) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottle List
        if (bottles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bottles match search criteria",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(bottles, key = { it.id }) { bottle ->
                    BottleListItem(
                        bottle = bottle,
                        isSelected = selectedBottle?.id == bottle.id,
                        onClick = { onSelectBottle(bottle) },
                        onStockChange = { newLevel ->
                            onUpdateStockLevel(bottle.id, newLevel)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottleListItem(
    bottle: Bottle,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStockChange: (String) -> Unit
) {
    GlassCard(
        isSelected = isSelected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Bottle Icon + Name + Stock Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AmberPrimary.copy(alpha = 0.12f)),
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

                Text(
                    text = bottle.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                StockBadge(status = bottle.stockStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Row: Category Specs on Left + Quick Stock Adjuster on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bottle.category,
                        fontSize = 12.sp,
                        color = AmberPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (bottle.proof != null) {
                            Text(
                                text = "${bottle.proof.toInt()}° Proof",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                        if (bottle.volume != null) {
                            Text(
                                text = " • ${bottle.volume}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                QuickStockAdjuster(
                    currentLevel = bottle.stockLevel,
                    onLevelChange = onStockChange
                )
            }
        }
    }
}
