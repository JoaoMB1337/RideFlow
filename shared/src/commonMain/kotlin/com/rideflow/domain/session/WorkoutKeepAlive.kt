package com.rideflow.domain.session

/**
 * Impede que o sistema operativo suspenda o processo enquanto há um treino a decorrer.
 *
 * O [WorkoutSessionManager] é código comum e não sabe nada sobre serviços Android nem sobre
 * background modes do iOS — apenas declara "preciso de continuar a correr". Cada plataforma
 * regista a sua implementação no `platformModule`, tal como o `SharingService`.
 */
interface WorkoutKeepAlive {
    /** Chamado ao arrancar um treino. Deve ser idempotente. */
    fun acquire()

    /** Chamado ao terminar (ou abortar) um treino. Deve ser idempotente. */
    fun release()
}

/** Usado nas plataformas onde não há nada a fazer (ou ainda não está implementado). */
class NoOpWorkoutKeepAlive : WorkoutKeepAlive {
    override fun acquire() = Unit
    override fun release() = Unit
}
