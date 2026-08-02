package com.ethan.orbitlab.data.captura

/**
 * Tabela de regras versionada — dados, não lógica espalhada.
 * Quando um banco muda o texto do aviso, sobe a [VERSAO] e ajusta o regex.
 * Futuro: baixar do manifesto de updates sem recompilar.
 */
object CapturaRegras {
    const val VERSAO = 1

    /** Dias sem aviso → banco marcado "quieto" (só se já teve ao menos um). */
    const val DIAS_QUIETO = 8

    val bancos = listOf(
        BancoCapturaDef(
            id = "nubank",
            rotulo = "Nubank",
            pacotes = listOf("com.nu.production"),
        ),
        BancoCapturaDef(
            id = "inter",
            rotulo = "Inter",
            pacotes = listOf("br.com.intermedium", "br.com.bancointer.android"),
        ),
        BancoCapturaDef(
            id = "itau",
            rotulo = "Itaú",
            pacotes = listOf("com.itau", "com.itau.iti"),
        ),
        BancoCapturaDef(
            id = "c6",
            rotulo = "C6 Bank",
            pacotes = listOf("com.c6bank.app"),
        ),
        BancoCapturaDef(
            id = "picpay",
            rotulo = "PicPay",
            pacotes = listOf("com.picpay"),
        ),
        BancoCapturaDef(
            id = "bradesco",
            rotulo = "Bradesco",
            pacotes = listOf("com.bradesco"),
        ),
        BancoCapturaDef(
            id = "santander",
            rotulo = "Santander",
            pacotes = listOf("com.santander.app"),
        ),
        BancoCapturaDef(
            id = "bb",
            rotulo = "Banco do Brasil",
            pacotes = listOf("com.bb.android", "br.com.bb.android"),
        ),
    )

    private val porPacote: Map<String, BancoCapturaDef> =
        bancos.flatMap { b -> b.pacotes.map { p -> p to b } }.toMap()

    fun bancoPorPacote(pacote: String): BancoCapturaDef? = porPacote[pacote]

    /**
     * Regexes por banco — cada um tenta extrair valor + descrição do texto do aviso.
     * Grupo 1 = valor (com ou sem R$), grupo 2 = estabelecimento (quando houver).
     */
    data class RegraParse(
        val bancoId: String,
        val padrao: Regex,
        val valorGrupo: Int = 1,
        val descGrupo: Int = 2,
        val exigeCompra: Boolean = true,
    )

    val regras: List<RegraParse> = listOf(
        // Nubank: "Compra aprovada · R$ 32,90 em IFOOD"
        RegraParse(
            "nubank",
            Regex(
                """(?i)compra.*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(?:em|no|na)\s+(.+)""",
            ),
        ),
        RegraParse(
            "nubank",
            Regex(
                """(?i)(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s+(?:em|no|na)\s+(.+)""",
            ),
        ),
        // Inter / genérico BR
        RegraParse(
            "inter",
            Regex(
                """(?i)(?:compra|pagamento|pix).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2}).*?(?:em|para|em favor de)?\s*(.+)""",
            ),
        ),
        RegraParse(
            "itau",
            Regex(
                """(?i)(?:compra|pagamento).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(?:em|no|na)?\s*(.*)""",
            ),
        ),
        RegraParse(
            "c6",
            Regex(
                """(?i)(?:compra|pagamento).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(?:em|no)?\s*(.*)""",
            ),
        ),
        RegraParse(
            "picpay",
            Regex(
                """(?i)(?:pagou|pagamento|compra).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(?:para|em)?\s*(.*)""",
            ),
        ),
        RegraParse(
            "bradesco",
            Regex(
                """(?i)(?:compra|pagamento).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(.*)""",
            ),
        ),
        RegraParse(
            "santander",
            Regex(
                """(?i)(?:compra|pagamento).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(.*)""",
            ),
        ),
        RegraParse(
            "bb",
            Regex(
                """(?i)(?:compra|pagamento|pix).*?(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\s*(.*)""",
            ),
        ),
        // Fallback genérico: qualquer R$ XX,XX no texto de um pacote conhecido
        RegraParse(
            "*",
            Regex("""(?i)R\$\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})\b(?:\s*(?:em|no|na)\s+(.+))?"""),
            exigeCompra = false,
        ),
    )
}
