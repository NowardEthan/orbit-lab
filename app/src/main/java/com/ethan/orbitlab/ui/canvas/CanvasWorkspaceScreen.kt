package com.ethan.orbitlab.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.data.canvas.CanvasElement
import com.ethan.orbitlab.data.canvas.CanvasWorkspace
import com.ethan.orbitlab.data.canvas.ElementoBounds
import com.ethan.orbitlab.data.canvas.ElementoTipo
import com.ethan.orbitlab.data.canvas.ElementoStyle
import com.ethan.orbitlab.data.canvas.TextProps
import com.ethan.orbitlab.data.canvas.WorkspaceUi
import com.ethan.orbitlab.data.canvas.WorkspaceRepository
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import kotlinx.coroutines.launch

/**
 * Tela principal do Canvas Workspace.
 *
 * @param workspaceId ID do workspace a carregar
 * @param repository Repositório de workspaces
 * @param uid UID do usuário logado
 * @param onVoltar Callback para voltar à tela anterior
 * @param onAbrirChat Callback para abrir o chat com contexto do canvas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasWorkspaceScreen(
    workspaceId: String,
    repository: WorkspaceRepository,
    uid: String,
    onVoltar: () -> Unit,
    onAbrirChat: (workspaceId: String) -> Unit = {},
) {
    val workspace by repository.workspaceAtual.collectAsState()
    val elementos by repository.elementos.collectAsState()
    val loading by repository.loading.collectAsState()
    val error by repository.error.collectAsState()

    var selecionadoId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showAddElement by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Iniciar escuta
    LaunchedEffect(workspaceId) {
        repository.iniciarEscutaWorkspace(uid, workspaceId)
    }

    // Limpar ao sair
    DisposableEffect(Unit) {
        onDispose {
            repository.pararEscutaWorkspace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = workspace?.titulo ?: "Canvas",
                        style = OrbitType.Headline.SM,
                        color = OrbitTokens.textHiN,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = OrbitTokens.textHiN,
                        )
                    }
                },
                actions = {
                    // Botão de chat direto na toolbar
                    IconButton(onClick = { onAbrirChat(workspaceId) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Conversar com Luna",
                            tint = OrbitTokens.accent,
                        )
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Histórico",
                            tint = OrbitTokens.textMid,
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais",
                            tint = OrbitTokens.textMid,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Renomear") },
                            onClick = {
                                showMenu = false
                                // TODO: Dialog de renomear
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar") },
                            onClick = {
                                showMenu = false
                                repository.duplicar(uid, workspaceId) { _ -> }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Abrir no chat") },
                            onClick = {
                                showMenu = false
                                onAbrirChat(workspaceId)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Apagar", color = OrbitTokens.danger) },
                            onClick = {
                                showMenu = false
                                repository.apagar(uid, workspaceId) { onVoltar() }
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrbitTokens.graphiteBg,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddElement = true },
                containerColor = OrbitTokens.accent,
                contentColor = OrbitTokens.onBluePastel,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar elemento")
            }
        },
        containerColor = OrbitTokens.graphiteBg,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                loading && workspace == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = OrbitTokens.accent,
                        )
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Erro ao carregar",
                                style = OrbitType.Body.LG,
                                color = OrbitTokens.danger,
                            )
                            Text(
                                text = error ?: "",
                                style = OrbitType.Body.SM,
                                color = OrbitTokens.textMid,
                            )
                        }
                    }
                }
                else -> {
                    // Canvas principal
                    InfiniteCanvas(
                        elementos = elementos,
                        selecionadoId = selecionadoId,
                        onElementoClicado = { id ->
                            selecionadoId = if (selecionadoId == id) null else id
                        },
                        onCanvasClicado = { offset ->
                            selecionadoId = null
                            // TODO: Menu de contexto para criar elemento na posição
                        },
                        onBoundsChange = { id, bounds ->
                            repository.moverElemento(uid, workspaceId, id, bounds.x, bounds.y)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Toolbar (lateral esquerdo)
                    CanvasToolbar(
                        onAdicionarCard = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.card)
                        },
                        onAdicionarShape = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.shape)
                        },
                        onAdicionarTexto = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.text)
                        },
                        onAdicionarImagem = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.image)
                        },
                        onZoomIn = { /* TODO */ },
                        onZoomOut = { /* TODO */ },
                        onFitToScreen = { /* TODO */ },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp),
                    )

                    // Selection bar (inferior quando algo selecionado)
                    AnimatedVisibility(
                        visible = selecionadoId != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                    ) {
                        selecionadoId?.let { id ->
                            val el = elementos[id]
                            if (el != null) {
                                SelectionBar(
                                    elementoNome = el.tipo.name,
                                    onDelete = {
                                        repository.removerElemento(uid, workspaceId, id)
                                        selecionadoId = null
                                    },
                                    onDuplicate = {
                                        val novo = el.copy(
                                            id = CanvasElement.novoId(),
                                            bounds = el.bounds.copy(
                                                x = el.bounds.x + 20,
                                                y = el.bounds.y + 20,
                                            ),
                                        )
                                        repository.adicionarElemento(uid, workspaceId, novo)
                                        selecionadoId = novo.id
                                    },
                                    onBringToFront = { /* TODO: z-index */ },
                                    onSendToBack = { /* TODO: z-index */ },
                                )
                            }
                        }
                    }

                    // Minimap (inferior direito)
                    CanvasMinimap(
                        elementos = elementos,
                        viewportBounds = ElementoBounds(0f, 0f, 400f, 300f), // TODO: viewport real
                        onNavigate = { x, y -> /* TODO: navegar para posição */ },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                    )
                }
            }
        }
    }

    // Bottom sheet para adicionar elemento
    if (showAddElement) {
        ModalBottomSheet(
            onDismissRequest = { showAddElement = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = OrbitTokens.graphiteSurf,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(OrbitMetrics.pagePadding),
            ) {
                Text(
                    text = "Adicionar elemento",
                    style = OrbitType.Headline.SM,
                    color = OrbitTokens.textHiN,
                )
                Spacer(modifier = Modifier.padding(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AddElementButton(
                        label = "Card",
                        icon = Icons.Default.Add,
                        onClick = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.card)
                            showAddElement = false
                        },
                    )
                    AddElementButton(
                        label = "Shape",
                        icon = Icons.Default.Add,
                        onClick = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.shape)
                            showAddElement = false
                        },
                    )
                    AddElementButton(
                        label = "Texto",
                        icon = Icons.Default.TextFields,
                        onClick = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.text)
                            showAddElement = false
                        },
                    )
                    AddElementButton(
                        label = "Imagem",
                        icon = Icons.Default.Image,
                        onClick = {
                            adicionarElemento(repository, uid, workspaceId, ElementoTipo.image)
                            showAddElement = false
                        },
                    )
                }
                Spacer(modifier = Modifier.padding(24.dp))
            }
        }
    }

    // Bottom sheet de histórico
    if (showHistory) {
        CanvasHistorySheet(
            repository = repository,
            uid = uid,
            workspaceId = workspaceId,
            onDismiss = { showHistory = false },
            onRestaurar = { versao ->
                repository.restaurarVersao(uid, workspaceId, versao)
                showHistory = false
            },
        )
    }
}

@Composable
private fun AddElementButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteRaised)
            .padding(16.dp),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = OrbitTokens.textMid,
            )
        }
        Text(
            text = label,
            style = OrbitType.Body.XS,
            color = OrbitTokens.textMid,
        )
    }
}

/**
 * Bottom sheet com histórico de versões.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasHistorySheet(
    repository: WorkspaceRepository,
    uid: String,
    workspaceId: String,
    onDismiss: () -> Unit,
    onRestaurar: (Int) -> Unit,
) {
    var versoes by remember { mutableStateOf<List<com.ethan.orbitlab.data.canvas.CanvasVersaoUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(workspaceId) {
        loading = true
        versoes = repository.listarVersoes(uid, workspaceId)
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = OrbitTokens.graphiteSurf,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OrbitMetrics.pagePadding),
        ) {
            Text(
                text = "Histórico",
                style = OrbitType.Headline.SM,
                color = OrbitTokens.textHiN,
            )
            Spacer(modifier = Modifier.padding(16.dp))

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (versoes.isEmpty()) {
                Text(
                    text = "Sem versões salvas",
                    style = OrbitType.Body.MD,
                    color = OrbitTokens.textMid,
                )
            } else {
                versoes.forEach { versao ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
                            .background(OrbitTokens.graphiteRaised)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Versão ${versao.numero}",
                                style = OrbitType.Body.MD,
                                color = OrbitTokens.textHiN,
                            )
                            Text(
                                text = "${versao.autor} • ${formatTimestamp(versao.timestamp)}",
                                style = OrbitType.Body.XS,
                                color = OrbitTokens.textMid,
                            )
                            versao.delta?.let {
                                Text(
                                    text = it,
                                    style = OrbitType.Body.XS,
                                    color = OrbitTokens.textLow,
                                )
                            }
                        }
                        IconButton(onClick = { onRestaurar(versao.numero) }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Restaurar",
                                tint = OrbitTokens.accent,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(8.dp))
                }
            }
            Spacer(modifier = Modifier.padding(24.dp))
        }
    }
}

private fun adicionarElemento(
    repository: WorkspaceRepository,
    uid: String,
    workspaceId: String,
    tipo: ElementoTipo,
) {
    val elemento = CanvasElement.novo(
        tipo = tipo,
        x = 100f,
        y = 100f,
        width = 150f,
        height = 100f,
    ).copy(
        style = ElementoStyle(fill = "#1A1C22"),
        textProps = if (tipo == ElementoTipo.text) TextProps(content = "Texto") else null,
    )
    repository.adicionarElemento(uid, workspaceId, elemento)
}

private fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "agora"
        diff < 3_600_000 -> "${diff / 60_000}m atrás"
        diff < 86_400_000 -> "${diff / 3_600_000}h atrás"
        else -> "${diff / 86_400_000}d atrás"
    }
}
