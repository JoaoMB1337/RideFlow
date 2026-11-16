package com.rideflow.presentation.history.detail

import kotlin.test.Test
import kotlin.test.assertEquals

class NiceAxisMaxTest {
    @Test
    fun roundsUpToOneTwoFiveSteps() {
        assertEquals(400.0, niceAxisMax(237.0))  // passo 100
        assertEquals(200.0, niceAxisMax(200.0))  // já redondo
        assertEquals(40.0, niceAxisMax(32.5))    // passo 10
        assertEquals(4.0, niceAxisMax(0.0))      // sem dados: mínimo 1 por divisão
        assertEquals(2000.0, niceAxisMax(1100.0)) // passo 500
    }
}
