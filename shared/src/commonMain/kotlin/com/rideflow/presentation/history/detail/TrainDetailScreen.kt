package com.rideflow.presentation.history.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import com.rideflow.util.formatSpeedValue
import kotlin.math.roundToInt
import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import com.rideflow.util.formatDate
import com.rideflow.util.formatDistance
import com.rideflow.util.formatDuration
import com.rideflow.util.formatSpeed
import com.rideflow.util.formatTimeOfDay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainDetailScreen(
    trainId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TrainDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }

    LaunchedEffect(trainId) {
        viewModel.onIntent(TrainDetailIntent.LoadDetail(trainId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }, enabled = uiState.train != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit name and notes")
                    }
                    IconButton(onClick = { viewModel.onIntent(TrainDetailIntent.ShareSummary) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share ride")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete ride", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Em ecrãs largos (tablet / paisagem) os gráficos ficam lado a lado.
                val chartColumns = if (maxWidth >= 600.dp) 2 else 1

                Column(
                    modifier = Modifier
                        .widthIn(max = 1000.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.train?.let { train ->
                        TrainSummaryCard(train)
                        TrainStatsTable(train)
                    }


                    // Zonas de HR calculadas a partir do perfil do utilizador (fallback para defaults se não houver perfil)
                    val maxHr = uiState.userMaxHeartRate ?: 190
                    val hrZones = remember(maxHr) { buildHrZones(maxHr) }

                    val charts: List<@Composable (Modifier) -> Unit> = listOf(
                        { m ->
                            ChartContainer(
                                label = "Heart rate",
                                unit = "bpm",
                                samples = uiState.samples,
                                valueSelector = { it.heartRate?.toDouble() },
                                color = MaterialTheme.colorScheme.tertiary,
                                zones = hrZones,
                                maxOverride = (maxHr + 10).toDouble(),
                                modifier = m
                            )
                        },
                        { m ->
                            ChartContainer(
                                label = "Power",
                                unit = "W",
                                samples = uiState.samples,
                                valueSelector = { it.power?.toDouble() },
                                color = MaterialTheme.colorScheme.primary,
                                smoothingWindow = 3, // média móvel de 3s para suavizar picos instantâneos
                                modifier = m
                            )
                        },
                        { m ->
                            ChartContainer(
                                label = "Speed",
                                unit = "km/h",
                                format = ::formatSpeedValue,
                                samples = uiState.samples,
                                valueSelector = { it.speed?.toDouble() },
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = m
                            )
                        },
                        { m ->
                            ChartContainer(
                                label = "Resistance",
                                unit = "",
                                samples = uiState.samples,
                                valueSelector = { it.resistance?.toDouble() },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = m
                            )
                        }
                    )

                    charts.chunked(chartColumns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { chart -> chart(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete ride?") },
            text = { Text("This permanently removes the ride and all its recorded data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(TrainDetailIntent.DeleteTrain)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val train = uiState.train
    if (showEdit && train != null) {
        var name by remember { mutableStateOf(train.name.orEmpty()) }
        var notes by remember { mutableStateOf(train.notes.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit ride") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text(formatDate(train.date)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onIntent(TrainDetailIntent.UpdateInfo(name, notes))
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Cancel") }
            }
        )
    }

    if (uiState.isSharing) {
        AlertDialog(
            onDismissRequest = { viewModel.onShareComplete() },
            title = { Text("Share ride") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Creating your workout summary image...")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onShareComplete() }) {
                    Text("Done")
                }
            }
        )
    }
}

/** Cabeçalho: data/hora do treino + as duas métricas principais (duração e distância). */
@Composable
fun TrainSummaryCard(train: Train) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(train.name ?: formatDate(train.date), style = MaterialTheme.typography.headlineSmall)
        Text(
            if (train.name != null) "${formatDate(train.date)} · ${formatTimeOfDay(train.date)}"
            else "Started at ${formatTimeOfDay(train.date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryItem(Modifier.weight(1f), "Duration", formatDuration(train.duration))
            SummaryItem(Modifier.weight(1f), "Distance", formatDistance(train.distance))
            SummaryItem(Modifier.weight(1f), "Calories", "${train.calories} kcal")
        }
        train.notes?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = "$label $value" }) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tabela média / máximo por métrica. */
@Composable
private fun TrainStatsTable(train: Train) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Spacer(Modifier.weight(1.4f))
                TableHeader("Avg", Modifier.weight(1f))
                TableHeader("Max", Modifier.weight(1f))
            }
            StatsRow("Heart rate",
                "${train.avgHeartRate} bpm", "${train.maxHeartRate} bpm")
            StatsRow("Power",
                "${train.avgPower} W", "${train.maxPower} W")
            StatsRow("Speed",
                formatSpeed(train.avgSpeed), formatSpeed(train.maxSpeed))
            StatsRow("Resistance",
                "${train.avgResistance}", "${train.maxResistance}")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier
    )
}

@Composable
private fun StatsRow(label: String, avg: String, max: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clearAndSetSemantics { contentDescription = "$label: average $avg, maximum $max" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1.4f))
        Text(avg, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.weight(1f))
        Text(max, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

/**
 * Ponto de dados já resolvido para desenho: mantém o timestamp original
 * mesmo quando o valor vem de um sample com falha de sensor (null).
 */
private data class PlotPoint(val timeSeconds: Long, val value: Double)

/**
 * Gráfico ao estilo Garmin Connect: área preenchida sob a linha (por zonas na FC),
 * linha tracejada da média, eixo Y "redondo" à direita e leitura do valor ao arrastar o dedo.
 */
@Composable
fun ChartContainer(
    label: String,
    unit: String,
    samples: List<TrainingData>,
    valueSelector: (TrainingData) -> Double?,
    color: Color,
    zones: List<ChartZone> = emptyList(),
    maxOverride: Double? = null,
    smoothingWindow: Int = 1, // 1 = sem suavização
    format: (Double) -> String = { it.roundToInt().toString() },
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cursorColor = MaterialTheme.colorScheme.onSurface
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = axisColor)

    val rawPoints = remember(samples) {
        samples.mapNotNull { sample -> valueSelector(sample)?.let { PlotPoint(sample.timestampOffset, it) } }
    }
    val points = remember(rawPoints, smoothingWindow) {
        if (smoothingWindow <= 1) rawPoints else smoothMovingAverage(rawPoints, smoothingWindow)
    }
    val totalSeconds = samples.lastOrNull()?.timestampOffset?.coerceAtLeast(1L) ?: 1L
    val avg = remember(rawPoints) { rawPoints.map { it.value }.average() }
    val peak = remember(rawPoints) { rawPoints.maxOfOrNull { it.value } ?: 0.0 }
    val axisMax = remember(points, maxOverride) { niceAxisMax(maxOverride ?: points.maxOfOrNull { it.value } ?: 1.0) }
    var scrub by remember(points) { mutableStateOf<PlotPoint?>(null) }
    val unitSuffix = if (unit.isEmpty()) "" else " $unit"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                val current = scrub
                if (current != null) {
                    Text(
                        "${formatDuration(current.timeSeconds)}   ${format(current.value)}$unitSuffix",
                        style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.SemiBold
                    )
                } else if (rawPoints.isNotEmpty()) {
                    Text(
                        "Avg ${format(avg)}  ·  Max ${format(peak)}$unitSuffix",
                        style = MaterialTheme.typography.bodySmall, color = axisColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (points.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("No data recorded", style = MaterialTheme.typography.bodyMedium, color = axisColor)
                }
                return@Column
            }

            Row(Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Só arrasto horizontal: o scroll vertical do ecrã continua a funcionar.
                        .pointerInput(points) {
                            fun pointAt(x: Float): PlotPoint {
                                val t = (x / size.width * totalSeconds).toLong()
                                val i = points.binarySearchBy(t) { it.timeSeconds }
                                return points[if (i >= 0) i else (-i - 1).coerceAtMost(points.lastIndex)]
                            }
                            detectHorizontalDragGestures(
                                onDragStart = { scrub = pointAt(it.x) },
                                onDragEnd = { scrub = null },
                                onDragCancel = { scrub = null },
                                onHorizontalDrag = { change, _ -> scrub = pointAt(change.position.x) }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    fun Double.toY() = (h - (this / axisMax * h).toFloat()).coerceIn(0f, h)
                    fun Long.toX() = (this.toFloat() / totalSeconds * w).coerceIn(0f, w)
                    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))

                    for (i in 0..AXIS_STEPS) {
                        val y = (axisMax / AXIS_STEPS * i).toY()
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), 1.dp.toPx(), pathEffect = if (i == 0) null else dash)
                    }

                    val line = Path()
                    points.forEachIndexed { i, pt ->
                        if (i == 0) line.moveTo(pt.timeSeconds.toX(), pt.value.toY()) else line.lineTo(pt.timeSeconds.toX(), pt.value.toY())
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(points.last().timeSeconds.toX(), h)
                        lineTo(points.first().timeSeconds.toX(), h)
                        close()
                    }

                    if (zones.isEmpty()) {
                        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.05f))))
                        drawPath(line, color, style = Stroke(2.dp.toPx()))
                    } else {
                        // Cada faixa da área fica com a cor da sua zona de FC.
                        zones.forEach { zone ->
                            clipRect(top = zone.max.toY(), bottom = zone.min.toY()) { drawPath(area, zone.color) }
                        }
                        drawPath(line, color, style = Stroke(1.5.dp.toPx()))
                    }

                    val avgY = avg.toY()
                    drawLine(axisColor, Offset(0f, avgY), Offset(w, avgY), 1.dp.toPx(), pathEffect = dash)

                    scrub?.let { pt ->
                        val x = pt.timeSeconds.toX()
                        drawLine(cursorColor, Offset(x, 0f), Offset(x, h), 1.dp.toPx())
                        drawCircle(cursorColor, 5.dp.toPx(), Offset(x, pt.value.toY()))
                        drawCircle(color, 3.5.dp.toPx(), Offset(x, pt.value.toY()))
                    }
                }

                Column(
                    Modifier.fillMaxHeight().padding(start = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in AXIS_STEPS downTo 0) {
                        Text((axisMax / AXIS_STEPS * i).roundToInt().toString(), style = axisStyle)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(0), style = axisStyle)
                Text(formatDuration(totalSeconds / 2), style = axisStyle)
                Text(formatDuration(totalSeconds), style = axisStyle)
            }
        }
    }
}

private const val AXIS_STEPS = 4

/** Topo do eixo Y arredondado para que cada uma das 4 divisões seja 1, 2 ou 5 × 10ⁿ (ex.: 237 → 400). */
internal fun niceAxisMax(value: Double): Double {
    val rawStep = value.coerceAtLeast(1.0) / AXIS_STEPS
    var magnitude = 1.0
    while (magnitude * 10 <= rawStep) magnitude *= 10
    val step = listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= rawStep } * magnitude
    return step * AXIS_STEPS
}

/**
 * Média móvel simples sobre uma janela de N pontos, preservando o timestamp
 * do ponto mais recente da janela (não do centro), para não introduzir
 * desfasamento visual entre power e HR no mesmo gráfico multi-série.
 */
private fun smoothMovingAverage(points: List<PlotPoint>, window: Int): List<PlotPoint> {
    if (points.size <= 1 || window <= 1) return points
    val result = ArrayList<PlotPoint>(points.size)
    var sum = 0.0
    val buffer = ArrayDeque<Double>()
    for (point in points) {
        buffer.addLast(point.value)
        sum += point.value
        if (buffer.size > window) {
            sum -= buffer.removeFirst()
        }
        result.add(PlotPoint(point.timeSeconds, sum / buffer.size))
    }
    return result
}

/**
 * Zonas de FC como % da FCmáx (modelo standard de 5 zonas).
 * Z1 50-60% · Z2 60-70% · Z3 70-80% · Z4 80-90% · Z5 90-100%
 */
private fun buildHrZones(maxHr: Int): List<ChartZone> {
    val max = maxHr.toDouble()
    return listOf(
        ChartZone(0.0, max * 0.60, Color(0xFF9E9E9E)),       // Z1 Recuperação
        ChartZone(max * 0.60, max * 0.70, Color(0xFF3D8BD9)), // Z2 Resistência
        ChartZone(max * 0.70, max * 0.80, Color(0xFF4CAF50)), // Z3 Tempo
        ChartZone(max * 0.80, max * 0.90, Color(0xFFF29D38)), // Z4 Limiar
        ChartZone(max * 0.90, max * 2, Color(0xFFE5483B))     // Z5 VO2max
    )
}

data class ChartZone(
    val min: Double,
    val max: Double,
    val color: Color
)
