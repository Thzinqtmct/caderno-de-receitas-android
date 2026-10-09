package br.com.caderno.receitas.dados.remoto

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Fonte remota: o que o app precisa da API (interface para trocar por um fake nos testes). */
interface FonteRemotaReceitas {
    suspend fun buscarPorNome(nome: String): List<RefeicaoDto>
    suspend fun filtrarPorCategoria(categoria: String): List<RefeicaoDto>
    suspend fun detalhes(id: String): RefeicaoDto?
    suspend fun categorias(): List<CategoriaDto>
}

/**
 * Cliente da TheMealDB (https://www.themealdb.com/api.php) feito com Ktor.
 * Recebe a "engine" de fora: no app é a do Android; nos testes, uma MockEngine.
 */
class TheMealDbApi(
    engine: HttpClientEngine,
    chave: String,
) : FonteRemotaReceitas {

    private val json = Json { ignoreUnknownKeys = true }

    private val cliente = HttpClient(engine) {
        expectSuccess = true // respostas 4xx/5xx viram exceção
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
        defaultRequest { url("https://www.themealdb.com/api/json/v1/$chave/") }
    }

    override suspend fun buscarPorNome(nome: String): List<RefeicaoDto> =
        cliente.get("search.php") { parameter("s", nome) }
            .body<RespostaRefeicoesDto>()
            .refeicoes()

    override suspend fun filtrarPorCategoria(categoria: String): List<RefeicaoDto> =
        cliente.get("filter.php") { parameter("c", categoria) }
            .body<RespostaRefeicoesDto>()
            .refeicoes()

    override suspend fun detalhes(id: String): RefeicaoDto? =
        cliente.get("lookup.php") { parameter("i", id) }
            .body<RespostaRefeicoesDto>()
            .refeicoes()
            .firstOrNull()

    override suspend fun categorias(): List<CategoriaDto> =
        cliente.get("categories.php").body<RespostaCategoriasDto>().categories.orEmpty()

    private fun RespostaRefeicoesDto.refeicoes(): List<RefeicaoDto> =
        meals.orEmpty().map { it.paraRefeicaoDto(json) }
}
