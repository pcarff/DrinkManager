package com.example.drinkmanager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkmanager.model.ServerConfig
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.GlassCardBorder
import com.example.drinkmanager.theme.GoldHighlight
import com.example.drinkmanager.theme.StockInStock
import com.example.drinkmanager.theme.StockOutOfStock
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.GlassCard

@Composable
fun SettingsScreen(
    config: ServerConfig,
    isLoading: Boolean,
    onSaveUrl: (String) -> Unit,
    onRefreshData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Connection & Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Configure your DrinkManager home server or cloud remote address",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Status Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (config.isOnline) StockInStock.copy(alpha = 0.15f) else StockOutOfStock.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (config.isOnline) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (config.isOnline) StockInStock else StockOutOfStock,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (config.isOnline) "Connected to DrinkManager" else "Offline Cache Mode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = config.syncStatusMessage,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server URL Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "DrinkManager Server Address",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AmberPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter your home LAN IP (e.g. http://192.168.1.100:3005) or Cloud Remote HTTPS URL (e.g. https://bar.yourdomain.com)",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                leadingIcon = { Icon(Icons.Default.Router, contentDescription = null, tint = AmberPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberPrimary,
                    unfocusedBorderColor = GlassCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onSaveUrl(urlInput) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text(text = "Save & Connect", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onRefreshData() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassCardBorder),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Information Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Target Hardware Support", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldHighlight)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "• Pixel 10 Pro Fold: Dual-Pane List-Detail Adaptive Layout", fontSize = 12.sp, color = TextPrimary)
            Text(text = "• Pixel 8 Pro: Single-Column Responsive Smartphone Layout", fontSize = 12.sp, color = TextPrimary)
            Text(text = "• In-Store Offline Mode: Active with local cache", fontSize = 12.sp, color = TextPrimary)
        }
    }
}
