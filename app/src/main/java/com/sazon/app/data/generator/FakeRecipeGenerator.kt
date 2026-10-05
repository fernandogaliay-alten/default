package com.sazon.app.data.generator

import com.sazon.app.domain.generator.GenerationConstraints
import com.sazon.app.domain.generator.RecipeGenerator
import com.sazon.app.domain.profile.PreferenceProfile
import com.sazon.app.domain.recipe.MeasureUnit
import com.sazon.app.domain.recipe.Nutrition
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.recipe.RecipeIngredient
import com.sazon.app.domain.recipe.RecipeSource
import com.sazon.app.domain.recipe.RecipeStep

/**
 * Implementacion falsa y determinista del puerto [RecipeGenerator].
 *
 * Devuelve siempre el mismo conjunto fijo de recetas, ignorando por completo su entrada (perfil,
 * consulta, ingredientes y ajustes). No usa `Random`, ni reloj, ni `UUID`, ni ningun estado mutable,
 * y no hace ninguna llamada de red: es codigo de desarrollo para construir y verificar el flujo
 * completo antes de elegir proveedor de IA.
 *
 * El conjunto incluye a proposito recetas que el Validador debe descartar (ingrediente libre "Pan"
 * que activa la restriccion de gluten y un ingrediente de carne con `tags` de la Taxonomia). Este
 * falso **no** filtra: aplicar el perfil y las restricciones es responsabilidad del Validador.
 *
 * Las recetas son `source = AI` y `matchScore = null` (el score es derivado y no persistido). Los
 * `id` son de relleno deterministas y la app debera reasignarlos al persistir.
 */
class FakeRecipeGenerator : RecipeGenerator {

    override suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe> = recetasFijas

    override suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe> = recetasFijas

    override suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe> = recetasFijas
}

/**
 * Conjunto fijo de recetas de desarrollo. Inmutable y sin fuentes de entropia: dos llamadas
 * devuelven instancias iguales por el `equals` estructural de los `data class`.
 *
 * Ids de Taxonomia reales usados donde aplica; "Pan" se deja como ingrediente libre
 * (`ingredientId = null`) para ejercer el caso negativo del Validador.
 */
private val recetasFijas: List<Recipe> = listOf(
    Recipe(
        id = "fake-arroz-verduras",
        source = RecipeSource.AI,
        title = "Arroz salteado con verduras",
        description = "Un arroz rapido con verduras de nevera, ideal para aprovechar sobras.",
        categoryId = "rice",
        mealTypeId = "lunch",
        cuisineId = "asian",
        totalMinutes = 25,
        prepMinutes = 10,
        cookMinutes = 15,
        difficultyId = "easy",
        servings = 2,
        ingredients = listOf(
            RecipeIngredient("Arroz", 200.0, MeasureUnit.GRAM, ingredientId = "arroz"),
            RecipeIngredient("Zanahoria", 1.0, MeasureUnit.UNIT, ingredientId = "zanahoria"),
            RecipeIngredient("Pimiento", 1.0, MeasureUnit.UNIT, ingredientId = "pimiento"),
            RecipeIngredient("Aceite de oliva", 2.0, MeasureUnit.TABLESPOON, ingredientId = "aceite_oliva"),
            RecipeIngredient("Sal", 1.0, MeasureUnit.TEASPOON, isPantry = true, ingredientId = "sal"),
        ),
        steps = listOf(
            RecipeStep(1, "Cocer el arroz", "Cuece el arroz en agua con sal.", 12),
            RecipeStep(2, "Saltear las verduras", "Saltea las verduras y mezcla con el arroz.", 13),
        ),
        tips = listOf("Anade las verduras al final para que queden al dente."),
        nutrition = Nutrition(kcal = 380, protein = 8, carbs = 70, fat = 7),
        declaredAllergens = emptyList(),
        matchScore = null,
    ),
    Recipe(
        id = "fake-lentejas-estofadas",
        source = RecipeSource.AI,
        title = "Lentejas estofadas",
        description = "Un guiso lento y reconfortante de lentejas con verduras.",
        categoryId = "legume_stew",
        mealTypeId = "lunch",
        cuisineId = "spanish",
        totalMinutes = 50,
        prepMinutes = 15,
        cookMinutes = 35,
        difficultyId = "easy",
        servings = 4,
        ingredients = listOf(
            RecipeIngredient("Lentejas", 300.0, MeasureUnit.GRAM, ingredientId = "lenteja"),
            RecipeIngredient("Cebolla", 1.0, MeasureUnit.UNIT, ingredientId = "cebolla"),
            RecipeIngredient("Zanahoria", 2.0, MeasureUnit.UNIT, ingredientId = "zanahoria"),
            RecipeIngredient("Tomate", 2.0, MeasureUnit.UNIT, ingredientId = "tomate"),
            RecipeIngredient("Ajo", 2.0, MeasureUnit.UNIT, ingredientId = "ajo"),
            RecipeIngredient("Aceite de oliva", 2.0, MeasureUnit.TABLESPOON, ingredientId = "aceite_oliva"),
            RecipeIngredient("Sal", 1.0, MeasureUnit.TEASPOON, isPantry = true, ingredientId = "sal"),
        ),
        steps = listOf(
            RecipeStep(1, "Pochar las verduras", "Sofrie cebolla, zanahoria, tomate y ajo.", 15),
            RecipeStep(2, "Cocer el guiso", "Anade las lentejas y agua, y cuece a fuego lento.", 35),
        ),
        tips = listOf("Deja reposar unos minutos antes de servir."),
        nutrition = Nutrition(kcal = 320, protein = 18, carbs = 45, fat = 8),
        declaredAllergens = emptyList(),
        matchScore = null,
    ),
    Recipe(
        id = "fake-pollo-ajillo",
        source = RecipeSource.AI,
        title = "Pollo al ajillo",
        description = "Pollo dorado con ajo y perejil, muy rapido de preparar.",
        categoryId = "poultry",
        mealTypeId = "dinner",
        cuisineId = "spanish",
        totalMinutes = 35,
        prepMinutes = 10,
        cookMinutes = 25,
        difficultyId = "medium",
        servings = 2,
        ingredients = listOf(
            RecipeIngredient("Pollo", 500.0, MeasureUnit.GRAM, ingredientId = "pollo"),
            RecipeIngredient("Ajo", 4.0, MeasureUnit.UNIT, ingredientId = "ajo"),
            RecipeIngredient("Perejil", 1.0, MeasureUnit.TABLESPOON, ingredientId = "perejil"),
            RecipeIngredient("Aceite de oliva", 3.0, MeasureUnit.TABLESPOON, ingredientId = "aceite_oliva"),
            RecipeIngredient("Sal", 1.0, MeasureUnit.TEASPOON, isPantry = true, ingredientId = "sal"),
        ),
        steps = listOf(
            RecipeStep(1, "Dorar el pollo", "Dora el pollo por todos lados.", 15),
            RecipeStep(2, "Anadir el ajo", "Incorpora el ajo laminado y el perejil.", 10),
        ),
        tips = listOf("No muevas demasiado el pollo al principio para que se dore."),
        nutrition = Nutrition(kcal = 520, protein = 42, carbs = 6, fat = 34),
        declaredAllergens = emptyList(),
        matchScore = null,
    ),
    Recipe(
        id = "fake-bocadillo-tomate",
        source = RecipeSource.AI,
        title = "Bocadillo de tomate",
        description = "Un bocadillo sencillo de tomate y aceite de oliva.",
        categoryId = "sandwich_wrap",
        mealTypeId = "breakfast",
        cuisineId = "spanish",
        totalMinutes = 5,
        prepMinutes = 5,
        cookMinutes = 0,
        difficultyId = "easy",
        servings = 1,
        ingredients = listOf(
            // Disparador de Restriccion dura: ingrediente libre "Pan"; el Validador lo casara por
            // las keywords de gluten (la Taxonomia incluye "pan").
            RecipeIngredient("Pan", 1.0, unit = null, ingredientId = null),
            RecipeIngredient("Tomate", 1.0, MeasureUnit.UNIT, ingredientId = "tomate"),
            RecipeIngredient("Aceite de oliva", 1.0, MeasureUnit.TABLESPOON, ingredientId = "aceite_oliva"),
            RecipeIngredient("Sal", 1.0, MeasureUnit.TEASPOON, isPantry = true, ingredientId = "sal"),
        ),
        steps = listOf(
            RecipeStep(1, "Montar el bocadillo", "Unta el tomate y aliña con aceite y sal.", 5),
        ),
        tips = emptyList(),
        nutrition = null,
        declaredAllergens = listOf("gluten"),
        matchScore = null,
    ),
)
