package br.com.caderno.receitas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.caderno.receitas.ui.navegacao.CadernoDeReceitasApp
import br.com.caderno.receitas.ui.tema.TemaCaderno
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Id de receita vindo de um deep link, esperando a interface abrir a tela. */
    private val linkPendente = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Só na primeira criação: ao girar a tela a pilha já está restaurada e o link já foi usado.
        if (savedInstanceState == null) linkPendente.value = LinkProfundo.receitaId(intent?.data)

        val dependencias = (application as CadernoApplication).dependencias
        setContent {
            TemaCaderno {
                val link by linkPendente.collectAsStateWithLifecycle()
                CadernoDeReceitasApp(
                    dependencias = dependencias,
                    linkDeReceita = link,
                    aoConsumirLink = { linkPendente.value = null },
                )
            }
        }
    }

    /** Com o app já aberto (launchMode singleTop), um novo link chega por aqui. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LinkProfundo.receitaId(intent.data)?.let { linkPendente.value = it }
    }
}

/**
 * Deep link `caderno://receita/52772`. O id vem de fora do app, então é validado:
 * só números, no máximo 10 dígitos. Qualquer outra coisa é ignorada.
 */
object LinkProfundo {
    private val ID_VALIDO = Regex("""^\d{1,10}$""")

    fun receitaId(uri: Uri?): String? {
        if (uri == null || uri.scheme != "caderno" || uri.host != "receita") return null
        return uri.pathSegments.firstOrNull()?.takeIf { ID_VALIDO.matches(it) }
    }
}
