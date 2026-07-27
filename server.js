const http = require('http');
const fs = require('fs');
const path = require('path');
const dbModule = require('./db.js');

const PORT = process.env.PORT || 3005;
const HOST = '0.0.0.0'; // Allow local network & mobile access
const ROOT_DIR = __dirname;
const PUBLIC_DIR = path.join(ROOT_DIR, 'public');
const PHOTOS_DIR = path.join(ROOT_DIR, 'photos');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.png': 'image/png',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.dng': 'application/octet-stream'
};

function parseRequestBody(req, callback) {
  let body = '';
  req.on('data', chunk => {
    body += chunk.toString();
  });
  req.on('end', () => {
    try {
      const parsed = body ? JSON.parse(body) : {};
      callback(null, parsed);
    } catch (err) {
      callback(err, null);
    }
  });
}

const server = http.createServer((req, res) => {
  console.log(`${new Date().toISOString()} [${req.method}] ${req.url}`);

  // CORS headers for Android app & cross-origin clients
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  let pathname = decodeURIComponent(parsedUrl.pathname);

  // ----------------------------------------------------
  // API ENDPOINTS
  // ----------------------------------------------------

  // 1. GET /api/inventory or /api/bottles
  if (req.method === 'GET' && (pathname === '/api/inventory' || pathname === '/api/bottles')) {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    try {
      const bottles = dbModule.getAllBottles();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(bottles));
    } catch (err) {
      console.error('Error fetching bottles:', err);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Failed to fetch inventory' }));
    }
    return;
  }

  // 2. GET /api/bottles/:id
  const getBottleMatch = pathname.match(/^\/api\/bottles\/(\d+)$/);
  if (req.method === 'GET' && getBottleMatch) {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    const bottleId = parseInt(getBottleMatch[1], 10);
    const bottle = dbModule.getBottleById(bottleId);
    if (!bottle) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Bottle not found' }));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(bottle));
    return;
  }

  // 3. POST /api/bottles (Add New Bottle)
  if (req.method === 'POST' && (pathname === '/api/bottles' || pathname === '/api/inventory')) {
    parseRequestBody(req, (err, payload) => {
      if (err || !payload.name) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Bottle name is required' }));
        return;
      }
      try {
        // Check for duplicates unless force flag is set
        if (!payload.force) {
          const existing = dbModule.findDuplicateBottle(payload.name);
          if (existing) {
            console.log(`Duplicate bottle detected: "${payload.name}" matches existing id=${existing.id}`);
            res.writeHead(409, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              error: 'duplicate',
              message: `A bottle named "${existing.name}" already exists`,
              existingBottle: existing
            }));
            return;
          }
        }
        const newBottle = dbModule.createBottle(payload);
        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(newBottle));
      } catch (createErr) {
        console.error('Error creating bottle:', createErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to create bottle' }));
      }
    });
    return;
  }

  // 4. PUT /api/bottles/:id/stock (Update Stock Level)
  const updateStockMatch = pathname.match(/^\/api\/(?:bottles|inventory)\/(\d+)\/stock$/);
  if (req.method === 'PUT' && updateStockMatch) {
    const bottleId = parseInt(updateStockMatch[1], 10);
    parseRequestBody(req, (err, payload) => {
      if (err || !payload.stockLevel) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'stockLevel parameter required' }));
        return;
      }
      try {
        const updated = dbModule.updateBottleStock(bottleId, payload.stockLevel);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(updated));
      } catch (updateErr) {
        console.error('Error updating bottle stock:', updateErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update stock level' }));
      }
    });
    return;
  }

  // 5. PUT /api/bottles/:id (Edit Bottle Details)
  const editBottleMatch = pathname.match(/^\/api\/(?:bottles|inventory)\/(\d+)$/);
  if (req.method === 'PUT' && editBottleMatch) {
    const bottleId = parseInt(editBottleMatch[1], 10);
    parseRequestBody(req, (err, payload) => {
      if (err || !payload.name) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Bottle name is required' }));
        return;
      }
      try {
        const updated = dbModule.updateBottle(bottleId, payload);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(updated));
      } catch (updateErr) {
        console.error('Error updating bottle:', updateErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update bottle' }));
      }
    });
    return;
  }

  // 6. DELETE /api/bottles/:id (Delete Bottle)
  if (req.method === 'DELETE' && editBottleMatch) {
    const bottleId = parseInt(editBottleMatch[1], 10);
    try {
      dbModule.deleteBottle(bottleId);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ message: 'Bottle deleted successfully' }));
    } catch (delErr) {
      console.error('Error deleting bottle:', delErr);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Failed to delete bottle' }));
    }
    return;
  }

  // 7. POST /api/inventory/:id/cocktails (Add Cocktail)
  const addCocktailMatch = pathname.match(/^\/api\/(?:inventory|bottles)\/(\d+)\/cocktails$/);
  if (req.method === 'POST' && addCocktailMatch) {
    const bottleId = parseInt(addCocktailMatch[1], 10);
    parseRequestBody(req, (err, recipe) => {
      if (err || !recipe.name) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Recipe name is required' }));
        return;
      }
      try {
        const updated = dbModule.addCocktail(bottleId, recipe);
        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Cocktail added', bottle: updated }));
      } catch (cErr) {
        console.error('Error adding cocktail:', cErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to add cocktail' }));
      }
    });
    return;
  }

  // 8. PUT /api/inventory/:id/cocktails/:idx (Edit Cocktail)
  const editCocktailMatch = pathname.match(/^\/api\/(?:inventory|bottles)\/(\d+)\/cocktails\/(\d+)$/);
  if (req.method === 'PUT' && editCocktailMatch) {
    const bottleId = parseInt(editCocktailMatch[1], 10);
    const cocktailIdx = parseInt(editCocktailMatch[2], 10);

    parseRequestBody(req, (err, recipe) => {
      if (err || !recipe.name) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Recipe name is required' }));
        return;
      }
      try {
        const updated = dbModule.updateCocktail(bottleId, cocktailIdx, recipe);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Cocktail updated', bottle: updated }));
      } catch (cErr) {
        console.error('Error updating cocktail:', cErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update cocktail' }));
      }
    });
    return;
  }

  // 9. DELETE /api/inventory/:id/cocktails/:idx (Delete Cocktail)
  if (req.method === 'DELETE' && editCocktailMatch) {
    const bottleId = parseInt(editCocktailMatch[1], 10);
    const cocktailIdx = parseInt(editCocktailMatch[2], 10);
    try {
      const updated = dbModule.deleteCocktail(bottleId, cocktailIdx);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ message: 'Cocktail deleted', bottle: updated }));
    } catch (cErr) {
      console.error('Error deleting cocktail:', cErr);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Failed to delete cocktail' }));
    }
    return;
  }

  // 9b. PUT /api/bottles/:id/favorite
  const bottleFavoriteMatch = pathname.match(/^\/api\/(?:inventory|bottles)\/(\d+)\/favorite$/);
  if (req.method === 'PUT' && bottleFavoriteMatch) {
    const bottleId = parseInt(bottleFavoriteMatch[1], 10);
    parseRequestBody(req, (err, body) => {
      try {
        const isFavorite = body ? body.isFavorite : 1;
        const updated = dbModule.toggleBottleFavorite(bottleId, isFavorite);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Bottle favorite updated', bottle: updated }));
      } catch (fErr) {
        console.error('Error toggling bottle favorite:', fErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update bottle favorite' }));
      }
    });
    return;
  }

  // 9c. PUT /api/pantry/:id/favorite
  const pantryFavoriteMatch = pathname.match(/^\/api\/pantry\/(\d+)\/favorite$/);
  if (req.method === 'PUT' && pantryFavoriteMatch) {
    const pantryId = parseInt(pantryFavoriteMatch[1], 10);
    parseRequestBody(req, (err, body) => {
      try {
        const isFavorite = body ? body.isFavorite : 1;
        const updated = dbModule.togglePantryFavorite(pantryId, isFavorite);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Pantry favorite updated', item: updated }));
      } catch (fErr) {
        console.error('Error toggling pantry favorite:', fErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update pantry favorite' }));
      }
    });
    return;
  }

  // 9d. PUT /api/cocktails/favorite
  if (req.method === 'PUT' && pathname === '/api/cocktails/favorite') {
    parseRequestBody(req, (err, body) => {
      try {
        const { id, name, isFavorite } = body || {};
        dbModule.toggleCocktailFavorite(id, isFavorite, name);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Cocktail favorite updated' }));
      } catch (fErr) {
        console.error('Error toggling cocktail favorite:', fErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update cocktail favorite' }));
      }
    });
    return;
  }

  // 10. PUT /api/inventory/:id/mocktail (Update Mocktail)
  const mocktailMatch = pathname.match(/^\/api\/(?:inventory|bottles)\/(\d+)\/mocktail$/);
  if ((req.method === 'PUT' || req.method === 'POST') && mocktailMatch) {
    const bottleId = parseInt(mocktailMatch[1], 10);
    parseRequestBody(req, (err, mocktailData) => {
      if (err || !mocktailData.name) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Mocktail name is required' }));
        return;
      }
      try {
        const updated = dbModule.updateMocktail(bottleId, mocktailData);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Mocktail updated', bottle: updated }));
      } catch (mErr) {
        console.error('Error updating mocktail:', mErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update mocktail' }));
      }
    });
    return;
  }

  // 11. PANTRY API: GET & POST /api/pantry
  if (pathname === '/api/pantry') {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    if (req.method === 'GET') {
      try {
        const items = dbModule.getAllPantryItems();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(items));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to fetch pantry items' }));
      }
      return;
    }
    if (req.method === 'POST') {
      parseRequestBody(req, (err, payload) => {
        if (err || !payload.name) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Pantry item name is required' }));
          return;
        }
        try {
          const item = dbModule.createPantryItem(payload);
          res.writeHead(201, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(item));
        } catch (pErr) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Failed to create pantry item' }));
        }
      });
      return;
    }
  }

  // 12. PANTRY ITEM PUT & DELETE: /api/pantry/:id and /api/pantry/:id/stock
  const pantryStockMatch = pathname.match(/^\/api\/pantry\/(\d+)\/stock$/);
  if (req.method === 'PUT' && pantryStockMatch) {
    const id = parseInt(pantryStockMatch[1], 10);
    parseRequestBody(req, (err, payload) => {
      if (err || !payload.stockStatus) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'stockStatus required' }));
        return;
      }
      try {
        const updated = dbModule.updatePantryStock(id, payload.stockStatus);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(updated));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to update pantry stock' }));
      }
    });
    return;
  }

  const pantryItemMatch = pathname.match(/^\/api\/pantry\/(\d+)$/);
  if (pantryItemMatch) {
    const id = parseInt(pantryItemMatch[1], 10);
    if (req.method === 'PUT') {
      parseRequestBody(req, (err, payload) => {
        if (err || !payload.name) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Name is required' }));
          return;
        }
        try {
          const updated = dbModule.updatePantryItem(id, payload);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(updated));
        } catch (err) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Failed to update pantry item' }));
        }
      });
      return;
    }
    if (req.method === 'DELETE') {
      try {
        dbModule.deletePantryItem(id);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Pantry item deleted' }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to delete pantry item' }));
      }
      return;
    }
  }

  // 13. SHOPPING LIST API: GET & POST /api/shopping-list
  if (pathname === '/api/shopping-list') {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    if (req.method === 'GET') {
      try {
        const list = dbModule.getShoppingList();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(list));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to fetch shopping list' }));
      }
      return;
    }
    if (req.method === 'POST') {
      parseRequestBody(req, (err, payload) => {
        if (err || !payload.itemName) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'itemName required' }));
          return;
        }
        try {
          const item = dbModule.addShoppingItem(payload);
          res.writeHead(201, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(item));
        } catch (err) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Failed to add to shopping list' }));
        }
      });
      return;
    }
  }

  const shoppingItemMatch = pathname.match(/^\/api\/shopping-list\/(\d+)$/);
  if (shoppingItemMatch) {
    const id = parseInt(shoppingItemMatch[1], 10);
    if (req.method === 'PUT') {
      parseRequestBody(req, (err, payload) => {
        try {
          const updated = dbModule.toggleShoppingItemPurchased(id, payload.isPurchased);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(updated));
        } catch (err) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Failed to update shopping item' }));
        }
      });
      return;
    }
    if (req.method === 'DELETE') {
      try {
        dbModule.deleteShoppingItem(id);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ message: 'Item deleted' }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to delete shopping item' }));
      }
      return;
    }
  }

  // 14. CAN I MAKE THIS ENGINE: GET /api/can-i-make/:recipeId
  const canMakeMatch = pathname.match(/^\/api\/can-i-make\/(\d+)$/);
  if (req.method === 'GET' && canMakeMatch) {
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    const recipeId = parseInt(canMakeMatch[1], 10);
    const result = dbModule.checkCanMakeRecipe(recipeId);
    if (!result) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Recipe not found' }));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(result));
    return;
  }

  // 15b. ANALYZE BOTTLE IMAGE: POST /api/analyze-bottle (AI image analysis via Gemini)
  if (req.method === 'POST' && pathname === '/api/analyze-bottle') {
    parseRequestBody(req, async (err, payload) => {
      if (err || !payload.imageBase64) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'imageBase64 is required' }));
        return;
      }

      const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
      if (!GEMINI_API_KEY) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'GEMINI_API_KEY environment variable not set' }));
        return;
      }

      try {
        // Save the photo to disk
        const finalFilename = `scan_${Date.now()}.jpg`;
        const targetPath = path.join(PHOTOS_DIR, finalFilename);
        const base64Data = payload.imageBase64.replace(/^data:image\/\w+;base64,/, '');
        const buffer = Buffer.from(base64Data, 'base64');
        fs.writeFileSync(targetPath, buffer);
        console.log(`Saved scanned bottle photo: ${finalFilename}`);

        // Call Gemini API for image analysis
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`;
        const prompt = `You are an expert liquor and spirits identifier. Analyze this bottle image and extract the following information.
Return ONLY a valid JSON object (no markdown, no explanation) with these fields:
{
  "name": "Full product name as shown on the label",
  "brand": "Brand/distillery name",
  "category": "One of: whiskey, bourbon, scotch, rye, rum, tequila, gin, vodka, brandy, liqueur, amaro, aperitivo, wine, vermouth, port, moonshine, schnapps, syrup, other",
  "subCategory": "More specific type if applicable (e.g. 'Single Malt', 'Reposado', 'London Dry')",
  "proof": numeric proof value or null if not visible,
  "abvPercent": numeric ABV percentage or null if not visible,
  "volume": "bottle volume as string like '750ml', '1L', etc. or null if not visible",
  "notes": "Brief tasting notes or description from the label if visible, otherwise a brief description of the spirit"
}
If you cannot determine a field, use null. For category, pick the closest match from the list.`;

        const geminiBody = JSON.stringify({
          contents: [{
            parts: [
              { text: prompt },
              {
                inline_data: {
                  mime_type: 'image/jpeg',
                  data: base64Data
                }
              }
            ]
          }],
          generationConfig: {
            temperature: 0.1,
            maxOutputTokens: 1024
          }
        });

        const geminiResp = await fetch(geminiUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: geminiBody
        });

        if (!geminiResp.ok) {
          const errText = await geminiResp.text();
          console.error('Gemini API error:', geminiResp.status, errText);
          res.writeHead(502, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Gemini API request failed', details: errText, photoFilename: finalFilename }));
          return;
        }

        const geminiResult = await geminiResp.json();
        let responseText = geminiResult.candidates?.[0]?.content?.parts?.[0]?.text || '{}';

        // Strip markdown code fences if present
        responseText = responseText.replace(/```json\s*/g, '').replace(/```\s*/g, '').trim();

        let bottleData;
        try {
          bottleData = JSON.parse(responseText);
        } catch (parseErr) {
          console.error('Failed to parse Gemini response:', responseText);
          bottleData = { name: 'Unknown Bottle', category: 'other', notes: responseText };
        }

        // Attach the photo filename
        bottleData.photoFilename = finalFilename;

        console.log('Gemini bottle analysis:', JSON.stringify(bottleData, null, 2));

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(bottleData));
      } catch (analyzeErr) {
        console.error('Analyze bottle error:', analyzeErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to analyze bottle image' }));
      }
    });
    return;
  }

  // 15. PHOTO UPLOAD: POST /api/upload-photo (Base64 image upload)
  if (req.method === 'POST' && pathname === '/api/upload-photo') {
    parseRequestBody(req, (err, payload) => {
      if (err || !payload.imageBase64 || !payload.filename) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'filename and imageBase64 are required' }));
        return;
      }
      try {
        const cleanFilename = path.basename(payload.filename).replace(/[^a-zA-Z0-9_\.-]/g, '_');
        const finalFilename = `upload_${Date.now()}_${cleanFilename}`;
        const targetPath = path.join(PHOTOS_DIR, finalFilename);

        const base64Data = payload.imageBase64.replace(/^data:image\/\w+;base64,/, '');
        const buffer = Buffer.from(base64Data, 'base64');

        fs.writeFileSync(targetPath, buffer);

        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ photoFilename: finalFilename, url: `/photos/${finalFilename}` }));
      } catch (uploadErr) {
        console.error('Upload failed:', uploadErr);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to save uploaded photo' }));
      }
    });
    return;
  }

  // ----------------------------------------------------
  // STATIC FILES
  // ----------------------------------------------------

  if (pathname.startsWith('/photos/')) {
    const photoFilename = pathname.substring('/photos/'.length);
    const photoPath = path.join(PHOTOS_DIR, photoFilename);
    serveStaticFile(photoPath, res);
    return;
  }

  if (pathname === '/') {
    pathname = '/index.html';
  }

  const filePath = path.join(PUBLIC_DIR, pathname);

  if (!filePath.startsWith(PUBLIC_DIR) && !filePath.startsWith(PHOTOS_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  serveStaticFile(filePath, res);
});

function serveStaticFile(filePath, res) {
  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('404 Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    const headers = {
      'Content-Type': contentType,
      'Content-Length': stats.size
    };

    // No caching for HTML/CSS/JS so changes apply instantly
    if (ext === '.html' || ext === '.css' || ext === '.js') {
      headers['Cache-Control'] = 'no-cache, no-store, must-revalidate';
      headers['Pragma'] = 'no-cache';
      headers['Expires'] = '0';
    } else {
      headers['Cache-Control'] = 'public, max-age=3600';
    }

    res.writeHead(200, headers);
    const stream = fs.createReadStream(filePath);
    stream.pipe(res);
  });
}

server.listen(PORT, HOST, () => {
  console.log(`====================================================`);
  console.log(`🍸 DrinkManager Server running at http://${HOST}:${PORT}`);
  console.log(`📱 Available on local network for Android app access`);
  console.log(`====================================================`);
});
