package br.com.caderno.receitas.falsos

import br.com.caderno.receitas.Dependencias
import br.com.caderno.receitas.dados.CadernoRepository
import br.com.caderno.receitas.dados.ReceitaRepository
import br.com.caderno.receitas.dados.arquivos.ArmazenamentoFotos
import br.com.caderno.receitas.dados.preferencias.FiltroCaderno
import br.com.caderno.receitas.dados.preferencias.Preferencias
import br.com.caderno.receitas.dados.preferencias.PreferenciasRepository
import br.com.caderno.receitas.dados.remoto.CategoriaDto
import br.com.caderno.receitas.dados.remoto.FonteRemotaReceitas
import br.com.caderno.receitas.dados.remoto.IngredienteDto
import br.com.caderno.receitas.dados.remoto.RefeicaoDto
import br.com.caderno.receitas.dominio.Carregado
import br.com.caderno.receitas.dominio.Categoria
import br.com.caderno.receitas.dominio.Consulta
import br.com.caderno.receitas.dominio.FalhaAoCarregar
import br.com.caderno.receitas.dominio.Ingrediente
import br.com.caderno.receitas.dominio.ItemCaderno
import br.com.caderno.receitas.dominio.Receita
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.ResumoReceita
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.TipoFalha
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

/* Implementações falsas (fakes) das interfaces, usadas no lugar da API, do Room e do DataStore. */

fun receitaDeTeste(
    id: String = "1",
    nome: String = "Bolo de teste",
    categoria: String? = "Dessert",
) = Receita(
    id = id,
    nome = nome,
    categoria = categoria,
    origem = "Brazilian",
    imagemUrl = null,
    ingredientes = listOf(Ingrediente("Farinha", "2 xícaras"), Ingrediente("Ovos", "3")),
    passos = listOf("Misture tudo.", "Asse por 40 minutos."),
    tags = listOf("Cake"),
    videoUrl = null,
    fonteUrl = null,
)

fun refeicaoDeTeste(id: String, nome: String, categoria: String? = null) = RefeicaoDto(
    idMeal = id,
    strMeal = nome,
    strCategory = categoria,
    strInstructions = "Passo um.\r\nPasso dois.",
    ingredientes = listOf(IngredienteDto("Farinha", "1 xícara"), IngredienteDto("", "")),
)

/** Fonte remota falsa: conta as chamadas e pode simular falta de internet. */
class FonteRemotaFalsa : FonteRemotaReceitas {
    var porNome: Map<String, List<RefeicaoDto>> = emptyMap()
    var porCategoria: Map<String, List<RefeicaoDto>> = emptyMap()
    var detalhes: Map<String, RefeicaoDto> = emptyMap()
    var categorias: List<CategoriaDto> = listOf(CategoriaDto("Dessert"), CategoriaDto("Seafood"))
    var semInternet = false
    val chamadas = mutableListOf<String>()

    private fun registrar(chamada: String) {
        chamadas += chamada
        if (semInternet) throw IOException("sem internet")
    }

    override suspend fun buscarPorNome(nome: String): List<RefeicaoDto> {
        registrar("nome:$nome")
        return porNome[nome].orEmpty()
    }

    override suspend fun filtrarPorCategoria(categoria: String): List<RefeicaoDto> {
        registrar("categoria:$categoria")
        return porCategoria[categoria].orEmpty()
    }

    override suspend fun detalhes(id: String): RefeicaoDto? {
        registrar("detalhes:$id")
        return detalhes[id]
    }

    override suspend fun categorias(): List<CategoriaDto> {
        registrar("categorias")
        return categorias
    }
}

/** Repository de receitas falso, sem cache: responde na hora (ou com um atraso, se configurado). */
class ReceitaRepositoryFalso : ReceitaRepository {
    val receitas = mutableMapOf<String, Receita>()
    var categorias = listOf(Categoria("Dessert", null), Categoria("Seafood", null))
    var falha: TipoFalha? = null
    var atrasoMs = 0L
    val consultas = mutableListOf<Consulta>()

    override suspend fun categorias(forcarAtualizacao: Boolean): Carregado<List<Categoria>> = Carregado(categorias)

    override suspend fun buscar(consulta: Consulta, forcarAtualizacao: Boolean): Carregado<List<ResumoReceita>> {
        consultas += consulta
        if (atrasoMs > 0) delay(atrasoMs)
        falha?.let { throw FalhaAoCarregar(it) }
        val encontradas = receitas.values.filter {
            when (consulta) {
                is Consulta.PorNome -> it.nome.contains(consulta.texto, ignoreCase = true)
                is Consulta.PorCategoria -> it.categoria == consulta.categoria
            }
        }
        return Carregado(encontradas.map { ResumoReceita(it.id, it.nome, it.imagemUrl, it.categoria, it.origem) })
    }

    override suspend fun receita(id: String, forcarAtualizacao: Boolean): Carregado<Receita> {
        falha?.let { throw FalhaAoCarregar(it) }
        return Carregado(receitas[id] ?: throw FalhaAoCarregar(TipoFalha.NAO_ENCONTRADA))
    }

    override suspend fun resumoSalvo(id: String): ResumoReceita? =
        receitas[id]?.let { ResumoReceita(it.id, it.nome, it.imagemUrl, it.categoria, it.origem) }
}

class CadernoRepositoryFalso(private val receitas: ReceitaRepositoryFalso) : CadernoRepository {
    val registros = MutableStateFlow<Map<String, RegistroCaderno>>(emptyMap())
    val fotosGuardadas = mutableListOf<String>()

    override fun observarItens(): Flow<List<ItemCaderno>> = registros.map { mapa ->
        mapa.values.sortedByDescending { it.atualizadoEm }.map { registro ->
            val receita = receitas.receitas.getValue(registro.receitaId)
            ItemCaderno(registro, receita.nome, receita.imagemUrl, receita.categoria)
        }
    }

    override fun observarRegistro(receitaId: String): Flow<RegistroCaderno?> = registros.map { it[receitaId] }

    override fun observarSituacoes(): Flow<Map<String, Situacao>> =
        registros.map { mapa -> mapa.mapValues { it.value.situacao } }

    override suspend fun registro(receitaId: String): RegistroCaderno? = registros.value[receitaId]

    override suspend fun salvar(registro: RegistroCaderno, fotoNovaUri: String?) {
        val foto = fotoNovaUri?.let { "/fotos/${it.substringAfterLast('/')}.jpg".also(fotosGuardadas::add) }
        registros.update { it + (registro.receitaId to registro.copy(fotoCaminho = foto ?: registro.fotoCaminho)) }
    }

    override suspend fun remover(receitaId: String) {
        registros.update { it - receitaId }
    }
}

class PreferenciasFalsas(inicial: Preferencias = Preferencias()) : PreferenciasRepository {
    private val estado = MutableStateFlow(inicial)
    override val preferencias: Flow<Preferencias> = estado

    override suspend fun salvarCategoria(categoria: String) = estado.update { it.copy(categoria = categoria) }

    override suspend fun salvarFiltroCaderno(filtro: FiltroCaderno) = estado.update { it.copy(filtroCaderno = filtro) }
}

class ArmazenamentoFotosFalso : ArmazenamentoFotos {
    val apagadas = mutableListOf<String>()
    private var contador = 0

    override suspend fun guardar(uriOrigem: String): String = "/fotos/prato-${++contador}.jpg"

    override suspend fun apagar(caminho: String) {
        apagadas += caminho
    }
}

/** Todas as dependências falsas juntas, para testar telas e o app inteiro. */
class DependenciasFalsas : Dependencias {
    override val receitas = ReceitaRepositoryFalso()
    override val caderno = CadernoRepositoryFalso(receitas)
    override val preferencias = PreferenciasFalsas()
}
