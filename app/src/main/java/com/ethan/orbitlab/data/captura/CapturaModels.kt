package com.ethan.orbitlab.data.captura

/**
 * Aviso bruto parseado no aparelho — nunca sobe ao servidor até o usuário confirmar
 * (aí vira [com.ethan.orbitlab.data.financas.Lancamento] com origem captura).
 */
data class CapturaAviso(
    val id: String,
    val bancoId: String,
    val bancoRotulo: String,
    val pacote: String,
    val valorCentavos: Long,
    val descricao: String,
    val quandoMs: Long,
    val raw: String,
    val tipo: String = "saida",
)

/** Sugestão pronta pra UI — categoria/carteira com palpite. */
data class SugestaoCaptura(
    val aviso: CapturaAviso,
    val categoriaId: String,
    val carteiraId: String?,
    val criadaEmMs: Long = System.currentTimeMillis(),
)

data class BancoCapturaDef(
    val id: String,
    val rotulo: String,
    val pacotes: List<String>,
)

enum class SaudeBanco {
    OK,
    QUIETO,
    NUNCA,
}

data class StatusBancoCaptura(
    val banco: BancoCapturaDef,
    val ultimoAvisoMs: Long?,
    val saude: SaudeBanco,
    val detalhe: String,
)

data class CapturaStatusGeral(
    val consentimento: Boolean,
    val listenerAtivo: Boolean,
    val bateriaOk: Boolean,
    val ultimaCapturaMs: Long?,
    val pendentes: Int,
    val bancos: List<StatusBancoCaptura>,
)
