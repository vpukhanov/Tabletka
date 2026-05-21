package ru.pukhanov.tabletka.util

/**
 * Formats a dose quantity (e.g. 1.0 -> "1 dose", 2.0 -> "2 doses", 1.5 -> "1.5 doses").
 */
fun formatDoses(doses: Double): String {
    val dosesText = if (doses % 1.0 == 0.0) {
        doses.toInt().toString()
    } else {
        doses.toString()
    }
    return "$dosesText ${if (doses == 1.0) "dose" else "doses"}"
}

/**
 * Formats the full description of a medication with name, dosage, and dose text.
 */
fun formatMedicationDescription(
    title: String,
    brandName: String?,
    dosage: String?,
    doses: Double
): String {
    val displayName = brandName?.takeIf { it.isNotBlank() } ?: title
    val dosageText = if (!dosage.isNullOrBlank()) " • $dosage" else ""
    val dosesText = formatDoses(doses)
    return "$displayName$dosageText × $dosesText"
}
