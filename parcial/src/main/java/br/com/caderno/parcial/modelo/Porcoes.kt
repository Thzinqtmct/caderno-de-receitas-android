package br.com.caderno.parcial.modelo

import java.util.Locale

/** Regras para recalcular e escrever as quantidades quando o usuário muda as porções. */
object Porcoes {

    const val MINIMO = 1
    const val MAXIMO = 50

    /** Quantidade ajustada: regra de três entre o rendimento original e as porções escolhidas. */
    fun ajustar(ingrediente: Ingrediente, rendimentoOriginal: Int, porcoes: Int): Double? =
        ingrediente.quantidade?.let { it * porcoes / rendimentoOriginal }

    /** "2 xícaras de farinha", "3 ovos", "Sal a gosto". */
    fun descrever(ingrediente: Ingrediente, rendimentoOriginal: Int, porcoes: Int): String {
        val quantidade = ajustar(ingrediente, rendimentoOriginal, porcoes)
            ?: return "${ingrediente.nome} a gosto"
        val numero = formatarNumero(quantidade)
        val unidade = ingrediente.unidade ?: return "$numero ${ingrediente.nome}"
        val unidadeFlexionada = if (quantidade >= 2) plural(unidade) else unidade
        return "$numero $unidadeFlexionada de ${ingrediente.nome}"
    }

    /** "lata" -> "latas", "colher de sopa" -> "colheres de sopa"; siglas (g, kg, ml) não mudam. */
    fun plural(unidade: String): String {
        if (unidade.lowercase() in SIGLAS) return unidade
        val primeira = unidade.substringBefore(' ')
        val resto = unidade.removePrefix(primeira)
        val flexionada = when {
            primeira.endsWith("r") || primeira.endsWith("z") -> primeira + "es"
            primeira.endsWith("s") -> primeira
            else -> primeira + "s"
        }
        return flexionada + resto
    }

    private val SIGLAS = setOf("g", "kg", "mg", "ml", "l")

    /** 2.0 -> "2", 0.5 -> "½", 1.25 -> "1,25" (no máximo duas casas, vírgula brasileira). */
    fun formatarNumero(valor: Double): String {
        val inteiro = valor.toLong()
        val resto = valor - inteiro
        val fracao = when {
            resto < 0.01 -> ""
            kotlin.math.abs(resto - 0.5) < 0.01 -> "½"
            kotlin.math.abs(resto - 0.25) < 0.01 -> "¼"
            kotlin.math.abs(resto - 0.75) < 0.01 -> "¾"
            else -> null
        }
        return when {
            fracao == null -> String.format(Locale.forLanguageTag("pt-BR"), "%.2f", valor).trimEnd('0').trimEnd(',')
            inteiro == 0L && fracao.isNotEmpty() -> fracao
            else -> "$inteiro$fracao"
        }
    }
}
