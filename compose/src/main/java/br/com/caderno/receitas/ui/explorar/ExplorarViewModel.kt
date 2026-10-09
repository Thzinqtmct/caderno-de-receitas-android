package br.com.caderno.receitas.ui.explorar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.caderno.receitas.dados.CadernoRepository
import br.com.caderno.receitas.dados.ReceitaRepository
import br.com.caderno.receitas.dados.preferencias.PreferenciasRepository
import br.com.caderno.receitas.dominio.Categoria
import br.com.caderno.receitas.dominio.Consulta
import br.com.caderno.receitas.dominio.FalhaAoCarregar
import br.com.caderno.receitas.dominio.ResumoReceita
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estados da área de resultados: cada um tem um visual diferente na tela. */
sealed interface EstadoResultados {
    data object Carregando : EstadoResultados
    data class Conteudo(val receitas: List<ResumoReceita>, val desatualizado: Boolean) : EstadoResultados
    data class Vazio(val consulta: Consulta) : EstadoResultados
    data class Erro(val tipo: TipoFalha) : EstadoResultados
}

sealed interface EstadoCategorias {
    data object Carregando : EstadoCategorias
    data class Conteudo(val categorias: List<Categoria>) : EstadoCategorias
    data object Erro : EstadoCategorias
}

data class ExplorarUiState(
    val texto: String = "",
    val categoriaSelecionada: String? = null,
    val categorias: EstadoCategorias = EstadoCategorias.Carregando,
    val resultados: EstadoResultados = EstadoResultados.Carregando,
    val atualizando: Boolean = false,
    /** Situação de cada receita no caderno, para mostrar o selo na lista. */
    val situacoes: Map<String, Situacao> = emptyMap(),
) {
    /** Com texto digitado, a busca é por nome e nenhuma categoria fica destacada. */
    val buscandoPorNome: Boolean get() = texto.isNotBlank()
}

/**
 * ViewModel da tela Explorar. Fluxo unidirecional: a tela chama as funções `aoAlgumaCoisa`
 * (eventos) e só lê [uiState]. O texto digitado e a categoria ficam no [SavedStateHandle],
 * então sobrevivem à rotação e até ao Android matar o app em segundo plano.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExplorarViewModel(
    private val estadoSalvo: SavedStateHandle,
    private val receitas: ReceitaRepository,
    caderno: CadernoRepository,
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    private val texto: StateFlow<String> = estadoSalvo.getStateFlow(CHAVE_TEXTO, "")
    private val categoria: StateFlow<String?> = estadoSalvo.getStateFlow(CHAVE_CATEGORIA, null)
    private val categorias = MutableStateFlow<EstadoCategorias>(EstadoCategorias.Carregando)
    private val atualizando = MutableStateFlow(false)

    /** Pedidos de recarregar a busca atual: true = "puxou para atualizar" (ignora o cache). */
    private val recarregar = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private var jobCategorias: Job? = null

    /**
     * Qual busca fazer. O texto espera [ESPERA_DIGITACAO_MS] sem mudanças (debounce) para não
     * chamar a API a cada letra; trocar de categoria é imediato.
     */
    private val consulta: Flow<Consulta> = combine(
        texto.debounce { if (it.isBlank()) 0L else ESPERA_DIGITACAO_MS }.map { it.trim() },
        categoria.filterNotNull(),
    ) { textoAtual, categoriaAtual ->
        if (textoAtual.isEmpty()) Consulta.PorCategoria(categoriaAtual) else Consulta.PorNome(textoAtual)
    }.distinctUntilChanged()

    /**
     * flatMapLatest: quando chega uma consulta nova, a busca anterior (se ainda estiver
     * rodando) é CANCELADA automaticamente. Assim uma resposta antiga nunca sobrescreve a nova.
     */
    private val resultados: StateFlow<EstadoResultados> = consulta
        .flatMapLatest { atual ->
            recarregar.onStart { emit(false) }.flatMapLatest { forcar -> buscar(atual, forcar) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoResultados.Carregando)

    val uiState: StateFlow<ExplorarUiState> = combine(
        combine(texto, categoria, ::Pair),
        categorias,
        resultados,
        atualizando,
        caderno.observarSituacoes(),
    ) { (textoAtual, categoriaAtual), estadoCategorias, estadoResultados, estaAtualizando, situacoes ->
        ExplorarUiState(
            texto = textoAtual,
            categoriaSelecionada = categoriaAtual,
            categorias = estadoCategorias,
            resultados = estadoResultados,
            atualizando = estaAtualizando,
            situacoes = situacoes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExplorarUiState())

    init {
        // Primeira abertura: começa na última categoria usada (salva no DataStore).
        if (categoria.value == null) {
            viewModelScope.launch {
                estadoSalvo[CHAVE_CATEGORIA] = preferencias.preferencias.first().categoria
            }
        }
        carregarCategorias(forcar = false)
    }

    fun aoMudarTexto(novo: String) {
        estadoSalvo[CHAVE_TEXTO] = novo.take(MAX_TEXTO)
    }

    fun aoLimparTexto() {
        estadoSalvo[CHAVE_TEXTO] = ""
    }

    fun aoEscolherCategoria(nome: String) {
        estadoSalvo[CHAVE_TEXTO] = ""
        estadoSalvo[CHAVE_CATEGORIA] = nome
        viewModelScope.launch { preferencias.salvarCategoria(nome) }
    }

    /** "Puxar para atualizar" ou o botão do aviso de dados antigos: busca de novo na API. */
    fun aoAtualizar() {
        recarregar.tryEmit(true)
    }

    fun aoTentarDeNovo() {
        recarregar.tryEmit(false)
        if (categorias.value is EstadoCategorias.Erro) carregarCategorias(forcar = false)
    }

    fun aoTentarCategoriasDeNovo() = carregarCategorias(forcar = true)

    private fun buscar(consulta: Consulta, forcar: Boolean): Flow<EstadoResultados> = flow {
        // Ao atualizar, a lista atual continua visível (só aparece o indicador de atualização).
        if (forcar) atualizando.value = true else emit(EstadoResultados.Carregando)
        val estado = try {
            val carregado = receitas.buscar(consulta, forcarAtualizacao = forcar)
            if (carregado.dados.isEmpty()) {
                EstadoResultados.Vazio(consulta)
            } else {
                EstadoResultados.Conteudo(carregado.dados, carregado.desatualizado)
            }
        } catch (e: CancellationException) {
            throw e // a busca foi cancelada por uma mais nova: não é erro
        } catch (e: FalhaAoCarregar) {
            EstadoResultados.Erro(e.tipo)
        } catch (e: Exception) {
            EstadoResultados.Erro(TipoFalha.SERVIDOR)
        } finally {
            atualizando.value = false
        }
        emit(estado)
    }

    private fun carregarCategorias(forcar: Boolean) {
        jobCategorias?.cancel()
        jobCategorias = viewModelScope.launch {
            categorias.value = EstadoCategorias.Carregando
            categorias.value = try {
                EstadoCategorias.Conteudo(receitas.categorias(forcar).dados)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EstadoCategorias.Erro
            }
        }
    }

    companion object {
        const val ESPERA_DIGITACAO_MS = 500L
        const val MAX_TEXTO = 60
        private const val CHAVE_TEXTO = "texto"
        private const val CHAVE_CATEGORIA = "categoria"
    }
}
