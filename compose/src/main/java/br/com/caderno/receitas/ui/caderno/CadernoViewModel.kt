package br.com.caderno.receitas.ui.caderno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.caderno.receitas.dados.CadernoRepository
import br.com.caderno.receitas.dados.preferencias.FiltroCaderno
import br.com.caderno.receitas.dados.preferencias.PreferenciasRepository
import br.com.caderno.receitas.dominio.ItemCaderno
import br.com.caderno.receitas.dominio.Situacao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CadernoUiState {
    data object Carregando : CadernoUiState

    /** O caderno inteiro está vazio (nenhuma receita registrada). */
    data object Vazio : CadernoUiState

    data object Erro : CadernoUiState

    data class Conteudo(
        val itens: List<ItemCaderno>,
        val filtro: FiltroCaderno,
        val contagem: Map<FiltroCaderno, Int>,
        /** Média das notas das receitas já feitas (null se nenhuma tem nota). */
        val mediaNotas: Double?,
    ) : CadernoUiState
}

/** "Meu caderno": lista reativa do Room + filtro salvo no DataStore. */
class CadernoViewModel(
    caderno: CadernoRepository,
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    val uiState: StateFlow<CadernoUiState> =
        combine(caderno.observarItens(), preferencias.preferencias.map { it.filtroCaderno }) { itens, filtro ->
            if (itens.isEmpty()) {
                CadernoUiState.Vazio
            } else {
                val feitas = itens.filter { it.registro.situacao == Situacao.JA_FIZ }
                CadernoUiState.Conteudo(
                    itens = itens.filter { it.combinaCom(filtro) },
                    filtro = filtro,
                    contagem = FiltroCaderno.entries.associateWith { f -> itens.count { it.combinaCom(f) } },
                    mediaNotas = feitas.mapNotNull { it.registro.nota }.takeIf { it.isNotEmpty() }?.average(),
                )
            }
        }
            .catch { emit(CadernoUiState.Erro) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CadernoUiState.Carregando)

    fun aoEscolherFiltro(filtro: FiltroCaderno) {
        viewModelScope.launch { preferencias.salvarFiltroCaderno(filtro) }
    }
}

private fun ItemCaderno.combinaCom(filtro: FiltroCaderno): Boolean = when (filtro) {
    FiltroCaderno.TODAS -> true
    FiltroCaderno.QUERO_FAZER -> registro.situacao == Situacao.QUERO_FAZER
    FiltroCaderno.JA_FIZ -> registro.situacao == Situacao.JA_FIZ
}
