package br.com.caderno.receitas.ui.receita

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.caderno.receitas.dados.CadernoRepository
import br.com.caderno.receitas.dados.ReceitaRepository
import br.com.caderno.receitas.dominio.FalhaAoCarregar
import br.com.caderno.receitas.dominio.Receita
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.TipoFalha
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReceitaUiState {
    data object Carregando : ReceitaUiState
    data class Erro(val tipo: TipoFalha) : ReceitaUiState
    data class Conteudo(
        val receita: Receita,
        /** O que a pessoa registrou no caderno (null = ainda não registrou). */
        val registro: RegistroCaderno?,
        val desatualizada: Boolean,
        val atualizando: Boolean = false,
    ) : ReceitaUiState
}

private sealed interface Carga {
    data object Carregando : Carga
    data class Pronta(val receita: Receita, val desatualizada: Boolean, val atualizando: Boolean) : Carga
    data class Falhou(val tipo: TipoFalha) : Carga
}

/**
 * Detalhes de uma receita. Junta duas fontes: a receita (API/cache, carregada uma vez)
 * e o registro do caderno (Flow do Room: se a pessoa salvar no formulário, esta tela já mostra).
 */
class ReceitaViewModel(
    private val receitaId: String,
    private val receitas: ReceitaRepository,
    caderno: CadernoRepository,
) : ViewModel() {

    private val carga = MutableStateFlow<Carga>(Carga.Carregando)
    private var job: Job? = null

    val uiState: StateFlow<ReceitaUiState> =
        combine(carga, caderno.observarRegistro(receitaId)) { cargaAtual, registro ->
            when (cargaAtual) {
                Carga.Carregando -> ReceitaUiState.Carregando
                is Carga.Falhou -> ReceitaUiState.Erro(cargaAtual.tipo)
                is Carga.Pronta -> ReceitaUiState.Conteudo(
                    receita = cargaAtual.receita,
                    registro = registro,
                    desatualizada = cargaAtual.desatualizada,
                    atualizando = cargaAtual.atualizando,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceitaUiState.Carregando)

    init {
        carregar(forcar = false)
    }

    fun aoTentarDeNovo() = carregar(forcar = false)

    fun aoAtualizar() = carregar(forcar = true)

    private fun carregar(forcar: Boolean) {
        job?.cancel() // se já havia um carregamento em andamento, ele é cancelado
        job = viewModelScope.launch {
            val anterior = carga.value
            carga.value = if (forcar && anterior is Carga.Pronta) {
                anterior.copy(atualizando = true)
            } else {
                Carga.Carregando
            }
            carga.value = try {
                val resultado = receitas.receita(receitaId, forcarAtualizacao = forcar)
                Carga.Pronta(resultado.dados, resultado.desatualizado, atualizando = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FalhaAoCarregar) {
                Carga.Falhou(e.tipo)
            } catch (e: Exception) {
                Carga.Falhou(TipoFalha.SERVIDOR)
            }
        }
    }
}
