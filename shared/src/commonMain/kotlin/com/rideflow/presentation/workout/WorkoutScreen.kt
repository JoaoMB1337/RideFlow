package com.rideflow.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.util.distanceParts
import com.rideflow.util.formatDuration
import com.rideflow.util.formatSpeedValue
import org.koin.compose.viewmodel.koinViewModel

private const val RESISTANCE_STEP = 10
private const val RESISTANCE_MAX = 100

@Composable
fun WorkoutScreen(
    onWorkoutActiveChanged: (Boolean) -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmStop by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isWorkoutActive) {
        onWorkoutActiveChanged(uiState.isWorkoutActive)
        if (!uiState.isWorkoutActive) confirmStop = false
    }

    Scaffold { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        if (uiState.isWorkoutActive) {
            ActiveWorkout(
                uiState = uiState,
                onResistanceChange = { viewModel.onIntent(WorkoutIntent.AdjustResistance(it)) },
                onStop = { confirmStop = true },
                // Ecrã sempre ligado enquanto o treino decorre: o ciclista não toca no telemóvel.
                modifier = contentModifier.keepScreenOn()
            )
        } else {
            IdleWorkout(
                uiState = uiState,
                onStart = { viewModel.onIntent(WorkoutIntent.StartWorkout) },
                modifier = contentModifier
            )
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            icon = { Icon(Icons.Filled.Stop, contentDescription = null) },
            title = { Text("Finish ride?") },
            text = { Text("The ride will be stopped and saved to your history.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmStop = false
                        viewModel.onIntent(WorkoutIntent.StopWorkout)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("Keep riding") }
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Antes do treino
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleWorkout(uiState: WorkoutUiState, onStart: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val landscape = maxWidth > maxHeight

        val info: @Composable ColumnScope.() -> Unit = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SensorStatusPill("Heart rate", uiState.heartRateConnection, showStatus = true)
                SensorStatusPill("Bike", uiState.bikeConnection, showStatus = true)
            }
            if (uiState.bikeConnection !is SensorConnectionState.Connected) {
                Text(
                    "Bike not connected. Pair it in Settings to record power and speed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Pré-visualização dos sensores para confirmar que estão a enviar dados antes de arrancar.
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PowerTile(uiState, Modifier.weight(1f).fillMaxHeight())
                HeartRateTile(uiState, Modifier.weight(1f).fillMaxHeight())
            }
        }

        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), content = info)
                StartButton(onStart, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                info()
                Spacer(Modifier.weight(1f))
                StartButton(onStart, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.height(72.dp)
    ) {
        Text("Start ride", style = MaterialTheme.typography.titleLarge)
    }
}

// ---------------------------------------------------------------------------------------------
// Durante o treino — pensado para ser lido a ~1 m de distância
// ---------------------------------------------------------------------------------------------

@Composable
private fun ActiveWorkout(
    uiState: WorkoutUiState,
    onResistanceChange: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val gap = 12.dp
        val timeHeight = minOf(96.dp, maxHeight * 0.14f)
        val primaryMetrics: @Composable (Modifier) -> Unit = { m ->
            Row(m.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                PowerTile(uiState, Modifier.weight(1f).fillMaxHeight())
                HeartRateTile(uiState, Modifier.weight(1f).fillMaxHeight())
            }
        }
        val secondaryMetrics: @Composable (Modifier) -> Unit = { m ->
            val (distance, distanceUnit) = distanceParts(uiState.distance)
            Row(m.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                MetricTile(
                    label = "Speed", value = uiState.speed?.let(::formatSpeedValue) ?: "--", unit = "km/h",
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                MetricTile(
                    label = "Distance", value = distance, unit = distanceUnit,
                    accent = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        if (maxWidth > maxHeight) {
            // Paisagem: métricas à esquerda, tempo e controlos à direita.
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(gap)) {
                    primaryMetrics(Modifier.weight(1.4f))
                    secondaryMetrics(Modifier.weight(1f))
                }
                Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(gap)) {
                    LiveHeader(uiState)
                    ElapsedTime(uiState.durationSeconds, Modifier.fillMaxWidth().weight(1f))
                    ResistanceControl(uiState.resistance, onResistanceChange)
                    FinishButton(onStop)
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                LiveHeader(uiState)
                ElapsedTime(uiState.durationSeconds, Modifier.fillMaxWidth().height(timeHeight))
                primaryMetrics(Modifier.weight(1.4f))
                secondaryMetrics(Modifier.weight(1f))
                ResistanceControl(uiState.resistance, onResistanceChange)
                FinishButton(onStop)
            }
        }
    }
}

@Composable
private fun LiveHeader(uiState: WorkoutUiState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        SensorStatusPill("HR", uiState.heartRateConnection)
        Spacer(Modifier.width(16.dp))
        SensorStatusPill("Bike", uiState.bikeConnection)
    }
}

@Composable
private fun ElapsedTime(seconds: Long, modifier: Modifier) {
    val text = formatDuration(seconds)
    Box(modifier.semantics(mergeDescendants = true) { contentDescription = "Elapsed time $text" }, contentAlignment = Alignment.Center) {
        AutoSizeNumber(text, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PowerTile(uiState: WorkoutUiState, modifier: Modifier) = MetricTile(
    label = "Power", value = uiState.power?.toString() ?: "--", unit = "W",
    accent = MaterialTheme.colorScheme.primary,
    modifier = modifier
)

@Composable
private fun HeartRateTile(uiState: WorkoutUiState, modifier: Modifier) = MetricTile(
    label = "Heart rate", value = uiState.heartRate?.toString() ?: "--", unit = "bpm",
    accent = MaterialTheme.colorScheme.tertiary,
    modifier = modifier
)

@Composable
private fun MetricTile(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        // Um único nó para o leitor de ecrã: "Power 250 W".
        modifier = modifier.clearAndSetSemantics {
            contentDescription = if (value == "--") "$label: no data" else "$label $value $unit"
        },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row {
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(unit, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                AutoSizeNumber(value, accent, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Número que cresce até ocupar o espaço disponível — essencial para ler a métrica de longe. */
@Composable
private fun AutoSizeNumber(text: String, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        // lineHeight fixo do displayLarge cortaria os algarismos quando o autoSize aumenta a fonte.
        style = MaterialTheme.typography.displayLarge.copy(color = color, textAlign = TextAlign.Center, lineHeight = TextUnit.Unspecified),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 160.sp)
    )
}

@Composable
private fun ResistanceControl(level: Int, onLevelChange: (Int) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = { onLevelChange((level - RESISTANCE_STEP).coerceAtLeast(0)) },
                enabled = level > 0,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease resistance", modifier = Modifier.size(36.dp))
            }
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Resistance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(level.toString(), style = MaterialTheme.typography.displaySmall)
            }
            FilledTonalIconButton(
                onClick = { onLevelChange((level + RESISTANCE_STEP).coerceAtMost(RESISTANCE_MAX)) },
                enabled = level < RESISTANCE_MAX,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase resistance", modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun FinishButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text("Finish ride", style = MaterialTheme.typography.titleMedium)
    }
}

// ---------------------------------------------------------------------------------------------
// Estado dos sensores (reutilizado nas Definições)
// ---------------------------------------------------------------------------------------------

@Composable
fun SensorConnectionState.statusColor(): Color = when (this) {
    is SensorConnectionState.Connected -> MaterialTheme.colorScheme.primary
    is SensorConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
    is SensorConnectionState.Error -> MaterialTheme.colorScheme.error
    SensorConnectionState.Disconnected -> MaterialTheme.colorScheme.outline
}

fun SensorConnectionState.statusLabel(): String = when (this) {
    is SensorConnectionState.Connected -> "Connected"
    is SensorConnectionState.Connecting -> "Connecting…"
    is SensorConnectionState.Error -> "Connection error"
    SensorConnectionState.Disconnected -> "Not connected"
}

/** Indicador compacto: ponto colorido + nome (e, opcionalmente, o estado por extenso). */
@Composable
fun SensorStatusPill(label: String, state: SensorConnectionState, showStatus: Boolean = false) {
    Row(
        Modifier.clearAndSetSemantics { contentDescription = "$label: ${state.statusLabel()}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(state.statusColor(), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            if (showStatus) "$label · ${state.statusLabel()}" else label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
