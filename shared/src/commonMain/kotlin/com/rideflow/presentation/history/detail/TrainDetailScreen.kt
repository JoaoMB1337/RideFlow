package com.rideflow.presentation.history.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
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

                    Text(
                        "Charts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )

                    // Zonas de HR calculadas a partir do perfil do utilizador (fallback para defaults se não houver perfil)
                    val maxHr = uiState.userMaxHeartRate ?: 190
                    val hrZones = remember(maxHr) { buildHrZones(maxHr) }

                    val charts: List<@Composable (Modifier) -> Unit> = listOf(
                        { m ->
                            ChartContainer(
                                label = "Heart rate (bpm)",
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
                                label = "Power (W)",
                                samples = uiState.samples,
                                valueSelector = { it.power?.toDouble() },
                                color = MaterialTheme.colorScheme.primary,
                                smoothingWindow = 3, // média móvel de 3s para suavizar picos instantâneos
                                modifier = m
                            )
                        },
                        { m ->
                            ChartContainer(
                                label = "Speed (km/h)",
                                samples = uiState.samples,
                                valueSelector = { it.speed?.toDouble() },
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = m
                            )
                        },
                        { m ->
                            ChartContainer(
                                label = "Resistance",
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(formatDate(train.date), style = MaterialTheme.typography.titleLarge)
            Text("Started at ${formatTimeOfDay(train.date)}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Duration", formatDuration(train.duration))
                SummaryItem(Modifier.weight(1f), "Distance", formatDistance(train.distance))
            }
        }
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier.clearAndSetSemantics { contentDescription = "$label $value" }) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.displaySmall, maxLines = 1)
    }
}

/** Tabela média / máximo por métrica. */
@Composable
private fun TrainStatsTable(train: Train) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Spacer(Modifier.weight(1.4f))
                TableHeader("AVG", Modifier.weight(1f))
                TableHeader("MAX", Modifier.weight(1f))
            }
            StatsRow(Icons.Default.Favorite, MaterialTheme.colorScheme.tertiary, "Heart rate",
                "${train.avgHeartRate} bpm", "${train.maxHeartRate} bpm")
            StatsRow(Icons.Default.Bolt, MaterialTheme.colorScheme.primary, "Power",
                "${train.avgPower} W", "${train.maxPower} W")
            StatsRow(Icons.Default.Speed, MaterialTheme.colorScheme.secondary, "Speed",
                formatSpeed(train.avgSpeed), formatSpeed(train.maxSpeed))
            StatsRow(Icons.Default.FitnessCenter, MaterialTheme.colorScheme.onSurfaceVariant, "Resistance",
                "${train.avgResistance}", "${train.maxResistance}")
        }
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
private fun StatsRow(icon: ImageVector, tint: Color, label: String, avg: String, max: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clearAndSetSemantics { contentDescription = "$label: average $avg, maximum $max" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Text(avg, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.weight(1f))
        Text(max, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

/**
 * Ponto de dados já resolvido para desenho: mantém o timestamp original
 * mesmo quando o valor vem de um sample com falha de sensor (null),
 * evitando o desalinhamento do eixo X que existia na versão anterior.
 */
private data class PlotPoint(val timeSeconds: Long, val value: Double)

@Composable
fun ChartContainer(
    label: String,
    samples: List<TrainingData>,
    valueSelector: (TrainingData) -> Double?,
    color: Color,
    zones: List<ChartZone> = emptyList(),
    maxOverride: Double? = null,
    smoothingWindow: Int = 1, // 1 = sem suavização
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.fillMaxWidth().height(220.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxSize()) {

                // Pares (tempo, valor) — só os samples com valor não-nulo entram,
                // mas o tempo real de cada ponto é preservado (fix do bug de alinhamento).
                val rawPoints = remember(samples) {
                    samples.mapNotNull { sample ->
                        valueSelector(sample)?.let { PlotPoint(sample.timestampOffset, it) }
                    }
                }

                val plotPoints = remember(rawPoints, smoothingWindow) {
                    if (smoothingWindow <= 1) rawPoints else smoothMovingAverage(rawPoints, smoothingWindow)
                }

                val totalDurationSeconds = remember(samples) { samples.lastOrNull()?.timestampOffset ?: 0L }

                if (plotPoints.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data recorded", style = MaterialTheme.typography.bodyMedium, color = axisColor)
                    }
                } else {
                    val maxVal = remember(plotPoints, maxOverride) {
                        maxOverride ?: (plotPoints.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0)
                    }
                    val steps = 4

                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                val minVal = 0.0
                                val safeTotalTime = totalDurationSeconds.coerceAtLeast(1L).toFloat()

                                fun Double.toY(): Float =
                                    (height - ((this.toFloat() - minVal.toFloat()) / (maxVal.toFloat() - minVal.toFloat()) * height))
                                        .coerceIn(0f, height)

                                fun Long.toX(): Float =
                                    (this.toFloat() / safeTotalTime * width).coerceIn(0f, width)

                                // Draw Zones
                                zones.forEach { zone ->
                                    val yStart = zone.max.toY()
                                    val yEnd = zone.min.toY()
                                    drawRect(
                                        color = zone.color,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, yStart),
                                        size = androidx.compose.ui.geometry.Size(width, yEnd - yStart)
                                    )
                                }

                                // Helper horizontal lines
                                for (i in 0..steps) {
                                    val yVal = (maxVal / steps) * i
                                    val yPos = yVal.toY()
                                    drawLine(
                                        color = gridColor.copy(alpha = 0.6f),
                                        start = androidx.compose.ui.geometry.Offset(0f, yPos),
                                        end = androidx.compose.ui.geometry.Offset(width, yPos),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Linha desenhada por tempo real (x), não por índice —
                                // gaps de sensor já não distorcem a escala temporal.
                                val path = androidx.compose.ui.graphics.Path()
                                plotPoints.forEachIndexed { index, point ->
                                    val x = point.timeSeconds.toX()
                                    val y = point.value.toY()
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = color,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )
                            }

                            // Y-axis text labels
                            Column(
                                modifier = Modifier.fillMaxHeight().padding(start = 4.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (i in steps downTo 0) {
                                    val yVal = (maxVal / steps) * i
                                    Text(
                                        text = yVal.toInt().toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = axisColor
                                    )
                                }
                            }
                        }

                        // X-axis (Time)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0:00", style = MaterialTheme.typography.labelSmall, color = axisColor)
                            Text(formatDuration(totalDurationSeconds / 2), style = MaterialTheme.typography.labelSmall, color = axisColor)
                            Text(formatDuration(totalDurationSeconds), style = MaterialTheme.typography.labelSmall, color = axisColor)
                        }
                    }
                }
            }
        }
    }
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
        ChartZone(max * 0.50, max * 0.60, Color(0xFF9E9E9E).copy(alpha = 0.15f)), // Z1 Recuperação
        ChartZone(max * 0.60, max * 0.70, Color(0xFF2196F3).copy(alpha = 0.15f)), // Z2 Resistência
        ChartZone(max * 0.70, max * 0.80, Color(0xFF4CAF50).copy(alpha = 0.15f)), // Z3 Tempo
        ChartZone(max * 0.80, max * 0.90, Color(0xFFFFC107).copy(alpha = 0.15f)), // Z4 Limiar
        ChartZone(max * 0.90, max, Color(0xFFF44336).copy(alpha = 0.15f))          // Z5 VO2max
    )
}

data class ChartZone(
    val min: Double,
    val max: Double,
    val color: Color
)
