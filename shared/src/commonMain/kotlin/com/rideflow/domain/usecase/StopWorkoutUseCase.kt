package com.rideflow.domain.usecase

import com.rideflow.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

class StopWorkoutUseCase(
    private val repository: WorkoutRepository
) {
    /**
     * Calcula e grava o resumo do treino a partir dos samples.
     *
     * [duration] `null` = deriva do último sample (recuperação de um treino interrompido, em que
     * o cronómetro da sessão já não existe).
     */
    suspend operator fun invoke(trainId: Long, duration: Long? = null) {
        val samples = repository.getTrainingDataForTrain(trainId).first()

        // Sem samples não há treino: antes ficava uma linha vazia (0s, 0km) no histórico.
        if (samples.isEmpty()) {
            repository.deleteTrain(trainId)
            return
        }

        // average() sobre lista vazia devolve NaN — sem este guard, um sensor que nunca
        // reportou nada gravava NaN na coluna REAL e a UI mostrava "NaNkm/h".
        val avgHeartRate = samples.mapNotNull { it.heartRate }.averageOrZero().toInt()
        val avgPower = samples.mapNotNull { it.power }.averageOrZero().toInt()
        val avgSpeed = samples.mapNotNull { it.speed }.averageOrZero()
        val avgResistance = samples.mapNotNull { it.resistance }.averageOrZero().toInt()

        val maxHeartRate = samples.mapNotNull { it.heartRate }.maxOrNull() ?: 0
        val maxPower = samples.mapNotNull { it.power }.maxOrNull() ?: 0
        val maxSpeed = samples.mapNotNull { it.speed }.maxOrNull() ?: 0.0
        val maxResistance = samples.mapNotNull { it.resistance }.maxOrNull() ?: 0

        val distance = samples.lastOrNull()?.distance ?: 0.0

        // Um sample por segundo, logo Σ potência (W) = energia em J. kJ mecânicos ≈ kcal gastas
        // (eficiência humana ~24% cancela o 4.184 J/cal). Segundos sem potência contam 0, o que
        // é mais honesto do que avgPower * duration quando o sensor só esteve ligado parte do treino.
        val calories = (samples.sumOf { (it.power ?: 0).toLong() } / 1000).toInt()

        val train = repository.getTrainById(trainId)?.copy(
            duration = duration ?: samples.last().timestampOffset,
            distance = distance,
            avgHeartRate = avgHeartRate,
            avgPower = avgPower,
            avgSpeed = avgSpeed,
            avgResistance = avgResistance,
            maxHeartRate = maxHeartRate,
            maxPower = maxPower,
            maxSpeed = maxSpeed,
            maxResistance = maxResistance,
            calories = calories
        )

        train?.let { repository.updateTrainSummary(it) }
    }
}

private fun List<Number>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size
