package br.com.caderno.receitas

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import br.com.caderno.receitas.dados.CadernoRepository
import br.com.caderno.receitas.dados.CadernoRepositoryPadrao
import br.com.caderno.receitas.dados.ReceitaRepository
import br.com.caderno.receitas.dados.ReceitaRepositoryPadrao
import br.com.caderno.receitas.dados.arquivos.ArmazenamentoFotosInterno
import br.com.caderno.receitas.dados.local.BancoCaderno
import br.com.caderno.receitas.dados.preferencias.PreferenciasDataStore
import br.com.caderno.receitas.dados.preferencias.PreferenciasRepository
import br.com.caderno.receitas.dados.remoto.TheMealDbApi
import io.ktor.client.engine.android.Android

private val Context.dataStore by preferencesDataStore(name = "preferencias")

/**
 * Injeção de dependências manual: este é o único lugar que sabe quais classes concretas
 * o app usa. As telas recebem só as interfaces; nos testes entra um conjunto de fakes.
 */
interface Dependencias {
    val receitas: ReceitaRepository
    val caderno: CadernoRepository
    val preferencias: PreferenciasRepository
}

class DependenciasDoApp(context: Context) : Dependencias {

    private val appContext = context.applicationContext

    // "by lazy": cada objeto só é criado na primeira vez que alguém precisa dele.
    private val banco by lazy { BancoCaderno.criar(appContext) }
    private val api by lazy { TheMealDbApi(Android.create(), BuildConfig.THEMEALDB_API_KEY) }

    override val receitas: ReceitaRepository by lazy {
        ReceitaRepositoryPadrao(api, banco.receitaDao())
    }

    override val caderno: CadernoRepository by lazy {
        CadernoRepositoryPadrao(banco.registroDao(), ArmazenamentoFotosInterno(appContext))
    }

    override val preferencias: PreferenciasRepository by lazy {
        PreferenciasDataStore(appContext.dataStore)
    }
}
