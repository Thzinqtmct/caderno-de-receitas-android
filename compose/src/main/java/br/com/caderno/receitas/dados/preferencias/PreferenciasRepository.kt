package br.com.caderno.receitas.dados.preferencias

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Filtros da tela "Meu caderno". */
enum class FiltroCaderno { TODAS, QUERO_FAZER, JA_FIZ }

data class Preferencias(
    /** Última categoria escolhida em "Explorar" (abre nela da próxima vez). */
    val categoria: String = CATEGORIA_PADRAO,
    val filtroCaderno: FiltroCaderno = FiltroCaderno.TODAS,
) {
    companion object {
        const val CATEGORIA_PADRAO = "Dessert"
    }
}

interface PreferenciasRepository {
    val preferencias: Flow<Preferencias>
    suspend fun salvarCategoria(categoria: String)
    suspend fun salvarFiltroCaderno(filtro: FiltroCaderno)
}

/** Preferências simples guardadas com DataStore (chave-valor, assíncrono e reativo). */
class PreferenciasDataStore(
    private val dataStore: DataStore<Preferences>,
) : PreferenciasRepository {

    override val preferencias: Flow<Preferencias> = dataStore.data
        .catch { erro -> if (erro is IOException) emit(emptyPreferences()) else throw erro }
        .map { prefs ->
            Preferencias(
                categoria = prefs[CATEGORIA] ?: Preferencias.CATEGORIA_PADRAO,
                filtroCaderno = prefs[FILTRO]
                    ?.let { nome -> FiltroCaderno.entries.firstOrNull { it.name == nome } }
                    ?: FiltroCaderno.TODAS,
            )
        }

    override suspend fun salvarCategoria(categoria: String) {
        dataStore.edit { it[CATEGORIA] = categoria }
    }

    override suspend fun salvarFiltroCaderno(filtro: FiltroCaderno) {
        dataStore.edit { it[FILTRO] = filtro.name }
    }

    private companion object {
        val CATEGORIA = stringPreferencesKey("categoria")
        val FILTRO = stringPreferencesKey("filtro_caderno")
    }
}
