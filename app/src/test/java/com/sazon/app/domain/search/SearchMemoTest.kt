package com.sazon.app.domain.search

import com.sazon.app.domain.generation.GenerationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Escenarios del memo de sesion: `get`/`put`, clave normalizada (trim + minusculas) y ausencia.
 * Logica pura, sin Android ni Robolectric.
 */
class SearchMemoTest {

    @Test
    fun ausente_devuelve_null() {
        val memo = SearchMemo()
        assertNull(memo.get("pollo"))
    }

    @Test
    fun put_y_get_devuelven_el_mismo_resultado() {
        val memo = SearchMemo()
        val stored = GenerationResult(accepted = emptyList(), discarded = emptyList())

        memo.put("pollo", stored)

        assertSame(stored, memo.get("pollo"))
    }

    @Test
    fun la_clave_se_normaliza_con_trim_y_minusculas() {
        assertEquals("pollo y arroz", SearchMemo.key("  Pollo y Arroz  "))
        assertEquals("", SearchMemo.key("   "))
    }

    @Test
    fun la_misma_clave_normalizada_devuelve_la_misma_entrada() {
        val memo = SearchMemo()
        val stored = GenerationResult(accepted = emptyList(), discarded = emptyList())

        memo.put(SearchMemo.key("  Pollo  "), stored)

        assertSame(stored, memo.get(SearchMemo.key("pollo")))
    }

    @Test
    fun sobrescribir_la_misma_clave_gana_la_ultima() {
        val memo = SearchMemo()
        val first = GenerationResult(accepted = emptyList(), discarded = emptyList())
        val second = GenerationResult(accepted = emptyList(), discarded = emptyList(), aiUnavailable = true)

        memo.put("pollo", first)
        memo.put("pollo", second)

        assertSame(second, memo.get("pollo"))
        assertTrue(memo.get("pollo")!!.aiUnavailable)
    }
}
