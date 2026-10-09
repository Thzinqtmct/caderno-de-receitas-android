package br.com.caderno.receitas.ui

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.caderno.receitas.LinkProfundo
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha
import br.com.caderno.receitas.falsos.DependenciasFalsas
import br.com.caderno.receitas.falsos.receitaDeTeste
import br.com.caderno.receitas.ui.componentes.SeletorNota
import br.com.caderno.receitas.ui.componentes.TelaErro
import br.com.caderno.receitas.ui.navegacao.CadernoDeReceitasApp
import br.com.caderno.receitas.ui.tema.TemaCaderno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Testes de interface em Compose, rodando na JVM com Robolectric (sem emulador). */
@RunWith(AndroidJUnit4::class)
class InterfaceTest {

    @get:Rule
    val compose = createComposeRule()

    private val dependencias = DependenciasFalsas().apply {
        receitas.receitas["10"] = receitaDeTeste("10", "Bolo de cenoura", categoria = "Dessert")
    }

    @Test
    fun telaErro_mostraMensagemEBotaoTentarDeNovo() {
        var tentativas = 0
        compose.setContent { TemaCaderno { TelaErro(TipoFalha.SEM_CONEXAO, aoTentarDeNovo = { tentativas++ }) } }

        compose.onNodeWithText("Sem internet").assertIsDisplayed()
        compose.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, tentativas)
    }

    @Test
    fun seletorNota_cadaEstrelaEhSelecionavelPeloLeitorDeTela() {
        var nota: Int? = null
        compose.setContent { TemaCaderno { SeletorNota(nota = 3, aoEscolher = { nota = it }) } }

        compose.onNodeWithContentDescription("Nota 3 de 5").assertIsSelected()
        compose.onNodeWithContentDescription("Nota 5 de 5").performClick()
        assertEquals(5, nota)
    }

    @Test
    fun fluxoCompleto_explorarAbrirRegistrarEVerNoCaderno() {
        compose.setContent { TemaCaderno { CadernoDeReceitasApp(dependencias) } }

        // 1. Explorar: abre na categoria salva ("Dessert") e mostra a receita
        compose.onNodeWithText("Bolo de cenoura").performClick()

        // 2. Detalhes: ingredientes e botão de registrar
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("2 xícaras — Farinha"))
        compose.onNodeWithText("2 xícaras — Farinha").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Registrar no caderno"))
        compose.onNodeWithText("Registrar no caderno").performScrollTo().performClick()

        // 3. Formulário: "Já fiz" sem preencher -> erros de validação
        compose.onNodeWithText("Já fiz").performClick()
        compose.onNodeWithText("Salvar no caderno").performScrollTo().performClick()
        compose.onNodeWithText("Corrija os 2 campos destacados para salvar.").assertIsDisplayed()
        assertNull(dependencias.caderno.registros.value["10"])

        // 4. Corrige e salva
        compose.onNodeWithText("Quantas vezes você já fez?").performTextInput("2")
        compose.onNodeWithContentDescription("Nota 4 de 5").performScrollTo().performClick()
        compose.onNodeWithText("Salvar no caderno").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(Situacao.JA_FIZ, dependencias.caderno.registros.value["10"]?.situacao)

        // 5. Voltou aos detalhes, que já mostram o registro
        compose.onNodeWithText("Editar registro").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Feita 2 vezes").performScrollTo().assertIsDisplayed()

        // 6. Volta para Explorar e abre "Meu caderno"
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithContentDescription("Abrir meu caderno. Receitas salvas: 1").performClick()
        compose.onNodeWithText("Já fiz (1)").assertIsDisplayed()
        compose.onAllNodesWithText("Bolo de cenoura")[0].assertIsDisplayed()
    }

    @Test
    fun cadernoVazio_mostraEstadoVazioComAtalho() {
        compose.setContent { TemaCaderno { CadernoDeReceitasApp(dependencias) } }

        compose.onNodeWithContentDescription("Abrir meu caderno. Receitas salvas: 0").performClick()
        compose.onNodeWithText("Seu caderno está vazio").assertIsDisplayed()
        compose.onNodeWithText("Explorar receitas").performClick()
        compose.onNodeWithText("Bolo de cenoura").assertIsDisplayed()
    }

    @Test
    fun deepLink_abreAReceitaDireto() {
        compose.setContent { TemaCaderno { CadernoDeReceitasApp(dependencias, linkDeReceita = "10") } }

        compose.onNodeWithText("Registrar no caderno").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun linkProfundo_aceitaSoIdsNumericos() {
        assertEquals("52772", LinkProfundo.receitaId(Uri.parse("caderno://receita/52772")))
        assertNull(LinkProfundo.receitaId(Uri.parse("caderno://receita/../../etc")))
        assertNull(LinkProfundo.receitaId(Uri.parse("caderno://outra/52772")))
        assertNull(LinkProfundo.receitaId(Uri.parse("https://receita/52772")))
    }
}
