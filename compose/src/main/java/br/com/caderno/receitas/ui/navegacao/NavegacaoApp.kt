package br.com.caderno.receitas.ui.navegacao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import br.com.caderno.receitas.Dependencias
import br.com.caderno.receitas.R
import br.com.caderno.receitas.ui.caderno.CadernoScreen
import br.com.caderno.receitas.ui.caderno.CadernoViewModel
import br.com.caderno.receitas.ui.explorar.ExplorarScreen
import br.com.caderno.receitas.ui.explorar.ExplorarViewModel
import br.com.caderno.receitas.ui.receita.ReceitaScreen
import br.com.caderno.receitas.ui.receita.ReceitaViewModel
import br.com.caderno.receitas.ui.registro.RegistroScreen
import br.com.caderno.receitas.ui.registro.RegistroViewModel
import kotlinx.coroutines.flow.map

/**
 * Raiz da interface. A navegação (Navigation 3) é uma pilha de rotas que o próprio app controla:
 * abrir uma tela = `pilha.add(rota)`, voltar = `pilha.removeLastOrNull()`.
 *
 * Com a estratégia "lista + detalhe", em telas largas (tablet, celular deitado grande) a lista
 * e a receita aparecem lado a lado; no celular em pé, uma de cada vez.
 *
 * @param linkDeReceita id vindo de um deep link (caderno://receita/ID), se houver.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CadernoDeReceitasApp(
    dependencias: Dependencias,
    linkDeReceita: String? = null,
    aoConsumirLink: () -> Unit = {},
) {
    val pilha = rememberNavBackStack(RotaExplorar)
    val estrategiaListaDetalhe = rememberListDetailSceneStrategy<NavKey>()

    val quantidadeNoCaderno by remember(dependencias) {
        dependencias.caderno.observarSituacoes().map { it.size }
    }.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(linkDeReceita) {
        if (linkDeReceita != null) {
            pilha.abrirReceita(linkDeReceita)
            aoConsumirLink()
        }
    }

    NavDisplay(
        backStack = pilha,
        onBack = { pilha.removeLastOrNull() },
        sceneStrategies = listOf(estrategiaListaDetalhe),
        entryDecorators = listOf(
            // Guarda o estado "rememberSaveable" de cada tela da pilha.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Cada tela ganha o próprio ViewModel, descartado quando ela sai da pilha.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<RotaExplorar>(
                metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { PainelSemReceita() }),
            ) {
                val vm = viewModel {
                    ExplorarViewModel(
                        createSavedStateHandle(),
                        dependencias.receitas,
                        dependencias.caderno,
                        dependencias.preferencias,
                    )
                }
                ExplorarScreen(
                    viewModel = vm,
                    quantidadeNoCaderno = quantidadeNoCaderno,
                    aoAbrirReceita = { id -> pilha.abrirReceita(id) },
                    aoAbrirCaderno = { if (pilha.lastOrNull() != RotaCaderno) pilha.add(RotaCaderno) },
                )
            }
            entry<RotaCaderno>(
                metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { PainelSemReceita() }),
            ) {
                val vm = viewModel { CadernoViewModel(dependencias.caderno, dependencias.preferencias) }
                CadernoScreen(
                    viewModel = vm,
                    mostrarVoltar = pilha.size > 1,
                    aoVoltar = { pilha.removeLastOrNull() },
                    aoAbrirReceita = { id -> pilha.abrirReceita(id) },
                    aoExplorar = { pilha.voltarPara(RotaExplorar) },
                )
            }
            entry<RotaReceita>(metadata = ListDetailSceneStrategy.detailPane()) { rota ->
                val vm = viewModel {
                    ReceitaViewModel(rota.receitaId, dependencias.receitas, dependencias.caderno)
                }
                ReceitaScreen(
                    viewModel = vm,
                    mostrarVoltar = true,
                    aoVoltar = { pilha.removeLastOrNull() },
                    aoRegistrar = { pilha.add(RotaRegistro(rota.receitaId)) },
                )
            }
            entry<RotaRegistro>(metadata = ListDetailSceneStrategy.extraPane()) { rota ->
                val vm = viewModel {
                    RegistroViewModel(
                        rota.receitaId,
                        createSavedStateHandle(),
                        dependencias.receitas,
                        dependencias.caderno,
                    )
                }
                RegistroScreen(
                    viewModel = vm,
                    aoFechar = { if (pilha.lastOrNull() == rota) pilha.removeLastOrNull() },
                )
            }
        },
    )
}

/** Abre uma receita. Se já havia uma receita (ou formulário) no topo, ela é trocada, não empilhada. */
private fun NavBackStack<NavKey>.abrirReceita(id: String) {
    while (lastOrNull() is RotaReceita || lastOrNull() is RotaRegistro) removeAt(lastIndex)
    add(RotaReceita(id))
}

/** Volta até a rota indicada (ou recomeça nela, se ela não estiver na pilha). */
private fun NavBackStack<NavKey>.voltarPara(rota: NavKey) {
    val posicao = lastIndexOf(rota)
    if (posicao >= 0) {
        while (lastIndex > posicao) removeAt(lastIndex)
    } else {
        clear()
        add(rota)
    }
}

/** O que aparece no painel da direita (telas largas) antes de escolher uma receita. */
@Composable
private fun PainelSemReceita() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.escolha_uma_receita),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}
