package br.com.caderno.receitas.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.caderno.receitas.R
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha
import coil3.compose.AsyncImage

/*
 * Componentes reutilizados em várias telas. Os quatro estados de tela (carregando, vazio,
 * erro e aviso de dados antigos) têm visual próprio para a pessoa sempre saber o que está acontecendo.
 */

@Composable
fun TelaCarregando(texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun TelaVazia(
    emoji: String,
    titulo: String,
    mensagem: String,
    modifier: Modifier = Modifier,
    textoAcao: String? = null,
    aoAgir: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium, modifier = Modifier.clearAndSetSemantics {})
        Spacer(Modifier.height(12.dp))
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (textoAcao != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = aoAgir, modifier = Modifier.height(48.dp)) { Text(textoAcao) }
        }
    }
}

@Composable
fun TelaErro(tipo: TipoFalha, aoTentarDeNovo: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(tituloFalha(tipo)),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(mensagemFalha(tipo)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = aoTentarDeNovo, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tentar_de_novo))
        }
    }
}

/** Faixa mostrada quando a internet falhou e a tela está usando a cópia salva no aparelho. */
@Composable
fun AvisoDesatualizado(aoAtualizar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 4.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.aviso_desatualizado),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
            TextButton(onClick = aoAtualizar, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.atualizar))
            }
        }
    }
}

/** Foto da receita (ou do prato) carregada com Coil; mostra uma cor de fundo enquanto carrega. */
@Composable
fun ImagemReceita(
    modelo: Any?,
    descricao: String?,
    modifier: Modifier = Modifier,
) {
    val fundo = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    AsyncImage(
        model = modelo,
        contentDescription = descricao,
        placeholder = fundo,
        error = fundo,
        fallback = fundo,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
fun SeloSituacao(situacao: Situacao, modifier: Modifier = Modifier) {
    val (fundo, texto) = when (situacao) {
        Situacao.JA_FIZ -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        Situacao.QUERO_FAZER -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(color = fundo, contentColor = texto, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            stringResource(textoSituacao(situacao)),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Cartão de receita usado nas listas. Todo o cartão é uma área de toque só. */
@Composable
fun CartaoReceita(
    nome: String,
    imagemUrl: String?,
    linhaDeApoio: String?,
    situacao: Situacao?,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable (() -> Unit)? = null,
) {
    val forma = RoundedCornerShape(16.dp)
    Surface(
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .clickable(
                onClickLabel = stringResource(R.string.abrir_receita),
                role = Role.Button,
                onClick = aoClicar,
            ),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ImagemReceita(
                modelo = imagemUrl,
                descricao = null, // o nome ao lado já descreve; evita leitura repetida
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(nome, style = MaterialTheme.typography.titleMedium)
                if (linhaDeApoio != null) {
                    Text(
                        linhaDeApoio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (situacao != null) SeloSituacao(situacao)
                extra?.invoke()
            }
        }
    }
}

/** Estrelas só para mostrar a nota (sem interação). */
@Composable
fun EstrelasNota(nota: Int, modifier: Modifier = Modifier) {
    val descricao = stringResource(R.string.nota_x_de_5, nota)
    Row(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = descricao }) {
        repeat(5) { i ->
            Icon(
                if (i < nota) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Escolha de nota de 1 a 5. Cada estrela tem 48dp de área de toque e é anunciada pelo
 * leitor de tela como "Nota 3 de 5, selecionado".
 */
@Composable
fun SeletorNota(
    nota: Int?,
    aoEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.selectableGroup()) {
        for (valor in 1..5) {
            val descricao = stringResource(R.string.nota_x_de_5, valor)
            val marcada = nota != null && valor <= nota
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .selectable(
                        selected = nota == valor,
                        onClick = { aoEscolher(valor) },
                        role = Role.RadioButton,
                    )
                    .semantics { contentDescription = descricao },
            ) {
                Icon(
                    if (marcada) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** Limita a largura do conteúdo em telas largas, para as linhas de texto não ficarem enormes. */
fun Modifier.larguraLegivel(): Modifier = this.widthIn(max = 720.dp)
