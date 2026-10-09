package br.com.caderno.receitas.ui.registro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.caderno.receitas.dados.CadernoRepository
import br.com.caderno.receitas.dados.ReceitaRepository
import br.com.caderno.receitas.dominio.Campo
import br.com.caderno.receitas.dominio.DadosFormulario
import br.com.caderno.receitas.dominio.ErroCampo
import br.com.caderno.receitas.dominio.RegistroCaderno
import br.com.caderno.receitas.dominio.ResultadoValidacao
import br.com.caderno.receitas.dominio.Situacao
import br.com.caderno.receitas.dominio.ValidadorRegistro
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Tudo o que a pessoa preencheu. É salvo no SavedStateHandle (como JSON) a cada mudança. */
@Serializable
data class Formulario(
    val situacao: Situacao = Situacao.QUERO_FAZER,
    val vezesTexto: String = "",
    val nota: Int? = null,
    val observacao: String = "",
    /** Foto já salva no app (de um registro anterior). */
    val fotoSalva: String? = null,
    /** Foto escolhida agora no Photo Picker (ainda não copiada). */
    val fotoNovaUri: String? = null,
    /** Depois da 1ª tentativa de salvar, os erros passam a aparecer enquanto a pessoa corrige. */
    val tentouSalvar: Boolean = false,
    val jaExistia: Boolean = false,
) {
    val dados get() = DadosFormulario(situacao, vezesTexto, nota, observacao)
}

enum class Salvamento { PARADO, SALVANDO, FALHOU, CONCLUIDO }

data class RegistroUiState(
    val carregando: Boolean = true,
    val nomeReceita: String? = null,
    val formulario: Formulario = Formulario(),
    val erros: Map<Campo, ErroCampo> = emptyMap(),
    val salvamento: Salvamento = Salvamento.PARADO,
) {
    /** O que mostrar como foto: a nova escolhida ou a que já estava salva. */
    val foto: String? get() = formulario.fotoNovaUri ?: formulario.fotoSalva
}

/**
 * Formulário "Registrar no caderno". Valida com [ValidadorRegistro] e salva pelo repository.
 * O rascunho fica no SavedStateHandle: girar a tela ou o Android recriar o app não apaga o que foi digitado.
 */
class RegistroViewModel(
    private val receitaId: String,
    private val estadoSalvo: SavedStateHandle,
    private val receitas: ReceitaRepository,
    private val caderno: CadernoRepository,
    private val agora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    /** null enquanto o registro existente (se houver) ainda não foi lido do banco. */
    private val formulario = MutableStateFlow(
        estadoSalvo.get<String>(CHAVE_FORMULARIO)?.let { json.decodeFromString(Formulario.serializer(), it) },
    )
    private val nomeReceita = MutableStateFlow<String?>(null)
    private val salvamento = MutableStateFlow(Salvamento.PARADO)

    val uiState: StateFlow<RegistroUiState> =
        combine(formulario, nomeReceita, salvamento) { form, nome, estadoSalvamento ->
            if (form == null) {
                RegistroUiState(carregando = true, nomeReceita = nome)
            } else {
                val validacao = ValidadorRegistro.validar(form.dados)
                RegistroUiState(
                    carregando = false,
                    nomeReceita = nome,
                    formulario = form,
                    erros = if (form.tentouSalvar && validacao is ResultadoValidacao.Invalido) validacao.erros else emptyMap(),
                    salvamento = estadoSalvamento,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegistroUiState())

    init {
        viewModelScope.launch {
            nomeReceita.value = receitas.resumoSalvo(receitaId)?.nome
            if (formulario.value == null) {
                // Primeira vez: preenche com o que já estava no caderno (edição) ou com valores padrão.
                val existente = caderno.registro(receitaId)
                alterar {
                    existente?.let {
                        Formulario(
                            situacao = it.situacao,
                            vezesTexto = if (it.vezesFeita > 0) it.vezesFeita.toString() else "",
                            nota = it.nota,
                            observacao = it.observacao.orEmpty(),
                            fotoSalva = it.fotoCaminho,
                            jaExistia = true,
                        )
                    } ?: Formulario()
                }
            }
        }
    }

    fun aoEscolherSituacao(situacao: Situacao) = alterar { it.copy(situacao = situacao) }

    /** Só aceita dígitos (no máximo 2): o teclado numérico ainda deixa colar outras coisas. */
    fun aoMudarVezes(texto: String) = alterar { it.copy(vezesTexto = texto.filter(Char::isDigit).take(2)) }

    fun aoEscolherNota(nota: Int) = alterar { it.copy(nota = nota.coerceIn(1, 5)) }

    /** Deixa digitar um pouco além do limite para a pessoa ver o erro, mas não infinitamente. */
    fun aoMudarObservacao(texto: String) =
        alterar { it.copy(observacao = texto.take(ValidadorRegistro.MAX_OBSERVACAO + 50)) }

    fun aoEscolherFoto(uri: String) = alterar { it.copy(fotoNovaUri = uri) }

    fun aoRemoverFoto() = alterar { it.copy(fotoNovaUri = null, fotoSalva = null) }

    fun aoSalvar() {
        if (salvamento.value == Salvamento.SALVANDO) return // evita salvar duas vezes com toque duplo
        alterar { it.copy(tentouSalvar = true) }
        val form = formulario.value ?: return
        val valido = ValidadorRegistro.validar(form.dados) as? ResultadoValidacao.Valido ?: return

        viewModelScope.launch {
            salvamento.value = Salvamento.SALVANDO
            salvamento.value = try {
                caderno.salvar(
                    RegistroCaderno(
                        receitaId = receitaId,
                        situacao = form.situacao,
                        vezesFeita = valido.vezesFeita,
                        nota = valido.nota,
                        observacao = valido.observacao,
                        fotoCaminho = form.fotoSalva,
                        atualizadoEm = agora(),
                    ),
                    fotoNovaUri = form.fotoNovaUri,
                )
                Salvamento.CONCLUIDO
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Salvamento.FALHOU
            }
        }
    }

    fun aoRemoverDoCaderno() {
        viewModelScope.launch {
            caderno.remover(receitaId)
            salvamento.value = Salvamento.CONCLUIDO
        }
    }

    private fun alterar(mudanca: (Formulario) -> Formulario) {
        formulario.update { atual -> mudanca(atual ?: Formulario()) }
        if (salvamento.value == Salvamento.FALHOU) salvamento.value = Salvamento.PARADO
        estadoSalvo[CHAVE_FORMULARIO] = json.encodeToString(Formulario.serializer(), formulario.value!!)
    }

    private companion object {
        const val CHAVE_FORMULARIO = "formulario"
    }
}
