package br.com.caderno.receitas.dados

import br.com.caderno.receitas.dados.arquivos.ArmazenamentoFotos
import br.com.caderno.receitas.dados.local.RegistroDao
import br.com.caderno.receitas.dados.local.RegistroEntity
import br.com.caderno.receitas.dominio.ItemCaderno
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.Situacao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** O caderno do usuário (o que ele quer fazer / já fez). Exposto como Flow: a tela reage sozinha. */
interface CadernoRepository {
    fun observarItens(): Flow<List<ItemCaderno>>
    fun observarRegistro(receitaId: String): Flow<RegistroCaderno?>
    fun observarSituacoes(): Flow<Map<String, Situacao>>
    suspend fun registro(receitaId: String): RegistroCaderno?

    /** Salva o registro. Se veio uma foto nova ([fotoNovaUri]), ela é copiada para o app antes. */
    suspend fun salvar(registro: RegistroCaderno, fotoNovaUri: String? = null)
    suspend fun remover(receitaId: String)
}

class CadernoRepositoryPadrao(
    private val dao: RegistroDao,
    private val fotos: ArmazenamentoFotos,
) : CadernoRepository {

    override fun observarItens(): Flow<List<ItemCaderno>> =
        dao.observarComReceitas().map { linhas ->
            linhas.map { ItemCaderno(it.registro.paraDominio(), it.nome, it.imagemUrl, it.categoria) }
        }

    override fun observarRegistro(receitaId: String): Flow<RegistroCaderno?> =
        dao.observar(receitaId).map { it?.paraDominio() }

    override fun observarSituacoes(): Flow<Map<String, Situacao>> =
        dao.observarSituacoes().map { lista -> lista.associate { it.receitaId to it.situacao } }

    override suspend fun registro(receitaId: String): RegistroCaderno? = dao.registro(receitaId)?.paraDominio()

    override suspend fun salvar(registro: RegistroCaderno, fotoNovaUri: String?) {
        val anterior = dao.registro(registro.receitaId)
        val caminhoFoto = if (fotoNovaUri != null) fotos.guardar(fotoNovaUri) else registro.fotoCaminho
        dao.salvar(registro.copy(fotoCaminho = caminhoFoto).paraEntidade())
        // A foto antiga só é apagada depois que o novo registro foi salvo com sucesso.
        val fotoAntiga = anterior?.fotoCaminho
        if (fotoAntiga != null && fotoAntiga != caminhoFoto) fotos.apagar(fotoAntiga)
    }

    override suspend fun remover(receitaId: String) {
        val registro = dao.registro(receitaId) ?: return
        dao.apagar(receitaId)
        registro.fotoCaminho?.let { fotos.apagar(it) }
    }
}

private fun RegistroEntity.paraDominio() = RegistroCaderno(
    receitaId = receitaId,
    situacao = situacao,
    vezesFeita = vezesFeita,
    nota = nota,
    observacao = observacao,
    fotoCaminho = fotoCaminho,
    atualizadoEm = atualizadoEm,
)

private fun RegistroCaderno.paraEntidade() = RegistroEntity(
    receitaId = receitaId,
    situacao = situacao,
    vezesFeita = vezesFeita,
    nota = nota,
    observacao = observacao,
    fotoCaminho = fotoCaminho,
    atualizadoEm = atualizadoEm,
)
