package com.example.drinkmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkmanager.model.Cocktail
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.GlassCardBg
import com.example.drinkmanager.theme.GoldHighlight
import com.example.drinkmanager.theme.StockInStock
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.GlassCard
import com.example.drinkmanager.ui.components.SearchBarComponent

@Composable
fun CocktailsScreen(
    cocktails: List<Cocktail>,
    selectedCocktail: Cocktail?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    canMakeOnly: Boolean,
    onToggleCanMakeOnly: () -> Unit,
    onSelectCocktail: (Cocktail) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SearchBarComponent(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Search cocktail & mocktail recipes..."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Can Make Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassCardBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StockInStock, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Can Make Now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Filter recipes with in-stock ingredients",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Switch(
                checked = canMakeOnly,
                onCheckedChange = { onToggleCanMakeOnly() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmberPrimary,
                    checkedTrackColor = AmberPrimary.copy(alpha = 0.3f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Cocktail Recipes (${cocktails.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AmberPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(cocktails) { cocktail ->
                GlassCard(
                    isSelected = selectedCocktail?.name == cocktail.name,
                    onClick = { onSelectCocktail(cocktail) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmberPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalBar,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cocktail.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (cocktail.bottleName != null) {
                                Text(
                                    text = "Base: ${cocktail.bottleName}",
                                    fontSize = 12.sp,
                                    color = AmberPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cocktail.ingredients.joinToString(" • "),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                    }

                    if (selectedCocktail?.name == cocktail.name && !cocktail.instructions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Preparation Instructions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldHighlight)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cocktail.instructions!!,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
