package com.ethan.orbitlab.data.captura

import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.TipoLancamento
import com.ethan.orbitlab.data.financas.parsearReaisParaCentavos
import java.util.UUID

object CapturaParser {

    /**
     * Tenta extrair compra do título+texto. Retorna null se não for aviso de gasto
     * ou se o valor não der pra ler.
     */
    fun parsear(
        pacote: String,
        titulo: String?,
        texto: String?,
        quandoMs: Long,
        notifKey: String,
    ): CapturaAviso? {
        val banco = CapturaRegras.bancoPorPacote(pacote) ?: return null
        val bruto = listOfNotNull(titulo?.trim(), texto?.trim())
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (bruto.isBlank()) return null

        // Ignora avisos que claramente não são compra
        if (pareceRuido(bruto)) return null

        val regras = CapturaRegras.regras.filter {
            it.bancoId == banco.id || it.bancoId == "*"
        }
        for (regra in regras) {
            val m = regra.padrao.find(bruto) ?: continue
            val valorStr = m.groups[regra.valorGrupo]?.value ?: continue
            val centavos = parsearReaisParaCentavos(valorStr) ?: continue
            if (centavos <= 0L) continue
            val desc = m.groupValues.getOrNull(regra.descGrupo)
                ?.trim()
                ?.trim('·', '-', '—', ' ')
                ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
                ?: limparDescricao(titulo ?: banco.rotulo)
            return CapturaAviso(
                id = "cap_${notifKey.hashCode().toUInt().toString(16)}_${quandoMs}",
                bancoId = banco.id,
                bancoRotulo = banco.rotulo,
                pacote = pacote,
                valorCentavos = centavos,
                descricao = desc.take(80),
                quandoMs = quandoMs,
                raw = bruto.take(500),
                tipo = TipoLancamento.SAIDA,
            )
        }
        return null
    }

    /** Aviso de demonstração — pra validar a UI sem notificação real. */
    fun exemploDemo(bancoId: String = "nubank"): CapturaAviso {
        val banco = CapturaRegras.bancos.first { it.id == bancoId }
        return CapturaAviso(
            id = "cap_demo_${UUID.randomUUID().toString().take(8)}",
            bancoId = banco.id,
            bancoRotulo = banco.rotulo,
            pacote = banco.pacotes.first(),
            valorCentavos = 3290L,
            descricao = "IFOOD",
            quandoMs = System.currentTimeMillis(),
            raw = "${banco.rotulo} · Compra aprovada · R$ 32,90 em IFOOD",
            tipo = TipoLancamento.SAIDA,
        )
    }

    fun palpiteCategoria(descricao: String): String {
        val d = descricao.lowercase()
        val mapa = listOf(
            CategoriasFinanca.alimentacao.id to listOf(
                "ifood", "rappi", "uber eats", "mcdonald", "burger", "padaria",
                "mercado", "supermerc", "restaurante", "lanche", "cafe", "starbucks",
                "outback", "habib", "pizza",
            ),
            CategoriasFinanca.transporte.id to listOf(
                "uber", "99", "taxi", "posto", "shell", "ipiranga", "combust",
                "estaciona", "metro", "onibus", "passagem",
            ),
            CategoriasFinanca.lazer.id to listOf(
                "netflix", "spotify", "steam", "cinema", "ingresso", "playstation",
                "xbox", "ingresso", "disney", "hbo", "ingresso",
            ),
            CategoriasFinanca.saude.id to listOf(
                "farmacia", "drogaria", "raia", "pacheco", "hospital", "clinica",
                "laboratorio", "unimed",
            ),
            CategoriasFinanca.moradia.id to listOf(
                "aluguel", "condominio", "enel", "cemig", "sabesp", "internet",
                "vivo", "claro", "tim", "oi ",
            ),
            CategoriasFinanca.contas.id to listOf(
                "conta de", "fatura", "boleto", "anuidade",
            ),
        )
        for ((cat, palavras) in mapa) {
            if (palavras.any { it in d }) return cat
        }
        return CategoriasFinanca.outros.id
    }

    private fun pareceRuido(texto: String): Boolean {
        val t = texto.lowercase()
        val ruido = listOf(
            "senha", "token", "código", "codigo", "aprovação", "aprovacao",
            "entre no app", "atualize", "promoção", "promocao", "cashback disponível",
            "lembrete de fatura", "sua fatura fecha", "limite disponível",
            "transferência recebida", "transferencia recebida", "pix recebido",
            "depósito", "deposito",
        )
        return ruido.any { it in t }
    }

    private fun limparDescricao(s: String): String =
        s.replace(Regex("""(?i)compra aprovada|compra no débito|compra no credito|compra no crédito"""), "")
            .trim('·', '-', ' ', '—')
            .ifBlank { "Compra" }
}
