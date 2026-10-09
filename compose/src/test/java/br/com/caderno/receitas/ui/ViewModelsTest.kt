package br.com.caderno.receitas.ui

import androidx.lifecycle.SavedStateHandle
import br.com.caderno.receitas.dados.preferencias.Preferencias
import br.com.caderno.receitas.dominio.Campo
import br.com.caderno.receitas.dominio.Consulta
import br.com.caderno.receitas.dominio.ErroCampo
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha
import br.com.caderno.receitas.falsos.CadernoRepositoryFalso
import br.com.caderno.receitas.falsos.PreferenciasFalsas
import br.com.caderno.receitas.falsos.ReceitaRepositoryFalso
import br.com.caderno.receitas.falsos.RegraDispatcherPrincipal
import br.com.caderno.receitas.falsos.receitaDeTeste
import br.com.caderno.receitas.ui.explorar.EstadoResultados
import br.com.caderno.receitas.ui.explorar.ExplorarViewModel
import br.com.caderno.receitas.ui.registro.RegistroViewModel
import br.com.caderno.receitas.ui.registro.Salvamento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    @get:Rule
    val regra = RegraDispatcherPrincipal()

    private val receitas = ReceitaRepositoryFalso().apply {
        receitas["1"] = receitaDeTeste("1", "Chocolate cake", categoria = "Dessert")
        receitas["2"] = receitaDeTeste("2", "Fish pie", categoria = "Seafood")
    }
    private val caderno = CadernoRepositoryFalso(receitas)

    private fun explorar(estado: SavedStateHandle = SavedStateHandle()) =
        ExplorarViewModel(estado, receitas, caderno, PreferenciasFalsas(Preferencias(categoria = "Seafood")))

    @Test
    fun explorar_comecaNaUltimaCategoriaSalva() = runTest(regra.dispatcher) {
        val vm = explorar()
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        val resultados = vm.uiState.value.resultados as EstadoResultados.Conteudo
        assertEquals(listOf("Fish pie"), resultados.receitas.map { it.nome })
        assertEquals("Seafood", vm.uiState.value.categoriaSelecionada)
    }

    @Test
    fun explorar_debounce_soBuscaDepoisQueAPessoaParaDeDigitar() = runTest(regra.dispatcher) {
        val vm = explorar()
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        receitas.consultas.clear()

        listOf("c", "ch", "cho", "choc").forEach { texto ->
            vm.aoMudarTexto(texto)
            advanceTimeBy(100) // digitando rápido: menos que os 500 ms de espera
        }
        assertTrue(receitas.consultas.isEmpty())

        advanceTimeBy(ExplorarViewModel.ESPERA_DIGITACAO_MS + 1)
        runCurrent()
        assertEquals(listOf<Consulta>(Consulta.PorNome("choc")), receitas.consultas)
        assertEquals("Chocolate cake", (vm.uiState.value.resultados as EstadoResultados.Conteudo).receitas.single().nome)
    }

    @Test
    fun explorar_novaBuscaCancelaAAnterior() = runTest(regra.dispatcher) {
        val vm = explorar()
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        receitas.atrasoMs = 2_000 // a API está lenta

        vm.aoEscolherCategoria("Dessert")
        advanceTimeBy(500) // ainda esperando a resposta de "Dessert"...
        vm.aoEscolherCategoria("Seafood") // ...e a pessoa já trocou de categoria
        advanceUntilIdle()

        // A resposta de "Dessert" foi cancelada: o que aparece é "Seafood"
        val resultados = vm.uiState.value.resultados as EstadoResultados.Conteudo
        assertEquals(listOf("Fish pie"), resultados.receitas.map { it.nome })
    }

    @Test
    fun explorar_estadosVazioEErro() = runTest(regra.dispatcher) {
        val vm = explorar()
        vm.uiState.launchIn(backgroundScope)

        vm.aoMudarTexto("lasanha")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resultados is EstadoResultados.Vazio)

        receitas.falha = TipoFalha.SEM_CONEXAO
        vm.aoMudarTexto("pie")
        advanceUntilIdle()
        assertEquals(EstadoResultados.Erro(TipoFalha.SEM_CONEXAO), vm.uiState.value.resultados)

        receitas.falha = null
        vm.aoTentarDeNovo()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resultados is EstadoResultados.Conteudo)
    }

    @Test
    fun explorar_textoSobreviveARecriacao() = runTest(regra.dispatcher) {
        val estadoSalvo = SavedStateHandle()
        explorar(estadoSalvo).aoMudarTexto("cake")

        // Um ViewModel novo com o mesmo SavedStateHandle = Android recriou o app
        val recriado = explorar(estadoSalvo)
        recriado.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals("cake", recriado.uiState.value.texto)
    }

    @Test
    fun registro_jaFizSemDados_mostraErrosENaoSalva() = runTest(regra.dispatcher) {
        val vm = RegistroViewModel("1", SavedStateHandle(), receitas, caderno) { 42L }
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        vm.aoEscolherSituacao(Situacao.JA_FIZ)
        assertTrue("erros só aparecem depois de tentar salvar", vm.uiState.value.erros.isEmpty())
        vm.aoSalvar()
        advanceUntilIdle()

        assertEquals(
            mapOf(Campo.VEZES to ErroCampo.VEZES_OBRIGATORIO, Campo.NOTA to ErroCampo.NOTA_OBRIGATORIA),
            vm.uiState.value.erros,
        )
        assertTrue(caderno.registros.value.isEmpty())

        // Corrigindo os campos, os erros somem na hora
        vm.aoMudarVezes("2x") // letras são descartadas
        vm.aoEscolherNota(4)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.erros.isEmpty())
        assertEquals("2", vm.uiState.value.formulario.vezesTexto)
    }

    @Test
    fun registro_valido_salvaEConclui() = runTest(regra.dispatcher) {
        val vm = RegistroViewModel("1", SavedStateHandle(), receitas, caderno) { 42L }
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals("Chocolate cake", vm.uiState.value.nomeReceita)

        vm.aoEscolherSituacao(Situacao.JA_FIZ)
        vm.aoMudarVezes("3")
        vm.aoEscolherNota(5)
        vm.aoMudarObservacao("  Menos açúcar  ")
        vm.aoEscolherFoto("content://media/7")
        vm.aoSalvar()
        advanceUntilIdle()

        assertEquals(Salvamento.CONCLUIDO, vm.uiState.value.salvamento)
        assertEquals(
            RegistroCaderno("1", Situacao.JA_FIZ, 3, 5, "Menos açúcar", "/fotos/7.jpg", atualizadoEm = 42L),
            caderno.registros.value["1"],
        )
    }

    @Test
    fun registro_edicao_preencheComORegistroExistente() = runTest(regra.dispatcher) {
        caderno.registros.value = mapOf(
            "1" to RegistroCaderno("1", Situacao.JA_FIZ, 7, 3, "Repetir", null, atualizadoEm = 1),
        )
        val vm = RegistroViewModel("1", SavedStateHandle(), receitas, caderno)
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        val form = vm.uiState.value.formulario
        assertEquals(Situacao.JA_FIZ, form.situacao)
        assertEquals("7", form.vezesTexto)
        assertEquals(3, form.nota)
        assertEquals("Repetir", form.observacao)
        assertTrue(form.jaExistia)
    }

    @Test
    fun registro_rascunhoSobreviveARecriacao() = runTest(regra.dispatcher) {
        val estadoSalvo = SavedStateHandle()
        val primeiro = RegistroViewModel("1", estadoSalvo, receitas, caderno)
        primeiro.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        primeiro.aoMudarObservacao("rascunho")

        val recriado = RegistroViewModel("1", estadoSalvo, receitas, caderno)
        recriado.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals("rascunho", recriado.uiState.value.formulario.observacao)
    }
}
