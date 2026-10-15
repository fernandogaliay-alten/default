package com.sazon.app.data.cooking

import com.sazon.app.domain.recipe.RecipeJson
import com.sazon.app.ui.cooking.CookingSession
import com.sazon.app.ui.cooking.CookingTimerState
import com.sazon.app.ui.cooking.CookingTimerStatus

/**
 * Conversion entre la fila unica de Room y la sesion **pura** del Modo cocina.
 *
 * La receta se guarda con [RecipeJson] (la misma configuracion que usa el Lote) y el marcado como CSV
 * de `RecipeStep.order`, con el mismo criterio que el `Saver` aceptado de `CookingModeState`. Las
 * conversiones **pueden** fallar con una fila corrupta (JSON invalido o nombre de estado desconocido):
 * quien las usa (`RoomCookingSessionStore`) envuelve en `runCatching`, de modo que una fila ilegible se
 * comporta como "sin sesion" sin lanzar.
 */
fun CookingSessionEntity.toDomain(): CookingSession = CookingSession(
    recipe = RecipeJson.parse(recipeJson),
    stepIndex = stepIndex,
    completedOrders = parseCompletedOrders(completedOrders),
    timer = CookingTimerState(
        status = CookingTimerStatus.valueOf(timerStatus),
        totalMillis = totalMillis,
        remainingMillis = remainingMillis,
    ),
    endAtElapsedMillis = endAtElapsedMillis,
)

fun CookingSession.toEntity(): CookingSessionEntity = CookingSessionEntity(
    id = CookingSessionEntity.SINGLETON_ID,
    recipeJson = RecipeJson.encode(recipe),
    stepIndex = stepIndex,
    completedOrders = completedOrders.joinToString(separator = ","),
    timerStatus = timer.status.name,
    totalMillis = timer.totalMillis,
    remainingMillis = timer.remainingMillis,
    endAtElapsedMillis = endAtElapsedMillis,
)

/** CSV de `order` marcados; un valor ilegible se descarta (misma tolerancia que el `Saver`). */
private fun parseCompletedOrders(value: String): Set<Int> = value
    .takeIf { it.isNotEmpty() }
    ?.split(",")
    ?.mapNotNull { it.toIntOrNull() }
    ?.toSet()
    .orEmpty()
