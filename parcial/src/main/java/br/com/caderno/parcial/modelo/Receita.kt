package br.com.caderno.parcial.modelo

/**
 * Modelos imutáveis (só `val`): para "mudar" um valor criamos uma cópia com `copy(...)`.
 * Campos que podem faltar são anuláveis (`?`) e a tela mostra um texto alternativo.
 */
data class Receita(
    val id: String,
    val nome: String,
    val categoria: Categoria,
    val emoji: String,
    /** Nem toda receita informa o tempo de preparo. */
    val tempoPreparoMin: Int?,
    val dificuldade: Dificuldade,
    /** Quantas porções/unidades a receita original rende (base para recalcular os ingredientes). */
    val rendimento: Int,
    val unidadeRendimento: String,
    val ingredientes: List<Ingrediente>,
    val passos: List<String>,
    /** Dica opcional do autor da receita. */
    val dica: String? = null,
)

data class Ingrediente(
    val nome: String,
    /** `null` quando o ingrediente é "a gosto" (sal, pimenta...). */
    val quantidade: Double?,
    /** `null` quando a quantidade é em unidades ("3 ovos"). */
    val unidade: String?,
)

enum class Categoria(val rotulo: String) {
    DOCE("Doce"),
    SALGADO("Salgado"),
    PRATO_PRINCIPAL("Prato principal"),
    ACOMPANHAMENTO("Acompanhamento"),
}

enum class Dificuldade(val rotulo: String) {
    FACIL("Fácil"),
    MEDIA("Média"),
    DIFICIL("Difícil"),
}

/** Item da lista: a receita + se está nas favoritas (estado da tela, separado do dado). */
data class ReceitaNaLista(
    val receita: Receita,
    val favorita: Boolean,
)
