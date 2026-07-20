package com.ethan.orbitlab.data.rotina

import com.ethan.orbitlab.domain.rotina.BlocoRotina
import com.ethan.orbitlab.domain.rotina.EstadoDoBloco
import com.ethan.orbitlab.domain.rotina.ItensDoDia
import com.ethan.orbitlab.domain.rotina.ROTINA_NORMAL
import com.ethan.orbitlab.domain.rotina.RotinaSet
import com.ethan.orbitlab.domain.rotina.SubTarefa
import com.ethan.orbitlab.domain.rotina.horaParaMinuto
import java.util.Calendar
import java.util.UUID

/**
 * Fixtures da rotina — dados locais até plugar o Firestore.
 * Espelha um dia típico do Ethan (Normal) + uma alternativa «Férias».
 */
object RotinaFixtures {

    fun setsDemo(hojeISO: String): List<RotinaSet> {
        // Alternativa que NÃO cobre hoje — só para o chip existir; calendário futuro.
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 14)
        val de = iso(cal)
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val ate = iso(cal)
        return listOf(
            RotinaSet(
                id = "ferias",
                nome = "Férias",
                cor = "#57C77E",
                de = de,
                ate = ate,
            ),
        )
    }

    fun blocosDemo(): List<BlocoRotina> {
        val diasUteis = listOf(1, 2, 3, 4, 5) // seg–sex
        val todos = listOf(0, 1, 2, 3, 4, 5, 6)
        return listOf(
            BlocoRotina(
                id = "acordar",
                titulo = "Acordar",
                dias = diasUteis,
                inicio = horaParaMinuto("07:00"),
                fim = horaParaMinuto("07:30"),
                cor = "#E6B84A",
                notificar = true,
                alarme = true,
                subtarefas = listOf(
                    SubTarefa(id = "a1", texto = "Levantar da cama"),
                    SubTarefa(id = "a2", texto = "Beber água"),
                ),
                roteiro = "Começa leve. Sem ecrã nos primeiros 10 minutos.",
            ),
            BlocoRotina(
                id = "trabalho",
                titulo = "Trabalho",
                dias = diasUteis,
                inicio = horaParaMinuto("09:00"),
                fim = horaParaMinuto("17:00"),
                cor = "#6AA0FF",
                notificar = true,
                subtarefas = listOf(
                    SubTarefa(id = "t1", texto = "Rever inbox", hora = horaParaMinuto("09:15")),
                    SubTarefa(id = "t2", texto = "Bloco de foco profundo"),
                    SubTarefa(id = "t3", texto = "Stand-up / sync"),
                ),
                roteiro = "Dois blocos de foco antes do almoço. Protege o primeiro.",
                guia = "Pomodoro 50/10. Sem redes no primeiro bloco.",
            ),
            BlocoRotina(
                id = "almoco",
                titulo = "Almoço",
                dias = diasUteis,
                inicio = horaParaMinuto("12:00"),
                fim = horaParaMinuto("13:00"),
                cor = "#E08A5B",
                notificar = true,
                subtarefas = listOf(
                    SubTarefa(id = "al1", texto = "Preparar comida"),
                    SubTarefa(id = "al2", texto = "Comer sem ecrã"),
                ),
            ),
            BlocoRotina(
                id = "treino",
                titulo = "Treino",
                dias = listOf(1, 3, 5),
                inicio = horaParaMinuto("18:30"),
                fim = horaParaMinuto("19:30"),
                cor = "#57C77E",
                notificar = true,
                subtarefas = listOf(
                    SubTarefa(id = "tr1", texto = "Aquecimento 5 min"),
                    SubTarefa(id = "tr2", texto = "Série principal"),
                ),
            ),
            BlocoRotina(
                id = "estudo",
                titulo = "Estudo",
                dias = listOf(2, 4),
                inicio = horaParaMinuto("20:00"),
                fim = horaParaMinuto("21:30"),
                cor = "#9B7DD9",
                notificar = true,
                subtarefas = listOf(
                    SubTarefa(id = "e1", texto = "Abrir a trilha Lumen"),
                    SubTarefa(id = "e2", texto = "Uma estrela completa"),
                ),
                roteiro = "Uma estrela = um assunto atómico. Não force duas.",
            ),
            // Bloco da alternativa Férias
            BlocoRotina(
                id = "ferias-manha",
                titulo = "Manhã livre",
                dias = todos,
                inicio = horaParaMinuto("09:00"),
                fim = horaParaMinuto("12:00"),
                cor = "#57C77E",
                setId = "ferias",
                subtarefas = listOf(
                    SubTarefa(id = "f1", texto = "Passeio ou descanso"),
                ),
            ),
        )
    }

    fun itensHojeDemo(hojeISO: String): Map<String, ItensDoDia> = mapOf(
        "trabalho" to ItensDoDia(
            subsFeitas = listOf("t1"),
            tarefasDoDia = listOf(
                SubTarefa(id = "hoje-1", texto = "Enviar patch do OrbitLab"),
                SubTarefa(id = "hoje-2", texto = "Revisar PR do auto-update"),
            ),
        ),
        "acordar" to ItensDoDia(subsFeitas = listOf("a1", "a2")),
    )

    fun estadosHojeDemo(): Map<String, EstadoDoBloco> = mapOf(
        "acordar" to EstadoDoBloco.Feito,
    )

    fun chaveDoDia(cal: Calendar = Calendar.getInstance()): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    /** Dia da semana 0=dom … 6=sáb. */
    fun diaSemanaAgora(cal: Calendar = Calendar.getInstance()): Int {
        // Calendar: SUNDAY=1 … SATURDAY=7 → 0..6
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun minutoAtual(cal: Calendar = Calendar.getInstance()): Int {
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun iso(cal: Calendar): String = chaveDoDia(cal)

    fun novoId(): String = UUID.randomUUID().toString().take(8)
}
