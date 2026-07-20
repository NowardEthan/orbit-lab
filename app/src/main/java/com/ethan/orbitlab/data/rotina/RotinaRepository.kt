package com.ethan.orbitlab.data.rotina

import com.ethan.orbitlab.domain.rotina.BlocoRotina
import com.ethan.orbitlab.domain.rotina.EstadoDoBloco
import com.ethan.orbitlab.domain.rotina.ItensDoDia
import com.ethan.orbitlab.domain.rotina.ROTINA_NORMAL
import com.ethan.orbitlab.domain.rotina.RotinaSet
import com.ethan.orbitlab.domain.rotina.SubTarefa
import com.ethan.orbitlab.domain.rotina.rotinaVigenteEm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repositório in-memory da rotina (MVP).
 * Contrato alinhado ao Firestore do mobile — trocar a implementação depois.
 */
object RotinaRepository {

    private val hojeISO = RotinaFixtures.chaveDoDia()

    private val _sets = MutableStateFlow(RotinaFixtures.setsDemo(hojeISO))
    val sets: StateFlow<List<RotinaSet>> = _sets.asStateFlow()

    private val _blocos = MutableStateFlow(RotinaFixtures.blocosDemo())
    val blocos: StateFlow<List<BlocoRotina>> = _blocos.asStateFlow()

    private val _estadosHoje =
        MutableStateFlow(RotinaFixtures.estadosHojeDemo())
    val estadosHoje: StateFlow<Map<String, EstadoDoBloco>> = _estadosHoje.asStateFlow()

    private val _itensHoje =
        MutableStateFlow(RotinaFixtures.itensHojeDemo(hojeISO))
    val itensHoje: StateFlow<Map<String, ItensDoDia>> = _itensHoje.asStateFlow()

    /** null = seguir a vigente; senão id do set a ver/editar. */
    private val _setVisto = MutableStateFlow<String?>(null)
    val setVisto: StateFlow<String?> = _setVisto.asStateFlow()

    fun vigenteHoje(): String = rotinaVigenteEm(_sets.value, hojeISO)

    fun setAtivo(): String = _setVisto.value ?: vigenteHoje()

    fun selecionarRotina(setId: String?) {
        // null ou "normal" → Normal (ausência de setId nos blocos)
        _setVisto.value = when (setId) {
            null, ROTINA_NORMAL -> null
            else -> setId
        }
    }

    fun blocosDoSetAtivo(): List<BlocoRotina> {
        val ativo = setAtivo()
        return _blocos.value.filter { b ->
            if (ativo == ROTINA_NORMAL) b.setId == null
            else b.setId == ativo
        }
    }

    fun alternarFeitoHoje(blocoId: String) {
        _estadosHoje.update { map ->
            val atual = map[blocoId]
            if (atual == EstadoDoBloco.Feito) {
                map - blocoId
            } else {
                map + (blocoId to EstadoDoBloco.Feito)
            }
        }
    }

    fun marcarTarefa(blocoId: String, tarefaId: String) {
        _itensHoje.update { mapa ->
            val itens = mapa[blocoId] ?: ItensDoDia()
            val feitas = itens.subsFeitas.toMutableSet()
            if (tarefaId in feitas) feitas.remove(tarefaId) else feitas.add(tarefaId)
            mapa + (blocoId to itens.copy(subsFeitas = feitas.toList()))
        }
    }

    fun adicionarTarefaDoDia(blocoId: String, texto: String) {
        val t = SubTarefa(id = RotinaFixtures.novoId(), texto = texto.trim())
        if (t.texto.isEmpty()) return
        _itensHoje.update { mapa ->
            val itens = mapa[blocoId] ?: ItensDoDia()
            mapa + (blocoId to itens.copy(tarefasDoDia = itens.tarefasDoDia + t))
        }
    }

    fun upsertBloco(bloco: BlocoRotina) {
        _blocos.update { lista ->
            val idx = lista.indexOfFirst { it.id == bloco.id }
            if (idx < 0) listOf(bloco) + lista
            else lista.toMutableList().also { it[idx] = bloco }
        }
    }

    fun apagarBloco(id: String) {
        _blocos.update { it.filterNot { b -> b.id == id } }
        _estadosHoje.update { it - id }
        _itensHoje.update { it - id }
    }
}
