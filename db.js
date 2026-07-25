const { DatabaseSync } = require('node:sqlite');
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, 'drinkmanager.db');
const INVENTORY_JSON_PATH = path.join(__dirname, 'inventory.json');

const db = new DatabaseSync(DB_PATH);

// Enable foreign keys
db.exec('PRAGMA foreign_keys = ON;');

function initDatabase() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS bottles (
      id INTEGER PRIMARY KEY,
      name TEXT NOT NULL,
      category TEXT NOT NULL,
      sub_category TEXT,
      brand TEXT NOT NULL,
      proof REAL,
      abv_percent REAL,
      volume TEXT,
      photo_filename TEXT,
      all_photos TEXT,
      notes TEXT,
      stock_status TEXT DEFAULT 'in-stock',
      stock_level TEXT DEFAULT 'full',
      is_favorite INTEGER DEFAULT 0,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS pantry_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      category TEXT NOT NULL,
      stock_status TEXT DEFAULT 'in-stock',
      is_favorite INTEGER DEFAULT 0,
      notes TEXT,
      updated_at TEXT DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS cocktails (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      bottle_id INTEGER NOT NULL,
      name TEXT NOT NULL,
      glass TEXT,
      instructions TEXT,
      is_mocktail INTEGER DEFAULT 0,
      is_favorite INTEGER DEFAULT 0,
      FOREIGN KEY (bottle_id) REFERENCES bottles(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS recipe_ingredients (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      cocktail_id INTEGER NOT NULL,
      raw_text TEXT NOT NULL,
      linked_bottle_id INTEGER,
      linked_pantry_id INTEGER,
      FOREIGN KEY (cocktail_id) REFERENCES cocktails(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS shopping_list (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      item_name TEXT NOT NULL,
      category TEXT,
      linked_pantry_id INTEGER,
      linked_bottle_id INTEGER,
      is_purchased INTEGER DEFAULT 0,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Check if bottles table is populated
  const bottleCount = db.prepare('SELECT COUNT(*) as count FROM bottles').get().count;

  if (bottleCount === 0 && fs.existsSync(INVENTORY_JSON_PATH)) {
    console.log('📦 Migrating inventory.json into SQLite database...');
    migrateFromJson();
  }

  // Seed default pantry items if empty
  const pantryCount = db.prepare('SELECT COUNT(*) as count FROM pantry_items').get().count;
  if (pantryCount === 0) {
    seedPantryItems();
  }
}

function migrateFromJson() {
  try {
    const rawData = fs.readFileSync(INVENTORY_JSON_PATH, 'utf8');
    const items = JSON.parse(rawData);

    const insertBottle = db.prepare(`
      INSERT INTO bottles (
        id, name, category, sub_category, brand, proof, abv_percent, volume,
        photo_filename, all_photos, notes, stock_status, stock_level, is_favorite
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
    `);

    const insertCocktail = db.prepare(`
      INSERT INTO cocktails (bottle_id, name, glass, instructions, is_mocktail)
      VALUES (?, ?, ?, ?, ?)
    `);

    const insertIngredient = db.prepare(`
      INSERT INTO recipe_ingredients (cocktail_id, raw_text)
      VALUES (?, ?)
    `);

    db.exec('BEGIN TRANSACTION;');

    for (const item of items) {
      insertBottle.run(
        item.id,
        item.name,
        item.category || 'Other',
        item.subCategory || null,
        item.brand || 'Unknown',
        item.proof !== undefined ? item.proof : null,
        item.abvPercent !== undefined ? item.abvPercent : null,
        item.volume || '750ml',
        item.photoFilename || '',
        JSON.stringify(item.allPhotos || [item.photoFilename]),
        item.notes || '',
        item.stockStatus || 'in-stock',
        item.stockLevel || 'full'
      );

      // Cocktails
      if (Array.isArray(item.cocktails)) {
        for (const c of item.cocktails) {
          const res = insertCocktail.run(item.id, c.name, c.glass || 'Rocks Glass', c.instructions || '', 0);
          const cocktailId = res.lastInsertRowid;
          if (Array.isArray(c.ingredients)) {
            for (const ing of c.ingredients) {
              insertIngredient.run(cocktailId, ing);
            }
          }
        }
      }

      // Mocktail
      if (item.mocktail) {
        const m = item.mocktail;
        const res = insertCocktail.run(item.id, m.name, m.glass || 'Highball Glass', m.instructions || '', 1);
        const cocktailId = res.lastInsertRowid;
        if (Array.isArray(m.ingredients)) {
          for (const ing of m.ingredients) {
            insertIngredient.run(cocktailId, ing);
          }
        }
      }
    }

    db.exec('COMMIT;');
    console.log(`✅ Successfully migrated ${items.length} bottles into SQLite database.`);
  } catch (err) {
    db.exec('ROLLBACK;');
    console.error('❌ Migration failed:', err);
  }
}

function seedPantryItems() {
  const defaultPantry = [
    { name: "Angostura Bitters", category: "bitters", stock_status: "in-stock" },
    { name: "Peychaud's Bitters", category: "bitters", stock_status: "in-stock" },
    { name: "Orange Bitters", category: "bitters", stock_status: "in-stock" },
    { name: "Pecan Bitters", category: "bitters", stock_status: "in-stock" },
    { name: "Chocolate Bitters", category: "bitters", stock_status: "in-stock" },
    { name: "Simple Syrup", category: "syrup", stock_status: "in-stock" },
    { name: "Demerara Syrup", category: "syrup", stock_status: "in-stock" },
    { name: "Honey Syrup", category: "syrup", stock_status: "in-stock" },
    { name: "Grenadine", category: "syrup", stock_status: "in-stock" },
    { name: "Orgeat Syrup", category: "syrup", stock_status: "in-stock" },
    { name: "Fresh Lemon Juice", category: "juice", stock_status: "in-stock" },
    { name: "Fresh Lime Juice", category: "juice", stock_status: "in-stock" },
    { name: "Fresh Orange Juice", category: "juice", stock_status: "in-stock" },
    { name: "Pineapple Juice", category: "juice", stock_status: "in-stock" },
    { name: "Cranberry Juice", category: "juice", stock_status: "in-stock" },
    { name: "Club Soda", category: "mixer", stock_status: "in-stock" },
    { name: "Ginger Beer", category: "mixer", stock_status: "in-stock" },
    { name: "Tonic Water", category: "mixer", stock_status: "in-stock" },
    { name: "Cola / Soda", category: "mixer", stock_status: "in-stock" },
    { name: "Coconut Water / Cream", category: "mixer", stock_status: "in-stock" },
    { name: "Mint Leaves", category: "garnish", stock_status: "in-stock" },
    { name: "Luxardo Maraschino Cherries", category: "garnish", stock_status: "in-stock" },
    { name: "Orange & Lemon Peels", category: "garnish", stock_status: "in-stock" },
    { name: "Olives / Brine", category: "garnish", stock_status: "in-stock" },
    { name: "Large Crystal Ice Spheres", category: "ice", stock_status: "in-stock" },
    { name: "Crushed Ice", category: "ice", stock_status: "in-stock" }
  ];

  const insertPantry = db.prepare(`
    INSERT INTO pantry_items (name, category, stock_status) VALUES (?, ?, ?)
  `);

  db.exec('BEGIN TRANSACTION;');
  for (const p of defaultPantry) {
    insertPantry.run(p.name, p.category, p.stock_status);
  }
  db.exec('COMMIT;');
  console.log(`✅ Seeded ${defaultPantry.length} common bar supplies into pantry_items.`);
}

initDatabase();

module.exports = {
  db,
  // Helper Queries
  getAllBottles() {
    const bottles = db.prepare('SELECT * FROM bottles ORDER BY id ASC').all();
    const getCocktails = db.prepare('SELECT * FROM cocktails WHERE bottle_id = ? AND is_mocktail = 0');
    const getMocktail = db.prepare('SELECT * FROM cocktails WHERE bottle_id = ? AND is_mocktail = 1 LIMIT 1');
    const getIngredients = db.prepare('SELECT raw_text FROM recipe_ingredients WHERE cocktail_id = ?');

    return bottles.map(b => {
      const cocktails = getCocktails.all(b.id).map(c => ({
        id: c.id,
        name: c.name,
        glass: c.glass,
        instructions: c.instructions,
        is_favorite: c.is_favorite,
        ingredients: getIngredients.all(c.id).map(i => i.raw_text)
      }));

      const mocktailRow = getMocktail.get(b.id);
      const mocktail = mocktailRow ? {
        id: mocktailRow.id,
        name: mocktailRow.name,
        glass: mocktailRow.glass,
        instructions: mocktailRow.instructions,
        is_favorite: mocktailRow.is_favorite,
        ingredients: getIngredients.all(mocktailRow.id).map(i => i.raw_text)
      } : null;

      let photos = [];
      try { photos = JSON.parse(b.all_photos || '[]'); } catch (e) { photos = [b.photo_filename]; }

      return {
        id: b.id,
        name: b.name,
        category: b.category,
        subCategory: b.sub_category,
        brand: b.brand,
        proof: b.proof,
        abvPercent: b.abv_percent,
        volume: b.volume,
        photoFilename: b.photo_filename,
        allPhotos: photos,
        notes: b.notes,
        stockStatus: b.stock_status || 'in-stock',
        stockLevel: b.stock_level || 'full',
        isFavorite: b.is_favorite || 0,
        cocktails,
        mocktail
      };
    });
  },

  getBottleById(id) {
    const b = db.prepare('SELECT * FROM bottles WHERE id = ?').get(id);
    if (!b) return null;

    const getCocktails = db.prepare('SELECT * FROM cocktails WHERE bottle_id = ? AND is_mocktail = 0');
    const getMocktail = db.prepare('SELECT * FROM cocktails WHERE bottle_id = ? AND is_mocktail = 1 LIMIT 1');
    const getIngredients = db.prepare('SELECT raw_text FROM recipe_ingredients WHERE cocktail_id = ?');

    const cocktails = getCocktails.all(b.id).map(c => ({
      id: c.id,
      name: c.name,
      glass: c.glass,
      instructions: c.instructions,
      is_favorite: c.is_favorite,
      ingredients: getIngredients.all(c.id).map(i => i.raw_text)
    }));

    const mocktailRow = getMocktail.get(b.id);
    const mocktail = mocktailRow ? {
      id: mocktailRow.id,
      name: mocktailRow.name,
      glass: mocktailRow.glass,
      instructions: mocktailRow.instructions,
      is_favorite: mocktailRow.is_favorite,
      ingredients: getIngredients.all(mocktailRow.id).map(i => i.raw_text)
    } : null;

    let photos = [];
    try { photos = JSON.parse(b.all_photos || '[]'); } catch (e) { photos = [b.photo_filename]; }

    return {
      id: b.id,
      name: b.name,
      category: b.category,
      subCategory: b.sub_category,
      brand: b.brand,
      proof: b.proof,
      abvPercent: b.abv_percent,
      volume: b.volume,
      photoFilename: b.photo_filename,
      allPhotos: photos,
      notes: b.notes,
      stockStatus: b.stock_status || 'in-stock',
      stockLevel: b.stock_level || 'full',
      isFavorite: b.is_favorite || 0,
      cocktails,
      mocktail
    };
  },

  updateBottleStock(id, stockLevel) {
    let stockStatus = 'in-stock';
    if (stockLevel === 'empty') stockStatus = 'empty';
    else if (stockLevel === 'quarter' || stockLevel === 'almost-empty') stockStatus = 'low';

    db.prepare('UPDATE bottles SET stock_level = ?, stock_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?')
      .run(stockLevel, stockStatus, id);

    return this.getBottleById(id);
  },

  createBottle(data) {
    const maxIdRow = db.prepare('SELECT MAX(id) as maxId FROM bottles').get();
    const newId = (maxIdRow && maxIdRow.maxId) ? maxIdRow.maxId + 1 : 1;

    db.prepare(`
      INSERT INTO bottles (
        id, name, category, sub_category, brand, proof, abv_percent, volume,
        photo_filename, all_photos, notes, stock_status, stock_level
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      newId,
      data.name,
      data.category || 'Other',
      data.subCategory || null,
      data.brand || 'Unknown',
      data.proof !== undefined ? data.proof : null,
      data.abvPercent !== undefined ? data.abvPercent : null,
      data.volume || '750ml',
      data.photoFilename || 'placeholder.jpg',
      JSON.stringify(data.allPhotos || [data.photoFilename || 'placeholder.jpg']),
      data.notes || '',
      data.stockStatus || 'in-stock',
      data.stockLevel || 'full'
    );

    return this.getBottleById(newId);
  },

  updateBottle(id, data) {
    db.prepare(`
      UPDATE bottles SET
        name = ?, category = ?, sub_category = ?, brand = ?, proof = ?, abv_percent = ?,
        volume = ?, notes = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(
      data.name,
      data.category || 'Other',
      data.subCategory || null,
      data.brand || 'Unknown',
      data.proof !== undefined ? data.proof : null,
      data.abvPercent !== undefined ? data.abvPercent : null,
      data.volume || '750ml',
      data.notes || '',
      id
    );

    return this.getBottleById(id);
  },

  deleteBottle(id) {
    db.prepare('DELETE FROM bottles WHERE id = ?').run(id);
    return true;
  },

  addCocktail(bottleId, recipe) {
    const res = db.prepare(`
      INSERT INTO cocktails (bottle_id, name, glass, instructions, is_mocktail)
      VALUES (?, ?, ?, ?, 0)
    `).run(bottleId, recipe.name, recipe.glass || 'Rocks Glass', recipe.instructions || '');

    const cocktailId = res.lastInsertRowid;
    if (Array.isArray(recipe.ingredients)) {
      const insertIng = db.prepare('INSERT INTO recipe_ingredients (cocktail_id, raw_text) VALUES (?, ?)');
      for (const ing of recipe.ingredients) {
        insertIng.run(cocktailId, ing);
      }
    }

    return this.getBottleById(bottleId);
  },

  updateCocktail(bottleId, index, recipe) {
    const cocktails = db.prepare('SELECT id FROM cocktails WHERE bottle_id = ? AND is_mocktail = 0 ORDER BY id ASC').all(bottleId);
    if (!cocktails[index]) return null;

    const cocktailId = cocktails[index].id;
    db.prepare('UPDATE cocktails SET name = ?, glass = ?, instructions = ? WHERE id = ?')
      .run(recipe.name, recipe.glass || 'Rocks Glass', recipe.instructions || '', cocktailId);

    db.prepare('DELETE FROM recipe_ingredients WHERE cocktail_id = ?').run(cocktailId);
    if (Array.isArray(recipe.ingredients)) {
      const insertIng = db.prepare('INSERT INTO recipe_ingredients (cocktail_id, raw_text) VALUES (?, ?)');
      for (const ing of recipe.ingredients) {
        insertIng.run(cocktailId, ing);
      }
    }

    return this.getBottleById(bottleId);
  },

  deleteCocktail(bottleId, index) {
    const cocktails = db.prepare('SELECT id FROM cocktails WHERE bottle_id = ? AND is_mocktail = 0 ORDER BY id ASC').all(bottleId);
    if (!cocktails[index]) return null;

    db.prepare('DELETE FROM cocktails WHERE id = ?').run(cocktails[index].id);
    return this.getBottleById(bottleId);
  },

  updateMocktail(bottleId, recipe) {
    const existing = db.prepare('SELECT id FROM cocktails WHERE bottle_id = ? AND is_mocktail = 1 LIMIT 1').get(bottleId);

    let cocktailId;
    if (existing) {
      cocktailId = existing.id;
      db.prepare('UPDATE cocktails SET name = ?, glass = ?, instructions = ? WHERE id = ?')
        .run(recipe.name, recipe.glass || 'Highball Glass', recipe.instructions || '', cocktailId);
      db.prepare('DELETE FROM recipe_ingredients WHERE cocktail_id = ?').run(cocktailId);
    } else {
      const res = db.prepare('INSERT INTO cocktails (bottle_id, name, glass, instructions, is_mocktail) VALUES (?, ?, ?, ?, 1)')
        .run(bottleId, recipe.name, recipe.glass || 'Highball Glass', recipe.instructions || '');
      cocktailId = res.lastInsertRowid;
    }

    if (Array.isArray(recipe.ingredients)) {
      const insertIng = db.prepare('INSERT INTO recipe_ingredients (cocktail_id, raw_text) VALUES (?, ?)');
      for (const ing of recipe.ingredients) {
        insertIng.run(cocktailId, ing);
      }
    }

    return this.getBottleById(bottleId);
  },

  // Pantry Items CRUD
  getAllPantryItems() {
    return db.prepare('SELECT * FROM pantry_items ORDER BY category ASC, name ASC').all();
  },

  createPantryItem(data) {
    const res = db.prepare('INSERT INTO pantry_items (name, category, stock_status, notes) VALUES (?, ?, ?, ?)')
      .run(data.name, data.category || 'other', data.stockStatus || 'in-stock', data.notes || '');
    return db.prepare('SELECT * FROM pantry_items WHERE id = ?').get(res.lastInsertRowid);
  },

  updatePantryItem(id, data) {
    db.prepare('UPDATE pantry_items SET name = ?, category = ?, stock_status = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?')
      .run(data.name, data.category, data.stockStatus, data.notes || '', id);
    return db.prepare('SELECT * FROM pantry_items WHERE id = ?').get(id);
  },

  updatePantryStock(id, stockStatus) {
    db.prepare('UPDATE pantry_items SET stock_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?')
      .run(stockStatus, id);
    return db.prepare('SELECT * FROM pantry_items WHERE id = ?').get(id);
  },

  deletePantryItem(id) {
    db.prepare('DELETE FROM pantry_items WHERE id = ?').run(id);
    return true;
  },

  // Shopping List CRUD
  getShoppingList() {
    return db.prepare('SELECT * FROM shopping_list ORDER BY is_purchased ASC, created_at DESC').all();
  },

  addShoppingItem(data) {
    const res = db.prepare('INSERT INTO shopping_list (item_name, category, linked_pantry_id, linked_bottle_id) VALUES (?, ?, ?, ?)')
      .run(data.itemName, data.category || 'other', data.linkedPantryId || null, data.linkedBottleId || null);
    return db.prepare('SELECT * FROM shopping_list WHERE id = ?').get(res.lastInsertRowid);
  },

  toggleShoppingItemPurchased(id, isPurchased) {
    db.prepare('UPDATE shopping_list SET is_purchased = ? WHERE id = ?').run(isPurchased ? 1 : 0, id);
    return db.prepare('SELECT * FROM shopping_list WHERE id = ?').get(id);
  },

  deleteShoppingItem(id) {
    db.prepare('DELETE FROM shopping_list WHERE id = ?').run(id);
    return true;
  },

  // Can I Make This Engine
  checkCanMakeRecipe(recipeId) {
    const cocktail = db.prepare('SELECT * FROM cocktails WHERE id = ?').get(recipeId);
    if (!cocktail) return null;

    const bottle = db.prepare('SELECT * FROM bottles WHERE id = ?').get(cocktail.bottle_id);
    const bottleInStock = bottle && bottle.stock_status !== 'empty';

    const ingredients = db.prepare('SELECT * FROM recipe_ingredients WHERE cocktail_id = ?').all(recipeId);
    const pantryItems = db.prepare('SELECT * FROM pantry_items').all();
    const bottles = db.prepare('SELECT * FROM bottles').all();

    const missing = [];
    const available = [];

    // Check base bottle
    if (!bottleInStock) {
      missing.push(`Base Spirit: ${bottle ? bottle.name : 'Base Bottle'} (Empty)`);
    } else {
      available.push(`Base Spirit: ${bottle.name}`);
    }

    for (const ing of ingredients) {
      const ingLower = ing.raw_text.toLowerCase();
      // Skip simple base spirit mention if it matches bottle name
      if (bottle && ingLower.includes(bottle.brand.toLowerCase())) {
        continue;
      }

      // Check if matches any out-of-stock pantry item
      let matchedPantry = pantryItems.find(p => ingLower.includes(p.name.toLowerCase()));
      if (matchedPantry) {
        if (matchedPantry.stock_status === 'out') {
          missing.push(ing.raw_text);
        } else {
          available.push(ing.raw_text);
        }
        continue;
      }

      // Check if matches any empty bottle referenced as ingredient (e.g. sweet vermouth, Kahlua, Campari)
      let matchedBottle = bottles.find(b => ingLower.includes(b.brand.toLowerCase()) || ingLower.includes(b.name.toLowerCase()));
      if (matchedBottle) {
        if (matchedBottle.stock_status === 'empty') {
          missing.push(ing.raw_text);
        } else {
          available.push(ing.raw_text);
        }
        continue;
      }

      // Default assume available unless known out
      available.push(ing.raw_text);
    }

    return {
      recipeId,
      recipeName: cocktail.name,
      canMake: missing.length === 0,
      missingCount: missing.length,
      missingIngredients: missing,
      availableIngredients: available
    };
  }
};
