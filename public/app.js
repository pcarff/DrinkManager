// DrinkManager Client Application Logic - SQLite & Inventory Management

document.addEventListener('DOMContentLoaded', () => {
  let inventoryData = [];
  let pantryData = [];
  let shoppingListData = [];

  let currentViewMode = 'inventory'; // 'inventory' | 'recipes' | 'pantry' | 'shopping' | 'favorites'
  let currentFilter = 'all';
  let currentStockFilter = 'all'; // 'all' | 'in-stock' | 'low' | 'empty'
  let currentRecipeFilter = 'all'; // 'all' | 'can-make' | 'cocktails' | 'mocktails' | 'fav-recipes'
  let currentPantryFilter = 'all';
  let searchQuery = '';
  let currentSort = 'id';
  let currentActiveBottleId = null;

  // Favorites Sets (Persisted in localStorage)
  let favoriteBottles = new Set(JSON.parse(localStorage.getItem('drinkManager_favBottles') || '[]'));
  let favoriteRecipes = new Set(JSON.parse(localStorage.getItem('drinkManager_favRecipes') || '[]'));

  // DOM Elements
  const inventoryGrid = document.getElementById('inventoryGrid');
  const recipesGrid = document.getElementById('recipesGrid');
  const pantrySection = document.getElementById('pantrySection');
  const pantryGrid = document.getElementById('pantryGrid');
  const shoppingSection = document.getElementById('shoppingSection');
  const shoppingList = document.getElementById('shoppingList');

  const searchInput = document.getElementById('searchInput');
  const clearSearchBtn = document.getElementById('clearSearchBtn');

  const stockFilterBar = document.getElementById('stockFilterBar');
  const inventoryFilterChips = document.getElementById('inventoryFilterChips');
  const recipeFilterChips = document.getElementById('recipeFilterChips');
  const pantryFilterChips = document.getElementById('pantryFilterChips');

  const sortSelect = document.getElementById('sortSelect');
  const sortSelectorContainer = document.getElementById('sortSelectorContainer');
  const resultsCountText = document.getElementById('resultsCountText');
  const emptyState = document.getElementById('emptyState');
  const emptyStateTitle = document.getElementById('emptyStateTitle');
  const emptyStateText = document.getElementById('emptyStateText');
  const resetFiltersBtn = document.getElementById('resetFiltersBtn');

  // View Mode Tabs
  const viewTabs = document.querySelectorAll('.view-tab');

  // Add Bottle Modal Elements
  const addBottleBtn = document.getElementById('addBottleBtn');
  const bottleFormModal = document.getElementById('bottleFormModal');
  const closeBottleFormBtn = document.getElementById('closeBottleFormBtn');
  const cancelBottleBtn = document.getElementById('cancelBottleBtn');
  const bottleForm = document.getElementById('bottleForm');
  const bottleFormTitle = document.getElementById('bottleFormTitle');
  const formEditBottleId = document.getElementById('formEditBottleId');
  const bottleNameInput = document.getElementById('bottleNameInput');
  const bottleBrandInput = document.getElementById('bottleBrandInput');
  const bottleCategoryInput = document.getElementById('bottleCategoryInput');
  const bottleSubCategoryInput = document.getElementById('bottleSubCategoryInput');
  const bottleVolumeInput = document.getElementById('bottleVolumeInput');
  const bottleProofInput = document.getElementById('bottleProofInput');
  const bottleAbvInput = document.getElementById('bottleAbvInput');
  const bottleStockLevelInput = document.getElementById('bottleStockLevelInput');
  const bottlePhotoInput = document.getElementById('bottlePhotoInput');
  const photoPreviewName = document.getElementById('photoPreviewName');
  const bottleNotesInput = document.getElementById('bottleNotesInput');

  // Pantry Modal Elements
  const addPantryItemBtn = document.getElementById('addPantryItemBtn');
  const pantryFormModal = document.getElementById('pantryFormModal');
  const closePantryFormBtn = document.getElementById('closePantryFormBtn');
  const cancelPantryBtn = document.getElementById('cancelPantryBtn');
  const pantryForm = document.getElementById('pantryForm');
  const pantryFormTitle = document.getElementById('pantryFormTitle');
  const formEditPantryId = document.getElementById('formEditPantryId');
  const pantryNameInput = document.getElementById('pantryNameInput');
  const pantryCategoryInput = document.getElementById('pantryCategoryInput');
  const pantryStockInput = document.getElementById('pantryStockInput');
  const pantryNotesInput = document.getElementById('pantryNotesInput');

  // Shopping List Elements
  const addShoppingItemForm = document.getElementById('addShoppingItemForm');
  const shoppingItemInput = document.getElementById('shoppingItemInput');
  const autoRestockBtn = document.getElementById('autoRestockBtn');

  // Detail Modal Elements
  const detailModal = document.getElementById('detailModal');
  const modalContent = document.getElementById('modalContent');

  // Recipe Form Modal Elements
  const recipeFormModal = document.getElementById('recipeFormModal');
  const recipeForm = document.getElementById('recipeForm');
  const recipeFormTitle = document.getElementById('recipeFormTitle');
  const recipeFormSubtitle = document.getElementById('recipeFormSubtitle');
  const formBottleId = document.getElementById('formBottleId');
  const formRecipeType = document.getElementById('formRecipeType');
  const formRecipeIndex = document.getElementById('formRecipeIndex');
  const recipeNameInput = document.getElementById('recipeNameInput');
  const recipeGlassInput = document.getElementById('recipeGlassInput');
  const ingredientsContainer = document.getElementById('ingredientsContainer');
  const addIngredientRowBtn = document.getElementById('addIngredientRowBtn');
  const recipeInstructionsInput = document.getElementById('recipeInstructionsInput');

  // Initial Data Load
  fetchAllData();

  async function fetchAllData() {
    await Promise.all([fetchInventory(), fetchPantry(), fetchShoppingList()]);
    mergeServerFavorites();
    renderApp();
  }

  async function fetchInventory() {
    try {
      const response = await fetch('/api/bottles?t=' + Date.now());
      if (!response.ok) throw new Error('Failed to load inventory');
      inventoryData = await response.json();
    } catch (err) {
      console.error('Error fetching inventory:', err);
    }
  }

  async function fetchPantry() {
    try {
      const response = await fetch('/api/pantry?t=' + Date.now());
      if (!response.ok) throw new Error('Failed to load pantry');
      pantryData = await response.json();
    } catch (err) {
      console.error('Error fetching pantry:', err);
    }
  }

  async function fetchShoppingList() {
    try {
      const response = await fetch('/api/shopping-list?t=' + Date.now());
      if (!response.ok) throw new Error('Failed to load shopping list');
      shoppingListData = await response.json();
    } catch (err) {
      console.error('Error fetching shopping list:', err);
    }
  }

  // Update Category Badge Counts & Stat Totals
  function updateCategoryCounts() {
    // Inventory Counts
    document.getElementById('countAll').textContent = inventoryData.length;
    document.getElementById('countWhiskey').textContent = countCategory(['whiskey', 'bourbon', 'scotch', 'moonshine']);
    document.getElementById('countRum').textContent = countCategory(['rum']);
    document.getElementById('countLiqueur').textContent = countCategory(['liqueur', 'schnapps']);
    document.getElementById('countTequila').textContent = countCategory(['tequila']);
    document.getElementById('countAmaro').textContent = countCategory(['amaro', 'aperitivo']);
    document.getElementById('countWine').textContent = countCategory(['wine', 'vermouth', 'port']);
    document.getElementById('countGin').textContent = countCategory(['gin']);
    document.getElementById('countOther').textContent = countCategory(['brandy', 'syrup', 'decanter']);

    // Pantry Count
    document.getElementById('countAllPantry').textContent = pantryData.length;

    // Recipes Counts
    let allRecipesList = getAllRecipesList();
    let alcoholicCount = allRecipesList.filter(r => r.type === 'cocktail').length;
    let mocktailCount = allRecipesList.filter(r => r.type === 'mocktail').length;
    let favRecipesCount = allRecipesList.filter(r => favoriteRecipes.has(r.uniqueKey)).length;
    let canMakeCount = allRecipesList.filter(r => r.canMake).length;

    document.getElementById('countAllRecipes').textContent = allRecipesList.length;
    document.getElementById('countAlcoholicRecipes').textContent = alcoholicCount;
    document.getElementById('countMocktailRecipes').textContent = mocktailCount;
    document.getElementById('countFavRecipes').textContent = favRecipesCount;
    document.getElementById('countCanMakeRecipes').textContent = canMakeCount;

    // Header Stats
    const inStockBottles = inventoryData.filter(b => b.stockStatus !== 'empty').length;
    document.getElementById('totalBottlesCount').textContent = inventoryData.length;
    document.getElementById('inStockCount').textContent = inStockBottles;
    document.getElementById('totalCocktailsCount').textContent = alcoholicCount;
    document.getElementById('totalPantryCount').textContent = pantryData.filter(p => p.stock_status === 'in-stock').length;
    document.getElementById('totalFavoritesCount').textContent = favoriteBottles.size + favoriteRecipes.size;
  }

  function countCategory(keywords) {
    return inventoryData.filter(item => {
      const cat = (item.category + ' ' + (item.subCategory || '')).toLowerCase();
      return keywords.some(k => cat.includes(k));
    }).length;
  }

  // Helper to extract flat list of all recipes with "Can I Make This?" status
  function getAllRecipesList() {
    const list = [];
    inventoryData.forEach(bottle => {
      const isBottleInStock = bottle.stockStatus !== 'empty';

      if (bottle.cocktails) {
        bottle.cocktails.forEach((c, idx) => {
          const missing = checkRecipeMissingIngredients(bottle, c.ingredients);
          list.push({
            id: c.id,
            uniqueKey: `b${bottle.id}_c${idx}`,
            bottleId: bottle.id,
            bottleName: bottle.name,
            bottleBrand: bottle.brand,
            bottleStockStatus: bottle.stockStatus,
            type: 'cocktail',
            index: idx,
            name: c.name,
            glass: c.glass,
            ingredients: c.ingredients,
            instructions: c.instructions,
            canMake: isBottleInStock && missing.length === 0,
            missingIngredients: missing
          });
        });
      }
      if (bottle.mocktail) {
        const missing = checkRecipeMissingIngredients(bottle, bottle.mocktail.ingredients);
        list.push({
          id: bottle.mocktail.id,
          uniqueKey: `b${bottle.id}_m0`,
          bottleId: bottle.id,
          bottleName: bottle.name,
          bottleBrand: bottle.brand,
          bottleStockStatus: bottle.stockStatus,
          type: 'mocktail',
          index: 0,
          name: bottle.mocktail.name,
          glass: bottle.mocktail.glass,
          ingredients: bottle.mocktail.ingredients,
          instructions: bottle.mocktail.instructions,
          canMake: isBottleInStock && missing.length === 0,
          missingIngredients: missing
        });
      }
    });
    return list;
  }

  // Check if ingredients are missing against Pantry & Bottles
  function checkRecipeMissingIngredients(baseBottle, ingredients) {
    if (!ingredients) return [];
    const missing = [];

    if (baseBottle.stockStatus === 'empty') {
      missing.push(`Base: ${baseBottle.name} (Out of stock)`);
    }

    ingredients.forEach(ing => {
      const ingLower = ing.toLowerCase();
      // Skip base spirit string match if it matches base bottle
      if (baseBottle && ingLower.includes(baseBottle.brand.toLowerCase())) {
        return;
      }

      // Check if matches an out-of-stock pantry supply
      const pantryMatch = pantryData.find(p => ingLower.includes(p.name.toLowerCase()));
      if (pantryMatch && pantryMatch.stock_status === 'out') {
        missing.push(ing);
        return;
      }

      // Check if matches an empty bottle ingredient (e.g., sweet vermouth, Campari)
      const bottleMatch = inventoryData.find(b => b.id !== baseBottle.id && (ingLower.includes(b.name.toLowerCase()) || ingLower.includes(b.brand.toLowerCase())));
      if (bottleMatch && bottleMatch.stockStatus === 'empty') {
        missing.push(ing);
        return;
      }
    });

    return missing;
  }

  // Master Render Function
  function renderApp() {
    updateCategoryCounts();

    // Hide all main view sections first
    inventoryGrid.classList.add('hidden');
    recipesGrid.classList.add('hidden');
    pantrySection.classList.add('hidden');
    shoppingSection.classList.add('hidden');

    stockFilterBar.classList.add('hidden');
    inventoryFilterChips.classList.add('hidden');
    recipeFilterChips.classList.add('hidden');
    pantryFilterChips.classList.add('hidden');
    sortSelectorContainer.classList.remove('hidden');

    if (currentViewMode === 'inventory') {
      inventoryGrid.classList.remove('hidden');
      stockFilterBar.classList.remove('hidden');
      inventoryFilterChips.classList.remove('hidden');
      renderInventoryGrid();
    } else if (currentViewMode === 'recipes') {
      recipesGrid.classList.remove('hidden');
      recipeFilterChips.classList.remove('hidden');
      renderRecipesGrid();
    } else if (currentViewMode === 'pantry') {
      pantrySection.classList.remove('hidden');
      pantryFilterChips.classList.remove('hidden');
      sortSelectorContainer.classList.add('hidden');
      renderPantryGrid();
    } else if (currentViewMode === 'shopping') {
      shoppingSection.classList.remove('hidden');
      sortSelectorContainer.classList.add('hidden');
      renderShoppingList();
    } else if (currentViewMode === 'favorites') {
      inventoryGrid.classList.remove('hidden');
      recipesGrid.classList.remove('hidden');
      renderFavoritesView();
    }
  }

  // Render Inventory Bottles Grid
  function renderInventoryGrid() {
    const filtered = getFilteredBottles();
    resultsCountText.textContent = `Showing ${filtered.length} of ${inventoryData.length} bottles`;

    if (filtered.length === 0) {
      inventoryGrid.innerHTML = '';
      emptyStateTitle.textContent = 'No bottles match your search';
      emptyStateText.textContent = 'Try clearing your search query or choosing a different stock/category filter.';
      emptyState.classList.remove('hidden');
      return;
    }

    emptyState.classList.add('hidden');
    inventoryGrid.innerHTML = filtered.map(item => createBottleCardHTML(item)).join('');
  }

  function getFilteredBottles() {
    return inventoryData.filter(item => {
      let matchesStock = true;
      if (currentStockFilter === 'in-stock') {
        matchesStock = item.stockStatus === 'in-stock';
      } else if (currentStockFilter === 'low') {
        matchesStock = item.stockStatus === 'low';
      } else if (currentStockFilter === 'empty') {
        matchesStock = item.stockStatus === 'empty';
      }

      let matchesCategory = true;
      const cat = (item.category + ' ' + (item.subCategory || '')).toLowerCase();

      if (currentFilter === 'whiskey') {
        matchesCategory = ['whiskey', 'bourbon', 'scotch', 'moonshine'].some(k => cat.includes(k));
      } else if (currentFilter === 'rum') {
        matchesCategory = cat.includes('rum');
      } else if (currentFilter === 'liqueur') {
        matchesCategory = cat.includes('liqueur') || cat.includes('schnapps');
      } else if (currentFilter === 'tequila') {
        matchesCategory = cat.includes('tequila');
      } else if (currentFilter === 'amaro') {
        matchesCategory = cat.includes('amaro') || cat.includes('aperitivo');
      } else if (currentFilter === 'wine') {
        matchesCategory = ['wine', 'vermouth', 'port'].some(k => cat.includes(k));
      } else if (currentFilter === 'gin') {
        matchesCategory = cat.includes('gin');
      } else if (currentFilter === 'other') {
        matchesCategory = ['brandy', 'syrup', 'decanter'].some(k => cat.includes(k));
      }

      let matchesSearch = true;
      if (searchQuery.trim() !== '') {
        const q = searchQuery.toLowerCase();
        const itemText = (
          item.name + ' ' +
          item.brand + ' ' +
          item.category + ' ' +
          (item.subCategory || '') + ' ' +
          (item.notes || '') + ' ' +
          (item.cocktails ? item.cocktails.map(c => c.name + ' ' + c.ingredients.join(' ')).join(' ') : '') + ' ' +
          (item.mocktail ? item.mocktail.name + ' ' + item.mocktail.ingredients.join(' ') : '')
        ).toLowerCase();

        matchesSearch = itemText.includes(q);
      }

      return matchesStock && matchesCategory && matchesSearch;
    }).sort((a, b) => sortItems(a, b));
  }

  function sortItems(a, b) {
    if (currentSort === 'name') {
      return a.name.localeCompare(b.name);
    } else if (currentSort === 'brand') {
      return a.brand.localeCompare(b.brand);
    } else if (currentSort === 'stockLevel') {
      const order = { 'full': 5, 'three-quarter': 4, 'half': 3, 'quarter': 2, 'almost-empty': 1, 'empty': 0 };
      return (order[b.stockLevel] || 0) - (order[a.stockLevel] || 0);
    } else if (currentSort === 'proofDesc') {
      return (b.proof || 0) - (a.proof || 0);
    } else if (currentSort === 'proofAsc') {
      return (a.proof || 0) - (b.proof || 0);
    }
    return a.id - b.id;
  }

  function createBottleCardHTML(item) {
    const proofText = item.proof ? `${item.proof} Proof` : (item.abvPercent ? `${item.abvPercent}% ABV` : 'N/A');
    const photoUrl = `/photos/${item.photoFilename}`;
    const cocktailCount = item.cocktails ? item.cocktails.length : 0;
    const hasMocktail = item.mocktail ? 1 : 0;
    const isFav = favoriteBottles.has(item.id);

    const stockMap = {
      'full': { label: '🍾 Full', class: 'level-full' },
      'three-quarter': { label: '🍸 ¾ Bottle', class: 'level-three-quarter' },
      'half': { label: '🥃 ½ Bottle', class: 'level-half' },
      'quarter': { label: '🍹 ¼ Bottle', class: 'level-quarter' },
      'almost-empty': { label: '⚠️ Low', class: 'level-almost-empty' },
      'empty': { label: '❌ Empty', class: 'level-empty' }
    };
    const stockInfo = stockMap[item.stockLevel] || { label: '🍾 In Stock', class: 'level-full' };
    const isEmpty = item.stockLevel === 'empty';

    return `
      <div class="bottle-card ${isEmpty ? 'empty-bottle' : ''}" data-id="${item.id}">
        <button class="fav-bottle-toggle ${isFav ? 'active' : ''}" data-bottle-id="${item.id}" title="${isFav ? 'Remove from favorites' : 'Add bottle to favorites'}">
          🍾
        </button>
        <div class="card-image-wrap">
          <img src="${photoUrl}" alt="${escapeHTML(item.name)}" loading="lazy">
          <span class="category-tag">${escapeHTML(item.category)}</span>
          <span class="stock-tag ${stockInfo.class}">${stockInfo.label}</span>
        </div>
        <div class="card-content">
          <span class="bottle-brand">${escapeHTML(item.brand)}</span>
          <h3 class="bottle-name">${escapeHTML(item.name)}</h3>
          <div class="card-footer-info">
            <span>${escapeHTML(item.volume || '')} • ${proofText}</span>
            <span class="recipe-count-badge">🍸 ${cocktailCount} Cocktails ${hasMocktail ? '+ Mocktail' : ''}</span>
          </div>
        </div>
      </div>
    `;
  }

  // Render Recipes Grid View
  function renderRecipesGrid() {
    const allRecipes = getAllRecipesList();

    const filtered = allRecipes.filter(r => {
      let matchesFilter = true;
      if (currentRecipeFilter === 'can-make') {
        matchesFilter = r.canMake;
      } else if (currentRecipeFilter === 'cocktails') {
        matchesFilter = r.type === 'cocktail';
      } else if (currentRecipeFilter === 'mocktails') {
        matchesFilter = r.type === 'mocktail';
      } else if (currentRecipeFilter === 'fav-recipes') {
        matchesFilter = favoriteRecipes.has(r.uniqueKey);
      }

      let matchesSearch = true;
      if (searchQuery.trim() !== '') {
        const q = searchQuery.toLowerCase();
        const recipeText = (
          r.name + ' ' +
          r.bottleName + ' ' +
          r.bottleBrand + ' ' +
          r.glass + ' ' +
          r.ingredients.join(' ') + ' ' +
          r.instructions
        ).toLowerCase();
        matchesSearch = recipeText.includes(q);
      }

      return matchesFilter && matchesSearch;
    }).sort((a, b) => {
      if (currentSort === 'name') return a.name.localeCompare(b.name);
      if (currentSort === 'brand') return a.bottleBrand.localeCompare(b.bottleBrand);
      return a.bottleId - b.bottleId;
    });

    resultsCountText.textContent = `Showing ${filtered.length} of ${allRecipes.length} recipes`;

    if (filtered.length === 0) {
      recipesGrid.innerHTML = '';
      emptyStateTitle.textContent = 'No recipes match your search';
      emptyStateText.textContent = 'Try clearing your search query or switching recipe filters.';
      emptyState.classList.remove('hidden');
      return;
    }

    emptyState.classList.add('hidden');
    recipesGrid.innerHTML = filtered.map(r => createStandaloneRecipeCardHTML(r)).join('');
  }

  function createStandaloneRecipeCardHTML(r) {
    const isFav = favoriteRecipes.has(r.uniqueKey);
    const isMocktail = r.type === 'mocktail';

    const canMakeBadge = r.canMake ? `
      <span class="can-make-badge ready">✅ Can Make Now</span>
    ` : `
      <span class="can-make-badge missing" title="Missing: ${r.missingIngredients ? r.missingIngredients.join(', ') : 'Ingredients'}">⚠️ Missing Ingredients</span>
    `;

    return `
      <div class="recipe-card-standalone ${isMocktail ? 'mocktail-card' : ''}" data-bottle-id="${r.bottleId}" data-unique-key="${r.uniqueKey}">
        <div class="recipe-header">
          <div>
            <div style="display:flex; gap:0.4rem; align-items:center;">
              <span class="recipe-badge" style="${isMocktail ? 'background:rgba(42,157,143,0.2); color:var(--accent-teal); border-color:var(--accent-teal);' : ''}">
                ${isMocktail ? '🍹 Zero-Proof Mocktail' : '🍸 Cocktail'}
              </span>
              ${canMakeBadge}
            </div>
            <h4 class="recipe-name" style="margin-top:0.4rem;">${escapeHTML(r.name)}</h4>
          </div>
          <button class="action-icon-btn fav-recipe-btn ${isFav ? 'active' : ''}" data-unique-key="${r.uniqueKey}" title="${isFav ? 'Unfavorite recipe' : 'Favorite recipe 🍾'}">
            🍾
          </button>
        </div>
        <div class="recipe-base-bottle">🍾 <strong>Base:</strong> ${escapeHTML(r.bottleName)} (${r.bottleStockStatus === 'empty' ? '❌ Out of stock' : '✅ In stock'})</div>
        <div class="recipe-glass">🥂 <strong>Glass:</strong> ${escapeHTML(r.glass)}</div>
        <ul class="ingredients-list">
          ${r.ingredients.map(ing => `<li>${escapeHTML(ing)}</li>`).join('')}
        </ul>
        <p class="instructions-text"><strong>Preparation:</strong> ${escapeHTML(r.instructions)}</p>
      </div>
    `;
  }

  // Render Bar Supplies Pantry Grid
  function renderPantryGrid() {
    const filtered = pantryData.filter(item => {
      let matchesCat = true;
      if (currentPantryFilter !== 'all') {
        matchesCat = item.category === currentPantryFilter;
      }

      let matchesSearch = true;
      if (searchQuery.trim() !== '') {
        const q = searchQuery.toLowerCase();
        matchesSearch = (item.name + ' ' + item.category + ' ' + (item.notes || '')).toLowerCase().includes(q);
      }

      return matchesCat && matchesSearch;
    });

    resultsCountText.textContent = `Showing ${filtered.length} of ${pantryData.length} bar pantry supplies`;

    if (filtered.length === 0) {
      pantryGrid.innerHTML = '';
      emptyStateTitle.textContent = 'No pantry items match your search';
      emptyStateText.textContent = 'Try adding a new supply item or switching categories.';
      emptyState.classList.remove('hidden');
      return;
    }

    emptyState.classList.add('hidden');
    pantryGrid.innerHTML = filtered.map(item => `
      <div class="pantry-card" data-id="${item.id}">
        <div class="pantry-info">
          <span class="pantry-category-tag">${escapeHTML(item.category)}</span>
          <h4 class="pantry-name">${escapeHTML(item.name)}</h4>
          ${item.notes ? `<p class="pantry-notes">${escapeHTML(item.notes)}</p>` : ''}
        </div>
        <div class="pantry-actions">
          <button class="pantry-stock-toggle ${item.stock_status}" data-id="${item.id}" data-status="${item.stock_status}">
            ${item.stock_status === 'in-stock' ? '✅ In Stock' : (item.stock_status === 'low' ? '⚠️ Running Low' : '❌ Out of Stock')}
          </button>
          <button class="action-icon-btn edit-pantry-btn" data-id="${item.id}" title="Edit Supply">✏️</button>
          <button class="action-icon-btn delete-btn delete-pantry-btn" data-id="${item.id}" title="Delete Supply">🗑️</button>
        </div>
      </div>
    `).join('');
  }

  // Render Shopping List View
  function renderShoppingList() {
    resultsCountText.textContent = `Showing ${shoppingListData.length} items on your bar shopping list`;

    if (shoppingListData.length === 0) {
      shoppingList.innerHTML = `
        <li style="text-align:center; padding:2rem; color:var(--text-muted);">
          🛒 Your shopping list is empty! Click "Auto-Add Empty Items" above to populate depleted bottles and pantry items.
        </li>
      `;
      emptyState.classList.add('hidden');
      return;
    }

    emptyState.classList.add('hidden');
    shoppingList.innerHTML = shoppingListData.map(item => `
      <li class="shopping-item ${item.is_purchased ? 'purchased' : ''}" data-id="${item.id}">
        <div class="shopping-item-left">
          <input type="checkbox" class="shopping-checkbox" data-id="${item.id}" ${item.is_purchased ? 'checked' : ''}>
          <span class="shopping-item-name">${escapeHTML(item.item_name)}</span>
        </div>
        <button class="action-icon-btn delete-btn delete-shopping-btn" data-id="${item.id}" title="Delete Item">🗑️</button>
      </li>
    `).join('');
  }

  // Render Favorites Vault View
  function renderFavoritesView() {
    const favBottles = inventoryData.filter(b => favoriteBottles.has(b.id));
    const allRecipes = getAllRecipesList();
    const favRecipes = allRecipes.filter(r => favoriteRecipes.has(r.uniqueKey));

    resultsCountText.textContent = `Showing ${favBottles.length + favRecipes.length} favorited items (${favBottles.length} bottles, ${favRecipes.length} recipes)`;

    if (favBottles.length === 0 && favRecipes.length === 0) {
      inventoryGrid.innerHTML = '';
      recipesGrid.innerHTML = '';
      emptyStateTitle.textContent = 'No Favorites Yet 🍾';
      emptyStateText.textContent = 'Click the bottle 🍾 icon on any alcohol bottle or cocktail recipe to save it to your Favorites Vault!';
      emptyState.classList.remove('hidden');
      return;
    }

    emptyState.classList.add('hidden');
    inventoryGrid.innerHTML = favBottles.map(item => createBottleCardHTML(item)).join('');
    recipesGrid.innerHTML = favRecipes.map(r => createStandaloneRecipeCardHTML(r)).join('');
  }

  // Favorites Persistence — localStorage + Server Sync
  function toggleFavoriteBottle(bottleId) {
    const nowFavorite = !favoriteBottles.has(bottleId);
    if (nowFavorite) {
      favoriteBottles.add(bottleId);
      showToast(`🍾 Added bottle to Favorites Vault!`);
    } else {
      favoriteBottles.delete(bottleId);
      showToast(`Removed bottle from Favorites Vault`);
    }
    localStorage.setItem('drinkManager_favBottles', JSON.stringify(Array.from(favoriteBottles)));

    // Sync to server
    fetch(`/api/bottles/${bottleId}/favorite`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ isFavorite: nowFavorite ? 1 : 0 })
    }).catch(() => {});

    renderApp();
  }

  function toggleFavoriteRecipe(uniqueKey) {
    const nowFavorite = !favoriteRecipes.has(uniqueKey);
    if (nowFavorite) {
      favoriteRecipes.add(uniqueKey);
      showToast(`🍾 Added recipe to Favorites Vault!`);
    } else {
      favoriteRecipes.delete(uniqueKey);
      showToast(`Removed recipe from Favorites Vault`);
    }
    localStorage.setItem('drinkManager_favRecipes', JSON.stringify(Array.from(favoriteRecipes)));

    // Resolve uniqueKey to cocktail name and sync to server
    const recipe = getAllRecipesList().find(r => r.uniqueKey === uniqueKey);
    if (recipe) {
      fetch(`/api/cocktails/favorite`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: recipe.name, isFavorite: nowFavorite })
      }).catch(() => {});
    }

    renderApp();
  }

  // Bidirectional merge: server favorites → localStorage, and localStorage favorites → server
  function mergeServerFavorites() {
    // 1. Pull server favorites into localStorage
    inventoryData.forEach(bottle => {
      if (bottle.isFavorite === 1 || bottle.is_favorite === 1) {
        favoriteBottles.add(bottle.id);
      }
      if (bottle.cocktails) {
        bottle.cocktails.forEach((c, idx) => {
          const key = `b${bottle.id}_c${idx}`;
          if (c.isFavorite === true || c.is_favorite === 1 || c.isFavorite === 1) {
            favoriteRecipes.add(key);
          }
        });
      }
    });

    // 2. Push localStorage favorites to server (if server doesn't already have them)
    inventoryData.forEach(bottle => {
      const serverBottleFav = (bottle.isFavorite === 1 || bottle.is_favorite === 1);
      const localBottleFav = favoriteBottles.has(bottle.id);
      if (localBottleFav && !serverBottleFav) {
        fetch(`/api/bottles/${bottle.id}/favorite`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ isFavorite: 1 })
        }).catch(() => {});
      }

      if (bottle.cocktails) {
        bottle.cocktails.forEach((c, idx) => {
          const key = `b${bottle.id}_c${idx}`;
          const serverCocktailFav = (c.isFavorite === true || c.is_favorite === 1 || c.isFavorite === 1);
          const localCocktailFav = favoriteRecipes.has(key);
          if (localCocktailFav && !serverCocktailFav) {
            fetch(`/api/cocktails/favorite`, {
              method: 'PUT',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ name: c.name, isFavorite: true })
            }).catch(() => {});
          }
        });
      }
    });

    localStorage.setItem('drinkManager_favBottles', JSON.stringify(Array.from(favoriteBottles)));
    localStorage.setItem('drinkManager_favRecipes', JSON.stringify(Array.from(favoriteRecipes)));
  }

  // ====================================================
  // EVENT DELEGATION
  // ====================================================

  // Inventory Grid Listener
  inventoryGrid.addEventListener('click', (e) => {
    const favBtn = e.target.closest('.fav-bottle-toggle');
    if (favBtn) {
      e.stopPropagation();
      const bottleId = parseInt(favBtn.dataset.bottleId, 10);
      toggleFavoriteBottle(bottleId);
      return;
    }

    const card = e.target.closest('.bottle-card');
    if (card) {
      const itemId = parseInt(card.dataset.id, 10);
      openDetailModal(itemId);
    }
  });

  // Recipes Grid Listener
  recipesGrid.addEventListener('click', (e) => {
    const favBtn = e.target.closest('.fav-recipe-btn');
    if (favBtn) {
      e.stopPropagation();
      const uniqueKey = favBtn.dataset.uniqueKey;
      toggleFavoriteRecipe(uniqueKey);
      return;
    }

    const card = e.target.closest('.recipe-card-standalone');
    if (card) {
      const bottleId = parseInt(card.dataset.bottleId, 10);
      openDetailModal(bottleId);
    }
  });

  // Pantry Grid Listener
  pantryGrid.addEventListener('click', async (e) => {
    const toggleBtn = e.target.closest('.pantry-stock-toggle');
    if (toggleBtn) {
      const id = parseInt(toggleBtn.dataset.id, 10);
      const currentStatus = toggleBtn.dataset.status;
      const nextStatus = currentStatus === 'in-stock' ? 'low' : (currentStatus === 'low' ? 'out' : 'in-stock');

      try {
        const res = await fetch(`/api/pantry/${id}/stock`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ stockStatus: nextStatus })
        });
        if (res.ok) {
          showToast(`Updated pantry item stock`);
          await fetchPantry();
          renderApp();
        }
      } catch (err) {
        showToast('Error updating pantry stock');
      }
      return;
    }

    const editBtn = e.target.closest('.edit-pantry-btn');
    if (editBtn) {
      const id = parseInt(editBtn.dataset.id, 10);
      openPantryForm(id);
      return;
    }

    const deleteBtn = e.target.closest('.delete-pantry-btn');
    if (deleteBtn) {
      const id = parseInt(deleteBtn.dataset.id, 10);
      if (confirm('Delete this bar supply item?')) {
        try {
          await fetch(`/api/pantry/${id}`, { method: 'DELETE' });
          showToast('Pantry item deleted');
          await fetchPantry();
          renderApp();
        } catch (err) {
          showToast('Error deleting item');
        }
      }
      return;
    }
  });

  // Shopping List Listener
  shoppingList.addEventListener('click', async (e) => {
    const checkbox = e.target.closest('.shopping-checkbox');
    if (checkbox) {
      const id = parseInt(checkbox.dataset.id, 10);
      const isPurchased = checkbox.checked;
      try {
        await fetch(`/api/shopping-list/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ isPurchased })
        });
        await fetchShoppingList();
        renderApp();
      } catch (err) {
        showToast('Error updating item');
      }
      return;
    }

    const deleteBtn = e.target.closest('.delete-shopping-btn');
    if (deleteBtn) {
      const id = parseInt(deleteBtn.dataset.id, 10);
      try {
        await fetch(`/api/shopping-list/${id}`, { method: 'DELETE' });
        showToast('Item removed from list');
        await fetchShoppingList();
        renderApp();
      } catch (err) {
        showToast('Error deleting item');
      }
      return;
    }
  });

  // Auto-Restock Shopping List
  autoRestockBtn.addEventListener('click', async () => {
    let addedCount = 0;

    // Add empty bottles
    const emptyBottles = inventoryData.filter(b => b.stockStatus === 'empty');
    for (const b of emptyBottles) {
      if (!shoppingListData.some(s => s.item_name.includes(b.name))) {
        await fetch('/api/shopping-list', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ itemName: `🍾 ${b.name} (${b.brand})`, category: 'Bottle' })
        });
        addedCount++;
      }
    }

    // Add out-of-stock pantry supplies
    const outPantry = pantryData.filter(p => p.stock_status === 'out');
    for (const p of outPantry) {
      if (!shoppingListData.some(s => s.item_name.includes(p.name))) {
        await fetch('/api/shopping-list', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ itemName: `🧊 ${p.name}`, category: p.category })
        });
        addedCount++;
      }
    }

    showToast(`Added ${addedCount} depleted items to your restock list!`);
    await fetchShoppingList();
    renderApp();
  });

  // Add Shopping Item Form
  addShoppingItemForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const itemName = shoppingItemInput.value.trim();
    if (!itemName) return;

    try {
      await fetch('/api/shopping-list', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ itemName })
      });
      shoppingItemInput.value = '';
      showToast('Item added to shopping list!');
      await fetchShoppingList();
      renderApp();
    } catch (err) {
      showToast('Error adding item');
    }
  });

  // View Switcher Tabs Listener
  viewTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      viewTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentViewMode = tab.dataset.view;
      renderApp();
    });
  });

  // Stock Filter Chips Listener
  stockFilterBar.querySelectorAll('.stock-chip').forEach(btn => {
    btn.addEventListener('click', () => {
      stockFilterBar.querySelectorAll('.stock-chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      currentStockFilter = btn.dataset.stock;
      renderApp();
    });
  });

  // Category Filter Chips
  inventoryFilterChips.querySelectorAll('.chip').forEach(btn => {
    btn.addEventListener('click', () => {
      inventoryFilterChips.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      currentFilter = btn.dataset.category;
      renderApp();
    });
  });

  recipeFilterChips.querySelectorAll('.chip').forEach(btn => {
    btn.addEventListener('click', () => {
      recipeFilterChips.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      currentRecipeFilter = btn.dataset.recipeFilter;
      renderApp();
    });
  });

  pantryFilterChips.querySelectorAll('.chip').forEach(btn => {
    btn.addEventListener('click', () => {
      pantryFilterChips.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      currentPantryFilter = btn.dataset.pantryCategory;
      renderApp();
    });
  });

  // Search Input Listener
  searchInput.addEventListener('input', (e) => {
    searchQuery = e.target.value;
    clearSearchBtn.classList.toggle('hidden', searchQuery.trim() === '');
    renderApp();
  });

  clearSearchBtn.addEventListener('click', () => {
    searchInput.value = '';
    searchQuery = '';
    clearSearchBtn.classList.add('hidden');
    renderApp();
  });

  sortSelect.addEventListener('change', (e) => {
    currentSort = e.target.value;
    renderApp();
  });

  resetFiltersBtn.addEventListener('click', () => {
    searchInput.value = '';
    searchQuery = '';
    currentFilter = 'all';
    currentStockFilter = 'all';
    currentRecipeFilter = 'all';
    currentPantryFilter = 'all';
    currentSort = 'id';
    sortSelect.value = 'id';
    clearSearchBtn.classList.add('hidden');
    renderApp();
  });

  // BOTTLE DETAIL MODAL LOGIC
  function openDetailModal(id) {
    currentActiveBottleId = id;
    const item = inventoryData.find(b => b.id === id);
    if (!item) return;

    const photoUrl = `/photos/${item.photoFilename}`;
    const proofText = item.proof ? `${item.proof} Proof (${item.abvPercent}% ABV)` : (item.abvPercent ? `${item.abvPercent}% ABV` : 'Non-Alcoholic');
    const isBottleFav = favoriteBottles.has(item.id);

    const stockLevels = [
      { id: 'full', label: 'Full (100%)', icon: '🍾' },
      { id: 'three-quarter', label: '¾ Bottle', icon: '🍸' },
      { id: 'half', label: '½ Bottle', icon: '🥃' },
      { id: 'quarter', label: '¼ Bottle', icon: '🍹' },
      { id: 'almost-empty', label: 'Almost Empty', icon: '⚠️' },
      { id: 'empty', label: 'Empty (0%)', icon: '❌' }
    ];

    const stockPickerHTML = `
      <div class="stock-management-box">
        <div class="stock-box-header">
          <span class="stock-box-title">📊 Update Bottle Fill Level</span>
          <span style="font-size:0.8rem; color:var(--text-muted);">Current: <strong>${item.stockLevel.toUpperCase()}</strong></span>
        </div>
        <div class="stock-level-picker">
          ${stockLevels.map(sl => `
            <button class="stock-level-btn ${item.stockLevel === sl.id ? 'active' : ''}" data-bottle-id="${item.id}" data-level="${sl.id}">
              <span class="stock-icon">${sl.icon}</span>
              <span class="stock-lbl">${sl.label}</span>
            </button>
          `).join('')}
        </div>
      </div>
    `;

    const cocktailsHTML = (item.cocktails && item.cocktails.length > 0) ? item.cocktails.map((c, idx) => {
      const uniqueKey = `b${item.id}_c${idx}`;
      const isFav = favoriteRecipes.has(uniqueKey);
      return `
        <div class="recipe-card" data-index="${idx}">
          <div class="recipe-header">
            <div>
              <span class="recipe-badge">Cocktail #${idx + 1}</span>
              <h4 class="recipe-name">${escapeHTML(c.name)}</h4>
            </div>
            <div class="recipe-actions">
              <button class="action-icon-btn fav-recipe-btn ${isFav ? 'active' : ''}" data-unique-key="${uniqueKey}" title="${isFav ? 'Unfavorite recipe' : 'Favorite recipe 🍾'}">🍾</button>
              <button class="action-icon-btn edit-cocktail-btn" data-bottle-id="${item.id}" data-index="${idx}" title="Edit Recipe">✏️</button>
              <button class="action-icon-btn delete-btn delete-cocktail-btn" data-bottle-id="${item.id}" data-index="${idx}" title="Delete Recipe">🗑️</button>
            </div>
          </div>
          <div class="recipe-glass">🥂 <strong>Glass:</strong> ${escapeHTML(c.glass)}</div>
          <ul class="ingredients-list">
            ${c.ingredients.map(ing => `<li>${escapeHTML(ing)}</li>`).join('')}
          </ul>
          <p class="instructions-text"><strong>Preparation:</strong> ${escapeHTML(c.instructions)}</p>
        </div>
      `;
    }).join('') : `<p style="color:var(--text-muted); font-size:0.9rem;">No cocktails added yet for this bottle.</p>`;

    const mocktailKey = `b${item.id}_m0`;
    const isMocktailFav = favoriteRecipes.has(mocktailKey);

    const mocktailHTML = item.mocktail ? `
      <div class="recipe-card mocktail-card">
        <div class="recipe-header">
          <div>
            <span class="recipe-badge">🍹 Zero-Proof Mocktail</span>
            <h4 class="recipe-name">${escapeHTML(item.mocktail.name)}</h4>
          </div>
          <div class="recipe-actions">
            <button class="action-icon-btn fav-recipe-btn ${isMocktailFav ? 'active' : ''}" data-unique-key="${mocktailKey}" title="${isMocktailFav ? 'Unfavorite mocktail' : 'Favorite mocktail 🍾'}">🍾</button>
            <button class="action-icon-btn edit-mocktail-btn" data-bottle-id="${item.id}" title="Edit Zero-Proof Mocktail">✏️</button>
          </div>
        </div>
        <div class="recipe-glass">🥤 <strong>Glass:</strong> ${escapeHTML(item.mocktail.glass)}</div>
        <ul class="ingredients-list">
          ${item.mocktail.ingredients.map(ing => `<li>${escapeHTML(ing)}</li>`).join('')}
        </ul>
        <p class="instructions-text"><strong>Preparation:</strong> ${escapeHTML(item.mocktail.instructions)}</p>
      </div>
    ` : `
      <div style="margin-top:0.5rem;">
        <button class="secondary-btn small-btn add-mocktail-btn" data-bottle-id="${item.id}">+ Add Zero-Proof Mocktail</button>
      </div>
    `;

    modalContent.innerHTML = `
      <div class="detail-layout">
        <!-- Sidebar Image & Specs -->
        <div class="detail-sidebar">
          <div class="modal-image-container">
            <img src="${photoUrl}" alt="${escapeHTML(item.name)}">
          </div>

          <div class="spec-sheet">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.75rem; border-bottom:1px solid var(--border-color); padding-bottom:0.4rem;">
              <h4 class="spec-title" style="margin-bottom:0; border-bottom:none;">Bottle Specifications</h4>
              <button class="secondary-btn small-btn modal-fav-bottle-btn ${isBottleFav ? 'primary-btn' : ''}" data-bottle-id="${item.id}">
                ${isBottleFav ? '🍾 Favorited' : '🍾 Add to Favorites'}
              </button>
            </div>
            <div class="spec-row">
              <span class="spec-key">Brand</span>
              <span class="spec-val">${escapeHTML(item.brand)}</span>
            </div>
            <div class="spec-row">
              <span class="spec-key">Category</span>
              <span class="spec-val">${escapeHTML(item.category)}</span>
            </div>
            ${item.subCategory ? `
            <div class="spec-row">
              <span class="spec-key">Style</span>
              <span class="spec-val">${escapeHTML(item.subCategory)}</span>
            </div>` : ''}
            <div class="spec-row">
              <span class="spec-key">Proof / ABV</span>
              <span class="spec-val">${proofText}</span>
            </div>
            <div class="spec-row">
              <span class="spec-key">Volume</span>
              <span class="spec-val">${escapeHTML(item.volume || '750ml')}</span>
            </div>
            <div style="margin-top:1rem; display:flex; gap:0.5rem;">
              <button class="secondary-btn small-btn edit-bottle-info-btn" data-bottle-id="${item.id}" style="width:100%;">✏️ Edit Bottle Details</button>
              <button class="secondary-btn small-btn delete-bottle-btn" data-bottle-id="${item.id}" style="color:var(--accent-red); border-color:rgba(231,111,81,0.3);">🗑️</button>
            </div>
          </div>
        </div>

        <!-- Main Detail & Recipe Cards -->
        <div class="detail-main">
          <div class="modal-header-section">
            <span class="modal-brand">${escapeHTML(item.brand)}</span>
            <h2 class="modal-title">${escapeHTML(item.name)}</h2>
            <p class="modal-notes">💡 <strong>Cabinet Notes:</strong> ${escapeHTML(item.notes || 'No extra notes.')}</p>
          </div>

          <!-- Stock Level Selector Widget -->
          ${stockPickerHTML}

          <!-- Recommended Cocktails Section -->
          <div>
            <div class="recipe-section-header">
              <h3 class="recipe-section-title">🍸 Recommended Cocktails</h3>
              <button class="primary-btn small-btn add-cocktail-btn" data-bottle-id="${item.id}">+ Add Cocktail</button>
            </div>
            <div class="recipe-cards-grid" style="margin-top: 0.85rem;">
              ${cocktailsHTML}
            </div>
          </div>

          <!-- Non-Alcoholic Mocktail Section -->
          <div style="margin-top: 1.25rem;">
            <div class="recipe-section-header">
              <h3 class="recipe-section-title" style="color: var(--accent-teal);">🍹 Non-Alcoholic Alternative</h3>
            </div>
            <div style="margin-top: 0.85rem;">
              ${mocktailHTML}
            </div>
          </div>
        </div>
      </div>
    `;

    detailModal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }

  function closeModal() {
    detailModal.classList.add('hidden');
    document.body.style.overflow = '';
    currentActiveBottleId = null;
  }

  // EVENT DELEGATION ON DETAIL MODAL
  detailModal.addEventListener('click', async (e) => {
    if (e.target.closest('#closeModalBtn') || e.target === detailModal) {
      closeModal();
      return;
    }

    // Stock Level Picker Click
    const stockBtn = e.target.closest('.stock-level-btn');
    if (stockBtn) {
      const bottleId = parseInt(stockBtn.dataset.bottleId, 10);
      const newLevel = stockBtn.dataset.level;
      try {
        const res = await fetch(`/api/bottles/${bottleId}/stock`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ stockLevel: newLevel })
        });
        if (res.ok) {
          showToast(`Bottle stock level updated to ${newLevel.toUpperCase()}`);
          await fetchInventory();
          openDetailModal(bottleId);
          renderApp();
        }
      } catch (err) {
        showToast('Error updating stock level');
      }
      return;
    }

    const favBottleBtn = e.target.closest('.modal-fav-bottle-btn');
    if (favBottleBtn) {
      const bottleId = parseInt(favBottleBtn.dataset.bottleId, 10);
      toggleFavoriteBottle(bottleId);
      openDetailModal(bottleId);
      return;
    }

    const editBottleInfoBtn = e.target.closest('.edit-bottle-info-btn');
    if (editBottleInfoBtn) {
      const bottleId = parseInt(editBottleInfoBtn.dataset.bottleId, 10);
      openBottleForm(bottleId);
      return;
    }

    const deleteBottleBtn = e.target.closest('.delete-bottle-btn');
    if (deleteBottleBtn) {
      const bottleId = parseInt(deleteBottleBtn.dataset.bottleId, 10);
      if (confirm('Are you sure you want to delete this bottle from your inventory?')) {
        try {
          await fetch(`/api/bottles/${bottleId}`, { method: 'DELETE' });
          showToast('Bottle deleted from inventory');
          closeModal();
          await fetchInventory();
          renderApp();
        } catch (err) {
          showToast('Error deleting bottle');
        }
      }
      return;
    }

    const favRecipeBtn = e.target.closest('.fav-recipe-btn');
    if (favRecipeBtn) {
      const key = favRecipeBtn.dataset.uniqueKey;
      toggleFavoriteRecipe(key);
      if (currentActiveBottleId) openDetailModal(currentActiveBottleId);
      return;
    }

    const addCocktailBtn = e.target.closest('.add-cocktail-btn');
    if (addCocktailBtn) {
      const bottleId = parseInt(addCocktailBtn.dataset.bottleId, 10);
      openRecipeForm(bottleId, 'cocktail', -1);
      return;
    }

    const addMocktailBtn = e.target.closest('.add-mocktail-btn');
    if (addMocktailBtn) {
      const bottleId = parseInt(addMocktailBtn.dataset.bottleId, 10);
      openRecipeForm(bottleId, 'mocktail', -1);
      return;
    }

    const editCocktailBtn = e.target.closest('.edit-cocktail-btn');
    if (editCocktailBtn) {
      const bottleId = parseInt(editCocktailBtn.dataset.bottleId, 10);
      const index = parseInt(editCocktailBtn.dataset.index, 10);
      openRecipeForm(bottleId, 'cocktail', index);
      return;
    }

    const deleteCocktailBtn = e.target.closest('.delete-cocktail-btn');
    if (deleteCocktailBtn) {
      const bottleId = parseInt(deleteCocktailBtn.dataset.bottleId, 10);
      const index = parseInt(deleteCocktailBtn.dataset.index, 10);
      if (confirm(`Delete Cocktail #${index + 1}?`)) {
        deleteCocktailRecipe(bottleId, index);
      }
      return;
    }

    const editMocktailBtn = e.target.closest('.edit-mocktail-btn');
    if (editMocktailBtn) {
      const bottleId = parseInt(editMocktailBtn.dataset.bottleId, 10);
      openRecipeForm(bottleId, 'mocktail', -1);
      return;
    }
  });

  // ADD / EDIT BOTTLE FORM LOGIC
  addBottleBtn.addEventListener('click', () => {
    openBottleForm(null);
  });

  function openBottleForm(bottleId = null) {
    if (bottleId) {
      const b = inventoryData.find(item => item.id === bottleId);
      if (!b) return;
      bottleFormTitle.textContent = `✏️ Edit Bottle Details`;
      formEditBottleId.value = b.id;
      bottleNameInput.value = b.name;
      bottleBrandInput.value = b.brand;
      bottleCategoryInput.value = b.category || 'Whiskey / Bourbon';
      bottleSubCategoryInput.value = b.subCategory || '';
      bottleVolumeInput.value = b.volume || '750ml';
      bottleProofInput.value = b.proof !== null ? b.proof : '';
      bottleAbvInput.value = b.abvPercent !== null ? b.abvPercent : '';
      bottleStockLevelInput.value = b.stockLevel || 'full';
      bottleNotesInput.value = b.notes || '';
      photoPreviewName.textContent = b.photoFilename || 'Existing photo attached';
    } else {
      bottleFormTitle.textContent = `➕ Add New Alcohol Bottle`;
      formEditBottleId.value = '';
      bottleForm.reset();
      bottleVolumeInput.value = '750ml';
      bottleStockLevelInput.value = 'full';
      photoPreviewName.textContent = 'No file selected';
    }

    bottleFormModal.classList.remove('hidden');
  }

  function closeBottleForm() {
    bottleFormModal.classList.add('hidden');
  }

  bottlePhotoInput.addEventListener('change', () => {
    if (bottlePhotoInput.files.length > 0) {
      photoPreviewName.textContent = `📸 ${bottlePhotoInput.files[0].name}`;
    } else {
      photoPreviewName.textContent = 'No file selected';
    }
  });

  bottleForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const editId = formEditBottleId.value ? parseInt(formEditBottleId.value, 10) : null;
    let photoFilename = 'placeholder.jpg';

    // Handle Photo Upload if file selected
    if (bottlePhotoInput.files.length > 0) {
      const file = bottlePhotoInput.files[0];
      const base64 = await readFileAsBase64(file);
      try {
        const uploadRes = await fetch('/api/upload-photo', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ filename: file.name, imageBase64: base64 })
        });
        if (uploadRes.ok) {
          const uploadData = await uploadRes.json();
          photoFilename = uploadData.photoFilename;
        }
      } catch (uploadErr) {
        console.error('Photo upload failed:', uploadErr);
      }
    } else if (editId) {
      const existing = inventoryData.find(b => b.id === editId);
      if (existing) photoFilename = existing.photoFilename;
    }

    const payload = {
      name: bottleNameInput.value.trim(),
      brand: bottleBrandInput.value.trim(),
      category: bottleCategoryInput.value,
      subCategory: bottleSubCategoryInput.value.trim(),
      volume: bottleVolumeInput.value.trim(),
      proof: bottleProofInput.value ? parseFloat(bottleProofInput.value) : null,
      abvPercent: bottleAbvInput.value ? parseFloat(bottleAbvInput.value) : null,
      stockLevel: bottleStockLevelInput.value,
      notes: bottleNotesInput.value.trim(),
      photoFilename: photoFilename
    };

    try {
      let url = '/api/bottles';
      let method = 'POST';

      if (editId) {
        url = `/api/bottles/${editId}`;
        method = 'PUT';
      }

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Failed to save bottle');

      showToast(`✅ Bottle saved successfully!`);
      closeBottleForm();
      await fetchInventory();
      renderApp();
      if (editId) openDetailModal(editId);
    } catch (err) {
      showToast('❌ Error saving bottle');
    }
  });

  // PANTRY FORM LOGIC
  addPantryItemBtn.addEventListener('click', () => {
    openPantryForm(null);
  });

  function openPantryForm(id = null) {
    if (id) {
      const item = pantryData.find(p => p.id === id);
      if (!item) return;
      pantryFormTitle.textContent = `✏️ Edit Bar Supply Item`;
      formEditPantryId.value = item.id;
      pantryNameInput.value = item.name;
      pantryCategoryInput.value = item.category || 'bitters';
      pantryStockInput.value = item.stock_status || 'in-stock';
      pantryNotesInput.value = item.notes || '';
    } else {
      pantryFormTitle.textContent = `➕ Add Bar Supply Item`;
      formEditPantryId.value = '';
      pantryForm.reset();
      pantryCategoryInput.value = 'bitters';
      pantryStockInput.value = 'in-stock';
    }
    pantryFormModal.classList.remove('hidden');
  }

  function closePantryForm() {
    pantryFormModal.classList.add('hidden');
  }

  pantryForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const editId = formEditPantryId.value ? parseInt(formEditPantryId.value, 10) : null;

    const payload = {
      name: pantryNameInput.value.trim(),
      category: pantryCategoryInput.value,
      stockStatus: pantryStockInput.value,
      notes: pantryNotesInput.value.trim()
    };

    try {
      let url = '/api/pantry';
      let method = 'POST';

      if (editId) {
        url = `/api/pantry/${editId}`;
        method = 'PUT';
      }

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Failed to save supply');

      showToast('✅ Bar supply saved!');
      closePantryForm();
      await fetchPantry();
      renderApp();
    } catch (err) {
      showToast('❌ Error saving pantry item');
    }
  });

  // RECIPE CRUD FORM LOGIC
  function openRecipeForm(bottleId, type, index) {
    const item = inventoryData.find(b => b.id === bottleId);
    if (!item) return;

    formBottleId.value = bottleId;
    formRecipeType.value = type;
    formRecipeIndex.value = index;

    ingredientsContainer.innerHTML = '';

    if (type === 'cocktail') {
      if (index === -1) {
        recipeFormTitle.textContent = `➕ Add New Cocktail Recipe`;
        recipeFormSubtitle.textContent = `Add a cocktail for ${item.name}`;
        recipeNameInput.value = '';
        recipeGlassInput.value = 'Rocks Glass';
        recipeInstructionsInput.value = '';
        addIngredientRow(`${item.name} (1.5 oz)`);
        addIngredientRow('Fresh Lemon/Lime Juice');
      } else {
        const existing = item.cocktails[index];
        recipeFormTitle.textContent = `✏️ Edit Cocktail Recipe`;
        recipeFormSubtitle.textContent = `Editing "${existing.name}"`;
        recipeNameInput.value = existing.name;
        recipeGlassInput.value = existing.glass || 'Rocks Glass';
        recipeInstructionsInput.value = existing.instructions || '';
        (existing.ingredients || []).forEach(ing => addIngredientRow(ing));
      }
    } else if (type === 'mocktail') {
      if (item.mocktail) {
        recipeFormTitle.textContent = `✏️ Edit Zero-Proof Mocktail`;
        recipeFormSubtitle.textContent = `Editing "${item.mocktail.name}"`;
        recipeNameInput.value = item.mocktail.name;
        recipeGlassInput.value = item.mocktail.glass || 'Highball Glass';
        recipeInstructionsInput.value = item.mocktail.instructions || '';
        (item.mocktail.ingredients || []).forEach(ing => addIngredientRow(ing));
      } else {
        recipeFormTitle.textContent = `➕ Add Zero-Proof Mocktail`;
        recipeFormSubtitle.textContent = `Add a non-alcoholic drink alternative`;
        recipeNameInput.value = `Zero-Proof ${item.brand} Mocktail`;
        recipeGlassInput.value = 'Highball Glass';
        recipeInstructionsInput.value = '';
        addIngredientRow('Sparkling Mineral Water');
        addIngredientRow('Fresh Citrus Juice');
      }
    }

    recipeFormModal.classList.remove('hidden');
  }

  function closeRecipeForm() {
    recipeFormModal.classList.add('hidden');
  }

  function addIngredientRow(initialValue = '') {
    const row = document.createElement('div');
    row.className = 'ingredient-row';
    row.innerHTML = `
      <input type="text" class="ingredient-input" required placeholder="e.g. 2.0 oz Bourbon or 1 sugar cube" value="${escapeHTML(initialValue)}">
      <button type="button" class="remove-ing-btn" title="Remove ingredient">&times;</button>
    `;

    row.querySelector('.remove-ing-btn').addEventListener('click', () => {
      if (ingredientsContainer.children.length > 1) {
        row.remove();
      } else {
        showToast('Recipe must have at least 1 ingredient!');
      }
    });

    ingredientsContainer.appendChild(row);
  }

  addIngredientRowBtn.addEventListener('click', () => {
    addIngredientRow('');
  });

  recipeForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const bottleId = parseInt(formBottleId.value, 10);
    const type = formRecipeType.value;
    const index = parseInt(formRecipeIndex.value, 10);

    const recipeName = recipeNameInput.value.trim();
    const glassType = recipeGlassInput.value;
    const instructions = recipeInstructionsInput.value.trim();

    const ingredientInputs = ingredientsContainer.querySelectorAll('.ingredient-input');
    const ingredients = Array.from(ingredientInputs).map(inp => inp.value.trim()).filter(val => val !== '');

    if (ingredients.length === 0) {
      showToast('Please specify at least one ingredient.');
      return;
    }

    const recipePayload = {
      name: recipeName,
      glass: glassType,
      ingredients: ingredients,
      instructions: instructions
    };

    try {
      let url = `/api/bottles/${bottleId}/cocktails`;
      let method = 'POST';

      if (type === 'cocktail' && index >= 0) {
        url = `/api/bottles/${bottleId}/cocktails/${index}`;
        method = 'PUT';
      } else if (type === 'mocktail') {
        url = `/api/bottles/${bottleId}/mocktail`;
        method = 'PUT';
      }

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(recipePayload)
      });

      if (!res.ok) throw new Error('Failed to save recipe');

      showToast(`✅ Recipe saved successfully!`);
      closeRecipeForm();
      await fetchInventory();
      renderApp();
      if (currentActiveBottleId) openDetailModal(currentActiveBottleId);
    } catch (err) {
      showToast(`❌ Error saving recipe.`);
    }
  });

  async function deleteCocktailRecipe(bottleId, index) {
    try {
      const res = await fetch(`/api/bottles/${bottleId}/cocktails/${index}`, {
        method: 'DELETE'
      });

      if (!res.ok) throw new Error('Failed to delete recipe');

      showToast(`🗑️ Recipe deleted successfully`);
      await fetchInventory();
      renderApp();
      if (currentActiveBottleId) openDetailModal(currentActiveBottleId);
    } catch (err) {
      showToast(`❌ Error deleting recipe`);
    }
  }

  // Modal Backdrop & Close Button Events
  bottleFormModal.addEventListener('click', (e) => {
    if (e.target.closest('#closeBottleFormBtn') || e.target.closest('#cancelBottleBtn') || e.target === bottleFormModal) {
      closeBottleForm();
    }
  });

  pantryFormModal.addEventListener('click', (e) => {
    if (e.target.closest('#closePantryFormBtn') || e.target.closest('#cancelPantryBtn') || e.target === pantryFormModal) {
      closePantryForm();
    }
  });

  recipeFormModal.addEventListener('click', (e) => {
    if (e.target.closest('#closeRecipeFormBtn') || e.target.closest('#cancelRecipeBtn') || e.target === recipeFormModal) {
      closeRecipeForm();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      if (!bottleFormModal.classList.contains('hidden')) closeBottleForm();
      else if (!pantryFormModal.classList.contains('hidden')) closePantryForm();
      else if (!recipeFormModal.classList.contains('hidden')) closeRecipeForm();
      else if (!detailModal.classList.contains('hidden')) closeModal();
    }
  });

  // Helpers
  function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = error => reject(error);
      reader.readAsDataURL(file);
    });
  }

  function showToast(msg) {
    const toastContainer = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = msg;
    toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.5s ease';
      setTimeout(() => toast.remove(), 500);
    }, 3000);
  }

  function escapeHTML(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
});
