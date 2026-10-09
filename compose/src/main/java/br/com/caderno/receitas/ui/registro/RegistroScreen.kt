package br.com.caderno.receitas.ui.registro

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.caderno.receitas.R
import br.com.caderno.receitas.dominio.Campo
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.ValidadorRegistro
import br.com.caderno.receitas.ui.componentes.ImagemReceita
import br.com.caderno.receitas.ui.componentes.SeletorNota
import br.com.caderno.receitas.ui.componentes.TelaCarregando
import br.com.caderno.receitas.ui.componentes.larguraLegivel
import br.com.caderno.receitas.ui.componentes.textoErroCampo
import br.com.caderno.receitas.ui.componentes.textoSituacao
import java.io.File

@Composable
fun RegistroScreen(
    viewModel: RegistroViewModel,
    aoFechar: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Quando salvar (ou remover) termina, a tela fecha. LaunchedEffect roda só quando o valor muda.
    LaunchedEffect(estado.salvamento) {
        if (estado.salvamento == Salvamento.CONCLUIDO) aoFechar()
    }

    // Photo Picker do Android: não precisa de permissão de armazenamento.
    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.aoEscolherFoto(uri.toString())
    }

    RegistroConteudo(
        estado = estado,
        aoFechar = aoFechar,
        aoEscolherSituacao = viewModel::aoEscolherSituacao,
        aoMudarVezes = viewModel::aoMudarVezes,
        aoEscolherNota = viewModel::aoEscolherNota,
        aoMudarObservacao = viewModel::aoMudarObservacao,
        aoPedirFoto = {
            escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        aoRemoverFoto = viewModel::aoRemoverFoto,
        aoSalvar = viewModel::aoSalvar,
        aoRemoverDoCaderno = viewModel::aoRemoverDoCaderno,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroConteudo(
    estado: RegistroUiState,
    aoFechar: () -> Unit,
    aoEscolherSituacao: (Situacao) -> Unit,
    aoMudarVezes: (String) -> Unit,
    aoEscolherNota: (Int) -> Unit,
    aoMudarObservacao: (String) -> Unit,
    aoPedirFoto: () -> Unit,
    aoRemoverFoto: () -> Unit,
    aoSalvar: () -> Unit,
    aoRemoverDoCaderno: () -> Unit,
) {
    var confirmarRemocao by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.titulo_registro))
                        estado.nomeReceita?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = aoFechar) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_fechar_formulario))
                    }
                },
            )
        },
    ) { espacos ->
        if (estado.carregando) {
            TelaCarregando(stringResource(R.string.carregando), Modifier.padding(espacos))
            return@Scaffold
        }
        val form = estado.formulario
        val rolagem = rememberScrollState()
        // Quando aparecem erros, volta ao topo para a pessoa ver o resumo e o primeiro campo errado.
        val temErros = estado.erros.isNotEmpty()
        LaunchedEffect(temErros) {
            if (temErros) rolagem.animateScrollTo(0)
        }
        Box(
            modifier = Modifier
                .padding(espacos)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Rolável + imePadding: com fonte grande ou teclado aberto nada fica escondido.
            Column(
                modifier = Modifier
                    .larguraLegivel()
                    .fillMaxWidth()
                    .verticalScroll(rolagem)
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AnimatedVisibility(visible = estado.erros.isNotEmpty()) {
                    ResumoDeErros(quantidade = estado.erros.size)
                }

                Text(
                    stringResource(R.string.pergunta_situacao),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                val opcoes = Situacao.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    opcoes.forEachIndexed { i, situacao ->
                        SegmentedButton(
                            selected = form.situacao == situacao,
                            onClick = { aoEscolherSituacao(situacao) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = opcoes.size),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(textoSituacao(situacao)))
                        }
                    }
                }

                // Campos que só fazem sentido para quem já fez a receita aparecem com animação.
                AnimatedVisibility(visible = form.situacao == Situacao.JA_FIZ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val erroVezes = estado.erros[Campo.VEZES]?.let { stringResource(textoErroCampo(it)) }
                        OutlinedTextField(
                            value = form.vezesTexto,
                            onValueChange = aoMudarVezes,
                            label = { Text(stringResource(R.string.rotulo_vezes)) },
                            isError = erroVezes != null,
                            supportingText = { Text(erroVezes ?: stringResource(R.string.ajuda_vezes)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { if (erroVezes != null) error(erroVezes) },
                        )

                        Column {
                            Text(stringResource(R.string.rotulo_nota), style = MaterialTheme.typography.titleMedium)
                            SeletorNota(nota = form.nota, aoEscolher = aoEscolherNota)
                            estado.erros[Campo.NOTA]?.let { erro ->
                                TextoDeErro(stringResource(textoErroCampo(erro)))
                            }
                        }
                    }
                }

                val erroObservacao = estado.erros[Campo.OBSERVACAO]?.let { stringResource(textoErroCampo(it)) }
                OutlinedTextField(
                    value = form.observacao,
                    onValueChange = aoMudarObservacao,
                    label = { Text(stringResource(R.string.rotulo_observacao)) },
                    placeholder = { Text(stringResource(R.string.exemplo_observacao)) },
                    isError = erroObservacao != null,
                    supportingText = {
                        Row {
                            Text(erroObservacao ?: stringResource(R.string.opcional), modifier = Modifier.weight(1f))
                            Text("${form.observacao.trim().length}/${ValidadorRegistro.MAX_OBSERVACAO}")
                        }
                    },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { if (erroObservacao != null) error(erroObservacao) },
                )

                SecaoFoto(foto = estado.foto, aoPedirFoto = aoPedirFoto, aoRemoverFoto = aoRemoverFoto)

                if (estado.salvamento == Salvamento.FALHOU) {
                    TextoDeErro(stringResource(R.string.erro_ao_salvar))
                }

                val salvando = estado.salvamento == Salvamento.SALVANDO
                Button(
                    onClick = aoSalvar,
                    enabled = !salvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    if (salvando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.salvando))
                    } else {
                        Text(stringResource(R.string.salvar))
                    }
                }

                if (form.jaExistia) {
                    TextButton(
                        onClick = { confirmarRemocao = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.remover_do_caderno))
                    }
                }
            }
        }
    }

    if (confirmarRemocao) {
        AlertDialog(
            onDismissRequest = { confirmarRemocao = false },
            title = { Text(stringResource(R.string.remover_titulo)) },
            text = { Text(stringResource(R.string.remover_mensagem)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRemocao = false
                    aoRemoverDoCaderno()
                }) { Text(stringResource(R.string.remover)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRemocao = false }) { Text(stringResource(R.string.cancelar)) }
            },
        )
    }
}

@Composable
private fun ResumoDeErros(quantidade: Int) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            // liveRegion: o leitor de tela anuncia o aviso assim que ele aparece.
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                androidx.compose.ui.res.pluralStringResource(R.plurals.resumo_erros, quantidade, quantidade),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TextoDeErro(texto: String) {
    Text(
        texto,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun SecaoFoto(foto: String?, aoPedirFoto: () -> Unit, aoRemoverFoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.rotulo_foto), style = MaterialTheme.typography.titleMedium)
        if (foto != null) {
            ImagemReceita(
                // Foto nova = endereço "content://" do Photo Picker; foto salva = arquivo do app.
                modelo = if (foto.startsWith("content:")) foto else File(foto),
                descricao = stringResource(R.string.cd_foto_prato),
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = aoPedirFoto, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (foto == null) R.string.escolher_foto else R.string.trocar_foto))
            }
            if (foto != null) {
                TextButton(onClick = aoRemoverFoto, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.remover_foto))
                }
            }
        }
    }
}
