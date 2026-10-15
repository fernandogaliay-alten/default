package com.sazon.app.data.cooking

import com.sazon.app.ui.theme.AppResourceFiles
import com.sazon.app.ui.theme.androidAttr
import com.sazon.app.ui.theme.children
import com.sazon.app.ui.theme.elements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El **manifiesto** declara el servicio en primer plano y sus permisos
 * (`docs/specs/cooking-timer-foreground-service.md`, escenario 10), sin tocar el receptor ni los
 * permisos ya aceptados del aviso de fin.
 *
 * Test **JVM puro** (sin Robolectric): lee el manifiesto real con `AppResourceFiles`
 * (`android.manifest.path`, publicado por `app/build.gradle.kts`; **no** se crea ninguna infraestructura
 * nueva). El caso negativo muta el XML y exige que la comprobacion devuelva problemas, de modo que el
 * test no puede pasar en vacio (patron de `LauncherIconTest`/`SplashThemeTest`).
 *
 * Que la app **arranca y funciona sin ningun temporizador en marcha** (ningun servicio vivo, ninguna
 * notificacion) no es automatizable aqui: lo revisa la persona en el emulador (AGENTS.md).
 */
class CookingTimerServiceManifestTest {

    private val manifest: String get() = AppResourceFiles.manifestFile.readText()

    private val service: org.w3c.dom.Element
        get() = AppResourceFiles.parse(AppResourceFiles.manifestFile)
            .elements("service")
            .first { it.androidAttr("name") == SERVICIO }

    @Test
    fun el_manifiesto_declara_los_permisos_del_servicio_en_primer_plano() {
        val permisos = AppResourceFiles.parse(AppResourceFiles.manifestFile)
            .elements("uses-permission")
            .mapNotNull { it.androidAttr("name") }

        assertTrue(
            "Falta FOREGROUND_SERVICE (obligatorio para startForeground)",
            permisos.contains("android.permission.FOREGROUND_SERVICE"),
        )
        assertTrue(
            "Falta FOREGROUND_SERVICE_SPECIAL_USE (obligatorio para el tipo specialUse en API 34+)",
            permisos.contains("android.permission.FOREGROUND_SERVICE_SPECIAL_USE"),
        )
    }

    @Test
    fun el_servicio_se_declara_una_sola_vez_con_su_nombre_su_tipo_y_sin_exportar() {
        val servicios = AppResourceFiles.parse(AppResourceFiles.manifestFile).elements("service")

        assertEquals("Un unico <service> en el manifiesto", 1, servicios.size)
        assertEquals(SERVICIO, servicios.single().androidAttr("name"))
        assertEquals("false", servicios.single().androidAttr("exported"))
        assertEquals("specialUse", servicios.single().androidAttr("foregroundServiceType"))
    }

    @Test
    fun el_servicio_documenta_el_subtipo_de_special_use() {
        val properties = service.children("property")

        assertEquals("Un unico <property> del subtipo", 1, properties.size)
        assertEquals(
            "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE",
            properties.single().androidAttr("name"),
        )
        assertEquals(
            "Temporizador de cocina en curso",
            properties.single().androidAttr("value"),
        )
    }

    @Test
    fun el_receptor_y_los_permisos_aceptados_del_aviso_siguen_ahi() {
        val document = AppResourceFiles.parse(AppResourceFiles.manifestFile)
        val receptor = document.elements("receiver").single()
        val permisos = document.elements("uses-permission").mapNotNull { it.androidAttr("name") }

        assertEquals(".data.cooking.CookingAlarmReceiver", receptor.androidAttr("name"))
        assertEquals("El receptor sigue sin exportarse", "false", receptor.androidAttr("exported"))
        assertTrue("POST_NOTIFICATIONS sigue declarado", permisos.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue("USE_EXACT_ALARM sigue declarado", permisos.contains("android.permission.USE_EXACT_ALARM"))
        assertTrue("VIBRATE sigue declarado", permisos.contains("android.permission.VIBRATE"))
        assertTrue(
            "SCHEDULE_EXACT_ALARM sigue declarado con su tope de SDK",
            document.elements("uses-permission").any {
                it.androidAttr("name") == "android.permission.SCHEDULE_EXACT_ALARM" &&
                    it.androidAttr("maxSdkVersion") == "32"
            },
        )
    }

    @Test
    fun la_comprobacion_detecta_un_servicio_mal_declarado() {
        val roto = manifest
            .replace("android:foregroundServiceType=\"specialUse\"", "android:foregroundServiceType=\"shortService\"")
            .replace("android:name=\"android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE\"", "android:name=\"android.app.OTRA\"")
            .replace("android:exported=\"false\"", "android:exported=\"true\"", ignoreCase = false)

        val problemas = serviceProblems(roto)

        assertTrue("El caso negativo no puede pasar en vacio: $problemas", problemas.isNotEmpty())
        assertTrue(
            "Se detecta el tipo de primer plano equivocado",
            problemas.any { it.contains("foregroundServiceType") },
        )
        assertTrue(
            "Se detecta el subtipo equivocado",
            problemas.any { it.contains("PROPERTY_SPECIAL_USE_FGS_SUBTYPE") },
        )
    }

    @Test
    fun el_manifiesto_real_no_tiene_ningun_problema() {
        assertTrue(
            "El manifiesto declara el servicio correctamente",
            serviceProblems(manifest).isEmpty(),
        )
    }

    private companion object {
        const val SERVICIO = ".data.cooking.CookingTimerService"
    }
}

/**
 * Derivas del `<service>` del temporizador en un manifiesto **como texto** (para poder comprobar el
 * caso negativo con una copia mutada). Devuelve la lista de problemas; vacia significa correcto.
 */
private fun serviceProblems(xml: String): List<String> {
    val problems = mutableListOf<String>()
    val servicios = AppResourceFiles.parseText(xml).elements("service")
    if (servicios.size != 1) {
        problems += "Se esperaba un unico <service>, hay ${servicios.size}"
    }
    val servicio = servicios.firstOrNull { it.androidAttr("name") == ".data.cooking.CookingTimerService" }
    if (servicio == null) {
        problems += "Falta el <service> .data.cooking.CookingTimerService"
        return problems
    }
    if (servicio.androidAttr("exported") != "false") {
        problems += "android:exported debe ser \"false\" (el servicio solo lo arranca la app)"
    }
    if (servicio.androidAttr("foregroundServiceType") != "specialUse") {
        problems += "android:foregroundServiceType debe ser \"specialUse\""
    }
    val subtipo = servicio.children("property").firstOrNull()?.androidAttr("name")
    if (subtipo != "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE") {
        problems += "Falta el <property> PROPERTY_SPECIAL_USE_FGS_SUBTYPE (subtipo de specialUse)"
    }
    return problems
}
