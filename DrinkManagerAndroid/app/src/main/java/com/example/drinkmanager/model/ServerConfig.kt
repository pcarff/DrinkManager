package com.example.drinkmanager.model

data class ServerConfig(
    val baseUrl: String = "http://10.0.2.2:3005", // Default local emulator/LAN IP
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val syncStatusMessage: String = "Ready"
)
