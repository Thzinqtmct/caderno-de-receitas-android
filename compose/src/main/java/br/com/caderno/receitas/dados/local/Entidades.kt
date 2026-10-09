package br.com.caderno.receitas.dados.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import br.com.caderno.receitas.dominio.Ingrediente
import br.com.caderno.receitas.dominio.Situacao
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Receita guardada no aparelho. Serve de cache da API e também de "cópia" para o caderno
 * funcionar sem internet. [completa] = false quando só temos o resumo (veio de uma lista).
 */
@Entity(tableName = "receitas")
data class ReceitaEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val imagemUrl: String?,
    val categoria: String?,
    val origem: String?,
    val ingredientes: List<IngredienteSalvo>?,
    val passos: List<String>?,
    val tags: List<String>?,
    val videoUrl: String?,
    val fonteUrl: String?,
    val completa: Boolean,
    val atualizadaEm: Long,
)

@Serializable
data class IngredienteSalvo(val nome: String, val medida: String? = null) {
    fun paraDominio() = Ingrediente(nome, medida)
}

/** Resultado de uma busca já feita (ids na ordem em que a API devolveu). */
@Entity(tableName = "buscas")
data class BuscaEntity(
    @PrimaryKey val chave: String,
    val idsReceitas: List<String>,
    val atualizadaEm: Long,
)

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey val nome: String,
    val imagemUrl: String?,
    val ordem: Int,
    val atualizadaEm: Long,
)

/** Um registro do caderno. É o dado "do usuário": nunca é apagado pelo cache. */
@Entity(tableName = "registros")
data class RegistroEntity(
    @PrimaryKey val receitaId: String,
    val situacao: Situacao,
    val vezesFeita: Int,
    val nota: Int?,
    val observacao: String?,
    val fotoCaminho: String?,
    val atualizadoEm: Long,
)

/** Resultado do JOIN registros + receitas, para a tela do caderno. */
data class RegistroComReceita(
    @Embedded val registro: RegistroEntity,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "imagemUrl") val imagemUrl: String?,
    @ColumnInfo(name = "categoria") val categoria: String?,
)

data class SituacaoDaReceita(
    val receitaId: String,
    val situacao: Situacao,
)

/** Ensina o Room a guardar listas como texto JSON numa coluna. */
class Conversores {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun deIngredientes(lista: List<IngredienteSalvo>?): String? = lista?.let { json.encodeToString(it) }

    @TypeConverter
    fun paraIngredientes(texto: String?): List<IngredienteSalvo>? = texto?.let { json.decodeFromString(it) }

    @TypeConverter
    fun deTextos(lista: List<String>?): String? = lista?.let { json.encodeToString(it) }

    @TypeConverter
    fun paraTextos(texto: String?): List<String>? = texto?.let { json.decodeFromString(it) }
}
