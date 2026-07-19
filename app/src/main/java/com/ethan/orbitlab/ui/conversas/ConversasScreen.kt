package com.ethan.orbitlab.ui.conversas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.ui.theme.OrbitTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversasScreen(onOpenChat: (String) -> Unit = {}) {
    var query by remember { mutableStateOf("") }
    val conversas by ChatRepository.conversas.collectAsState()
    
    // Estado de seleção
    val selecionados = remember { mutableStateListOf<String>() }
    val isSelectionMode = selecionados.isNotEmpty()
    
    val filtradas = conversas.filter { 
        it.titulo.contains(query, ignoreCase = true) || 
        it.preview.contains(query, ignoreCase = true)
    }

    // Intercepta botão voltar para sair do modo de seleção
    BackHandler(enabled = isSelectionMode) {
        selecionados.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = 24.dp)
    ) {
        // Cabeçalho fixo no topo
        Column(Modifier.padding(horizontal = 24.dp)) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selecionados.clear() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = OrbitTokens.textHigh)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${selecionados.size} selecionadas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { 
                        ChatRepository.deletarConversas(selecionados)
                        selecionados.clear() 
                    }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Apagar", tint = OrbitTokens.danger)
                    }
                }
            } else {
                Text("Suas Conversas", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Spacer(Modifier.height(24.dp))
                SearchBar(query = query, onQueryChange = { query = it })
            }
            Spacer(Modifier.height(24.dp))
        }
        
        // Lista de Conversas
        ListaConversas(
            conversas = filtradas, 
            selecionados = selecionados,
            onOpenChat = { id ->
                if (isSelectionMode) {
                    if (selecionados.contains(id)) selecionados.remove(id) else selecionados.add(id)
                } else {
                    onOpenChat(id)
                }
            },
            onLongPress = { id ->
                if (!isSelectionMode) selecionados.add(id)
            }
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrbitTokens.surfaceRaised)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = OrbitTokens.textLow, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = OrbitTokens.textHigh, fontSize = 15.sp),
            modifier = Modifier.weight(1f),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text("Buscar no histórico...", color = OrbitTokens.textLow, fontSize = 15.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun ListaConversas(
    conversas: List<Conversa>, 
    selecionados: List<String>,
    onOpenChat: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(conversas, key = { it.id }) { conv ->
            val isSelected = selecionados.contains(conv.id)
            ConversaItem(conv, isSelected, onOpenChat, onLongPress)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversaItem(
    conv: Conversa, 
    isSelected: Boolean,
    onOpenChat: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    val borderColor = if (isSelected) OrbitTokens.accent else OrbitTokens.borderSoft
    val backgroundColor = if (isSelected) OrbitTokens.accent.copy(alpha = 0.1f) else OrbitTokens.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = { onOpenChat(conv.id) },
                onLongClick = { onLongPress(conv.id) }
            )
            .padding(22.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Título e Descrição ocupando todo o card
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = conv.titulo, 
                    color = OrbitTokens.textHigh, 
                    fontSize = 17.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = conv.tempoFormatado, color = OrbitTokens.textLow, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = conv.preview, 
                color = OrbitTokens.textMid, 
                fontSize = 15.sp, 
                lineHeight = 22.sp,
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun ConversasScreenPreview() {
    ConversasScreen()
}
