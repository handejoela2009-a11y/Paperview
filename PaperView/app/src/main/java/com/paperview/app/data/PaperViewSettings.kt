package com.paperview.app.data

/**
 * Todos los parámetros visuales que PaperView puede ajustar dentro de lo que
 * Android realmente permite mediante una ventana overlay (SYSTEM_ALERT_WINDOW):
 * un tintado de color con blend mode + alfa. No existe una API pública en
 * Android para aplicar una matriz de color a toda la pantalla sin permisos de
 * sistema/root, así que todo lo de abajo se traduce, en FilterEngine, a un
 * color ARGB + un modo de mezcla (ver filter/FilterEngine.kt para el porqué).
 *
 * Rangos: todos los valores están normalizados 0f..1f salvo que se indique
 * lo contrario, para que la UI (sliders) y el motor de filtro compartan una
 * única fuente de verdad.
 */
data class PaperViewSettings(
    val id: String = "papel_natural",
    val displayName: String = "Papel Natural",

    // 0 = frío/neutro, 1 = muy cálido (reduce componente azul progresivamente)
    val temperature: Float = 0.35f,

    // 0 = sin reducción, 1 = reducción máxima de componente azul disponible
    val blueReduction: Float = 0.30f,

    // 0 = blanco y negro, 0.5 = natural, 1 = saturación completa
    val saturation: Float = 0.42f,

    // 0 = contraste muy bajo (plano), 0.5 = natural, 1 = alto contraste
    val contrast: Float = 0.5f,

    // 0 = filtro casi imperceptible, 1 = máxima intensidad de todo lo anterior
    val intensity: Float = 0.55f,

    // 0 = sin oscurecer, 1 = oscurecimiento visual máximo (no es brillo real)
    val overlayDimming: Float = 0.0f,

    // Tono de fondo simulado cuando la app anfitriona tiene fondos claros:
    // 0 = blanco puro, 1 = crema intenso. Solo se usa como referencia visual
    // en la calibración; el overlay no puede repintar el fondo real de otras apps.
    val backgroundWarmth: Float = 0.3f,

    val autoAdaptationEnabled: Boolean = true,
    val transitionSpeedMs: Long = 1500L,
)

/** Catálogo de perfiles predefinidos descritos en las secciones 5-9 y 22-24. */
object PaperViewPresets {

    val PAPEL_NATURAL = PaperViewSettings(
        id = "papel_natural",
        displayName = "Papel Natural",
        temperature = 0.35f,
        blueReduction = 0.30f,
        saturation = 0.42f,
        contrast = 0.5f,
        intensity = 0.5f,
        overlayDimming = 0.0f,
        backgroundWarmth = 0.3f,
    )

    val PAPEL_BLANCO = PaperViewSettings(
        id = "papel_blanco",
        displayName = "Papel Blanco",
        temperature = 0.2f,
        blueReduction = 0.2f,
        saturation = 0.5f,
        contrast = 0.5f,
        intensity = 0.35f,
        overlayDimming = 0.0f,
        backgroundWarmth = 0.15f,
    )

    val PAPEL_CREMA = PaperViewSettings(
        id = "papel_crema",
        displayName = "Papel Crema",
        temperature = 0.45f,
        blueReduction = 0.35f,
        saturation = 0.4f,
        contrast = 0.48f,
        intensity = 0.55f,
        overlayDimming = 0.0f,
        backgroundWarmth = 0.5f,
    )

    val LIBRO = PaperViewSettings(
        id = "libro",
        displayName = "Libro",
        temperature = 0.4f,
        blueReduction = 0.3f,
        saturation = 0.3f,
        contrast = 0.45f,
        intensity = 0.5f,
        overlayDimming = 0.05f,
        backgroundWarmth = 0.4f,
    )

    val EINK_SIMULADO = PaperViewSettings(
        id = "eink_simulado",
        displayName = "E-Ink Simulado",
        temperature = 0.3f,
        blueReduction = 0.25f,
        saturation = 0.15f,
        contrast = 0.4f,
        intensity = 0.6f,
        overlayDimming = 0.0f,
        backgroundWarmth = 0.35f,
    )

    val LECTURA = PaperViewSettings(
        id = "lectura",
        displayName = "Lectura",
        temperature = 0.35f,
        blueReduction = 0.3f,
        saturation = 0.45f,
        contrast = 0.55f,
        intensity = 0.45f,
        overlayDimming = 0.0f,
        backgroundWarmth = 0.35f,
    )

    val ESTUDIO = PaperViewSettings(
        id = "estudio",
        displayName = "Estudio",
        temperature = 0.25f,
        blueReduction = 0.2f,
        saturation = 0.55f,
        contrast = 0.55f,
        intensity = 0.3f,
        overlayDimming = 0.0f,
        backgroundWarmth = 0.2f,
    )

    val NOCHE = PaperViewSettings(
        id = "noche",
        displayName = "Noche",
        temperature = 0.75f,
        blueReduction = 0.65f,
        saturation = 0.25f,
        contrast = 0.4f,
        intensity = 0.7f,
        overlayDimming = 0.35f,
        backgroundWarmth = 0.6f,
        transitionSpeedMs = 4000L,
    )

    val ALL = listOf(
        PAPEL_NATURAL, PAPEL_BLANCO, PAPEL_CREMA, LIBRO,
        EINK_SIMULADO, LECTURA, ESTUDIO, NOCHE,
    )

    fun byId(id: String): PaperViewSettings = ALL.find { it.id == id } ?: PAPEL_NATURAL
}
