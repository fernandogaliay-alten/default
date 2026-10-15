package com.sazon.app.ui.theme

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Acceso a los **recursos Android reales** desde los tests JVM del icono y del splash
 * (`LauncherIconTest`, `SplashThemeTest`).
 *
 * Los tests viven en el paquete de `DesignFrontmatter` y reutilizan su parser de `DESIGN.md`, pero a
 * diferencia de `DesignTokensTest` no comparan tokens de Compose sino **ficheros**: el manifiesto
 * (`android.manifest.path`) y el arbol `app/src/main/res/` (`android.res.dir`), publicados como
 * propiedades de sistema por `app/build.gradle.kts`. Nunca se resuelven rutas relativas a `user.dir`.
 *
 * El parseo XML usa `javax.xml.parsers` del **JDK**: en un test JVM sin Robolectric las clases de
 * `android.*` son stubs del `android.jar` (incluido `android.util.Xml`), asi que no se pueden usar.
 */
internal object AppResourceFiles {

    /** Ruta raiz de `app/src/main/res/`. */
    val resDir: File get() = propertyFile("android.res.dir")

    /** Ruta de `app/src/main/AndroidManifest.xml`. */
    val manifestFile: File get() = propertyFile("android.manifest.path")

    fun res(relativePath: String): File = File(resDir, relativePath)

    fun exists(relativePath: String): Boolean = res(relativePath).isFile

    fun text(relativePath: String): String {
        val file = res(relativePath)
        check(file.isFile) { "No existe el recurso $relativePath (${file.path})" }
        return file.readText()
    }

    fun parse(file: File): Document {
        check(file.isFile) { "No existe el fichero ${file.path}" }
        return parseText(file.readText())
    }

    fun parse(relativePath: String): Document = parse(res(relativePath))

    /** Parsea XML desde texto (util para comprobar copias mutadas en los casos negativos). */
    fun parseText(xml: String): Document =
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newDocumentBuilder()
            .parse(xml.byteInputStream(Charsets.UTF_8))

    /** Ficheros de `res/` con la extension dada (p. ej. `"webp"`), en orden estable. */
    fun files(extension: String): List<File> =
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals(extension, ignoreCase = true) }
            .sortedBy { it.path }
            .toList()

    /** Ficheros `.xml` (texto) de `res/`, en orden estable. */
    fun xmlFiles(): List<File> = files("xml")

    /** Ruta relativa a `res/` con separador `/`, para mensajes de error legibles. */
    fun relativePath(file: File): String =
        resDir.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')

    /** Mapa `nombre -> hex` de un `colors.xml` (hex en mayusculas para comparar). */
    fun colorResourceMap(relativePath: String): Map<String, String> =
        parse(relativePath).elements("color").associate { element ->
            element.getAttribute("name") to element.textContent.trim().uppercase()
        }

    /** Un `<style>` con su `parent` y sus `item` (`name` -> valor). */
    data class AndroidStyle(val name: String, val parent: String?, val items: Map<String, String>)

    fun style(relativePath: String, styleName: String): AndroidStyle {
        val document = parse(relativePath)
        val element = document.elements("style").firstOrNull { it.getAttribute("name") == styleName }
            ?: error("$relativePath no declara el estilo $styleName")
        val parent = element.getAttribute("parent").takeIf { it.isNotEmpty() }
        val items = element.elements("item").associate { it.getAttribute("name") to it.textContent.trim() }
        return AndroidStyle(styleName, parent, items)
    }

    private fun propertyFile(name: String): File {
        val path = checkNotNull(System.getProperty(name)) { "Falta la propiedad de sistema $name" }
        return File(path)
    }
}

/** Elementos hijos directos con el tag dado. */
internal fun Element.children(tag: String): List<Element> {
    val nodes = childNodes
    return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.filter { it.tagName == tag }
}

/** Todos los elementos con el tag dado (descendientes incluidos), desde un documento. */
internal fun Document.elements(tag: String): List<Element> = toElements(getElementsByTagName(tag))

/** Todos los elementos con el tag dado (descendientes incluidos), desde un elemento. */
internal fun Element.elements(tag: String): List<Element> = toElements(getElementsByTagName(tag))

private fun toElements(nodes: org.w3c.dom.NodeList): List<Element> =
    (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }

/** Atributo del namespace `android:` (los ficheros escriben siempre el prefijo). */
internal fun Element.androidAttr(name: String): String? =
    getAttribute("android:$name").takeIf { it.isNotEmpty() }

/** Pares `atributo -> valor` de color (`fillColor`/`strokeColor`) de todo el documento. */
internal fun Document.colorAttributes(): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val nodes = getElementsByTagName("*")
    for (i in 0 until nodes.length) {
        val element = nodes.item(i) as? Element ?: continue
        val attributes = element.attributes
        for (j in 0 until attributes.length) {
            val attribute = attributes.item(j)
            when (attribute.nodeName) {
                "android:fillColor", "android:strokeColor" ->
                    result += attribute.nodeName.removePrefix("android:") to attribute.nodeValue.trim()
            }
        }
    }
    return result
}

/** Vectores que participan en el contrato de color del icono y del splash. */
internal val ICON_VECTORS = listOf(
    "drawable/ic_launcher_background.xml",
    "drawable/ic_launcher_foreground.xml",
    "drawable/ic_launcher_monochrome.xml",
    "drawable/ic_splash_mark.xml",
)

internal const val LAUNCHER_FOREGROUND_VECTOR = "drawable/ic_launcher_foreground.xml"
internal const val MONOCHROME_VECTOR = "drawable/ic_launcher_monochrome.xml"

/** Silueta de la capa monocroma: el sistema la tine por alfa, asi que su negro es la unica excepcion. */
internal const val MONOCHROME_ONLY_COLOR = "#000000"

/**
 * Derivas de color del icono contra `DESIGN.md` y contra `values/`/`values-night/`.
 *
 * Recibe los datos **ya leidos** (frontmatter, mapas de color y vectores) para que los casos negativos
 * puedan mutar una copia y exigir que la comprobacion devuelva >= 1 problema: el test no puede pasar
 * en vacio (patron de `DesignTokensTest`/`TaxonomyIntegrityTest`).
 */
internal fun launcherColorProblems(
    designColors: Map<String, String>,
    lightColors: Map<String, String>,
    nightColors: Map<String, String>,
    vectors: Map<String, String>,
): List<String> {
    val problems = mutableListOf<String>()
    val palette = designColors.values.map { it.uppercase() }.toSet()

    fun checkToken(resource: String, token: String, actual: String?) {
        val expected = designColors[token]?.uppercase()
        if (expected == null) {
            problems += "DESIGN.md no declara colors.$token"
        } else if (actual?.uppercase() != expected) {
            problems += "$resource: DESIGN.md(colors.$token)=$expected repo=${actual?.uppercase() ?: "ausente"}"
        }
    }

    checkToken("sazon_launcher_background (values)", "background", lightColors["sazon_launcher_background"])
    checkToken(
        "sazon_launcher_background (values-night)",
        "darkBackground",
        nightColors["sazon_launcher_background"],
    )
    checkToken("sazon_launcher_motif", "primary", lightColors["sazon_launcher_motif"])
    checkToken("sazon_launcher_accent", "accent", lightColors["sazon_launcher_accent"])

    vectors.forEach { (path, xml) ->
        AppResourceFiles.parseText(xml).colorAttributes().forEach { (attribute, value) ->
            val resolved = when {
                value.startsWith("@color/") -> {
                    val name = value.removePrefix("@color/")
                    val light = lightColors[name]?.uppercase()
                    if (light == null) {
                        problems += "$path: $attribute apunta a $value, que no existe en values/colors.xml"
                    }
                    nightColors[name]?.uppercase()?.let { night ->
                        if (night !in palette) {
                            problems += "$path: $attribute en values-night/colors.xml usa $night, fuera de DESIGN.md"
                        }
                    }
                    light
                }
                value.startsWith("#") -> value.uppercase()
                else -> {
                    problems += "$path: $attribute usa $value, que no es un color resoluble"
                    null
                }
            }
            if (resolved == null) return@forEach
            val isMonochromeException = resolved == MONOCHROME_ONLY_COLOR
            if (resolved !in palette && !(isMonochromeException && path == MONOCHROME_VECTOR)) {
                problems += "$path: $attribute=$resolved no es un color declarado en DESIGN.md"
            }
            if (isMonochromeException && path != MONOCHROME_VECTOR) {
                problems += "$path: $attribute usa $MONOCHROME_ONLY_COLOR, reservado a $MONOCHROME_VECTOR"
            }
        }
    }
    return problems
}

/** El `foreground` debe cubrir los dos roles del motivo: trazo (`motif`) y vaho (`accent`). */
internal fun launcherRoleProblems(vectors: Map<String, String>): List<String> {
    val foreground = vectors[LAUNCHER_FOREGROUND_VECTOR]
        ?: return listOf("Falta el vector $LAUNCHER_FOREGROUND_VECTOR")
    val problems = mutableListOf<String>()
    if ("@color/sazon_launcher_motif" !in foreground) {
        problems += "$LAUNCHER_FOREGROUND_VECTOR no usa el motivo (@color/sazon_launcher_motif)"
    }
    if ("@color/sazon_launcher_accent" !in foreground) {
        problems += "$LAUNCHER_FOREGROUND_VECTOR no usa el acento (@color/sazon_launcher_accent)"
    }
    return problems
}

/** Vector de la marca del splash (el icono de splash del tema de plataforma). */
internal const val SPLASH_MARK_VECTOR = "drawable/ic_splash_mark.xml"

/**
 * Fraccion del canvas de un vector que el sistema deja **visible** en el icono de splash.
 *
 * En API 31+ `SplashscreenIconDrawableFactory` envuelve un vector plano (no adaptativo y no animable) en
 * `AdaptiveForegroundDrawable`, que dibuja al hijo en **1.5x** sus bounds: solo se ve el **2/3 central**
 * del canvas. `androidx.core:core-splashscreen` aplica la misma regla (`MASK_FACTOR = 2/3f`, el icono de
 * 288 dp se recorta a un circulo de 192 dp). No depende de `mNoBackgroundScale`/`mIconSize`.
 */
internal const val SPLASH_VISIBLE_FRACTION = 2.0 / 3.0

/** Punto en coordenadas del viewport del vector. */
private data class VectorPoint(val x: Double, val y: Double)

/** Un `<path>` medido: sus puntos ya transformados y el semiancho de trazo que hay que sumarles. */
private data class PathGeometry(val label: String, val points: List<VectorPoint>, val halfStroke: Double) {
    /** Radio maximo que alcanza el trazo del path respecto al centro del canvas. */
    fun maxRadius(centre: VectorPoint): Double =
        points.maxOf { hypot(it.x - centre.x, it.y - centre.y) + halfStroke }
}

/**
 * Transformacion acumulada de los `<group>` de un vector: `p -> scale * p + offset`.
 *
 * El pivote y la traslacion de cada grupo se pliegan en el offset al componer (el hijo se dibuja en el
 * espacio del padre), asi que no hace falta simular la pila de canvas. La **rotacion** no se soporta y
 * `splashMarkGeometryProblems` la declara como problema en vez de medir mal en silencio.
 */
private data class VectorTransform(
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
) {
    fun apply(point: VectorPoint): VectorPoint = VectorPoint(scaleX * point.x + offsetX, scaleY * point.y + offsetY)

    /** Compone este transform (padre) con el local de un `<group>` hijo. */
    fun compose(
        scaleX: Double,
        scaleY: Double,
        pivotX: Double,
        pivotY: Double,
        translateX: Double,
        translateY: Double,
    ): VectorTransform = VectorTransform(
        scaleX = this.scaleX * scaleX,
        scaleY = this.scaleY * scaleY,
        offsetX = this.scaleX * (pivotX * (1 - scaleX) + translateX) + offsetX,
        offsetY = this.scaleY * (pivotY * (1 - scaleY) + translateY) + offsetY,
    )

    /**
     * Semiancho de trazo **conservador**: un `<group>` que reduce la escala puede no reducir el trazo
     * (depende de la implementacion de `VectorDrawable`), asi que nunca se aplica un factor < 1.
     */
    fun halfStroke(strokeWidth: Double): Double =
        strokeWidth / 2 * maxOf(1.0, abs(scaleX), abs(scaleY))
}

/**
 * Geometria del icono de splash dentro del circulo visible de la plataforma.
 *
 * Comprueba que **todos** los puntos que acotan el trazado (extremos, puntos de control y muestras de los
 * arcos) mas el semiancho de trazo caen dentro del circulo de **2/3** del canvas (`SPLASH_VISIBLE_FRACTION`
 * respecto al centro): fuera de el la mascara del sistema recorta la marca. Recibe el XML **como texto**
 * para que el caso negativo pueda pasar una geometria desbordada y exigir >= 1 problema.
 */
internal fun splashMarkGeometryProblems(xml: String): List<String> {
    val problems = mutableListOf<String>()
    val document = AppResourceFiles.parseText(xml)
    val root = document.documentElement
    val viewportWidth = root.androidAttr("viewportWidth")?.toDoubleOrNull()
    val viewportHeight = root.androidAttr("viewportHeight")?.toDoubleOrNull()
    if (viewportWidth == null || viewportHeight == null) {
        return listOf("El vector no declara viewportWidth/viewportHeight: no se puede medir la geometria")
    }

    val centre = VectorPoint(viewportWidth / 2, viewportHeight / 2)
    val visibleRadius = minOf(viewportWidth, viewportHeight) / 2 * SPLASH_VISIBLE_FRACTION

    val paths = mutableListOf<PathGeometry>()
    collectPathGeometry(root, VectorTransform(), "", paths, problems)
    if (paths.isEmpty()) {
        problems += "El vector no tiene ningun <path> medible: la comprobacion quedaria vacia"
        return problems
    }

    paths.forEach { geometry ->
        val radius = geometry.maxRadius(centre)
        if (radius > visibleRadius) {
            problems += "${geometry.label}: el trazo llega a radio ${radius.round2()} > " +
                "${visibleRadius.round2()} (circulo visible de 2/3 del canvas)"
        }
    }
    return problems
}

private fun collectPathGeometry(
    element: Element,
    transform: VectorTransform,
    label: String,
    out: MutableList<PathGeometry>,
    problems: MutableList<String>,
) {
    element.children("group").forEachIndexed { index, group ->
        val childLabel = if (label.isEmpty()) "group[$index]" else "$label/group[$index]"
        val rotation = group.androidAttr("rotation")?.toDoubleOrNull() ?: 0.0
        if (rotation != 0.0) {
            problems += "$childLabel: usa android:rotation=$rotation, que la comprobacion no soporta"
        }
        collectPathGeometry(group, transform.compose(
            scaleX = group.numberAttr("scaleX", 1.0),
            scaleY = group.numberAttr("scaleY", 1.0),
            pivotX = group.numberAttr("pivotX", 0.0),
            pivotY = group.numberAttr("pivotY", 0.0),
            translateX = group.numberAttr("translateX", 0.0),
            translateY = group.numberAttr("translateY", 0.0),
        ), childLabel, out, problems)
    }

    element.children("path").forEachIndexed { index, path ->
        val pathLabel = if (label.isEmpty()) "path[$index]" else "$label/path[$index]"
        val pathData = path.androidAttr("pathData")
        if (pathData.isNullOrBlank()) {
            problems += "$pathLabel: sin android:pathData"
            return@forEachIndexed
        }
        val points = try {
            pathPoints(pathData)
        } catch (error: IllegalArgumentException) {
            problems += "$pathLabel: no se pudo interpretar android:pathData ($pathData): ${error.message}"
            return@forEachIndexed
        }
        out += PathGeometry(
            label = pathLabel,
            points = points.map { transform.apply(it) },
            halfStroke = transform.halfStroke(path.numberAttr("strokeWidth", 0.0)),
        )
    }
}

private fun Element.numberAttr(name: String, default: Double): Double =
    androidAttr(name)?.toDoubleOrNull() ?: default

/** Resuelve (x, y) de un comando como absoluto o relativo al cursor. */
private fun nextPoint(x: Double, y: Double, cursor: VectorPoint, relative: Boolean): VectorPoint =
    if (relative) VectorPoint(cursor.x + x, cursor.y + y) else VectorPoint(x, y)

/** Numero de argumentos de cada comando de `android:pathData` (en mayusculas). */
private val PATH_ARITY = mapOf(
    'M' to 2, 'L' to 2, 'H' to 1, 'V' to 1, 'C' to 6, 'S' to 4, 'Q' to 4, 'T' to 2, 'A' to 7,
)

private val PATH_TOKEN = Regex("([MmLlHhVvCcSsQqTtAaZz])|(-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)")

/**
 * Puntos que acotan (y muestrean) el trazado de un `android:pathData`.
 *
 * Se soportan los comandos usados en los vectores del icono (`M`/`m`, `h`/`H`, `a`/`A`, `c`/`C`) y el
 * resto de la familia de curvas: los puntos **de control** acotan la curva (propiedad de la envolvente
 * convexa) y los **arcos** se muestrean porque su abombamiento no lo cubren los extremos.
 */
private fun pathPoints(pathData: String): List<VectorPoint> {
    val tokens = PATH_TOKEN.findAll(pathData).map { it.value }.toList()
    val points = mutableListOf<VectorPoint>()
    var cursor = VectorPoint(0.0, 0.0)
    var subpathStart = cursor
    var index = 0
    var command: Char? = null

    while (index < tokens.size) {
        val token = tokens[index]
        if (token.length == 1 && token[0].isLetter()) {
            command = token[0]
            index++
            if (command == 'Z' || command == 'z') {
                cursor = subpathStart
                command = null
                continue
            }
        } else if (command == null) {
            throw IllegalArgumentException("numero '$token' sin comando previo")
        }

        val current = command ?: throw IllegalArgumentException("comando vacio")
        val arity = PATH_ARITY[current.uppercaseChar()]
            ?: throw IllegalArgumentException("comando '$current' no soportado")
        val slice = tokens.subList(index, minOf(index + arity, tokens.size))
        if (slice.size != arity) {
            throw IllegalArgumentException("faltan numeros para '$current' ($pathData)")
        }
        index += arity
        val numbers = slice.map { it.toDouble() }
        val relative = current.isLowerCase()

        when (current.uppercaseChar()) {
            'M' -> {
                cursor = nextPoint(numbers[0], numbers[1], cursor, relative)
                subpathStart = cursor
                points += cursor
            }
            'L' -> {
                cursor = nextPoint(numbers[0], numbers[1], cursor, relative)
                points += cursor
            }
            'H' -> {
                val x = if (relative) cursor.x + numbers[0] else numbers[0]
                cursor = VectorPoint(x, cursor.y)
                points += cursor
            }
            'V' -> {
                val y = if (relative) cursor.y + numbers[0] else numbers[0]
                cursor = VectorPoint(cursor.x, y)
                points += cursor
            }
            'C' -> {
                val first = nextPoint(numbers[0], numbers[1], cursor, relative)
                val second = nextPoint(numbers[2], numbers[3], cursor, relative)
                val end = nextPoint(numbers[4], numbers[5], cursor, relative)
                points += listOf(first, second, end)
                cursor = end
            }
            'S', 'Q' -> {
                val control = nextPoint(numbers[0], numbers[1], cursor, relative)
                val end = nextPoint(numbers[2], numbers[3], cursor, relative)
                points += listOf(control, end)
                cursor = end
            }
            'T' -> {
                cursor = nextPoint(numbers[0], numbers[1], cursor, relative)
                points += cursor
            }
            'A' -> {
                val end = nextPoint(numbers[5], numbers[6], cursor, relative)
                if (numbers[2] != 0.0) {
                    throw IllegalArgumentException("arco con rotation=${numbers[2]} no soportado")
                }
                points += arcPoints(
                    from = cursor,
                    rx = numbers[0],
                    ry = numbers[1],
                    largeArc = numbers[3] != 0.0,
                    sweep = numbers[4] != 0.0,
                    to = end,
                )
                cursor = end
            }
        }
        if (current == 'M') command = 'L' else if (current == 'm') command = 'l'
    }
    return points
}

/** Muestras de un arco eliptico de SVG (sin rotacion de ejes) con la parametrizacion de centro. */
private fun arcPoints(
    from: VectorPoint,
    rx: Double,
    ry: Double,
    largeArc: Boolean,
    sweep: Boolean,
    to: VectorPoint,
): List<VectorPoint> {
    if (rx == 0.0 || ry == 0.0 || (from.x == to.x && from.y == to.y)) return listOf(from, to)

    val radiusX = abs(rx)
    val radiusY = abs(ry)
    val halfChordX = (from.x - to.x) / 2
    val halfChordY = (from.y - to.y) / 2
    val lambda = halfChordX * halfChordX / (radiusX * radiusX) + halfChordY * halfChordY / (radiusY * radiusY)
    val scale = if (lambda > 1) sqrt(lambda) else 1.0
    val scaledX = radiusX * scale
    val scaledY = radiusY * scale
    val numerator = scaledX * scaledX * scaledY * scaledY -
        scaledX * scaledX * halfChordY * halfChordY - scaledY * scaledY * halfChordX * halfChordX
    val denominator = scaledX * scaledX * halfChordY * halfChordY + scaledY * scaledY * halfChordX * halfChordX
    val factor = (if (largeArc != sweep) 1.0 else -1.0) * sqrt((numerator / denominator).coerceAtLeast(0.0))
    val centreX = factor * scaledX * halfChordY / scaledY + (from.x + to.x) / 2
    val centreY = -factor * scaledY * halfChordX / scaledX + (from.y + to.y) / 2

    val startAngle = atan2((from.y - centreY) / scaledY, (from.x - centreX) / scaledX)
    val endAngle = atan2((to.y - centreY) / scaledY, (to.x - centreX) / scaledX)
    var sweepAngle = endAngle - startAngle
    if (sweep && sweepAngle < 0) sweepAngle += 2 * PI
    if (!sweep && sweepAngle > 0) sweepAngle -= 2 * PI
    if (largeArc && abs(sweepAngle) < PI - 1e-9) sweepAngle += if (sweepAngle < 0) -2 * PI else 2 * PI

    val samples = (0..ARC_SAMPLES).map { step ->
        val angle = startAngle + sweepAngle * step / ARC_SAMPLES
        VectorPoint(centreX + scaledX * cos(angle), centreY + scaledY * sin(angle))
    }
    require(hypot(samples.first().x - from.x, samples.first().y - from.y) < 1e-6) {
        "arco no interpretable (el inicio no coincide)"
    }
    require(hypot(samples.last().x - to.x, samples.last().y - to.y) < 1e-6) {
        "arco no interpretable (el final no coincide)"
    }
    return samples
}

private const val ARC_SAMPLES = 90

private fun Double.round2(): String = ((this * 100).roundToInt() / 100.0).toString()
