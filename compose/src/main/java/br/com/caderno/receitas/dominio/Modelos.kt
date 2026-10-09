package br.com.caderno.receitas.dominio

/*
 * Modelos de domínio: o que o app entende por "receita", "registro no caderno" etc.
 * Todos imutáveis (data class com val). Não dependem de Room, Ktor nem Compose.
 */

/** Versão curta, usada nas listas (a busca por categoria da API só traz isto). */
data class ResumoReceita(
    val id: String,
    val nome: String,
    val imagemUrl: String?,
    val categoria: String? = null,
    val origem: String? = null,
)

data class Receita(
    val id: String,
    val nome: String,
    val categoria: String?,
    val origem: String?,
    val imagemUrl: String?,
    val ingredientes: List<Ingrediente>,
    val passos: List<String>,
    val tags: List<String>,
    val videoUrl: String?,
    val fonteUrl: String?,
)

data class Ingrediente(
    val nome: String,
    /** "2 cups", "a pinch"... A API às vezes não informa. */
    val medida: String?,
)

data class Categoria(
    val nome: String,
    val imagemUrl: String?,
)

/** O que a pessoa quer buscar: por nome (texto digitado) ou por categoria (chip). */
sealed interface Consulta {
    /** Identifica a consulta no cache. */
    val chave: String

    data class PorNome(val texto: String) : Consulta {
        override val chave = "nome:${texto.trim().lowercase()}"
    }

    data class PorCategoria(val categoria: String) : Consulta {
        override val chave = "categoria:${categoria.lowercase()}"
    }
}

enum class Situacao { QUERO_FAZER, JA_FIZ }

/** O que a pessoa registrou sobre uma receita no caderno. */
data class RegistroCaderno(
    val receitaId: String,
    val situacao: Situacao,
    /** Quantas vezes já fez (0 quando ainda "quer fazer"). */
    val vezesFeita: Int,
    /** De 1 a 5; só existe quando já fez. */
    val nota: Int?,
    val observacao: String?,
    /** Caminho da foto do prato dentro do armazenamento do app (opcional). */
    val fotoCaminho: String?,
    val atualizadoEm: Long,
)

/** Linha da tela "Meu caderno": o registro + o básico da receita para mostrar. */
data class ItemCaderno(
    val registro: RegistroCaderno,
    val nome: String,
    val imagemUrl: String?,
    val categoria: String?,
)

/** Resultado de uma leitura que pode ter vindo do cache antigo (quando a rede falhou). */
data class Carregado<T>(
    val dados: T,
    /** true = a internet falhou e estamos mostrando a última cópia salva. */
    val desatualizado: Boolean = false,
)

/** Motivos de falha que a interface sabe explicar para a pessoa. */
enum class TipoFalha { SEM_CONEXAO, SERVIDOR, NAO_ENCONTRADA }

class FalhaAoCarregar(val tipo: TipoFalha, causa: Throwable? = null) : Exception(tipo.name, causa)
