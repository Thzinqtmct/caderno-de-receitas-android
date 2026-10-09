package br.com.caderno.receitas.dados.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceitaDao {

    @Query("SELECT * FROM receitas WHERE id = :id")
    suspend fun receita(id: String): ReceitaEntity?

    @Query("SELECT * FROM receitas WHERE id IN (:ids)")
    suspend fun receitas(ids: List<String>): List<ReceitaEntity>

    /** Versão completa (detalhes) substitui o que houver. */
    @Upsert
    suspend fun salvarCompleta(receita: ReceitaEntity)

    /** Resumos de uma lista não podem apagar detalhes já salvos: se já existe, ignora. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun salvarResumosSeNovos(receitas: List<ReceitaEntity>)

    @Query("SELECT * FROM buscas WHERE chave = :chave")
    suspend fun busca(chave: String): BuscaEntity?

    @Upsert
    suspend fun salvarBusca(busca: BuscaEntity)

    @Upsert
    suspend fun salvarCompletas(receitas: List<ReceitaEntity>)

    /** Grava a busca e as receitas juntas: ou entra tudo, ou nada. */
    @Transaction
    suspend fun salvarResultadoDaBusca(
        busca: BuscaEntity,
        resumos: List<ReceitaEntity>,
        completas: List<ReceitaEntity>,
    ) {
        salvarResumosSeNovos(resumos)
        salvarCompletas(completas)
        salvarBusca(busca)
    }

    @Query("SELECT * FROM categorias ORDER BY ordem")
    suspend fun categorias(): List<CategoriaEntity>

    @Query("DELETE FROM categorias")
    suspend fun apagarCategorias()

    @Insert
    suspend fun inserirCategorias(categorias: List<CategoriaEntity>)

    @Transaction
    suspend fun substituirCategorias(categorias: List<CategoriaEntity>) {
        apagarCategorias()
        inserirCategorias(categorias)
    }
}

@Dao
interface RegistroDao {

    /** Flow: a tela do caderno é atualizada sozinha sempre que a tabela muda. */
    @Query(
        """
        SELECT registros.*, receitas.nome, receitas.imagemUrl, receitas.categoria
        FROM registros
        INNER JOIN receitas ON receitas.id = registros.receitaId
        ORDER BY registros.atualizadoEm DESC
        """,
    )
    fun observarComReceitas(): Flow<List<RegistroComReceita>>

    @Query("SELECT * FROM registros WHERE receitaId = :receitaId")
    fun observar(receitaId: String): Flow<RegistroEntity?>

    @Query("SELECT receitaId, situacao FROM registros")
    fun observarSituacoes(): Flow<List<SituacaoDaReceita>>

    @Query("SELECT * FROM registros WHERE receitaId = :receitaId")
    suspend fun registro(receitaId: String): RegistroEntity?

    @Upsert
    suspend fun salvar(registro: RegistroEntity)

    @Query("DELETE FROM registros WHERE receitaId = :receitaId")
    suspend fun apagar(receitaId: String)
}
