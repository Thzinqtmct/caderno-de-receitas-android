package br.com.caderno.receitas.ui.componentes

import androidx.annotation.StringRes
import br.com.caderno.receitas.R
import br.com.caderno.receitas.dominio.ErroCampo
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha

/**
 * A API devolve as categorias em inglês. Traduzimos as conhecidas para mostrar na tela;
 * as desconhecidas aparecem como vieram (o nome original continua sendo usado nas buscas).
 */
fun traduzirCategoria(nome: String?): String? = nome?.let { CATEGORIAS[it.lowercase()] ?: it }

private val CATEGORIAS = mapOf(
    "beef" to "Carne bovina",
    "breakfast" to "Café da manhã",
    "chicken" to "Frango",
    "dessert" to "Sobremesa",
    "goat" to "Cabrito",
    "lamb" to "Cordeiro",
    "miscellaneous" to "Diversos",
    "pasta" to "Massas",
    "pork" to "Porco",
    "seafood" to "Frutos do mar",
    "side" to "Acompanhamentos",
    "starter" to "Entradas",
    "vegan" to "Vegana",
    "vegetarian" to "Vegetariana",
)

@StringRes
fun textoSituacao(situacao: Situacao): Int = when (situacao) {
    Situacao.QUERO_FAZER -> R.string.situacao_quero_fazer
    Situacao.JA_FIZ -> R.string.situacao_ja_fiz
}

@StringRes
fun tituloFalha(tipo: TipoFalha): Int = when (tipo) {
    TipoFalha.SEM_CONEXAO -> R.string.falha_sem_conexao_titulo
    TipoFalha.SERVIDOR -> R.string.falha_servidor_titulo
    TipoFalha.NAO_ENCONTRADA -> R.string.falha_nao_encontrada_titulo
}

@StringRes
fun mensagemFalha(tipo: TipoFalha): Int = when (tipo) {
    TipoFalha.SEM_CONEXAO -> R.string.falha_sem_conexao
    TipoFalha.SERVIDOR -> R.string.falha_servidor
    TipoFalha.NAO_ENCONTRADA -> R.string.falha_nao_encontrada
}

@StringRes
fun textoErroCampo(erro: ErroCampo): Int = when (erro) {
    ErroCampo.VEZES_OBRIGATORIO -> R.string.erro_vezes_obrigatorio
    ErroCampo.VEZES_INVALIDO -> R.string.erro_vezes_invalido
    ErroCampo.NOTA_OBRIGATORIA -> R.string.erro_nota_obrigatoria
    ErroCampo.OBSERVACAO_LONGA -> R.string.erro_observacao_longa
}
