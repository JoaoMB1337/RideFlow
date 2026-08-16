package com.rideflow.domain.model

/**
 * Estado de um treino em curso.
 *
 * Vive no [com.rideflow.domain.session.WorkoutSessionManager], não num ViewModel: um treino
 * sobrevive à navegação entre ecrãs e à destruição da Activity, portanto o seu ciclo de vida
 * não pode estar preso ao de um ecrã.
 */
data class WorkoutSession(
    val trainId: Long,
    val durationSeconds: Long = 0,
    val distanceKm: Double = 0.0
)
