package com.ethan.orbitlab.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Mensagem(
    val id: String = UUID.randomUUID().toString(),
    val texto: String,
    val isLuna: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class Conversa(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val mensagens: List<Mensagem> = emptyList(),
    val ultimaAtualizacao: Long = System.currentTimeMillis()
) {
    val preview: String
        get() = mensagens.lastOrNull()?.texto ?: "Nenhuma mensagem ainda."
        
    val tempoFormatado: String
        get() {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return "Hoje, ${format.format(Date(ultimaAtualizacao))}"
        }
}

object ChatRepository {
    private val _conversas = MutableStateFlow<List<Conversa>>(emptyList())
    val conversas: StateFlow<List<Conversa>> = _conversas.asStateFlow()

    init {
        // Popula com alguns mocks iniciais
        _conversas.value = listOf(
            Conversa(
                titulo = "O que é IA Generativa?",
                mensagens = listOf(
                    Mensagem(texto = "Olá! Como posso te ajudar hoje?", isLuna = true, timestamp = System.currentTimeMillis() - 100000),
                    Mensagem(texto = "Quero revisar IA Generativa.", isLuna = false, timestamp = System.currentTimeMillis() - 80000),
                    Mensagem(texto = "Entendi, então a rede neural aprende os padrões e repete de maneira criativa. Vamos testar um quiz?", isLuna = true, timestamp = System.currentTimeMillis() - 60000)
                ),
                ultimaAtualizacao = System.currentTimeMillis() - 60000
            ),
            Conversa(
                titulo = "Revisão de Metas",
                mensagens = listOf(
                    Mensagem(texto = "Você completou 3 blocos de foco profundo hoje. Excelente consistência!", isLuna = true)
                ),
                ultimaAtualizacao = System.currentTimeMillis() - 86400000
            )
        )
    }

    fun criarConversa(): String {
        val nova = Conversa(titulo = "Nova Conversa")
        _conversas.update { listOf(nova) + it }
        return nova.id
    }

    fun getConversa(id: String): Conversa? {
        return _conversas.value.find { it.id == id }
    }
    
    fun deletarConversas(ids: List<String>) {
        _conversas.update { lista ->
            lista.filterNot { it.id in ids }
        }
    }

    fun enviarMensagem(conversaId: String, texto: String, isLuna: Boolean = false) {
        _conversas.update { lista ->
            lista.map { conv ->
                if (conv.id == conversaId) {
                    // Atualiza o título se for a primeira mensagem do usuário
                    val novoTitulo = if (!isLuna && conv.mensagens.isEmpty()) {
                        texto.take(20) + if (texto.length > 20) "..." else ""
                    } else if (!isLuna && conv.titulo == "Nova Conversa") {
                        texto.take(20) + if (texto.length > 20) "..." else ""
                    } else {
                        conv.titulo
                    }
                    
                    conv.copy(
                        titulo = novoTitulo,
                        mensagens = conv.mensagens + Mensagem(texto = texto, isLuna = isLuna),
                        ultimaAtualizacao = System.currentTimeMillis()
                    )
                } else {
                    conv
                }
            }
        }
    }
}
