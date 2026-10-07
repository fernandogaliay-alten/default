package com.sazon.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sazon.app.domain.recipe.RecipeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Escenario 2 de la spec: el asset `seed-recipes.json` existe, se llama así y está empaquetado, de
 * modo que `AssetSeedRecipeSource.load()` lo lee sin excepción desde el contexto Android real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AssetSeedRecipeSourceTest {

    @Test
    fun carga_el_asset_empaquetado_descubriendo_el_catalogo() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val catalog = AssetSeedRecipeSource(context).load()

        assertTrue("Se esperaban al menos 12 recetas semilla", catalog.recipes.size >= 12)
        catalog.recipes.forEach { recipe ->
            assertEquals("Toda receta del asset debe ser seed", RecipeSource.SEED, recipe.source)
        }
    }
}
