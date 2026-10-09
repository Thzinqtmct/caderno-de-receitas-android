package br.com.caderno.receitas.dados

import br.com.caderno.receitas.dados.local.BuscaEntity
import br.com.caderno.receitas.dados.local.CategoriaEntity
import br.com.caderno.receitas.dados.local.IngredienteSalvo
import br.com.caderno.receitas.dados.local.ReceitaDao
import br.com.caderno.receitas.dados.local.ReceitaEntity
import br.com.caderno.receitas.dados.remoto.FonteRemotaReceitas
import br.com.caderno.receitas.dados.remoto.paraCategoria
import br.com.caderno.receitas.dados.remoto.paraReceita
import br.com.caderno.receitas.dados.remoto.paraResumo
import br.com.caderno.receitas.dominio.Carregado
import br.com.caderno.receitas.dominio.Categoria
import br.com.caderno.receitas.dominio.Consulta
import br.com.caderno.receitas.dominio.FalhaAoCarregar
import br.com.caderno.receitas.dominio.Receita
import br.com.caderno.receitas.dominio.ResumoReceita
import br.com.caderno.receitas.dominio.TipoFalha
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Catálogo de receitas (API + cache local). As telas só conhecem esta interface. */
interface ReceitaRepository {
    suspend fun categorias(forcarAtualizacao: Boolean = false): Carregado<List<Categoria>>
    suspend fun buscar(consulta: Consulta, forcarAtualizacao: Boolean = false): Carregado<List<ResumoReceita>>
    suspend fun receita(id: String, forcarAtualizacao: Boolean = false): Carregado<Receita>

    /** Só o que já está salvo no aparelho (sem rede). Usado pelo formulário. */
    suspend fun resumoSalvo(id: String): ResumoReceita?
}

/**
 * Política de cache:
 * - se a cópia local ainda é "nova" (dentro da validade), usa ela e nem chama a API;
 * - se venceu (ou a pessoa pediu para atualizar), chama a API e salva a resposta;
 * - se a API falhar e houver cópia antiga, mostra a antiga marcada como desatualizada (offline-first);
 * - se falhar e não houver nada salvo, lança [FalhaAoCarregar].
 */
class ReceitaRepositoryPadrao(
    private val remoto: FonteRemotaReceitas,
    private val dao: ReceitaDao,
    private val agora: () -> Long = System::currentTimeMillis,
) : ReceitaRepository {

    override suspend fun categorias(forcarAtualizacao: Boolean): Carregado<List<Categoria>> {
        val salvas = dao.categorias()
        val validade = salvas.minOfOrNull { it.atualizadaEm }
        return comCache(
            temCopia = salvas.isNotEmpty(),
            atualizadaEm = validade,
            validade = VALIDADE_CATEGORIAS,
            forcar = forcarAtualizacao,
            copiaLocal = { salvas.map { Categoria(it.nome, it.imagemUrl) } },
            buscarNaRede = {
                val categorias = remoto.categorias().map { it.paraCategoria() }
                dao.substituirCategorias(
                    categorias.mapIndexed { i, c -> CategoriaEntity(c.nome, c.imagemUrl, i, agora()) },
                )
                categorias
            },
        )
    }

    override suspend fun buscar(consulta: Consulta, forcarAtualizacao: Boolean): Carregado<List<ResumoReceita>> {
        val busca = dao.busca(consulta.chave)
        return comCache(
            temCopia = busca != null,
            atualizadaEm = busca?.atualizadaEm,
            validade = VALIDADE_BUSCA,
            forcar = forcarAtualizacao,
            copiaLocal = { resumosNaOrdem(busca?.idsReceitas.orEmpty()) },
            buscarNaRede = {
                val refeicoes = when (consulta) {
                    is Consulta.PorNome -> remoto.buscarPorNome(consulta.texto.trim())
                    is Consulta.PorCategoria -> remoto.filtrarPorCategoria(consulta.categoria)
                }
                val busca = BuscaEntity(consulta.chave, refeicoes.map { it.idMeal }, agora())
                when (consulta) {
                    // A busca por nome já traz a receita inteira: guardamos completa (detalhes abrem sem rede).
                    is Consulta.PorNome -> dao.salvarResultadoDaBusca(
                        busca = busca,
                        resumos = emptyList(),
                        completas = refeicoes.map { it.paraReceita().paraEntidade(agora()) },
                    )
                    // A busca por categoria só traz nome e foto: guardamos o resumo, sem apagar detalhes.
                    is Consulta.PorCategoria -> dao.salvarResultadoDaBusca(
                        busca = busca,
                        resumos = refeicoes.map {
                            it.paraResumo().copy(categoria = consulta.categoria).paraEntidadeResumo(agora())
                        },
                        completas = emptyList(),
                    )
                }
                resumosNaOrdem(busca.idsReceitas)
            },
        )
    }

    override suspend fun receita(id: String, forcarAtualizacao: Boolean): Carregado<Receita> {
        val salva = dao.receita(id)?.takeIf { it.completa }
        return comCache(
            temCopia = salva != null,
            atualizadaEm = salva?.atualizadaEm,
            validade = VALIDADE_RECEITA,
            forcar = forcarAtualizacao,
            copiaLocal = { salva!!.paraReceita() },
            buscarNaRede = {
                val dto = remoto.detalhes(id) ?: throw FalhaAoCarregar(TipoFalha.NAO_ENCONTRADA)
                val receita = dto.paraReceita()
                dao.salvarCompleta(receita.paraEntidade(agora()))
                receita
            },
        )
    }

    override suspend fun resumoSalvo(id: String): ResumoReceita? =
        dao.receita(id)?.let { ResumoReceita(it.id, it.nome, it.imagemUrl, it.categoria, it.origem) }

    private suspend fun <T> comCache(
        temCopia: Boolean,
        atualizadaEm: Long?,
        validade: Duration,
        forcar: Boolean,
        copiaLocal: suspend () -> T,
        buscarNaRede: suspend () -> T,
    ): Carregado<T> {
        val aindaValida = atualizadaEm != null && agora() - atualizadaEm < validade.inWholeMilliseconds
        if (temCopia && aindaValida && !forcar) return Carregado(copiaLocal())

        return try {
            Carregado(buscarNaRede())
        } catch (e: CancellationException) {
            throw e // cancelamento (ex.: nova busca) não é erro: repassa
        } catch (e: FalhaAoCarregar) {
            throw e
        } catch (e: Exception) {
            if (temCopia) Carregado(copiaLocal(), desatualizado = true) else throw e.paraFalha()
        }
    }

    private suspend fun resumosNaOrdem(ids: List<String>): List<ResumoReceita> {
        val porId = dao.receitas(ids).associateBy { it.id }
        return ids.mapNotNull { id ->
            porId[id]?.let { ResumoReceita(it.id, it.nome, it.imagemUrl, it.categoria, it.origem) }
        }
    }

    companion object {
        val VALIDADE_BUSCA = 1.hours
        val VALIDADE_RECEITA = 1.days
        val VALIDADE_CATEGORIAS = 7.days
    }
}

private fun Exception.paraFalha(): FalhaAoCarregar = when (this) {
    is ResponseException -> FalhaAoCarregar(TipoFalha.SERVIDOR, this)
    is IOException -> FalhaAoCarregar(TipoFalha.SEM_CONEXAO, this)
    else -> FalhaAoCarregar(TipoFalha.SERVIDOR, this)
}

private fun ResumoReceita.paraEntidadeResumo(agora: Long) = ReceitaEntity(
    id = id,
    nome = nome,
    imagemUrl = imagemUrl,
    categoria = categoria,
    origem = origem,
    ingredientes = null,
    passos = null,
    tags = null,
    videoUrl = null,
    fonteUrl = null,
    completa = false,
    atualizadaEm = agora,
)

private fun Receita.paraEntidade(agora: Long) = ReceitaEntity(
    id = id,
    nome = nome,
    imagemUrl = imagemUrl,
    categoria = categoria,
    origem = origem,
    ingredientes = ingredientes.map { IngredienteSalvo(it.nome, it.medida) },
    passos = passos,
    tags = tags,
    videoUrl = videoUrl,
    fonteUrl = fonteUrl,
    completa = true,
    atualizadaEm = agora,
)

private fun ReceitaEntity.paraReceita() = Receita(
    id = id,
    nome = nome,
    categoria = categoria,
    origem = origem,
    imagemUrl = imagemUrl,
    ingredientes = ingredientes.orEmpty().map { it.paraDominio() },
    passos = passos.orEmpty(),
    tags = tags.orEmpty(),
    videoUrl = videoUrl,
    fonteUrl = fonteUrl,
)
