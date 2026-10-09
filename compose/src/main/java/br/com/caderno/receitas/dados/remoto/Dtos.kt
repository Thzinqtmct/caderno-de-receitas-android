package br.com.caderno.receitas.dados.remoto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/*
 * DTOs: espelham o JSON da TheMealDB do jeito que ele vem (nomes em inglês, campos que podem
 * faltar). A conversão para os modelos do app fica em Mapeadores.kt.
 */

/** Resposta de search.php, filter.php e lookup.php. Sem resultados a API manda `"meals": null`. */
@Serializable
data class RespostaRefeicoesDto(
    val meals: List<JsonObject>? = null,
)

@Serializable
data class RefeicaoDto(
    val idMeal: String,
    val strMeal: String,
    val strCategory: String? = null,
    val strArea: String? = null,
    val strInstructions: String? = null,
    val strMealThumb: String? = null,
    val strTags: String? = null,
    val strYoutube: String? = null,
    val strSource: String? = null,
    /**
     * A API manda os ingredientes em 20 pares de campos soltos (strIngredient1..20 e
     * strMeasure1..20). Em vez de declarar 40 propriedades, lemos esses pares à mão.
     */
    @Transient val ingredientes: List<IngredienteDto> = emptyList(),
)

data class IngredienteDto(val nome: String?, val medida: String?)

@Serializable
data class RespostaCategoriasDto(
    val categories: List<CategoriaDto>? = null,
)

@Serializable
data class CategoriaDto(
    val strCategory: String,
    val strCategoryThumb: String? = null,
)

internal const val MAX_INGREDIENTES_API = 20

/** Converte um objeto "meal" do JSON em [RefeicaoDto], incluindo os pares de ingredientes. */
internal fun JsonObject.paraRefeicaoDto(json: Json): RefeicaoDto {
    val base = json.decodeFromJsonElement<RefeicaoDto>(this)
    val ingredientes = (1..MAX_INGREDIENTES_API).map { i ->
        IngredienteDto(
            nome = this["strIngredient$i"]?.jsonPrimitive?.contentOrNull,
            medida = this["strMeasure$i"]?.jsonPrimitive?.contentOrNull,
        )
    }
    return base.copy(ingredientes = ingredientes)
}
