package br.com.caderno.receitas.dados.remoto

import br.com.caderno.receitas.dominio.Categoria
import br.com.caderno.receitas.dominio.Ingrediente
import br.com.caderno.receitas.dominio.Receita
import br.com.caderno.receitas.dominio.ResumoReceita

/*
 * DTO -> domínio. Aqui ficam as "limpezas" dos dados da API, para o resto do app
 * não precisar se preocupar com textos vazios, espaços sobrando etc.
 */

fun RefeicaoDto.paraResumo(): ResumoReceita = ResumoReceita(
    id = idMeal,
    nome = strMeal.trim(),
    imagemUrl = strMealThumb.limpo(),
    categoria = strCategory.limpo(),
    origem = strArea.limpo(),
)

fun RefeicaoDto.paraReceita(): Receita = Receita(
    id = idMeal,
    nome = strMeal.trim(),
    categoria = strCategory.limpo(),
    origem = strArea.limpo(),
    imagemUrl = strMealThumb.limpo(),
    ingredientes = ingredientes.mapNotNull { it.paraIngrediente() },
    passos = separarPassos(strInstructions),
    tags = strTags.orEmpty().split(',').mapNotNull { it.limpo() }.distinct(),
    videoUrl = strYoutube.limpo()?.takeIf { it.startsWith("https://") },
    fonteUrl = strSource.limpo()?.takeIf { it.startsWith("https://") || it.startsWith("http://") },
)

fun CategoriaDto.paraCategoria(): Categoria = Categoria(
    nome = strCategory.trim(),
    imagemUrl = strCategoryThumb.limpo(),
)

private fun IngredienteDto.paraIngrediente(): Ingrediente? {
    val nomeLimpo = nome.limpo() ?: return null // par vazio = fim da lista de ingredientes
    return Ingrediente(nome = nomeLimpo, medida = medida.limpo())
}

/**
 * O modo de preparo vem num texto só. Quebramos por linha e descartamos o que não é passo:
 * linhas vazias, marcadores ("▢") e títulos como "STEP 1". Também tiramos numeração no início.
 */
fun separarPassos(instrucoes: String?): List<String> =
    instrucoes.orEmpty()
        .split("\r\n", "\n", "\r")
        .map { it.trim().replace(PREFIXO_NUMERACAO, "").trim() }
        .filter { linha -> linha.any { it.isLetter() } && !TITULO_PASSO.matches(linha) }

private val PREFIXO_NUMERACAO = Regex("""^(step\s*\d+\s*[:.)-]?|\d+\s*[.)-])\s*""", RegexOption.IGNORE_CASE)
private val TITULO_PASSO = Regex("""^step\s*\d*$""", RegexOption.IGNORE_CASE)

/** null, "" ou só espaços viram null; o resto vem sem espaços nas pontas. */
private fun String?.limpo(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
