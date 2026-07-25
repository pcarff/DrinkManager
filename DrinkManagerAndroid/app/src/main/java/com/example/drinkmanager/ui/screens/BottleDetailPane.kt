package com.example.drinkmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkmanager.model.Bottle
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.GlassCardBorder
import com.example.drinkmanager.theme.GoldHighlight
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.GlassCard
import com.example.drinkmanager.ui.components.SpecTag
import com.example.drinkmanager.ui.components.StockBadge
import com.example.drinkmanager.ui.components.StockLevelSelector

@Composable
fun BottleDetailPane(
    bottle: Bottle?,
    onUpdateStockLevel: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bottle == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a bottle to view details",
                color = TextSecondary,
                fontSize = 16.sp
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Card Header
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalBar,
                        contentDescription = "Bottle Icon",
                        tint = AmberPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bottle.brand ?: bottle.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                    Text(
                        text = bottle.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StockBadge(status = bottle.stockStatus)
                        Spacer(modifier = Modifier.width(8.dp))
                        if (bottle.volume != null) {
                            SpecTag(label = "Volume", value = bottle.volume)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = GlassCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Proof & ABV Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PROOF", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = bottle.proof?.let { "${it.toInt()}°" } ?: "N/A",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldHighlight
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ABV", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = bottle.abvPercent?.let { "$it%" } ?: "N/A",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "CATEGORY", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = bottle.category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stock Selector Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quick Stock Adjuster",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            StockLevelSelector(
                currentLevel = bottle.stockLevel,
                onLevelSelected = { newLevel ->
                    onUpdateStockLevel(bottle.id, newLevel)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar Location & Notes Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = AmberPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Location: ", fontSize = 13.sp, color = TextSecondary)
                Text(
                    text = bottle.location ?: "Home Bar Main Shelf",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (!bottle.notes.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Tasting & Distiller Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bottle.notes,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Matching Cocktails Card
        if (bottle.cocktails.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Matching Cocktails (${bottle.cocktails.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldHighlight
                )
                Spacer(modifier = Modifier.height(8.dp))

                bottle.cocktails.forEach { cocktail ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassCardBorder.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cocktail.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                cocktail.glass?.let {
                                    Text(text = it, fontSize = 11.sp, color = AmberPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cocktail.ingredients.joinToString(" • "),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
