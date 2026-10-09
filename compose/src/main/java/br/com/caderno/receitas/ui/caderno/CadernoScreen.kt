package br.com.caderno.receitas.ui.caderno

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.caderno.receitas.R
import br.com.caderno.receitas.dados.preferencias.FiltroCaderno
import br.com.caderno.receitas.dominio.ItemCaderno
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.ui.componentes.CartaoReceita
import br.com.caderno.receitas.ui.componentes.EstrelasNota
import br.com.caderno.receitas.ui.componentes.ImagemReceita
import br.com.caderno.receitas.ui.componentes.TelaCarregando
import br.com.caderno.receitas.ui.componentes.TelaVazia
import br.com.caderno.receitas.ui.componentes.traduzirCategoria
import java.io.File
import java.util.Locale

@Composable
fun CadernoScreen(
    viewModel: CadernoViewModel,
    mostrarVoltar: Boolean,
    aoVoltar: () -> Unit,
    aoAbrirReceita: (String) -> Unit,
    aoExplorar: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    CadernoConteudo(
        estado = estado,
        mostrarVoltar = mostrarVoltar,
        aoVoltar = aoVoltar,
        aoEscolherFiltro = viewModel::aoEscolherFiltro,
        aoAbrirReceita = aoAbrirReceita,
        aoExplorar = aoExplorar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadernoConteudo(
    estado: CadernoUiState,
    mostrarVoltar: Boolean,
    aoVoltar: () -> Unit,
    aoEscolherFiltro: (FiltroCaderno) -> Unit,
    aoAbrirReceita: (String) -> Unit,
    aoExplorar: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meu_caderno)) },
                navigationIcon = {
                    if (mostrarVoltar) {
                        IconButton(onClick = aoVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar))
                        }
                    }
                },
            )
        },
    ) { espacos ->
        Box(modifier = Modifier.padding(espacos)) {
            AnimatedContent(
                targetState = estado,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "caderno",
            ) { atual ->
                when (atual) {
                    CadernoUiState.Carregando -> TelaCarregando(stringResource(R.string.carregando))
                    CadernoUiState.Erro -> TelaVazia(
                        emoji = "⚠️",
                        titulo = stringResource(R.string.caderno_erro_titulo),
                        mensagem = stringResource(R.string.caderno_erro),
                    )
                    CadernoUiState.Vazio -> TelaVazia(
                        emoji = "📖",
                        titulo = stringResource(R.string.caderno_vazio_titulo),
                        mensagem = stringResource(R.string.caderno_vazio),
                        textoAcao = stringResource(R.string.explorar_receitas),
                        aoAgir = aoExplorar,
                    )
                    is CadernoUiState.Conteudo -> ListaCaderno(atual, aoEscolherFiltro, aoAbrirReceita)
                }
            }
        }
    }
}

@Composable
private fun ListaCaderno(
    estado: CadernoUiState.Conteudo,
    aoEscolherFiltro: (FiltroCaderno) -> Unit,
    aoAbrirReceita: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "resumo") {
            val feitas = estado.contagem[FiltroCaderno.JA_FIZ] ?: 0
            val resumo = buildString {
                append(
                    if (feitas == 0) {
                        stringResource(R.string.nenhuma_feita)
                    } else {
                        pluralStringResource(R.plurals.resumo_feitas, feitas, feitas)
                    },
                )
                estado.mediaNotas?.let {
                    append(" · ")
                    append(stringResource(R.string.media_notas, String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it)))
                }
            }
            Text(resumo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { heading() })
        }
        item(key = "filtros") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FiltroCaderno.entries, key = { it.name }) { filtro ->
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { aoEscolherFiltro(filtro) },
                        label = { Text(stringResource(filtro.rotulo(), estado.contagem[filtro] ?: 0)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        if (estado.itens.isEmpty()) {
            item(key = "filtro-vazio") {
                Text(
                    stringResource(R.string.filtro_vazio),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(estado.itens, key = { it.registro.receitaId }) { item ->
            CartaoReceita(
                nome = item.nome,
                imagemUrl = item.imagemUrl,
                linhaDeApoio = traduzirCategoria(item.categoria),
                situacao = item.registro.situacao,
                aoClicar = { aoAbrirReceita(item.registro.receitaId) },
                modifier = Modifier.animateItem(),
                extra = { DetalhesDoRegistro(item) },
            )
        }
    }
}

@Composable
private fun DetalhesDoRegistro(item: ItemCaderno) {
    val registro = item.registro
    if (registro.situacao == Situacao.JA_FIZ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            registro.nota?.let { EstrelasNota(it) }
            Spacer(Modifier.width(8.dp))
            Text(
                pluralStringResource(R.plurals.vezes_feita, registro.vezesFeita, registro.vezesFeita),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    registro.observacao?.let {
        Text(
            "“$it”",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    registro.fotoCaminho?.let {
        ImagemReceita(
            modelo = File(it),
            descricao = stringResource(R.string.cd_foto_prato),
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

private fun FiltroCaderno.rotulo(): Int = when (this) {
    FiltroCaderno.TODAS -> R.string.filtro_todas
    FiltroCaderno.QUERO_FAZER -> R.string.filtro_quero_fazer
    FiltroCaderno.JA_FIZ -> R.string.filtro_ja_fiz
}
