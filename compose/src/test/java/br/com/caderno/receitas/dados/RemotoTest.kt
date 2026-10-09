package br.com.caderno.receitas.dados

import br.com.caderno.receitas.dados.remoto.TheMealDbApi
import br.com.caderno.receitas.dados.remoto.paraReceita
import br.com.caderno.receitas.dados.remoto.separarPassos
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Testa o cliente Ktor com uma "MockEngine" (respostas falsas, sem internet) e o mapeamento DTO -> domínio. */
class RemotoTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val urlsChamadas = mutableListOf<String>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, corpo: String) = TheMealDbApi(
        engine = MockEngine { requisicao ->
            urlsChamadas += requisicao.url.toString()
            respond(corpo, status, json)
        },
        chave = "1",
    )

    @Test
    fun detalhes_montaUrlEConverteIngredientesEPassos() = runTest {
        val corpo = """
            {"meals":[{
              "idMeal":"52772","strMeal":" Teriyaki Chicken ","strCategory":"Chicken","strArea":"Japanese",
              "strInstructions":"STEP 1\r\nPreheat oven.\r\n\r\n▢\r\n2. Mix the sauce.",
              "strMealThumb":"https://img/1.jpg","strTags":"Meat, Casserole,,","strYoutube":"https://youtube.com/x",
              "strSource":"","strIngredient1":"soy sauce","strMeasure1":"3/4 cup",
              "strIngredient2":"water","strMeasure2":" ","strIngredient3":"","strMeasure3":"",
              "strIngredient4":null,"strMeasure4":null,"campoNovoQueAApiInventou":true
            }]}
        """.trimIndent()

        val dto = api(corpo = corpo).detalhes("52772")!!
        val receita = dto.paraReceita()

        assertEquals("https://www.themealdb.com/api/json/v1/1/lookup.php?i=52772", urlsChamadas.single())
        assertEquals("Teriyaki Chicken", receita.nome)
        assertEquals(listOf("soy sauce" to "3/4 cup", "water" to null), receita.ingredientes.map { it.nome to it.medida })
        assertEquals(listOf("Preheat oven.", "Mix the sauce."), receita.passos)
        assertEquals(listOf("Meat", "Casserole"), receita.tags)
        assertEquals("https://youtube.com/x", receita.videoUrl)
        assertNull(receita.fonteUrl)
    }

    @Test
    fun busca_semResultados_devolveListaVazia() = runTest {
        val resultado = api(corpo = """{"meals":null}""").buscarPorNome("xyz")

        assertTrue(resultado.isEmpty())
        assertEquals("https://www.themealdb.com/api/json/v1/1/search.php?s=xyz", urlsChamadas.single())
    }

    @Test
    fun erroDoServidor_viraExcecao() = runTest {
        try {
            api(HttpStatusCode.InternalServerError, corpo = "{}").categorias()
            fail("deveria lançar exceção")
        } catch (e: ResponseException) {
            assertEquals(HttpStatusCode.InternalServerError, e.response.status)
        }
    }

    @Test
    fun separarPassos_ignoraLinhasSemTexto() {
        assertEquals(listOf("Boil water.", "Add pasta."), separarPassos("1) Boil water.\n\nStep 2:\nAdd pasta.\n▢"))
        assertTrue(separarPassos(null).isEmpty())
    }
}
