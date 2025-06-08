package com.example.appescapetocinema.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.appescapetocinema.model.ChatMessage
import com.example.appescapetocinema.model.SenderType
import com.example.appescapetocinema.repository.ChatRepositoryImpl

@Composable
fun ChatScreenContainer(
    navController: NavController,
    chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(ChatRepositoryImpl()) // Instancia el repo aquí para la factory
    )
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState() // Para hacer scroll automático al nuevo mensaje

    // Hacer scroll al último mensaje cuando la lista de mensajes cambia
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ChatScreen(
        uiState = uiState,
        listState = listState,
        onInputChange = chatViewModel::onInputTextChange,
        onSendMessage = chatViewModel::sendMessage,
        onNavigateBack = { navController.popBackStack() }
    )
}

// --- Composable de UI Desacoplado ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState, // Tipo explícito
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CineBot Asistente", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.primary // Título en neón
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Lista de Mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = uiState.messages, key = { it.id }) { message ->
                    ChatMessageItem(message = message)
                }

                // Indicador de "Bot está escribiendo..."
                if (uiState.isBotTyping) {
                    item {
                        BotTypingIndicator()
                    }
                }
            }

            // Input del Usuario
            UserInputSection(
                currentInput = uiState.currentInput,
                onInputChange = onInputChange,
                onSendMessage = {
                    onSendMessage()
                },
                inputEnabled = uiState.inputEnabled,
                errorMessage = uiState.errorMessage // Para mostrar errores cerca del input
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.sender == SenderType.USER) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = when (message.sender) {
        SenderType.USER -> MaterialTheme.colorScheme.primaryContainer // Contenedor de tu color primario (NeonCyan)
        SenderType.BOT -> MaterialTheme.colorScheme.secondaryContainer // Contenedor de tu color secundario (NeonMagenta)
        SenderType.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    }
    val textColor = when (message.sender) {
        SenderType.USER -> MaterialTheme.colorScheme.onPrimaryContainer
        SenderType.BOT -> MaterialTheme.colorScheme.onSecondaryContainer
        SenderType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    val icon = when (message.sender) {
        SenderType.USER -> Icons.Filled.PersonOutline
        SenderType.BOT -> Icons.Filled.ChatBubbleOutline // O un icono de bot personalizado
        SenderType.ERROR -> Icons.Filled.ErrorOutline
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(alignment),
            verticalAlignment = Alignment.Bottom // Para que el icono quede alineado con la base del texto
        ) {
            // Mostrar icono a la izquierda para el BOT y a la derecha para el USER (opcional)
            if (message.sender == SenderType.BOT || message.sender == SenderType.ERROR) {
                Icon(
                    imageVector = icon,
                    contentDescription = message.sender.name,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp).padding(end = 4.dp, bottom = 4.dp)
                )
            }
            Card(
                shape = RoundedCornerShape(
                    topStart = if (message.sender == SenderType.USER) 16.dp else 4.dp,
                    topEnd = if (message.sender == SenderType.USER) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = textColor,
                    style = if (message.sender == SenderType.ERROR) MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                    else MaterialTheme.typography.bodyMedium // Orbitron Normal 14sp
                )
            }
            if (message.sender == SenderType.USER) {
                Icon(
                    imageVector = icon,
                    contentDescription = message.sender.name,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp).padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BotTypingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ChatBubbleOutline,
            contentDescription = "Bot",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp).padding(end = 4.dp)
        )
        Text(
            text = "CineBot está escribiendo...",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInputSection(
    currentInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    inputEnabled: Boolean,
    errorMessage: String?
) {
    Column {
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp)
                .navigationBarsPadding(), // Para que no se solape con la barra de gestos
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pregunta a CineBot...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (inputEnabled && currentInput.isNotBlank()) onSendMessage() }),
                enabled = inputEnabled,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendMessage,
                enabled = inputEnabled && currentInput.isNotBlank(),
                modifier = Modifier.size(48.dp), // Tamaño del botón
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary, // NeonCyan
                    contentColor = MaterialTheme.colorScheme.onPrimary, // OnPrimaryDark
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
            }
        }
    }
}