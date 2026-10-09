package br.com.caderno.receitas.dominio

/** Campos do formulário "Registrar no caderno". */
enum class Campo { VEZES, NOTA, OBSERVACAO }

/** Erros possíveis; a tela transforma cada um num texto. */
enum class ErroCampo {
    VEZES_OBRIGATORIO,
    VEZES_INVALIDO,
    NOTA_OBRIGATORIA,
    OBSERVACAO_LONGA,
}

/** O que a pessoa digitou/escolheu, do jeito que está na tela (texto cru). */
data class DadosFormulario(
    val situacao: Situacao = Situacao.QUERO_FAZER,
    val vezesTexto: String = "",
    val nota: Int? = null,
    val observacao: String = "",
)

sealed interface ResultadoValidacao {
    data class Valido(
        val vezesFeita: Int,
        val nota: Int?,
        val observacao: String?,
    ) : ResultadoValidacao

    data class Invalido(val erros: Map<Campo, ErroCampo>) : ResultadoValidacao
}

/**
 * Regras do formulário (regra de negócio pura, fácil de testar):
 * - "Quero fazer": vezes e nota são ignoradas;
 * - "Já fiz": precisa dizer quantas vezes (1 a 99) e dar uma nota de 1 a 5;
 * - observação é opcional, com no máximo [MAX_OBSERVACAO] caracteres.
 */
object ValidadorRegistro {

    const val MAX_OBSERVACAO = 300
    const val MAX_VEZES = 99

    fun validar(dados: DadosFormulario): ResultadoValidacao {
        val erros = mutableMapOf<Campo, ErroCampo>()
        val jaFez = dados.situacao == Situacao.JA_FIZ

        var vezes = 0
        if (jaFez) {
            val texto = dados.vezesTexto.trim()
            val numero = texto.toIntOrNull()
            when {
                texto.isEmpty() -> erros[Campo.VEZES] = ErroCampo.VEZES_OBRIGATORIO
                numero == null || numero !in 1..MAX_VEZES -> erros[Campo.VEZES] = ErroCampo.VEZES_INVALIDO
                else -> vezes = numero
            }
        }

        val nota = if (jaFez) dados.nota else null
        if (jaFez && (nota == null || nota !in 1..5)) {
            erros[Campo.NOTA] = ErroCampo.NOTA_OBRIGATORIA
        }

        val observacao = dados.observacao.trim()
        if (observacao.length > MAX_OBSERVACAO) {
            erros[Campo.OBSERVACAO] = ErroCampo.OBSERVACAO_LONGA
        }

        return if (erros.isEmpty()) {
            ResultadoValidacao.Valido(
                vezesFeita = vezes,
                nota = nota,
                observacao = observacao.ifEmpty { null },
            )
        } else {
            ResultadoValidacao.Invalido(erros)
        }
    }
}
