package br.com.caderno.receitas.ui.navegacao

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * Rotas do Navigation 3. Cada tela é uma chave serializável; os argumentos são propriedades
 * da própria rota (ex.: o id da receita). Por serem serializáveis, a pilha de navegação
 * (back stack) é salva e restaurada ao girar a tela ou quando o Android recria o app.
 */

@Serializable
data object RotaExplorar : NavKey

@Serializable
data object RotaCaderno : NavKey

@Serializable
data class RotaReceita(val receitaId: String) : NavKey

@Serializable
data class RotaRegistro(val receitaId: String) : NavKey
