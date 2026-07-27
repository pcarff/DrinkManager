package com.example.drinkmanager.ui.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkmanager.theme.AmberPrimary
import com.example.drinkmanager.theme.DarkBackground
import com.example.drinkmanager.theme.DarkSurface
import com.example.drinkmanager.theme.GlassCardBorder
import com.example.drinkmanager.theme.TextPrimary
import com.example.drinkmanager.theme.TextSecondary
import com.example.drinkmanager.ui.components.TopHeaderBar
import com.example.drinkmanager.ui.screens.BottleDetailPane
import com.example.drinkmanager.ui.screens.CocktailsScreen
import com.example.drinkmanager.ui.screens.InventoryListPane
import com.example.drinkmanager.ui.screens.PantryScreen
import com.example.drinkmanager.model.Cocktail
import com.example.drinkmanager.ui.screens.RecipeDetailDialog
import com.example.drinkmanager.ui.screens.AddBottleScreen
import com.example.drinkmanager.ui.screens.SettingsScreen
import com.example.drinkmanager.viewmodel.MainViewModel

data class NavItem(val title: String, val icon: ImageVector)

@Composable
fun MainNavigation(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStockFilter by viewModel.selectedStockFilter.collectAsState()

    val filteredBottles by viewModel.filteredBottles.collectAsState()
    val rawBottles by viewModel.rawBottles.collectAsState()
    val selectedBottle by viewModel.selectedBottle.collectAsState()

    val allCocktails by viewModel.allCocktails.collectAsState()
    val selectedCocktail by viewModel.selectedCocktail.collectAsState()
    val canMakeOnlyFilter by viewModel.canMakeOnlyFilter.collectAsState()
    val favoritesOnlyFilter by viewModel.favoritesOnlyFilter.collectAsState()

    val activeRecipeDialogCocktail by viewModel.activeRecipeDialogCocktail.collectAsState()
    val showAddBottle by viewModel.showAddBottle.collectAsState()
    val duplicateWarning by viewModel.duplicateWarning.collectAsState()

    val pantryItems by viewModel.pantryItems.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val navItems = listOf(
        NavItem("Inventory", Icons.Default.LocalBar),
        NavItem("Cocktails", Icons.Default.LocalDrink),
        NavItem("Pantry", Icons.Default.Kitchen),
        NavItem("Settings", Icons.Default.Settings)
    )

    var showMobileDetailView by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isWideScreen = maxWidth >= 600.dp // Foldable unfolded or wide tablet layout

        if (isWideScreen) {
            // ----------------------------------------------------
            // PIXEL 10 PRO FOLD (UNFOLDED / EXPANDED 2-PANE VIEW)
            // ----------------------------------------------------
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = DarkSurface,
                    contentColor = TextSecondary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalBar, contentDescription = "Logo", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(28.dp))

                    navItems.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = selectedTab == index,
                            onClick = {
                                viewModel.selectTab(index)
                                showMobileDetailView = false
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = AmberPrimary,
                                indicatorColor = AmberPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }

                VerticalDivider(color = GlassCardBorder, thickness = 1.dp)

                // Wide Main Content
                Column(modifier = Modifier.weight(1f)) {
                    TopHeaderBar(
                        isOnline = serverConfig.isOnline,
                        bottleCount = rawBottles.size,
                        syncStatusMessage = serverConfig.syncStatusMessage
                    )
                    HorizontalDivider(color = GlassCardBorder, thickness = 1.dp)

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> {
                                // Inventory 2-Pane Split
                                Row(modifier = Modifier.fillMaxSize()) {
                                    InventoryListPane(
                                        bottles = filteredBottles,
                                        selectedBottle = selectedBottle,
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        selectedCategory = selectedCategory,
                                        onCategoryChange = { viewModel.selectCategory(it) },
                                        selectedStockFilter = selectedStockFilter,
                                        onStockFilterChange = { viewModel.selectStockFilter(it) },
                                        onSelectBottle = { viewModel.selectBottle(it) },
                                        onUpdateStockLevel = { id, lvl -> viewModel.updateBottleStock(id, lvl) },
                                        onToggleFavorite = { viewModel.toggleBottleFavorite(it) },
                                        isFavoritesOnly = favoritesOnlyFilter,
                                        onToggleFavoritesOnly = { viewModel.toggleFavoritesOnly() },
                                        modifier = Modifier.weight(0.46f)
                                    )

                                    VerticalDivider(color = GlassCardBorder, thickness = 1.dp)

                                    BottleDetailPane(
                                        bottle = selectedBottle,
                                        onUpdateStockLevel = { id, lvl -> viewModel.updateBottleStock(id, lvl) },
                                        onToggleFavorite = { viewModel.toggleBottleFavorite(it) },
                                        onSelectCocktail = { viewModel.openRecipeDialog(it) },
                                        modifier = Modifier.weight(0.54f)
                                    )
                                }

                                // FAB for adding bottles
                                FloatingActionButton(
                                    onClick = { viewModel.showAddBottleDialog() },
                                    containerColor = AmberPrimary,
                                    contentColor = Color.Black,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = (-16).dp, y = (-16).dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan Bottle")
                                }
                            }
                            1 -> {
                                CocktailsScreen(
                                    cocktails = allCocktails,
                                    selectedCocktail = selectedCocktail,
                                    searchQuery = searchQuery,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    canMakeOnly = canMakeOnlyFilter,
                                    onToggleCanMakeOnly = { viewModel.toggleCanMakeOnly() },
                                    onSelectCocktail = { viewModel.openRecipeDialog(it) },
                                    onToggleFavorite = { viewModel.toggleCocktailFavorite(it) },
                                    isFavoritesOnly = favoritesOnlyFilter,
                                    onToggleFavoritesOnly = { viewModel.toggleFavoritesOnly() }
                                )
                            }
                            2 -> {
                                PantryScreen(
                                    items = pantryItems,
                                    onToggleStock = { viewModel.togglePantryStock(it) }
                                )
                            }
                            3 -> {
                                SettingsScreen(
                                    config = serverConfig,
                                    isLoading = isLoading,
                                    onSaveUrl = { viewModel.saveServerUrl(it) },
                                    onRefreshData = { viewModel.refreshServerData() }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ----------------------------------------------------
            // PIXEL 8 PRO / FOLDED OUTER DISPLAY
            // ----------------------------------------------------
            Scaffold(
                topBar = {
                    TopHeaderBar(
                        isOnline = serverConfig.isOnline,
                        bottleCount = rawBottles.size,
                        syncStatusMessage = serverConfig.syncStatusMessage
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextSecondary
                    ) {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    viewModel.selectTab(index)
                                    showMobileDetailView = false
                                },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = AmberPrimary,
                                    indicatorColor = AmberPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                },
                containerColor = DarkBackground
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (selectedTab == 0 && showMobileDetailView && selectedBottle != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showMobileDetailView = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AmberPrimary)
                                }
                                Text(
                                    text = selectedBottle?.name ?: "Bottle Details",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            HorizontalDivider(color = GlassCardBorder)
                            BottleDetailPane(
                                bottle = selectedBottle,
                                onUpdateStockLevel = { id, lvl -> viewModel.updateBottleStock(id, lvl) },
                                onToggleFavorite = { viewModel.toggleBottleFavorite(it) },
                                onSelectCocktail = { viewModel.openRecipeDialog(it) }
                            )
                        }
                    } else {
                        when (selectedTab) {
                            0 -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    InventoryListPane(
                                        bottles = filteredBottles,
                                        selectedBottle = selectedBottle,
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        selectedCategory = selectedCategory,
                                        onCategoryChange = { viewModel.selectCategory(it) },
                                        selectedStockFilter = selectedStockFilter,
                                        onStockFilterChange = { viewModel.selectStockFilter(it) },
                                        onSelectBottle = { bottle ->
                                            viewModel.selectBottle(bottle)
                                            showMobileDetailView = true
                                        },
                                        onUpdateStockLevel = { id, lvl -> viewModel.updateBottleStock(id, lvl) },
                                        onToggleFavorite = { viewModel.toggleBottleFavorite(it) },
                                        isFavoritesOnly = favoritesOnlyFilter,
                                        onToggleFavoritesOnly = { viewModel.toggleFavoritesOnly() }
                                    )

                                    // FAB for adding bottles (mobile)
                                    FloatingActionButton(
                                        onClick = { viewModel.showAddBottleDialog() },
                                        containerColor = AmberPrimary,
                                        contentColor = Color.Black,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(16.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Bottle")
                                    }
                                }
                            }
                            1 -> {
                                CocktailsScreen(
                                    cocktails = allCocktails,
                                    selectedCocktail = selectedCocktail,
                                    searchQuery = searchQuery,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    canMakeOnly = canMakeOnlyFilter,
                                    onToggleCanMakeOnly = { viewModel.toggleCanMakeOnly() },
                                    onSelectCocktail = { viewModel.openRecipeDialog(it) },
                                    onToggleFavorite = { viewModel.toggleCocktailFavorite(it) },
                                    isFavoritesOnly = favoritesOnlyFilter,
                                    onToggleFavoritesOnly = { viewModel.toggleFavoritesOnly() }
                                )
                            }
                            2 -> {
                                PantryScreen(
                                    items = pantryItems,
                                    onToggleStock = { viewModel.togglePantryStock(it) }
                                )
                            }
                            3 -> {
                                SettingsScreen(
                                    config = serverConfig,
                                    isLoading = isLoading,
                                    onSaveUrl = { viewModel.saveServerUrl(it) },
                                    onRefreshData = { viewModel.refreshServerData() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FULL RECIPE VIEW & EDIT POPUP DIALOG
        if (activeRecipeDialogCocktail != null) {
            RecipeDetailDialog(
                cocktail = activeRecipeDialogCocktail,
                onDismiss = { viewModel.closeRecipeDialog() },
                onToggleFavorite = { viewModel.toggleCocktailFavorite(it) },
                onSaveRecipe = { viewModel.saveRecipe(it) }
            )
        }

        // ADD BOTTLE VIA CAMERA SCAN DIALOG
        if (showAddBottle) {
            AddBottleScreen(
                onDismiss = { viewModel.hideAddBottleDialog() },
                onAnalyzeImage = { base64, callback ->
                    viewModel.analyzeBottleImage(base64, callback)
                },
                onSaveBottle = { formData ->
                    viewModel.saveNewBottle(formData)
                }
            )
        }

        // DUPLICATE BOTTLE WARNING DIALOG
        if (duplicateWarning != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearDuplicateWarning() },
                title = {
                    Text("Duplicate Detected", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                },
                text = {
                    Text(
                        duplicateWarning ?: "",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.forceAddBottle() }
                    ) {
                        Text("Add Anyway", color = AmberPrimary)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.clearDuplicateWarning() }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}
