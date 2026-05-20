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
import java.io.IOException

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
}
