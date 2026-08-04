package com.ethan.orbitlab.ui.chamados

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.chamados.AutorChamado
import com.ethan.orbitlab.data.chamados.Chamado
import com.ethan.orbitlab.data.chamados.ChamadoRascunho
import com.ethan.orbitlab.data.chamados.ChamadosRepository
import com.ethan.orbitlab.data.chamados.FirestoreChamados
import com.ethan.orbitlab.data.chamados.MensagemChamado
import com.ethan.orbitlab.data.chamados.StatusChamado
import com.ethan.orbitlab.data.chamados.TipoChamado
import com.ethan.orbitlab.data.chamados.contextoAppAtual
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Onde a tela está agora (ela navega dentro de si mesma — o C4 só a abre/fecha). */
private enum class Vista { LISTA, NOVO, CHAMADO }

/**
 * "Fala com o Ethan" — o canal humano de suporte, uma tela só que serve os dois lados.
 * Usuário comum: vê os próprios chamados e abre novos. Ethan ([ehAdmin]=true): vê a caixa
 * de entrada (todos) e responde. Quem escreve define o autor da mensagem.
 */
@Composable
fun FalaComEthanScreen(
    ehAdmin: Boolean = false,
    onFechar: () -> Unit = {},
) {
    var vista by remember { mutableStateOf(Vista.LISTA) }
    var abertoId by remember { mutableStateOf<String?>(null) }

    val meus by ChamadosRepository.meus.collectAsState()
    val todos by ChamadosRepository.todos.collectAsState()
    val lista = if (ehAdmin) todos else meus
    val aberto = abertoId?.let { id -> lista.firstOrNull { it.id == id } }

    fun voltar() {
        when (vista) {
            Vista.LISTA -> onFechar()
            else -> { vista = Vista.LISTA; abertoId = null }
        }
    }

    BackHandler(enabled = true) { voltar() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Cabecalho(
                titulo = when (vista) {
                    Vista.LISTA -> if (ehAdmin) "Caixa de entrada" else "Fala com o Ethan"
                    Vista.NOVO -> "Novo chamado"
                    Vista.CHAMADO -> aberto?.titulo ?: "Chamado"
                },
                subtitulo = when (vista) {
                    Vista.LISTA -> if (ehAdmin) "Quem falou com você" else "Falo eu, o Ethan — de pessoa pra pessoa"
                    Vista.CHAMADO -> aberto?.let { "${TipoChamado.rotulo(it.tipo)} · ${StatusChamado.rotulo(it.status)}" }
                    else -> null
                },
                onVoltar = ::voltar,
                fechar = vista == Vista.LISTA,
            )

            when (vista) {
                Vista.LISTA -> ListaVista(
                    lista = lista,
                    ehAdmin = ehAdmin,
                    onAbrir = { abertoId = it.id; vista = Vista.CHAMADO },
                    onNovo = { vista = Vista.NOVO },
                )
                Vista.NOVO -> NovoVista(
                    onAberto = { id -> abertoId = id; vista = Vista.CHAMADO },
                )
                Vista.CHAMADO -> {
                    val c = aberto
                    if (c == null) {
                        LaunchedEffect(Unit) { vista = Vista.LISTA }
                    } else {
                        ChamadoVista(chamado = c, ehAdmin = ehAdmin)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cabecalho(
    titulo: String,
    subtitulo: String?,
    onVoltar: () -> Unit,
    fechar: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onVoltar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (fechar) Icons.Rounded.Close else Icons.Rounded.ArrowBack,
                contentDescription = if (fechar) "Fechar" else "Voltar",
                tint = OrbitTokens.textMidN,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitulo.isNullOrBlank()) {
                Text(
                    subtitulo,
                    color = OrbitTokens.textMidN,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A cor carrega o tipo do chamado (sem pílula preenchida — só texto colorido). */
private fun corDoTipo(tipo: String): Color = when (tipo) {
    TipoChamado.BUG -> OrbitTokens.danger
    TipoChamado.IDEIA -> OrbitTokens.gold
    else -> OrbitTokens.bluePastel
}

/** A cor carrega o estado: aberto neutro, análise âmbar, respondido verde, resolvido apagado. */
private fun corDoStatus(status: String): Color = when (status) {
    StatusChamado.EM_ANALISE -> OrbitTokens.warning
    StatusChamado.RESPONDIDO -> OrbitTokens.online
    StatusChamado.RESOLVIDO -> OrbitTokens.textLowN
    else -> OrbitTokens.bluePastel
}

@Composable
private fun RotuloColorido(texto: String, cor: Color) {
    Text(
        texto.uppercase(),
        color = cor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
    )
}

private enum class Filtro(val rotulo: String) { TODOS("Todos"), ABERTOS("Abertos"), RESOLVIDOS("Resolvidos") }

@Composable
private fun ListaVista(
    lista: List<Chamado>,
    ehAdmin: Boolean,
    onAbrir: (Chamado) -> Unit,
    onNovo: () -> Unit,
) {
    var filtro by remember { mutableStateOf(Filtro.TODOS) }
    val filtrada = when (filtro) {
        Filtro.TODOS -> lista
        Filtro.ABERTOS -> lista.filter { it.status != StatusChamado.RESOLVIDO }
        Filtro.RESOLVIDOS -> lista.filter { it.status == StatusChamado.RESOLVIDO }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OrbitMetrics.pagePadding),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Filtro.values().forEach { f ->
                val ativo = f == filtro
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .orbitPressable(onClick = { filtro = f })
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        f.rotulo,
                        color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textLowN,
                        fontSize = 14.sp,
                        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(if (ativo) 20.dp else 0.dp)
                            .clip(CircleShape)
                            .background(if (ativo) OrbitTokens.bluePastel else Color.Transparent),
                    )
                }
            }
        }

        if (filtrada.isEmpty()) {
            EstadoVazioChamados(
                ehAdmin = ehAdmin,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = OrbitMetrics.pagePadding,
                    end = OrbitMetrics.pagePadding,
                    top = 4.dp,
                    bottom = 12.dp,
                ),
            ) {
                items(filtrada, key = { it.id }) { c ->
                    ChamadoRow(chamado = c, ehAdmin = ehAdmin, onClick = { onAbrir(c) })
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OrbitTokens.graphiteHair),
                    )
                }
            }
        }

        if (!ehAdmin) {
            Box(modifier = Modifier.padding(OrbitMetrics.pagePadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                        .background(OrbitTokens.bluePastel)
                        .orbitPressable(onClick = onNovo)
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "＋  Novo chamado",
                        color = OrbitTokens.onBluePastel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChamadoRow(
    chamado: Chamado,
    ehAdmin: Boolean,
    onClick: () -> Unit,
) {
    // O ponto não-lido é do lado de quem olha: admin quer ver o que o usuário mandou,
    // o usuário quer ver a resposta do Ethan.
    val naoLido = if (ehAdmin) chamado.naoLidoAdmin else chamado.naoLidoUsuario
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusChip))
            .orbitPressable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RotuloColorido(TipoChamado.rotulo(chamado.tipo), corDoTipo(chamado.tipo))
                RotuloColorido(StatusChamado.rotulo(chamado.status), corDoStatus(chamado.status))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                chamado.titulo,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val prefixo = if (chamado.ultimoAutor == AutorChamado.ETHAN) "Ethan: " else ""
            Text(
                text = if (ehAdmin) "${chamado.nomeUsuario} · ${chamado.ultimaMensagem}"
                else "$prefixo${chamado.ultimaMensagem}",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (naoLido) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.bluePastel),
            )
        }
    }
}

@Composable
private fun EstadoVazioChamados(ehAdmin: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.graphiteRaised)
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (ehAdmin) Icons.Rounded.Inbox else Icons.Rounded.SupportAgent,
                    contentDescription = null,
                    tint = OrbitTokens.textMidN,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (ehAdmin) "Nada por aqui" else "Fala comigo",
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (ehAdmin) "Quando alguém abrir um chamado, ele aparece aqui."
                else "Achou um problema, teve uma ideia ou ficou com dúvida? Me conta — sou eu do outro lado.",
                color = OrbitTokens.textMidN,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f),
            )
        }
    }
}

@Composable
private fun NovoVista(onAberto: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val session by AuthRepository.session.collectAsState()

    var tipo by remember { mutableStateOf(TipoChamado.BUG) }
    var titulo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    val rascunho = ChamadoRascunho(tipo = tipo, titulo = titulo, mensagem = mensagem)
    val podeEnviar = rascunho.valido() && !enviando && session != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OrbitMetrics.pagePadding),
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Sobre o quê?", color = OrbitTokens.textMidN, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoChamado.todos.forEach { t ->
                val ativo = t == tipo
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                        .background(if (ativo) OrbitTokens.graphiteRaised else OrbitTokens.graphiteSurf)
                        .border(
                            1.dp,
                            if (ativo) corDoTipo(t) else OrbitTokens.graphiteHair,
                            RoundedCornerShape(OrbitMetrics.radiusPill),
                        )
                        .orbitPressable(onClick = { tipo = t })
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        TipoChamado.rotulo(t),
                        color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textMidN,
                        fontSize = 13.sp,
                        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        CampoTexto(
            valor = titulo,
            onValor = { titulo = it },
            dica = "Título curto (ex.: o alarme não tocou)",
            linhaUnica = true,
        )
        Spacer(Modifier.height(12.dp))
        CampoTexto(
            valor = mensagem,
            onValor = { mensagem = it },
            dica = "Conta com detalhe o que rolou, o que você esperava…",
            linhaUnica = false,
            minAltura = 120.dp,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "Vou receber junto: ${contextoAppAtual()}",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
        )

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(if (podeEnviar) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                .orbitPressable(enabled = podeEnviar) {
                    val s = session ?: return@orbitPressable
                    enviando = true
                    val nome = s.displayName.ifBlank { s.email.substringBefore("@") }
                    scope.launch {
                        try {
                            val id = FirestoreChamados.abrir(
                                uid = s.uid,
                                nomeUsuario = nome,
                                rascunho = rascunho,
                                contextoApp = contextoAppAtual(),
                            )
                            onAberto(id)
                        } finally {
                            enviando = false
                        }
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                if (enviando) "Enviando…" else "Enviar pro Ethan",
                color = if (podeEnviar) OrbitTokens.onBluePastel else OrbitTokens.textLowN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(OrbitMetrics.pagePadding))
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onValor: (String) -> Unit,
    dica: String,
    linhaUnica: Boolean,
    minAltura: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(OrbitMetrics.radiusPill))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = valor,
            onValueChange = onValor,
            textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 15.sp, lineHeight = 21.sp),
            singleLine = linhaUnica,
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minAltura > 0.dp) Modifier.height(minAltura) else Modifier),
            decorationBox = { inner ->
                if (valor.isEmpty()) {
                    Text(dica, color = OrbitTokens.textLowN, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun ChamadoVista(chamado: Chamado, ehAdmin: Boolean) {
    val scope = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }

    // Ao abrir, zera o não-lido do meu lado.
    LaunchedEffect(chamado.id, ehAdmin) {
        FirestoreChamados.marcarLido(chamado.id, ehAdmin)
    }

    val mensagens by produceState(initialValue = emptyList<MensagemChamado>(), chamado.id) {
        val reg = FirestoreChamados.subscribeMensagens(chamado.id, onChange = { value = it })
        awaitDispose { reg.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (ehAdmin) {
            ControlesStatus(
                atual = chamado.status,
                onStatus = { s -> scope.launch { FirestoreChamados.atualizarStatus(chamado.id, s) } },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = OrbitMetrics.pagePadding,
                vertical = 12.dp,
            ),
        ) {
            if (!chamado.contextoApp.isNullOrBlank()) {
                items(listOf(chamado.contextoApp!!)) { ctx ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ctx,
                            color = OrbitTokens.textLowN,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            items(mensagens, key = { it.id }) { m ->
                val meu = (ehAdmin && m.autor == AutorChamado.ETHAN) ||
                    (!ehAdmin && m.autor == AutorChamado.USUARIO)
                Balao(mensagem = m, meu = meu)
                Spacer(Modifier.height(8.dp))
            }
        }

        ComposerChamado(
            valor = texto,
            onValor = { texto = it },
            onEnviar = {
                val t = texto.trim()
                if (t.isEmpty()) return@ComposerChamado
                texto = ""
                val autor = if (ehAdmin) AutorChamado.ETHAN else AutorChamado.USUARIO
                scope.launch { FirestoreChamados.enviar(chamado.id, autor, t) }
            },
        )
    }
}

@Composable
private fun ControlesStatus(atual: String, onStatus: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusChamado.todos.forEach { s ->
            val ativo = s == atual
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(OrbitMetrics.radiusChip))
                    .background(if (ativo) OrbitTokens.graphiteRaised else Color.Transparent)
                    .border(
                        1.dp,
                        if (ativo) corDoStatus(s) else OrbitTokens.graphiteHair,
                        RoundedCornerShape(OrbitMetrics.radiusChip),
                    )
                    .orbitPressable(onClick = { onStatus(s) })
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    StatusChamado.rotulo(s),
                    color = if (ativo) corDoStatus(s) else OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private val horaFmt = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

@Composable
private fun Balao(mensagem: MensagemChamado, meu: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (meu) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (meu) 16.dp else 4.dp,
                        bottomEnd = if (meu) 4.dp else 16.dp,
                    ),
                )
                .background(if (meu) OrbitTokens.bubbleUser else OrbitTokens.bubbleLuna)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                mensagem.texto,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
            if (mensagem.createdAtMs > 0L) {
                Spacer(Modifier.height(4.dp))
                Text(
                    horaFmt.format(Date(mensagem.createdAtMs)),
                    color = OrbitTokens.textLowN,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ComposerChamado(
    valor: String,
    onValor: (String) -> Unit,
    onEnviar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(OrbitTokens.graphiteRaised)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = valor,
                onValueChange = onValor,
                textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 15.sp, lineHeight = 21.sp),
                cursorBrush = SolidColor(OrbitTokens.bluePastel),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (valor.isEmpty()) {
                        Text("Escreve aqui…", color = OrbitTokens.textLowN, fontSize = 15.sp)
                    }
                    inner()
                },
            )
        }
        val ativo = valor.isNotBlank()
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (ativo) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                .orbitPressable(enabled = ativo, onClick = onEnviar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = "Enviar",
                tint = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textLowN,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
