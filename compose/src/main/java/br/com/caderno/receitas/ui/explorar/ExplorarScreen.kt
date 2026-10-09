package br.com.caderno.receitas.ui.explorar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.caderno.receitas.R
import br.com.caderno.receitas.dominio.Consulta
import br.com.caderno.receitas.dominio.ResumoReceita
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.ui.componentes.AvisoDesatualizado
import br.com.caderno.receitas.ui.componentes.CartaoReceita
import br.com.caderno.receitas.ui.componentes.TelaCarregando
import br.com.caderno.receitas.ui.componentes.TelaErro
import br.com.caderno.receitas.ui.componentes.TelaVazia
import br.com.caderno.receitas.ui.componentes.traduzirCategoria

/** Versão "com ViewModel": coleta o estado respeitando o ciclo de vida e repassa os eventos. */
@Composable
fun ExplorarScreen(
    viewModel: ExplorarViewModel,
    quantidadeNoCaderno: Int,
    aoAbrirReceita: (String) -> Unit,
    aoAbrirCaderno: () -> Unit,
) {
    // collectAsStateWithLifecycle: para de coletar quando o app vai para segundo plano.
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    ExplorarConteudo(
        estado = estado,
        quantidadeNoCaderno = quantidadeNoCaderno,
        aoMudarTexto = viewModel::aoMudarTexto,
        aoLimparTexto = viewModel::aoLimparTexto,
        aoEscolherCategoria = viewModel::aoEscolherCategoria,
        aoAtualizar = viewModel::aoAtualizar,
        aoTentarDeNovo = viewModel::aoTentarDeNovo,
        aoTentarCategoriasDeNovo = viewModel::aoTentarCategoriasDeNovo,
        aoAbrirReceita = aoAbrirReceita,
        aoAbrirCaderno = aoAbrirCaderno,
    )
}

/** Versão "sem estado": só desenha o que recebe (fácil de testar e de pré-visualizar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorarConteudo(
    estado: ExplorarUiState,
    quantidadeNoCaderno: Int,
    aoMudarTexto: (String) -> Unit,
    aoLimparTexto: () -> Unit,
    aoEscolherCategoria: (String) -> Unit,
    aoAtualizar: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoTentarCategoriasDeNovo: () -> Unit,
    aoAbrirReceita: (String) -> Unit,
    aoAbrirCaderno: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    val descricao = stringResource(R.string.cd_abrir_caderno, quantidadeNoCaderno)
                    IconButton(onClick = aoAbrirCaderno) {
                        BadgedBox(
                            badge = {
                                if (quantidadeNoCaderno > 0) Badge { Text(quantidadeNoCaderno.toString()) }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = descricao)
                        }
                    }
                },
            )
        },
    ) { espacos ->
        Column(
            modifier = Modifier
                .padding(espacos)
                .fillMaxSize(),
        ) {
            CampoBusca(
                texto = estado.texto,
                aoMudarTexto = aoMudarTexto,
                aoLimparTexto = aoLimparTexto,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LinhaCategorias(
                estado = estado,
                aoEscolherCategoria = aoEscolherCategoria,
                aoTentarDeNovo = aoTentarCategoriasDeNovo,
            )
            PullToRefreshBox(
                isRefreshing = estado.atualizando,
                onRefresh = aoAtualizar,
                modifier = Modifier.weight(1f),
            ) {
                // AnimatedContent: troca entre carregando/lista/vazio/erro com uma transição suave.
                AnimatedContent(
                    targetState = estado.resultados,
                    contentKey = { it::class },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "resultados",
                ) { resultados ->
                    when (resultados) {
                        EstadoResultados.Carregando ->
                            TelaCarregando(stringResource(R.string.carregando_receitas))

                        is EstadoResultados.Erro ->
                            TelaErro(resultados.tipo, aoTentarDeNovo = aoTentarDeNovo)

                        is EstadoResultados.Vazio -> TelaVazia(
                            emoji = "🍳",
                            titulo = stringResource(R.string.nada_encontrado),
                            mensagem = when (val consulta = resultados.consulta) {
                                is Consulta.PorNome -> stringResource(R.string.nada_encontrado_nome, consulta.texto)
                                is Consulta.PorCategoria -> stringResource(R.string.nada_encontrado_categoria)
                            },
                        )

                        is EstadoResultados.Conteudo -> ListaResultados(
                            receitas = resultados.receitas,
                            desatualizado = resultados.desatualizado,
                            situacoes = estado.situacoes,
                            aoAtualizar = aoAtualizar,
                            aoAbrirReceita = aoAbrirReceita,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoBusca(
    texto: String,
    aoMudarTexto: (String) -> Unit,
    aoLimparTexto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val foco = LocalFocusManager.current
    OutlinedTextField(
        value = texto,
        onValueChange = aoMudarTexto,
        label = { Text(stringResource(R.string.buscar_rotulo)) },
        placeholder = { Text(stringResource(R.string.buscar_exemplo)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = aoLimparTexto) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cd_limpar_busca))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun LinhaCategorias(
    estado: ExplorarUiState,
    aoEscolherCategoria: (String) -> Unit,
    aoTentarDeNovo: () -> Unit,
) {
    Box(modifier = Modifier.heightIn(min = 64.dp), contentAlignment = Alignment.CenterStart) {
        when (val categorias = estado.categorias) {
            EstadoCategorias.Carregando -> Text(
                stringResource(R.string.carregando_categorias),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            EstadoCategorias.Erro -> AssistChip(
                onClick = aoTentarDeNovo,
                label = { Text(stringResource(R.string.categorias_erro)) },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // LazyRow: só desenha os chips que aparecem na tela.
            is EstadoCategorias.Conteudo -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categorias.categorias, key = { it.nome }) { categoria ->
                    FilterChip(
                        selected = !estado.buscandoPorNome && categoria.nome == estado.categoriaSelecionada,
                        onClick = { aoEscolherCategoria(categoria.nome) },
                        label = { Text(traduzirCategoria(categoria.nome).orEmpty()) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListaResultados(
    receitas: List<ResumoReceita>,
    desatualizado: Boolean,
    situacoes: Map<String, Situacao>,
    aoAtualizar: () -> Unit,
    aoAbrirReceita: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "aviso") {
            AnimatedVisibility(visible = desatualizado) {
                AvisoDesatualizado(aoAtualizar = aoAtualizar, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        item(key = "titulo") {
            Text(
                pluralStringResource(R.plurals.receitas_encontradas, receitas.size, receitas.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(receitas, key = { it.id }) { receita ->
            CartaoReceita(
                nome = receita.nome,
                imagemUrl = receita.imagemUrl,
                linhaDeApoio = listOfNotNull(traduzirCategoria(receita.categoria), receita.origem)
                    .joinToString(" · ")
                    .ifEmpty { null },
                situacao = situacoes[receita.id],
                aoClicar = { aoAbrirReceita(receita.id) },
                // animateItem: os itens deslizam/esmaecem quando a lista muda.
                modifier = Modifier.animateItem(),
            )
        }
    }
}
