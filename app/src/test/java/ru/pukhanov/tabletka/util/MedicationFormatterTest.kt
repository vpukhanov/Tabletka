package ru.pukhanov.tabletka.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationFormatterTest {

    @Test
    fun formatDoses_formatsCorrectly() {
        assertEquals("1 dose", formatDoses(1.0))
        assertEquals("2 doses", formatDoses(2.0))
        assertEquals("1.5 doses", formatDoses(1.5))
        assertEquals("0.5 doses", formatDoses(0.5))
    }

    @Test
    fun formatMedicationDescription_usesBrandNameIfAvailable() {
        // Brand name present
        assertEquals(
            "Aspirin • 500mg × 1 dose",
            formatMedicationDescription(title = "Acetylsalicylic Acid", brandName = "Aspirin", dosage = "500mg", doses = 1.0)
        )
        // Brand name blank (fallback to title)
        assertEquals(
            "Acetylsalicylic Acid • 500mg × 1 dose",
            formatMedicationDescription(title = "Acetylsalicylic Acid", brandName = " ", dosage = "500mg", doses = 1.0)
        )
        // Brand name null (fallback to title)
        assertEquals(
            "Acetylsalicylic Acid • 500mg × 1 dose",
            formatMedicationDescription(title = "Acetylsalicylic Acid", brandName = null, dosage = "500mg", doses = 1.0)
        )
    }

    @Test
    fun formatMedicationDescription_handlesDosageCorrectly() {
        // Dosage present
        assertEquals(
            "Aspirin • 100mg × 2 doses",
            formatMedicationDescription(title = "Aspirin", brandName = null, dosage = "100mg", doses = 2.0)
        )
        // Dosage null
        assertEquals(
            "Aspirin × 2 doses",
            formatMedicationDescription(title = "Aspirin", brandName = null, dosage = null, doses = 2.0)
        )
        // Dosage blank
        assertEquals(
            "Aspirin × 2 doses",
            formatMedicationDescription(title = "Aspirin", brandName = null, dosage = " ", doses = 2.0)
        )
    }
}
