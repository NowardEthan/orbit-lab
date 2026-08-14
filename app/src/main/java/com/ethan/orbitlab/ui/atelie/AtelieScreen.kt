package com.ethan.orbitlab.ui.atelie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable

data class PedidoAtelie(
    val tituloArtefato: String,
    val promptVisivel: String,
    val promptModelo: String,
    val rascunhoMarkdown: String,
)

private data class BriefAtelie(
    val titulo: String,
    val subtitulo: String,
    val icone: ImageVector,
    val prompt: String,
) {
    fun toPedido(): PedidoAtelie = PedidoAtelie(
        tituloArtefato = "Atelie - $titulo",
        promptVisivel = prompt,
        promptModelo = prompt,
        rascunhoMarkdown = rascunhoAtelie(titulo, subtitulo),
    )
}

private val briefsAtelie = listOf(
    BriefAtelie(
        titulo = "Folder comercial",
        subtitulo = "Peça para empresa, serviço ou lançamento",
        icone = Icons.Rounded.Description,
        prompt = """
            Quero criar um folder comercial como artefato de design.

            Antes de criar, se faltar informação essencial, me faça no máximo 3 perguntas. Se já der para começar, crie uma primeira versão com:
            - conceito visual
            - público e objetivo
            - headline, subtítulos, blocos de texto e CTA
            - direção de layout para frente e verso ou peça digital
            - paleta, tipografia e estilo de imagens
            - uma variação mais premium

            Use o sistema de artefatos da Luna. O resultado precisa virar um artefato renderizável, com títulos, callouts, listas, copy pronta, direção visual, especificação de layout e variações.
        """.trimIndent(),
    ),
    BriefAtelie(
        titulo = "Identidade visual",
        subtitulo = "Paleta, tipografia, voz e sistema de marca",
        icone = Icons.Rounded.AutoAwesome,
        prompt = """
            Quero criar uma proposta de identidade visual como artefato de design.

            Use o sistema de artefatos da Luna. Monte um concept renderizável com direção criativa, personalidade da marca, paleta, tipografia, uso de formas, estilo de imagem, tom de voz e exemplos de aplicação. Se faltar contexto, faça até 3 perguntas antes.
        """.trimIndent(),
    ),
    BriefAtelie(
        titulo = "Landing concept",
        subtitulo = "Oferta, narrativa visual e seções",
        icone = Icons.Rounded.Storefront,
        prompt = """
            Quero criar um concept de landing page como artefato de design.

            Use o sistema de artefatos da Luna. Trabalhe a narrativa, hierarquia visual, seções, headlines, CTAs, composição, ritmo, paleta e variações. Código só se eu pedir depois; agora quero design e direção renderizados como artefato.
        """.trimIndent(),
    ),
    BriefAtelie(
        titulo = "Tela de app",
        subtitulo = "Produto digital com fluxo e estética",
        icone = Icons.Rounded.PhoneAndroid,
        prompt = """
            Quero criar um concept de tela de app como artefato de design.

            Use o sistema de artefatos da Luna. Pense como designer de produto: objetivo da tela, hierarquia, estados, componentes, densidade, tom visual, microcopy e variações. Não comece pelo código; comece pelo design e renderize como artefato.
        """.trimIndent(),
    ),
    BriefAtelie(
        titulo = "Post ou banner",
        subtitulo = "Peça social com copy e direção visual",
        icone = Icons.Rounded.Image,
        prompt = """
            Quero criar um post/banner como artefato de design.

            Use o sistema de artefatos da Luna. Crie uma direção visual completa com formato, composição, headline, legenda curta, elementos gráficos, paleta, estilo de imagem e 3 variações de tom. O resultado deve ficar renderizável no leitor de artefatos.
        """.trimIndent(),
    ),
)

@Composable
fun AtelieScreen(
    onCriarDesign: (PedidoAtelie) -> Unit,
    onAbrirGaleria: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 48.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AtelieHero(onCriarDesign = onCriarDesign)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Começar por um brief",
                    fontFamily = Bricolage,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrbitTokens.textHiN,
                )
                Text(
                    "design, não só UI",
                    fontFamily = Bricolage,
                    fontSize = 12.sp,
                    color = OrbitTokens.textLowN,
                )
            }

            briefsAtelie.forEachIndexed { index, brief ->
                BriefCard(
                    brief = brief,
                    modifier = Modifier.orbitEnter((index + 1) * 18),
                    onClick = { onCriarDesign(brief.toPedido()) },
                )
            }

            GaleriaAtalho(onClick = onAbrirGaleria)
        }
    }
}

@Composable
private fun AtelieHero(onCriarDesign: (PedidoAtelie) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        OrbitTokens.graphiteRaised,
                        OrbitTokens.graphiteSurf,
                    ),
                ),
            )
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(OrbitTokens.bluePastel.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Atelie da Luna",
                fontFamily = Bricolage,
                fontSize = 31.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OrbitTokens.textHiN,
            )
            Text(
                "Peça concepts, folders, identidade, posts, telas e direções visuais. A Luna cria um artefato de design para você iterar.",
                fontFamily = Bricolage,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = OrbitTokens.textMidN,
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(OrbitTokens.bluePastel)
                .orbitPressable {
                    val prompt = """
                        Quero criar um artefato de design do zero.

                        Me ajude como diretora criativa usando o sistema de artefatos da Luna. Primeiro entenda o objetivo, público, formato e estilo desejado. Se faltar contexto, faça até 3 perguntas. Depois crie um artefato renderizável com direção visual, copy, composição, paleta, tipografia e variações.
                    """.trimIndent()
                    onCriarDesign(
                        PedidoAtelie(
                            tituloArtefato = "Atelie - Design livre",
                            promptVisivel = prompt,
                            promptModelo = prompt,
                            rascunhoMarkdown = rascunhoAtelie(
                                "Design livre",
                                "Brief aberto para a Luna transformar em concept",
                            ),
                        ),
                    )
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Criar com a Luna",
                    fontFamily = Bricolage,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrbitTokens.onBluePastel,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = OrbitTokens.onBluePastel,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun BriefCard(
    brief: BriefAtelie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(18.dp))
            .orbitPressable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                brief.icone,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                brief.titulo,
                fontFamily = Bricolage,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = OrbitTokens.textHiN,
            )
            Text(
                brief.subtitulo,
                fontFamily = Bricolage,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = OrbitTokens.textMidN,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun GaleriaAtalho(onClick: () -> Unit) {
    Spacer(Modifier.height(2.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Transparent)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(18.dp))
            .orbitPressable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            tint = OrbitTokens.textMidN,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Ver artefatos criados",
                fontFamily = Bricolage,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OrbitTokens.textHiN,
            )
            Text(
                "Concepts, imagens e documentos ficam na Galeria.",
                fontFamily = Bricolage,
                fontSize = 12.sp,
                color = OrbitTokens.textLowN,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun rascunhoAtelie(titulo: String, subtitulo: String): String = """
    # $titulo

    > [!note]
    > Rascunho criado pelo Atelie da Luna. A primeira resposta deve preencher este artefato com uma proposta visual completa.

    ## Brief
    $subtitulo

    ## Objetivo
    A definir com o Ethan.

    ## Direção visual
    A Luna deve propor conceito, composição, paleta, tipografia, ritmo visual e referências de aplicação.

    ## Copy e conteúdo
    A Luna deve escrever os blocos principais com texto pronto para uso.

    ## Variações
    A Luna deve sugerir pelo menos duas alternativas de direção.
""".trimIndent()
