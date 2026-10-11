package com.rideflow.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.rideflow.db.RideFlowDb
import com.rideflow.domain.model.TrainingData
import com.rideflow.domain.usecase.StopWorkoutUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutRepositoryImplTest {

    // foreign_keys=true tal como nos drivers reais, senão o ON DELETE CASCADE não é testado.
    private val driver = JdbcSqliteDriver(
        JdbcSqliteDriver.IN_MEMORY,
        Properties().apply { put("foreign_keys", "true") }
    ).also { RideFlowDb.Schema.create(it) }
    private val repo = WorkoutRepositoryImpl(RideFlowDb(driver))
    private val stop = StopWorkoutUseCase(repo)

    @Test
    fun startRecordStopReadDelete() = runBlocking {
        val id = repo.startTrain(date = 1_000L)
        assertEquals(listOf(id), repo.getUnfinishedTrainIds())

        (1L..3L).forEach { t ->
            repo.insertTrainingData(
                TrainingData(
                    idTrain = id, timestampOffset = t, heartRate = 120 + t.toInt(),
                    power = 1000, speed = 30.0, distance = t * 0.01, resistance = 5
                )
            )
        }
        // duration = null → derivada do último sample, como na recuperação após crash
        stop(id)

        val train = repo.getTrainById(id)!!
        assertEquals(3, train.duration)
        assertEquals(0.03, train.distance, 1e-9)
        assertEquals(122, train.avgHeartRate)
        assertEquals(123, train.maxHeartRate)
        assertEquals(3, train.calories) // 3 s × 1000 W = 3 kJ ≈ 3 kcal
        assertTrue(repo.getUnfinishedTrainIds().isEmpty())

        repo.updateTrainInfo(id, name = "Intervalos", notes = "  ")
        assertEquals("Intervalos", repo.getTrainById(id)!!.name)
        assertNull(repo.getTrainById(id)!!.notes)

        repo.deleteTrain(id)
        assertNull(repo.getTrainById(id))
        assertTrue(repo.getTrainingDataForTrain(id).first().isEmpty(), "cascade deve apagar samples")
    }

    @Test
    fun stopWithoutSamplesDeletesTrain() = runBlocking {
        val id = repo.startTrain(date = 1_000L)
        stop(id, duration = 0)
        assertNull(repo.getTrainById(id))
    }
}
