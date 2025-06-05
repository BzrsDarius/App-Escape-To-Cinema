package com.example.appescapetocinema.ui.trivia

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appescapetocinema.R
import com.example.appescapetocinema.model.TriviaQuestion // Importar modelo

// --- TriviaScreen (UI Desacoplada) ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TriviaScreen(
    uiState: TriviaUiState,
    gameOverReason: GameOverReason?, // Null si no ha terminado
    onAnswerSelected: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onPlayAgain: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trivia de Cine", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            when {
                // --- Estado de Carga Inicial ---
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // --- Estado Fin del Juego ---
                uiState.isGameOver && gameOverReason != null -> {
                    GameOverContent(
                        score = uiState.currentScore,
                        totalQuestions = uiState.totalQuestions,
                        reason = gameOverReason,
                        onPlayAgain = onPlayAgain
                    )
                }
                // --- Mostrar Pregunta Actual ---
                uiState.currentQuestion != null -> {
                    QuestionContent(
                        question = uiState.currentQuestion,
                        questionNumber = uiState.currentQuestionIndex + 1, // +1 para mostrar (1 de 10)
                        totalQuestions = uiState.totalQuestions,
                        livesLeft = uiState.livesLeft,
                        timeLeft = uiState.timeLeft,
                        maxTime = uiState.maxTimePerQuestion,
                        timeRanOut = uiState.timeRanOut,
                        selectedAnswerIndex = uiState.selectedAnswerIndex,
                        answerSubmitted = uiState.answerSubmitted,
                        isAnswerCorrect = uiState.isAnswerCorrect,
                        onAnswerSelected = onAnswerSelected,
                        onSubmitAnswer = onSubmitAnswer,
                        onNextQuestion = onNextQuestion
                    )
                }
                // --- Estado Inesperado (ej. no cargó preguntas) ---
                else -> {
                    Text(
                        "Error al cargar la trivia.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// --- Composable para mostrar Pregunta y Opciones ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuestionContent(
    question: TriviaQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    livesLeft: Int,
    timeLeft: Int,
    maxTime: Int,
    timeRanOut: Boolean,
    selectedAnswerIndex: Int?,
    answerSubmitted: Boolean,
    isAnswerCorrect: Boolean?,
    onAnswerSelected: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(timeLeft, maxTime) {
        if (maxTime > 0) (timeLeft.toFloat() / maxTime.toFloat()).coerceIn(0f, 1f) else 0f
    }
    // Determina el color del progreso (ej. cambia a naranja/rojo cuando queda poco tiempo)
    val progressColor = when {
        timeRanOut -> MaterialTheme.colorScheme.error // Rojo si se acabó
        timeLeft <= 5 -> MaterialTheme.colorScheme.tertiary // Naranja si queda poco
        else -> MaterialTheme.colorScheme.primary // Cyan normal
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), // Para preguntas/respuestas largas
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // Separa elementos
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Información de Pregunta ---
            Text(
                text = "Pregunta $questionNumber de $totalQuestions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$timeLeft", // Muestra segundos restantes
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = progressColor // Color cambia con el tiempo
            )
            // --- Mostrar Vidas ---
            Row {
                repeat(livesLeft) { // Muestra un corazón por cada vida restante
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Vida", // Podría mejorarse para accesibilidad
                        tint = MaterialTheme.colorScheme.error, // Color rojo para vidas
                        modifier = Modifier.size(20.dp).padding(horizontal = 1.dp)
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress }, // El progreso calculado (usa lambda para animación)
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), // Más gruesa y redondeada
            color = progressColor, // Color dinámico
            trackColor = MaterialTheme.colorScheme.surfaceVariant // Color de fondo de la barra
        )
        Spacer(modifier = Modifier.height(16.dp)) // Espacio después del timer
        question.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.logo) // Reusa placeholder o crea uno específico
                    .error(R.drawable.logo)
                    .build(),
                contentDescription = "Imagen de la pregunta", // Descripción genérica
                modifier = Modifier
                    .fillMaxWidth() // Ocupa ancho
                    .height(200.dp) // Altura fija (ajustar)
                    .padding(bottom = 16.dp) // Espacio debajo
                    .clip(RoundedCornerShape(12.dp)), // Bordes redondeados
                contentScale = ContentScale.Fit // Ajusta imagen dentro de los límites
            )
        }

        // --- Texto de la Pregunta ---
        Text(
            text = question.questionText,
            style = MaterialTheme.typography.headlineSmall, // O titleLarge
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Opciones de Respuesta ---
        question.options.forEachIndexed { index, optionText ->
            val isSelected = selectedAnswerIndex == index
            val isCorrect = index == question.correctAnswerIndex
            val answerColor = when {
                answerSubmitted && isCorrect -> Color.Green.copy(alpha = 0.3f) // Fondo verde si correcta
                answerSubmitted && isSelected && !isCorrect -> Color.Red.copy(alpha = 0.3f) // Fondo rojo si seleccionada e incorrecta
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) // Fondo selección temporal
                else -> MaterialTheme.colorScheme.surfaceVariant // Fondo normal
            }
            val borderColor = when {
                answerSubmitted && isCorrect -> Color.Green // Borde verde si correcta
                answerSubmitted && isSelected && !isCorrect -> Color.Red // Borde rojo si seleccionada e incorrecta
                isSelected -> MaterialTheme.colorScheme.primary // Borde primario si seleccionada
                else -> MaterialTheme.colorScheme.outline // Borde normal
            }

            Surface( // Usamos Surface para poder aplicar borde y color fácilmente
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .selectable(
                        selected = isSelected,
                        onClick = { if (!answerSubmitted) onAnswerSelected(index) }, // Selecciona solo si no se ha enviado
                        role = Role.RadioButton // Para accesibilidad
                    ),
                shape = MaterialTheme.shapes.medium, // Forma con bordes redondeados
                color = answerColor, // Color de fondo basado en estado
                border = BorderStroke(1.dp, borderColor) // Borde basado en estado
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    // Mostrar icono de check/cruz si la respuesta fue enviada
                    AnimatedVisibility(visible = answerSubmitted && isSelected) {
                        Icon(
                            imageVector = if (isAnswerCorrect == true) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = if (isAnswerCorrect == true) "Correcto" else "Incorrecto",
                            tint = if (isAnswerCorrect == true) Color.Green else Color.Red
                        )
                    }
                }
            }
        } // Fin forEachIndexed

        Spacer(modifier = Modifier.height(24.dp))

        // --- Botón Enviar / Siguiente ---
        // Usamos Crossfade para animar el cambio de botón
        Crossfade(targetState = answerSubmitted, label = "SubmitNextButton") { submitted ->
            if (!submitted) {
                Button(
                    onClick = onSubmitAnswer,
                    enabled = selectedAnswerIndex != null // Habilitado solo si se seleccionó algo
                ) {
                    Text("Enviar Respuesta")
                }
            } else {
                Button(onClick = onNextQuestion) { // Botón para pasar a la siguiente
                    Text(if (questionNumber < totalQuestions) "Siguiente Pregunta" else "Ver Resultados")
                }
            }
        } // Fin Crossfade
        Spacer(modifier = Modifier.height(16.dp))
    } // Fin Column
}

// --- Composable para Pantalla Fin de Juego ---
@Composable
fun GameOverContent(
    score: Int,
    totalQuestions: Int,
    reason: GameOverReason,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Mensaje Principal basado en la razón ---
        Text(
            text = if (reason == GameOverReason.NO_LIVES) "¡Te quedaste sin vidas!" else "¡Trivia Completada!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        // --- Muestra puntuación ---
        Text(
            "Tu puntuación: $score de $totalQuestions",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onPlayAgain) {
            Text("Jugar de Nuevo")
        }
    }
}

@Composable
fun TriviaScreenContainer(
    navController: NavController,
    viewModel: TriviaViewModel = viewModel(factory = TriviaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- Determina la razón del Game Over ---
    val gameOverReason = if (uiState.isGameOver) {
        if (uiState.livesLeft <= 0) GameOverReason.NO_LIVES else GameOverReason.COMPLETED
    } else {
        null // No ha terminado
    }

    TriviaScreen(
        uiState = uiState,
        gameOverReason = gameOverReason, // <-- Pasa la razón calculada
        onAnswerSelected = viewModel::selectAnswer,
        onSubmitAnswer = viewModel::submitAnswer,
        onNextQuestion = viewModel::loadNextQuestion,
        onPlayAgain = viewModel::resetGame,
        onNavigateBack = { navController.popBackStack() }
    )
}

enum class GameOverReason {
    COMPLETED, // Terminó todas las preguntas
    NO_LIVES   // Se quedó sin vidas
}