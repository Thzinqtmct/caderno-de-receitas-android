package br.com.caderno.receitas.dados

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.caderno.receitas.dados.local.BancoCaderno
import br.com.caderno.receitas.dominio.Consulta
import br.com.caderno.receitas.dominio.FalhaAoCarregar
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha
import br.com.caderno.receitas.falsos.ArmazenamentoFotosFalso
import br.com.caderno.receitas.falsos.FonteRemotaFalsa
import br.com.caderno.receitas.falsos.refeicaoDeTeste
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Repositories com o Room de verdade (banco em memória) e a API falsa. */
@RunWith(AndroidJUnit4::class)
class RepositoriesTest {

    private val banco = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        BancoCaderno::class.java,
    ).allowMainThreadQueries().build()

    private val remoto = FonteRemotaFalsa().apply {
        porCategoria = mapOf("Dessert" to listOf(refeicaoDeTeste("1", "Brownie"), refeicaoDeTeste("2", "Pudim")))
        porNome = mapOf("brownie" to listOf(refeicaoDeTeste("1", "Brownie", categoria = "Dessert")))
        detalhes = mapOf("1" to refeicaoDeTeste("1", "Brownie", categoria = "Dessert"))
    }
    private var agora = 1_000_000L
    private val receitas = ReceitaRepositoryPadrao(remoto, banco.receitaDao()) { agora }
    private val fotos = ArmazenamentoFotosFalso()
    private val caderno = CadernoRepositoryPadrao(banco.registroDao(), fotos)

    @After
    fun fechar() = banco.close()

    @Test
    fun cacheValido_naoChamaAApiDeNovo() = runTest {
        val consulta = Consulta.PorCategoria("Dessert")
        receitas.buscar(consulta)
        agora += 30.minutes.inWholeMilliseconds
        val segunda = receitas.buscar(consulta)

        assertEquals(listOf("categoria:Dessert"), remoto.chamadas)
        assertEquals(listOf("Brownie", "Pudim"), segunda.dados.map { it.nome })
        assertEquals("Dessert", segunda.dados.first().categoria)
    }

    @Test
    fun cacheVencido_buscaDeNovo_eForcarIgnoraOCache() = runTest {
        val consulta = Consulta.PorCategoria("Dessert")
        receitas.buscar(consulta)
        agora += 2.hours.inWholeMilliseconds
        receitas.buscar(consulta)
        receitas.buscar(consulta, forcarAtualizacao = true)

        assertEquals(3, remoto.chamadas.size)
    }

    @Test
    fun semInternet_usaCopiaAntigaMarcadaComoDesatualizada() = runTest {
        receitas.receita("1")
        agora += 3.hours.inWholeMilliseconds
        remoto.semInternet = true

        val resultado = receitas.receita("1", forcarAtualizacao = true)

        assertTrue(resultado.desatualizado)
        assertEquals("Brownie", resultado.dados.nome)
        assertEquals(listOf("Farinha"), resultado.dados.ingredientes.map { it.nome })
    }

    @Test
    fun semInternetESemCopia_lancaFalhaSemConexao() = runTest {
        remoto.semInternet = true
        try {
            receitas.buscar(Consulta.PorNome("brownie"))
            fail("deveria falhar")
        } catch (e: FalhaAoCarregar) {
            assertEquals(TipoFalha.SEM_CONEXAO, e.tipo)
        }
    }

    @Test
    fun buscaPorNome_guardaReceitaCompleta_eCategoriaNaoApagaDetalhes() = runTest {
        receitas.buscar(Consulta.PorNome("brownie")) // traz a receita inteira
        receitas.buscar(Consulta.PorCategoria("Dessert")) // traz só o resumo da mesma receita
        remoto.semInternet = true

        // Os detalhes continuam no aparelho: abre sem internet e sem marcar como desatualizado
        val receita = receitas.receita("1")
        assertFalse(receita.desatualizado)
        assertEquals(listOf("Passo um.", "Passo dois."), receita.dados.passos)
    }

    @Test
    fun caderno_salvaListaComReceita_trocaFotoERemove() = runTest {
        receitas.receita("1") // a receita precisa estar salva para aparecer no caderno (JOIN)
        val registro = RegistroCaderno("1", Situacao.JA_FIZ, 2, 5, "Top", null, atualizadoEm = 10)

        caderno.salvar(registro, fotoNovaUri = "content://foto/1")
        val salvo = caderno.observarItens().first().single()
        assertEquals("Brownie", salvo.nome)
        assertEquals("/fotos/prato-1.jpg", salvo.registro.fotoCaminho)
        assertEquals(mapOf("1" to Situacao.JA_FIZ), caderno.observarSituacoes().first())

        // Trocar a foto apaga o arquivo antigo
        caderno.salvar(salvo.registro, fotoNovaUri = "content://foto/2")
        assertEquals(listOf("/fotos/prato-1.jpg"), fotos.apagadas)

        caderno.remover("1")
        assertTrue(caderno.observarItens().first().isEmpty())
        assertNull(caderno.registro("1"))
        assertEquals(listOf("/fotos/prato-1.jpg", "/fotos/prato-2.jpg"), fotos.apagadas)
    }
}
