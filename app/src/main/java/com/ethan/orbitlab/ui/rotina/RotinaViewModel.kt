package com.ethan.orbitlab.ui.rotina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethan.orbitlab.data.rotina.RotinaFixtures
import com.ethan.orbitlab.data.rotina.RotinaRepository
import com.ethan.orbitlab.domain.rotina.BlocoRotina
import com.ethan.orbitlab.domain.rotina.DiaSemana
import com.ethan.orbitlab.domain.rotina.EstadoAgora
import com.ethan.orbitlab.domain.rotina.EstadoDoBloco
import com.ethan.orbitlab.domain.rotina.ItemListaRotina
import com.ethan.orbitlab.domain.rotina.ItensDoDia
import com.ethan.orbitlab.domain.rotina.ROTINA_NORMAL
import com.ethan.orbitlab.domain.rotina.RotinaSet
import com.ethan.orbitlab.domain.rotina.blocosDoDia
import com.ethan.orbitlab.domain.rotina.conflitos
import com.ethan.orbitlab.domain.rotina.estadoAgora
import com.ethan.orbitlab.domain.rotina.itensComBuracos
import com.ethan.orbitlab.domain.rotina.rotinaVigenteEm
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RotinaUiState(
    val sets: List<RotinaSet> = emptyList(),
    val setAtivo: String = ROTINA_NORMAL,
    val vigenteHoje: String = ROTINA_NORMAL,
    val vendoAVigente: Boolean = true,
    val diaHoje: DiaSemana = 0,
    val diaVisto: DiaSemana = 0,
    val minutoAtual: Int = 0,
    val hojeISO: String = "",
    val blocos: List<BlocoRotina> = emptyList(),
    val itensLista: List<ItemListaRotina> = emptyList(),
    val estado: EstadoAgora = EstadoAgora(),
    val estadosHoje: Map<String, EstadoDoBloco> = emptyMap(),
    val itensHoje: Map<String, ItensDoDia> = emptyMap(),
    val conflitos: List<Pair<BlocoRotina, BlocoRotina>> = emptyList(),
    val contagemPorDia: Map<DiaSemana, Int> = emptyMap(),
)

class RotinaViewModel : ViewModel() {

    private val diaVisto = MutableStateFlow(RotinaFixtures.diaSemanaAgora())
    private val relogio = MutableStateFlow(agoraRelogio())

    private data class RepoSlice(
        val sets: List<RotinaSet>,
        val blocos: List<BlocoRotina>,
        val estados: Map<String, EstadoDoBloco>,
        val itens: Map<String, ItensDoDia>,
        val setVisto: String?,
    )

    private data class Relogio(val dia: DiaSemana, val minuto: Int, val iso: String)

    private val repo = combine(
        RotinaRepository.sets,
        RotinaRepository.blocos,
        RotinaRepository.estadosHoje,
        RotinaRepository.itensHoje,
        RotinaRepository.setVisto,
    ) { sets, blocos, estados, itens, setVisto ->
        RepoSlice(sets, blocos, estados, itens, setVisto)
    }

    val state: StateFlow<RotinaUiState> = combine(repo, diaVisto, relogio) { slice, diaV, rel ->
        val vigente = rotinaVigenteEm(slice.sets, rel.iso)
        val ativo = slice.setVisto ?: vigente
        val blocos = slice.blocos.filter { b ->
            if (ativo == ROTINA_NORMAL) b.setId == null else b.setId == ativo
        }
        val doDia = blocosDoDia(blocos, diaV)
        val ehHoje = diaV == rel.dia

        RotinaUiState(
            sets = slice.sets,
            setAtivo = ativo,
            vigenteHoje = vigente,
            vendoAVigente = ativo == vigente,
            diaHoje = rel.dia,
            diaVisto = diaV,
            minutoAtual = rel.minuto,
            hojeISO = rel.iso,
            blocos = blocos,
            itensLista = itensComBuracos(doDia),
            estado = if (ehHoje) estadoAgora(blocos, rel.dia, rel.minuto) else EstadoAgora(),
            estadosHoje = slice.estados,
            itensHoje = slice.itens,
            conflitos = if (ehHoje) conflitos(blocos, rel.dia) else emptyList(),
            contagemPorDia = (0..6).associateWith { d -> blocosDoDia(blocos, d).size },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RotinaUiState())

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                relogio.value = agoraRelogio()
            }
        }
    }

    fun selecionarDia(dia: DiaSemana) {
        diaVisto.value = dia
    }

    fun selecionarRotina(setId: String?) {
        RotinaRepository.selecionarRotina(setId)
    }

    fun alternarFeitoHoje(blocoId: String) {
        RotinaRepository.alternarFeitoHoje(blocoId)
    }

    fun marcarTarefa(blocoId: String, tarefaId: String) {
        RotinaRepository.marcarTarefa(blocoId, tarefaId)
    }

    fun adicionarTarefaDoDia(blocoId: String, texto: String) {
        RotinaRepository.adicionarTarefaDoDia(blocoId, texto)
    }

    private fun agoraRelogio() = Relogio(
        dia = RotinaFixtures.diaSemanaAgora(),
        minuto = RotinaFixtures.minutoAtual(),
        iso = RotinaFixtures.chaveDoDia(),
    )
}
