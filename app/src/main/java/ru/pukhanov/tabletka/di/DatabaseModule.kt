package ru.pukhanov.tabletka.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.pukhanov.tabletka.data.local.dao.MedicationDao
import ru.pukhanov.tabletka.data.local.database.AppDatabase
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tabletka_database"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )
        .build()
    }

    @Provides
    fun provideMedicationDao(database: AppDatabase): MedicationDao {
        return database.medicationDao()
    }

    @Provides
    @Singleton
    fun provideMedicationRepository(medicationDao: MedicationDao): MedicationRepository {
        return MedicationRepository(medicationDao)
    }
}
