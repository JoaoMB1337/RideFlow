package com.rideflow.domain.session

import com.rideflow.domain.model.WorkoutSession
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.domain.repository.WorkoutRepository
import com.rideflow.domain.usecase.RecordSampleUseCase
import com.rideflow.domain.usecase.StartWorkoutUseCase
import com.rideflow.domain.usecase.StopWorkoutUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

/**
 * Dono do ciclo de vida de um treino: cronómetro, acumulação de distância e gravação de samples.
 *
 * É um singleton com scope próprio, deliberadamente **fora** do `viewModelScope`. Antes esta
 * lógica vivia no `WorkoutViewModel`, o que significava que sair do ecrã de treino (ou o sistema
 * destruir a Activity) parava a gravação a meio do treino, em silêncio.
 */
class WorkoutSessionManager(
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val recordSampleUseCase: RecordSampleUseCase,
    private val stopWorkoutUseCase: StopWorkoutUseCase,
    private val sensorRepository: SensorRepository,
    private val keepAlive: WorkoutKeepAlive,
    private val workoutRepository: WorkoutRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _session = MutableStateFlow<WorkoutSession?>(null)

    /** `null` quando não há treino a decorrer. */
    val session: StateFlow<WorkoutSession?> = _session.asStateFlow()

    private var sessionJob: Job? = null

    /** Serializa start/stop: dois toques rápidos no botão não podem abrir dois treinos. */
    private val lifecycleMutex = Mutex()

    init {
        // Se o processo morreu a meio de um treino (ou o stop falhou), o Train ficou sem resumo
        // (0s, 0km no histórico) apesar de ter samples. Fecha-os agora, a partir dos samples.
        scope.launch {
            lifecycleMutex.withLock {
                // Sob o mutex: um start() que tenha ganho a corrida já pôs o seu id em _session,
                // e esse treino está vivo — não é para fechar.
                val activeId = _session.value?.trainId
                runCatching { recoverUnfinished(activeId) }
                    .onFailure { Napier.e(tag = TAG, throwable = it) { "Workout recovery failed" } }
            }
        }
    }

    private suspend fun recoverUnfinished(activeId: Long?) {
        workoutRepository.getUnfinishedTrainIds()
            .filter { it != activeId }
            .forEach { id ->
                stopWorkoutUseCase(id)
                Napier.i(tag = TAG) { "Recovered unfinished workout $id" }
            }
    }

    suspend fun start() = lifecycleMutex.withLock {
        if (sessionJob?.isActive == true) {
            Napier.w(tag = TAG) { "start() ignored: a workout is already running" }
            return@withLock
        }

        val trainId = startWorkoutUseCase()
        _session.value = WorkoutSession(trainId = trainId)
        keepAlive.acquire()
        Napier.i(tag = TAG) { "Workout $trainId started" }

        sessionJob = scope.launch { runSession(trainId) }
    }

    suspend fun stop() = lifecycleMutex.withLock {
        val current = _session.value ?: return@withLock

        // Parar o loop *antes* de calcular o resumo, senão o último tick podia inserir
        // um sample depois de as médias já terem sido calculadas.
        sessionJob?.cancelAndJoin()
        sessionJob = null

        try {
            stopWorkoutUseCase(current.trainId, current.durationSeconds)
            Napier.i(tag = TAG) { "Workout ${current.trainId} stopped at ${current.durationSeconds}s" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Failed to write summary for ${current.trainId}" }
        } finally {
            keepAlive.release()
            _session.value = null
        }
    }

    private suspend fun runSession(trainId: Long) {
        // Um único relógio monotónico como fonte de verdade do tempo decorrido. A versão
        // anterior tinha dois loops independentes de `delay(1000)` — um para o cronómetro,
        // outro para a amostragem — que derivavam entre si e face ao tempo real, porque cada
        // iteração demora sempre um pouco mais de 1000ms.
        val startMark = TimeSource.Monotonic.markNow()
        var lastElapsed = 0L

        while (currentCoroutineContext().isActive) {
            delay(TICK_MS)

            val elapsed = startMark.elapsedNow().inWholeSeconds
            val delta = elapsed - lastElapsed
            if (delta <= 0) continue // o tick acordou dentro do mesmo segundo
            lastElapsed = elapsed

            val speed = sensorRepository.speed.value
            // Integra sobre o delta real em vez de assumir exactamente 1s por tick, para que
            // um tick atrasado não perca distância.
            val distanceIncrement = (speed ?: 0.0) / 3600.0 * delta

            val updated = _session.updateAndGetSession { current ->
                current.copy(
                    durationSeconds = elapsed,
                    distanceKm = current.distanceKm + distanceIncrement
                )
            } ?: break

            try {
                recordSampleUseCase(
                    trainId = trainId,
                    offset = elapsed,
                    heartRate = sensorRepository.heartRate.value,
                    power = sensorRepository.power.value,
                    speed = speed,
                    distance = updated.distanceKm,
                    resistance = sensorRepository.resistance.value
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Uma escrita falhada não pode matar o treino — perde-se um sample, não a sessão.
                Napier.w(tag = TAG, throwable = e) { "Failed to record sample at ${elapsed}s" }
            }
        }
    }

    private companion object {
        const val TAG = "WorkoutSession"
        const val TICK_MS = 1_000L
    }
}

/**
 * Aplica [transform] atomicamente e devolve o resultado, ou `null` se entretanto a sessão
 * terminou (evita ressuscitar um treino já fechado a partir do loop de amostragem).
 */
private inline fun MutableStateFlow<WorkoutSession?>.updateAndGetSession(
    transform: (WorkoutSession) -> WorkoutSession
): WorkoutSession? {
    while (true) {
        val current = value ?: return null
        val next = transform(current)
        if (compareAndSet(current, next)) return next
    }
}
