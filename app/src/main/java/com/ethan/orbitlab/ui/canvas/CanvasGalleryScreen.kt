package com.ethan.orbitlab.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.canvas.CanvasWorkspace
import com.ethan.orbitlab.data.canvas.WorkspaceUi
import com.ethan.orbitlab.data.canvas.WorkspaceRepository
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela de galeria de workspaces — lista de canvases do usuário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasGalleryScreen(
    repository: WorkspaceRepository,
    uid: String,
    onVoltar: () -> Unit,
    onAbrirWorkspace: (String) -> Unit,
) {
    val workspaces by repository.workspaces.collectAsState()
    val loading by repository.loading.collectAsState()
    val error by repository.error.collectAsState()

    var showNovoDialog by remember { mutableStateOf(false) }
    var novoTitulo by remember { mutableStateOf("") }

    // Iniciar escuta
    LaunchedEffect(uid) {
        repository.iniciarEscutaWorkspaces(uid)
    }

    // Limpar ao sair
    DisposableEffect(Unit) {
        onDispose {
            repository.pararEscutaWorkspaces()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Canvases",
                        style = OrbitType.Headline.MD,
                        color = OrbitTokens.textHiN,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrbitTokens.graphiteBg,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNovoDialog = true },
                containerColor = OrbitTokens.accent,
                contentColor = OrbitTokens.onBluePastel,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo canvas")
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
                loading && workspaces.isEmpty() -> {
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
                workspaces.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = OrbitTokens.textLow,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nenhum canvas ainda",
                                style = OrbitType.Headline.SM,
                                color = OrbitTokens.textMid,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toque em + para criar seu primeiro canvas",
                                style = OrbitType.Body.MD,
                                color = OrbitTokens.textLow,
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(OrbitMetrics.pagePadding),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = workspaces,
                            key = { it.id },
                        ) { ws ->
                            WorkspaceCard(
                                workspace = ws,
                                onClick = { onAbrirWorkspace(ws.id) },
                                onApagar = {
                                    repository.apagar(uid, ws.id) {}
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog para criar novo workspace
    if (showNovoDialog) {
        AlertDialog(
            onDismissRequest = { showNovoDialog = false },
            title = {
                Text(
                    text = "Novo canvas",
                    style = OrbitType.Headline.SM,
                )
            },
            text = {
                OutlinedTextField(
                    value = novoTitulo,
                    onValueChange = { novoTitulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.criar(uid, novoTitulo.ifBlank { "Canvas sem título" }) { id ->
                            showNovoDialog = false
                            novoTitulo = ""
                            onAbrirWorkspace(id)
                        }
                    },
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNovoDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = OrbitTokens.graphiteSurf,
        )
    }
}

/**
 * Card de workspace na galeria.
 */
@Composable
private fun WorkspaceCard(
    workspace: WorkspaceUi,
    onClick: () -> Unit,
    onApagar: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusMd))
            .background(OrbitTokens.graphiteSurf)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        // Preview placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = OrbitTokens.textLow,
            )
            // Elementos count badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(OrbitTokens.accent.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${workspace.elementoCount}",
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.onBluePastel,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workspace.titulo,
                    style = OrbitType.Body.MD,
                    color = OrbitTokens.textHiN,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatRelativeTime(workspace.atualizadoEm),
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.textLow,
                )
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        modifier = Modifier.size(18.dp),
                        tint = OrbitTokens.textMid,
                    )
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Apagar", color = OrbitTokens.danger) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        },
                    )
                }
            }
        }
    }

    // Confirmação de exclusão
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Apagar canvas?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onApagar()
                        showDeleteConfirm = false
                    },
                ) {
                    Text("Apagar", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = OrbitTokens.graphiteSurf,
        )
    }
}

private fun formatRelativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "agora"
        diff < 3_600_000 -> "${diff / 60_000}min"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> "${diff / 604_800_000}sem"
    }
}
