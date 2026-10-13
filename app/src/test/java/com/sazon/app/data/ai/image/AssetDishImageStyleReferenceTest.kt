package com.sazon.app.data.ai.image

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Escenario 10 (costura Android) de `docs/specs/dish-style-reference.md`: el cargador lee el asset
 * **versionado** de `recipe-photos` (`img/recipes/default.<ext>`, hoy `default.jpg`) y deriva su media
 * type y nombre de fichero; una base inexistente devuelve `null` **sin lanzar**. Robolectric lee los
 * assets reales del modulo; no hay subida del usuario ni I/O fuera de `assets/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AssetDishImageStyleReferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // El asset neutro y versionado existe, no esta vacio y sale con su media type y filename.
    @Test
    fun el_asset_versionado_se_lee_con_su_media_type_y_nombre_de_fichero() {
        val reference = AssetDishImageStyleReference(context).load()

        assertNotNull("El asset img/recipes/default.<ext> debe existir en el modulo", reference)
        assertTrue("La referencia no puede estar vacia", reference!!.bytes.isNotEmpty())
        assertEquals("image/jpeg", reference.mediaType)
        assertEquals("dish-style-reference.jpg", reference.fileName)
    }

    // Una base inexistente no lanza: devuelve null y el adaptador degrada al camino de texto.
    @Test
    fun una_base_inexistente_devuelve_null_sin_lanzar() {
        val reference = AssetDishImageStyleReference(context, assetBase = "img/nope/ausente").load()

        assertNull(reference)
    }
}
