package br.com.caderno.receitas.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorRegistroTest {

    @Test
    fun queroFazer_ignoraVezesENota() {
        val resultado = ValidadorRegistro.validar(
            DadosFormulario(situacao = Situacao.QUERO_FAZER, vezesTexto = "abc", nota = 9),
        )
        assertEquals(ResultadoValidacao.Valido(vezesFeita = 0, nota = null, observacao = null), resultado)
    }

    @Test
    fun jaFiz_semVezesESemNota_temDoisErros() {
        val resultado = ValidadorRegistro.validar(DadosFormulario(situacao = Situacao.JA_FIZ))
        assertEquals(
            ResultadoValidacao.Invalido(
                mapOf(Campo.VEZES to ErroCampo.VEZES_OBRIGATORIO, Campo.NOTA to ErroCampo.NOTA_OBRIGATORIA),
            ),
            resultado,
        )
    }

    @Test
    fun jaFiz_vezesForaDoIntervalo_ehInvalido() {
        listOf("0", "100", "-1", "2,5").forEach { texto ->
            val resultado = ValidadorRegistro.validar(DadosFormulario(Situacao.JA_FIZ, texto, nota = 4))
            assertEquals(texto, ResultadoValidacao.Invalido(mapOf(Campo.VEZES to ErroCampo.VEZES_INVALIDO)), resultado)
        }
    }

    @Test
    fun jaFiz_completo_ehValidoELimpaObservacao() {
        val resultado = ValidadorRegistro.validar(
            DadosFormulario(Situacao.JA_FIZ, vezesTexto = " 3 ", nota = 5, observacao = "  Ficou ótimo!  "),
        )
        assertEquals(ResultadoValidacao.Valido(vezesFeita = 3, nota = 5, observacao = "Ficou ótimo!"), resultado)
    }

    @Test
    fun observacaoMuitoLonga_ehInvalida() {
        val resultado = ValidadorRegistro.validar(
            DadosFormulario(observacao = "a".repeat(ValidadorRegistro.MAX_OBSERVACAO + 1)),
        )
        assertTrue(resultado is ResultadoValidacao.Invalido)
        assertEquals(ErroCampo.OBSERVACAO_LONGA, (resultado as ResultadoValidacao.Invalido).erros[Campo.OBSERVACAO])
    }
}
