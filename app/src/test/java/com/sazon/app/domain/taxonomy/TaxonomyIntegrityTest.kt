package com.sazon.app.domain.taxonomy

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxonomyIntegrityTest {

    private val json = taxonomyJson()

    @Test
    fun catalogo_se_carga_con_los_tamanos_esperados() {
        val taxonomy = TaxonomyJson.parse(json)
        assertEquals(5, taxonomy.dietPatterns.size)
        assertEquals(11, taxonomy.hardRestrictions.size)
        assertEquals(7, taxonomy.softPreferences.size)
        assertEquals(5, taxonomy.mealTypes.size)
        assertEquals(10, taxonomy.cuisines.size)
        assertEquals(5, taxonomy.timeBuckets.size)
        assertEquals(3, taxonomy.difficulties.size)
        assertEquals(8, taxonomy.avatarColors.size)
        assertEquals(95, taxonomy.ingredients.size)
        assertEquals(18, taxonomy.recipeCategories.size)
    }

    @Test
    fun consulta_por_id_devuelve_el_objeto_o_null() {
        val taxonomy = TaxonomyJson.parse(json)
        assertEquals("Tomate", taxonomy.ingredient("tomate")?.label)
        assertNull(taxonomy.ingredient("no-existe"))
        assertEquals("Vegano", taxonomy.dietPattern("vegan")?.label)
        assertNull(taxonomy.dietPattern("no-existe"))
    }

    @Test
    fun la_taxonomia_real_no_presenta_violaciones() {
        val problems = violations(TaxonomyJson.parse(json))
        assertTrue("Violaciones encontradas: $problems", problems.isEmpty())
    }

    @Test
    fun un_catalogo_con_un_alergeno_inexistente_es_rechazado() {
        val roto = json.replace("\"allergens\": [\"gluten\"]", "\"allergens\": [\"gluten-inexistente\"]")
        assertNotEquals("El reemplazo no altero el JSON: el formato cambio", json, roto)
        val problems = violations(TaxonomyJson.parse(roto))
        assertTrue("Se esperaba al menos una violacion, y no hubo ninguna", problems.isNotEmpty())
    }

    @Test
    fun un_catalogo_con_un_tag_de_dieta_inexistente_es_rechazado() {
        val roto = json.replace("\"tags\": [\"poultry\"]", "\"tags\": [\"tag-inexistente\"]")
        assertNotEquals("El reemplazo no altero el JSON: el formato cambio", json, roto)
        val problems = violations(TaxonomyJson.parse(roto))
        assertTrue("Se esperaba al menos una violacion, y no hubo ninguna", problems.isNotEmpty())
    }
}

private val AVATAR_HEX_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")

private fun taxonomyJson(): String {
    val path = checkNotNull(System.getProperty("taxonomy.asset.path")) {
        "Falta la propiedad de sistema taxonomy.asset.path"
    }
    return File(path).readText()
}

private fun violations(taxonomy: Taxonomy): List<String> {
    val problems = mutableListOf<String>()

    val restrictionIds = taxonomy.hardRestrictions.map { it.id }.toSet()
    val dietTags = taxonomy.dietPatterns.flatMap { it.excludedTags }.toSet()
    val fallbackCategories = taxonomy.categoryFallbackIcon.keys

    duplicatedIds(taxonomy.dietPatterns.map { it.id }, "dietPatterns", problems)
    duplicatedIds(taxonomy.hardRestrictions.map { it.id }, "hardRestrictions", problems)
    duplicatedIds(taxonomy.softPreferences.map { it.id }, "softPreferences", problems)
    duplicatedIds(taxonomy.mealTypes.map { it.id }, "mealTypes", problems)
    duplicatedIds(taxonomy.cuisines.map { it.id }, "cuisines", problems)
    duplicatedIds(taxonomy.timeBuckets.map { it.id }, "timeBuckets", problems)
    duplicatedIds(taxonomy.difficulties.map { it.id }, "difficulties", problems)
    duplicatedIds(taxonomy.avatarColors.map { it.id }, "avatarColors", problems)
    duplicatedIds(taxonomy.ingredients.map { it.id }, "ingredients", problems)
    duplicatedIds(taxonomy.recipeCategories.map { it.id }, "recipeCategories", problems)

    taxonomy.ingredients.forEach { ingredient ->
        ingredient.allergens.forEach { allergen ->
            if (allergen !in restrictionIds) {
                problems += "ingrediente '${ingredient.id}': alergeno '$allergen' no declarado en hardRestrictions"
            }
        }
        ingredient.tags.forEach { tag ->
            if (tag !in dietTags) {
                problems += "ingrediente '${ingredient.id}': tag '$tag' no pertenece a ninguna dieta"
            }
        }
        if (ingredient.category !in fallbackCategories) {
            problems += "ingrediente '${ingredient.id}': categoria '${ingredient.category}' sin icono de fallback"
        }
    }

    taxonomy.avatarColors.forEach { color ->
        if (!AVATAR_HEX_PATTERN.matches(color.hex)) {
            problems += "color de avatar '${color.id}': hex '${color.hex}' con formato invalido"
        }
    }

    return problems
}

private fun duplicatedIds(ids: List<String>, collection: String, problems: MutableList<String>) {
    ids.groupingBy { it }.eachCount()
        .filter { it.value > 1 }
        .keys
        .forEach { problems += "$collection: id duplicado '$it'" }
}
