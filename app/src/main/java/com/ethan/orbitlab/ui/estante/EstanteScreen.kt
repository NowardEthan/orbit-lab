package com.ethan.orbitlab.ui.estante

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ethan.orbitlab.data.firebase.DocumentoUi
import com.ethan.orbitlab.data.firebase.FirestoreDocumentos
import com.ethan.orbitlab.data.firebase.FirestoreGaleria
import com.ethan.orbitlab.data.firebase.ImagemGaleria
import com.ethan.orbitlab.ui.chat.AttachmentKind
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.DocumentoReaderSheet
import com.ethan.orbitlab.ui.chat.ImagemGeradaIO
import com.ethan.orbitlab.ui.chat.MediaViewerDialog
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private enum class GaleriaAba { IMAGENS, ARTEFATOS }

/** O filtro de origem no grid de imagens — tudo, só as suas, ou só as que a Luna desenhou. */
private enum class FiltroOrigem(val rotulo: String) { TUDO("Tudo"), SUAS("Suas"), LUNA("Da Luna") }

/**
 * A Galeria — o lar de tudo que você e a Luna criaram, num lugar só. Duas visões:
 *
 *  - **Imagens**: as que a Luna desenhou e as que você anexou, num grid (ver [FirestoreGaleria]).
 *  - **Artefatos**: os documentos/criações da Luna, de qualquer conversa (ver [FirestoreDocumentos]).
 *
 * (O nome interno segue «Estante» de propósito — como a coleção `documentos` ficou apesar do rótulo
 * «artefato». O que muda é só o rosto: «Galeria». Sem migração, sem renomear meia dúzia de arquivos.)
 */
@Composable
fun EstanteScreen(onBack: (() -> Unit)? = null) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var aba by remember { mutableStateOf(GaleriaAba.IMAGENS) }

    // --- Artefatos (listener ao vivo, como antes) ---
    var artefatos by remember { mutableStateOf<List<DocumentoUi>>(emptyList()) }
    var carregandoArt by remember { mutableStateOf(true) }
    var busca by remember { mutableStateOf("") }
    var selecionado by remember { mutableStateOf<DocumentoUi?>(null) }
    var criando by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        val registro = uid?.let {
            FirestoreDocumentos.subscribeTodos(
                uid = it,
                onChange = { lista -> artefatos = lista; carregandoArt = false },
                onError = { carregandoArt = false },
            )
        }
        onDispose { registro?.remove() }
    }

    // --- Imagens (leitura sob demanda: varre as conversas uma vez) ---
    var imagens by remember { mutableStateOf<List<ImagemGaleria>>(emptyList()) }
    var carregandoImg by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) { carregandoImg = false; return@LaunchedEffect }
        carregandoImg = true
        imagens = runCatching { FirestoreGaleria.carregarImagens(uid) }.getOrDefault(emptyList())
        carregandoImg = false
    }

    val filtrados = remember(artefatos, busca) {
        val q = busca.trim()
        if (q.isBlank()) artefatos
        else artefatos.filter {
            it.titulo.contains(q, ignoreCase = true) || it.conteudo.contains(q, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        AtmosferaEstante()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp),
        ) {
            HeaderGaleria(aba = aba, totalImagens = imagens.size, totalArtefatos = artefatos.size)

            SegmentoGaleria(
                aba = aba,
                onAba = { aba = it },
                modifier = Modifier
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 10.dp, bottom = 4.dp),
            )

            when (aba) {
                GaleriaAba.IMAGENS -> GaleriaImagens(
                    imagens = imagens,
                    carregando = carregandoImg,
                    modifier = Modifier.weight(1f),
                )
                GaleriaAba.ARTEFATOS -> ArtefatosLista(
                    artefatos = artefatos,
                    filtrados = filtrados,
                    carregando = carregandoArt,
                    busca = busca,
                    onBusca = { busca = it },
                    onSelecionar = { selecionado = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // FAB criar página (só na aba Artefatos)
        if (aba == GaleriaAba.ARTEFATOS && uid != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = OrbitMetrics.pagePadding, bottom = 28.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.bluePastel)
                    .orbitPressable(enabled = !criando) {
                        criando = true
                        scope.launch {
                            val id = runCatching {
                                FirestoreDocumentos.criar(uid, titulo = "Sem título", conteudo = "")
                            }.getOrNull()
                            criando = false
                            if (id == null) {
                                Toast.makeText(context, "Não consegui criar", Toast.LENGTH_SHORT).show()
                            } else {
                                val criado = artefatos.firstOrNull { it.id == id }
                                if (criado != null) selecionado = criado
                                else Toast.makeText(context, "Página criada — toque pra abrir", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Nova página",
                    tint = OrbitTokens.graphiteBg,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }

    // O leitor tela cheia (o mesmo do chat) sobrepõe a galeria quando um artefato é tocado.
    selecionado?.let { doc ->
        DocumentoReaderSheet(doc = doc, onDismiss = { selecionado = null })
    }
}

// ---------------------------------------------------------------------------------------------
// Imagens
// ---------------------------------------------------------------------------------------------

@Composable
private fun GaleriaImagens(
    imagens: List<ImagemGaleria>,
    carregando: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var filtro by remember { mutableStateOf(FiltroOrigem.TUDO) }
    // Índice tocado (abre o visualizador) e a página visível dentro dele — o visualizador avisa
    // qual imagem está na tela pra o «baixar/partilhar» agir na certa.
    var abertoEm by remember { mutableStateOf<Int?>(null) }
    var paginaAtual by remember { mutableStateOf(0) }

    val visiveis = remember(imagens, filtro) {
        when (filtro) {
            FiltroOrigem.TUDO -> imagens
            FiltroOrigem.SUAS -> imagens.filter { !it.ehLuna }
            FiltroOrigem.LUNA -> imagens.filter { it.ehLuna }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (imagens.isNotEmpty()) {
            ChipsOrigem(
                filtro = filtro,
                onFiltro = { filtro = it },
                modifier = Modifier
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 4.dp, bottom = 8.dp),
            )
        }

        when {
            carregando && imagens.isEmpty() -> Unit // silêncio breve enquanto a varredura chega
            imagens.isEmpty() -> GaleriaVazia(
                icone = Icons.Rounded.Image,
                titulo = "Nenhuma imagem ainda",
                linha = "Peça uma imagem à Luna ou anexe uma foto no chat — elas se juntam aqui.",
            )
            visiveis.isEmpty() -> GaleriaVazia(
                icone = Icons.Rounded.Image,
                titulo = "Nada aqui",
                linha = if (filtro == FiltroOrigem.LUNA)
                    "A Luna ainda não desenhou nada. Peça uma imagem no chat."
                else "Você ainda não anexou nenhuma imagem no chat.",
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = OrbitMetrics.pagePadding,
                    end = OrbitMetrics.pagePadding,
                    top = 2.dp,
                    bottom = 120.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(visiveis, key = { it.id }) { img ->
                    CelulaImagem(
                        img = img,
                        onClick = { abertoEm = visiveis.indexOf(img) },
                    )
                }
            }
        }
    }

    // Visualizador tela cheia — desliza entre as imagens JÁ filtradas.
    abertoEm?.let { inicial ->
        val itens = remember(visiveis) { visiveis.map { it.toAttachment() } }
        if (itens.isNotEmpty()) {
            MediaViewerDialog(
                items = itens,
                initialIndex = inicial,
                onDismiss = { abertoEm = null },
                onPageChange = { paginaAtual = it },
                onDownload = {
                    val url = visiveis.getOrNull(paginaAtual)?.url ?: return@MediaViewerDialog
                    scope.launch {
                        val ok = ImagemGeradaIO.salvarNaGaleria(context, url)
                        Toast.makeText(
                            context,
                            if (ok) "Imagem salva na galeria" else "Não consegui salvar a imagem",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                onShareOverride = {
                    val url = visiveis.getOrNull(paginaAtual)?.url ?: return@MediaViewerDialog
                    scope.launch {
                        val ok = ImagemGeradaIO.compartilhar(context, url)
                        if (!ok) {
                            Toast.makeText(
                                context,
                                "Não consegui preparar a imagem",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun CelulaImagem(img: ImagemGaleria, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(img.url).crossfade(true).build(),
            contentDescription = img.prompt.ifBlank { if (img.ehLuna) "Imagem da Luna" else "Sua imagem" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Selo de origem — acende no cantinho (contraste, não moldura).
        SeloOrigem(
            ehLuna = img.ehLuna,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
        )
    }
}

@Composable
private fun SeloOrigem(ehLuna: Boolean, modifier: Modifier = Modifier) {
    val cor = if (ehLuna) OrbitTokens.bluePastel else OrbitTokens.gold
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xE6111214))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ehLuna) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(10.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = if (ehLuna) "Luna" else "Você",
            color = cor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChipsOrigem(
    filtro: FiltroOrigem,
    onFiltro: (FiltroOrigem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FiltroOrigem.entries.forEach { op ->
            val ativo = op == filtro
            val shape = RoundedCornerShape(999.dp)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(if (ativo) OrbitTokens.graphiteRaised else OrbitTokens.graphiteSurf)
                    .border(1.dp, if (ativo) OrbitTokens.bluePastel.copy(alpha = 0.5f) else OrbitTokens.graphiteHair, shape)
                    .orbitPressable(onClick = { onFiltro(op) })
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = op.rotulo,
                    color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textMidN,
                    fontSize = 13.sp,
                    fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

/** Converte a imagem da galeria num anexo que o [MediaViewerDialog] entende. */
private fun ImagemGaleria.toAttachment(): ComposerAttachment = ComposerAttachment(
    id = "gal-${url.hashCode()}",
    kind = AttachmentKind.IMAGE,
    name = prompt.ifBlank { if (ehLuna) "Imagem da Luna" else "Sua imagem" },
    sizeLabel = "—",
    mime = "image/*",
    uri = Uri.parse(url),
)

// ---------------------------------------------------------------------------------------------
// Artefatos (o que antes era a Estante inteira)
// ---------------------------------------------------------------------------------------------

@Composable
private fun ArtefatosLista(
    artefatos: List<DocumentoUi>,
    filtrados: List<DocumentoUi>,
    carregando: Boolean,
    busca: String,
    onBusca: (String) -> Unit,
    onSelecionar: (DocumentoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (artefatos.isNotEmpty()) {
            CampoBusca(
                valor = busca,
                onValor = onBusca,
                modifier = Modifier
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 4.dp, bottom = 4.dp),
            )
        }

        when {
            carregando && artefatos.isEmpty() -> Unit
            artefatos.isEmpty() -> GaleriaVazia(
                icone = Icons.Rounded.Description,
                titulo = "Nenhuma página ainda",
                linha = "Toque no + pra começar uma página vazia, ou peça à Luna pra escrever um plano, uma carta, uma nota.",
            )
            filtrados.isEmpty() -> GaleriaVazia(
                icone = Icons.Rounded.Description,
                titulo = "Nada encontrado",
                linha = "Nenhum artefato bate com «${busca.trim()}».",
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = OrbitMetrics.pagePadding,
                    end = OrbitMetrics.pagePadding,
                    top = 6.dp,
                    bottom = 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtrados, key = { it.id }) { doc ->
                    CartaoEstante(doc = doc, onClick = { onSelecionar(doc) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Chrome comum
// ---------------------------------------------------------------------------------------------

@Composable
private fun HeaderGaleria(aba: GaleriaAba, totalImagens: Int, totalArtefatos: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 2.dp),
    ) {
        Text(
            "Galeria",
            color = OrbitTokens.textHiN,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(1.dp))
        val sub = when (aba) {
            GaleriaAba.IMAGENS -> when (totalImagens) {
                0 -> "Imagens suas e da Luna"
                1 -> "1 imagem no seu acervo"
                else -> "$totalImagens imagens no seu acervo"
            }
            GaleriaAba.ARTEFATOS -> when (totalArtefatos) {
                0 -> "O que a Luna escreveu pra você"
                1 -> "1 artefato no seu acervo"
                else -> "$totalArtefatos artefatos no seu acervo"
            }
        }
        Text(text = sub, color = OrbitTokens.textMidN, fontSize = 12.sp)
    }
}

@Composable
private fun SegmentoGaleria(
    aba: GaleriaAba,
    onAba: (GaleriaAba) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SegmentoItem(Icons.Rounded.Image, "Imagens", aba == GaleriaAba.IMAGENS, Modifier.weight(1f)) {
            onAba(GaleriaAba.IMAGENS)
        }
        SegmentoItem(Icons.Rounded.Description, "Artefatos", aba == GaleriaAba.ARTEFATOS, Modifier.weight(1f)) {
            onAba(GaleriaAba.ARTEFATOS)
        }
    }
}

@Composable
private fun SegmentoItem(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    rotulo: String,
    ativo: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (ativo) OrbitTokens.graphiteRaised else Color.Transparent)
            .orbitPressable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (ativo) OrbitTokens.bluePastel else OrbitTokens.textLowN,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = rotulo,
            color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textMidN,
            fontSize = 14.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CampoBusca(
    valor: String,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusChip + 4.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = valor,
            onValueChange = onValor,
            textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 15.sp),
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (valor.isEmpty()) {
                    Text("Buscar nos artefatos…", color = OrbitTokens.textLowN, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun CartaoEstante(doc: DocumentoUi, onClick: () -> Unit) {
    // Espelha a linha da estante-no-fio (LinhaArtefatoEstante): cartão grafite quieto + ladrilho
    // NEUTRO com o ícone em azul pastel. O acento aparece só no ícone; contraste, não moldura.
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.titulo,
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = metaEstante(doc),
                color = OrbitTokens.textMidN,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun GaleriaVazia(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    linha: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.graphiteRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icone,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = linha,
                color = OrbitTokens.textMidN,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier.orbitEnter(),
            )
        }
    }
}

@Composable
private fun AtmosferaEstante() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(OrbitTokens.bluePastel.copy(alpha = 0.07f), Color.Transparent),
                        radius = 900f,
                    ),
                ),
        )
    }
}

/** "editado há 2 h · 340 palavras" — a linha discreta sob o título na estante. */
private fun metaEstante(doc: DocumentoUi): String {
    val editado = doc.updatedAtMs > doc.createdAtMs + 2000
    val verbo = if (editado) "editado" else "criado"
    val quando = tempoRelativoEstante(if (editado) doc.updatedAtMs else doc.createdAtMs)
    val palavras = contarPalavrasEstante(doc.conteudo)
    return buildList {
        add(listOf(verbo, quando).filter { it.isNotBlank() }.joinToString(" "))
        if (palavras > 0) add("$palavras ${if (palavras == 1) "palavra" else "palavras"}")
    }.joinToString("  ·  ")
}

private fun contarPalavrasEstante(texto: String): Int {
    val t = texto.trim()
    return if (t.isEmpty()) 0 else t.split(Regex("\\s+")).count { it.isNotBlank() }
}

private fun tempoRelativoEstante(ms: Long): String {
    if (ms <= 0L) return ""
    val diff = System.currentTimeMillis() - ms
    if (diff < 0) return "agora há pouco"
    val min = diff / 60_000
    val hora = diff / 3_600_000
    val dia = diff / 86_400_000
    return when {
        min < 1 -> "agora há pouco"
        min < 60 -> "há $min min"
        hora < 24 -> "há $hora h"
        dia == 1L -> "ontem"
        dia < 7 -> "há $dia dias"
        else -> "em " + java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
            .format(java.util.Date(ms))
    }
}
