package com.sazon.app.data

import android.content.Context
import com.sazon.app.domain.recipe.SeedRecipeCatalog
import com.sazon.app.domain.recipe.SeedRecipeJson

/**
 * Lee el catálogo de recetas semilla desde `assets/seed-recipes.json`.
 *
 * Es la costura de datos (depende de Android); el parseo puro vive en
 * [com.sazon.app.domain.recipe.SeedRecipeJson]. Mismo patrón que
 * [com.sazon.app.data.AssetTaxonomySource].
 */
class AssetSeedRecipeSource(private val context: Context) {

    fun load(): SeedRecipeCatalog =
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            SeedRecipeJson.parse(reader.readText())
        }

    private companion object {
        const val ASSET_NAME = "seed-recipes.json"
    }
}
