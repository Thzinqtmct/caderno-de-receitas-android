package br.com.caderno.receitas.ui.receita

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.caderno.receitas.R
import br.com.caderno.receitas.dominio.Receita
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.ui.componentes.AvisoDesatualizado
import br.com.caderno.receitas.ui.componentes.EstrelasNota
import br.com.caderno.receitas.ui.componentes.ImagemReceita
import br.com.caderno.receitas.ui.componentes.SeloSituacao
import br.com.caderno.receitas.ui.componentes.TelaCarregando
import br.com.caderno.receitas.ui.componentes.TelaErro
import br.com.caderno.receitas.ui.componentes.larguraLegivel
import br.com.caderno.receitas.ui.componentes.traduzirCategoria
import java.io.File

@Composable
fun ReceitaScreen(
    viewModel: ReceitaViewModel,
    mostrarVoltar: Boolean,
    aoVoltar: () -> Unit,
    aoRegistrar: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    ReceitaConteudo(
        estado = estado,
        mostrarVoltar = mostrarVoltar,
        aoVoltar = aoVoltar,
        aoRegistrar = aoRegistrar,
        aoTentarDeNovo = viewModel::aoTentarDeNovo,
        aoAtualizar = viewModel::aoAtualizar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceitaConteudo(
    estado: ReceitaUiState,
    mostrarVoltar: Boolean,
    aoVoltar: () -> Unit,
    aoRegistrar: () -> Unit,
    aoTentarDeNovo: () -> Unit,
    aoAtualizar: () -> Unit,
) {
    val contexto = LocalContext.current
    val receita = (estado as? ReceitaUiState.Conteudo)?.receita
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        receita?.nome ?: stringResource(R.string.receita),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (mostrarVoltar) {
                        IconButton(onClick = aoVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar))
                        }
                    }
                },
                actions = {
                    if (receita != null) {
                        IconButton(onClick = { compartilhar(contexto, receita) }) {
                            Icon(Icons.Filled.Share, stringResource(R.string.cd_compartilhar))
                        }
                    }
                },
            )
        },
    ) { espacos ->
        Box(
            modifier = Modifier
                .padding(espacos)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (estado) {
                ReceitaUiState.Carregando -> TelaCarregando(stringResource(R.string.carregando_receita))
                is ReceitaUiState.Erro -> TelaErro(estado.tipo, aoTentarDeNovo = aoTentarDeNovo)
                is ReceitaUiState.Conteudo -> PullToRefreshBox(
                    isRefreshing = estado.atualizando,
                    onRefresh = aoAtualizar,
                ) {
                    DetalhesReceita(
                        estado = estado,
                        aoRegistrar = aoRegistrar,
                        aoAtualizar = aoAtualizar,
                        aoVerVideo = { url -> abrirLink(contexto, url) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetalhesReceita(
    estado: ReceitaUiState.Conteudo,
    aoRegistrar: () -> Unit,
    aoAtualizar: () -> Unit,
    aoVerVideo: (String) -> Unit,
) {
    val receita = estado.receita
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .larguraLegivel(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (estado.desatualizada) {
            item { AvisoDesatualizado(aoAtualizar = aoAtualizar) }
        }
        item {
            ImagemReceita(
                modelo = receita.imagemUrl,
                descricao = stringResource(R.string.cd_foto_receita, receita.nome),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
        item {
            Text(
                receita.nome,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val etiquetas = listOfNotNull(traduzirCategoria(receita.categoria), receita.origem) + receita.tags
                etiquetas.forEach { Etiqueta(it) }
            }
        }
        item {
            CartaoDoCaderno(registro = estado.registro, aoRegistrar = aoRegistrar)
        }
        if (receita.videoUrl != null) {
            item {
                OutlinedButton(
                    onClick = { aoVerVideo(receita.videoUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.PlayCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ver_video))
                }
            }
        }
        item {
            Titulo(pluralStringResource(R.plurals.ingredientes, receita.ingredientes.size, receita.ingredientes.size))
        }
        itemsIndexed(receita.ingredientes, key = { i, _ -> "ing$i" }) { _, ingrediente ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    listOfNotNull(ingrediente.medida, ingrediente.nome).joinToString(" — "),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item { Titulo(stringResource(R.string.modo_preparo)) }
        if (receita.passos.isEmpty()) {
            item { Text(stringResource(R.string.sem_modo_preparo), style = MaterialTheme.typography.bodyLarge) }
        }
        itemsIndexed(receita.passos, key = { i, _ -> "passo$i" }) { i, passo ->
            Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(passo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Etiqueta só informativa (categoria, origem, tags), com contraste de texto normal. */
@Composable
private fun Etiqueta(texto: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

/** Resumo do que a pessoa registrou + botão para registrar/editar. */
@Composable
private fun CartaoDoCaderno(registro: RegistroCaderno?, aoRegistrar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), // cresce suavemente quando o registro aparece
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (registro == null) {
                Text(stringResource(R.string.fora_do_caderno), style = MaterialTheme.typography.bodyLarge)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.no_seu_caderno),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    SeloSituacao(registro.situacao)
                }
                if (registro.situacao == Situacao.JA_FIZ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pluralStringResource(R.plurals.vezes_feita, registro.vezesFeita, registro.vezesFeita),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.width(12.dp))
                        registro.nota?.let { EstrelasNota(it) }
                    }
                }
                registro.observacao?.let {
                    Text("“$it”", style = MaterialTheme.typography.bodyMedium)
                }
                registro.fotoCaminho?.let { caminho ->
                    ImagemReceita(
                        modelo = File(caminho),
                        descricao = stringResource(R.string.cd_foto_prato),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = aoRegistrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(if (registro == null) Icons.Filled.NoteAdd else Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (registro == null) R.string.registrar_no_caderno else R.string.editar_registro))
            }
        }
    }
}

/** Compartilhamento (Intent de envio): ingredientes + link que abre a receita no app. */
private fun compartilhar(contexto: Context, receita: Receita) {
    val texto = buildString {
        appendLine(receita.nome)
        appendLine()
        appendLine(contexto.getString(R.string.compartilhar_ingredientes))
        receita.ingredientes.forEach { appendLine("• " + listOfNotNull(it.medida, it.nome).joinToString(" ")) }
        appendLine()
        append(contexto.getString(R.string.compartilhar_link, receita.id))
    }
    val envio = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, receita.nome)
        .putExtra(Intent.EXTRA_TEXT, texto)
    contexto.startActivity(Intent.createChooser(envio, contexto.getString(R.string.cd_compartilhar)))
}

private fun abrirLink(contexto: Context, url: String) {
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // Nenhum app para abrir o link: não há o que fazer além de ignorar.
    }
}
