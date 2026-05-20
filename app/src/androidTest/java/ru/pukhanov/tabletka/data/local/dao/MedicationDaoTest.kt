package ru.pukhanov.tabletka.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.pukhanov.tabletka.data.local.database.AppDatabase
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules
import java.io.IOException
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var medicationDao: MedicationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        medicationDao = db.medicationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetMedicationWithBrandName() = runBlocking {
        val medication = Medication(
            title = "Aspirin",
            brandName = "Bayer"
        )
        val id = medicationDao.insert(medication)
        val retrieved = medicationDao.getById(id)

        assertNotNull(retrieved)
        assertEquals("Aspirin", retrieved?.title)
        assertEquals("Bayer", retrieved?.brandName)
    }

    @Test
    fun insertAndGetMedicationWithoutBrandName() = runBlocking {
        val medication = Medication(
            title = "Paracetamol",
            brandName = null
        )
        val id = medicationDao.insert(medication)
        val retrieved = medicationDao.getById(id)

        assertNotNull(retrieved)
        assertEquals("Paracetamol", retrieved?.title)
        assertNull(retrieved?.brandName)
    }

    @Test
    fun getAllMedications() = runBlocking {
        val med1 = Medication(title = "Medication 1")
        val med2 = Medication(title = "Medication 2", brandName = "Brand 2")

        medicationDao.insert(med1)
        medicationDao.insert(med2)

        val all = medicationDao.getAll().first()
        assertEquals(2, all.size)
        assertEquals("Medication 2", all[0].title)
        assertEquals("Medication 1", all[1].title)
    }

    @Test
    fun deleteMedication() = runBlocking {
        val medication = Medication(title = "To Delete")
        val id = medicationDao.insert(medication)
        val retrievedBefore = medicationDao.getById(id)
        assertNotNull(retrievedBefore)

        medicationDao.delete(retrievedBefore!!)
        val retrievedAfter = medicationDao.getById(id)
        assertNull(retrievedAfter)
    }

    @Test
    fun saveAndGetMedicationWithSchedules() = runBlocking {
        val medication = Medication(title = "Aspirin")
        val schedules = listOf(
            MedicationSchedule(hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)),
            MedicationSchedule(hour = 9, minute = 30, daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        )

        medicationDao.saveMedicationWithSchedules(medication, schedules)

        val allMedications = medicationDao.getAll().first()
        assertEquals(1, allMedications.size)
        val medId = allMedications[0].id

        val retrieved = medicationDao.getMedicationWithSchedules(medId)
        assertNotNull(retrieved)
        assertEquals("Aspirin", retrieved?.medication?.title)
        assertEquals(2, retrieved?.schedules?.size)

        val sched1 = retrieved?.schedules?.find { it.hour == 8 }
        assertNotNull(sched1)
        assertEquals(0, sched1?.minute)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), sched1?.daysOfWeek)

        val sched2 = retrieved?.schedules?.find { it.hour == 9 }
        assertNotNull(sched2)
        assertEquals(30, sched2?.minute)
        assertEquals(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), sched2?.daysOfWeek)
    }

    @Test
    fun saveMedicationWithSchedules_updatesExistingSchedules() = runBlocking {
        val medication = Medication(title = "Aspirin")
        val schedules = listOf(
            MedicationSchedule(hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY))
        )
        medicationDao.saveMedicationWithSchedules(medication, schedules)

        val medId = medicationDao.getAll().first()[0].id

        val updatedMedication = Medication(id = medId, title = "Aspirin Forte")
        val newSchedules = listOf(
            MedicationSchedule(hour = 10, minute = 15, daysOfWeek = setOf(DayOfWeek.FRIDAY))
        )
        medicationDao.saveMedicationWithSchedules(updatedMedication, newSchedules)

        val retrieved = medicationDao.getMedicationWithSchedules(medId)
        assertNotNull(retrieved)
        assertEquals("Aspirin Forte", retrieved?.medication?.title)
        assertEquals(1, retrieved?.schedules?.size)
        assertEquals(10, retrieved?.schedules?.first()?.hour)
        assertEquals(15, retrieved?.schedules?.first()?.minute)
        assertEquals(setOf(DayOfWeek.FRIDAY), retrieved?.schedules?.first()?.daysOfWeek)
    }

    @Test
    fun deleteMedication_cascadeDeletesSchedules() = runBlocking {
        val medication = Medication(title = "To Delete")
        val schedules = listOf(
            MedicationSchedule(hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY))
        )
        medicationDao.saveMedicationWithSchedules(medication, schedules)

        val med = medicationDao.getAll().first()[0]
        medicationDao.delete(med)

        val retrieved = medicationDao.getMedicationWithSchedules(med.id)
        assertNull(retrieved)
    }
}
