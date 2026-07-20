package com.ethan.orbitlab.domain.rotina

const val BURACO_MINIMO = 20

fun blocosDoDia(blocos: List<BlocoRotina>, dia: DiaSemana): List<BlocoRotina> =
    blocos.filter { dia in it.dias }.sortedBy { it.inicio }

fun estadoAgora(
    blocos: List<BlocoRotina>,
    dia: DiaSemana,
    minutoAtual: Minuto,
): EstadoAgora {
    val hoje = blocosDoDia(blocos, dia)
    val atual = hoje.find { minutoAtual >= it.inicio && minutoAtual < it.fim }
    val proximoHoje = hoje.find { it.inicio > minutoAtual }

    if (atual != null) {
        return EstadoAgora(
            atual = atual,
            faltamMinutos = atual.fim - minutoAtual,
            proximo = proximoHoje,
            emMinutos = proximoHoje?.let { it.inicio - minutoAtual },
        )
    }

    if (proximoHoje != null) {
        return EstadoAgora(
            proximo = proximoHoje,
            emMinutos = proximoHoje.inicio - minutoAtual,
            livreMinutos = proximoHoje.inicio - minutoAtual,
        )
    }

    for (salto in 1..7) {
        val d = (dia + salto) % 7
        val primeiro = blocosDoDia(blocos, d).firstOrNull() ?: continue
        val minutosAteMeiaNoite = 24 * 60 - minutoAtual
        val diasInteiros = salto - 1
        return EstadoAgora(
            proximo = primeiro,
            emMinutos = minutosAteMeiaNoite + diasInteiros * 24 * 60 + primeiro.inicio,
            proximoEhAmanha = salto == 1,
        )
    }
    return EstadoAgora()
}

fun frasePresenca(e: EstadoAgora): String? {
    val atual = e.atual
    if (atual != null) {
        val falta = duracaoLegivel(e.faltamMinutos ?: 0)
        val base = "Você está em «${atual.titulo}» (falta $falta)"
        val proximo = e.proximo
        if (proximo != null && (e.emMinutos ?: 0) <= 120) {
            return "$base. A seguir: ${proximo.titulo}, às ${minutoParaHora(proximo.inicio)}."
        }
        return "$base."
    }

    val proximo = e.proximo ?: return null
    if ((e.emMinutos ?: 0) > 180) return null
    val falta = duracaoLegivel(e.emMinutos ?: 0)
    val quando = if (e.proximoEhAmanha) "amanhã" else "às ${minutoParaHora(proximo.inicio)}"
    val livre = e.livreMinutos?.let { " Tem ${duracaoLegivel(it)} livres até lá." }.orEmpty()
    return "Nada agora. Daqui a $falta ($quando): ${proximo.titulo}.$livre"
}

fun conflitos(blocos: List<BlocoRotina>, dia: DiaSemana): List<Pair<BlocoRotina, BlocoRotina>> {
    val hoje = blocosDoDia(blocos, dia)
    val pares = mutableListOf<Pair<BlocoRotina, BlocoRotina>>()
    for (i in 0 until hoje.lastIndex) {
        if (hoje[i].fim > hoje[i + 1].inicio) {
            pares.add(hoje[i] to hoje[i + 1])
        }
    }
    return pares
}

/** Itens da lista com buracos livres ≥ [BURACO_MINIMO] entre blocos. */
sealed class ItemListaRotina {
    data class Bloco(val bloco: BlocoRotina) : ItemListaRotina()
    data class Vazio(val minutos: Int) : ItemListaRotina()
}

fun itensComBuracos(blocosDoDiaOrdenados: List<BlocoRotina>): List<ItemListaRotina> {
    val saida = mutableListOf<ItemListaRotina>()
    blocosDoDiaOrdenados.forEachIndexed { i, b ->
        if (i > 0) {
            val anterior = blocosDoDiaOrdenados[i - 1]
            val vao = b.inicio - anterior.fim
            if (vao >= BURACO_MINIMO) saida.add(ItemListaRotina.Vazio(vao))
        }
        saida.add(ItemListaRotina.Bloco(b))
    }
    return saida
}
