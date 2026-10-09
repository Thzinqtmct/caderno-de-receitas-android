package br.com.caderno.parcial

import android.app.Activity
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.caderno.parcial.databinding.ActivityDetalheReceitaBinding
import br.com.caderno.parcial.detalhe.DetalheReceitaActivity
import br.com.caderno.parcial.lista.ListaReceitasActivity
import br.com.caderno.parcial.modelo.Ingrediente
import br.com.caderno.parcial.modelo.Porcoes
import com.google.android.material.checkbox.MaterialCheckBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ParcialTest {

    @Test
    fun porcoes_recalculaQuantidadesETrataAGosto() {
        val leite = Ingrediente("leite condensado", 1.0, "lata")
        assertEquals("2 latas de leite condensado", Porcoes.descrever(leite, 20, 40))
        assertEquals("½ lata de leite condensado", Porcoes.descrever(leite, 20, 10))
        assertEquals("3 ovos", Porcoes.descrever(Ingrediente("ovos", 3.0, null), 10, 10))
        assertEquals("Sal a gosto", Porcoes.descrever(Ingrediente("Sal", null, null), 10, 99))
        assertEquals("0,33", Porcoes.formatarNumero(1.0 / 3))
        assertEquals("colheres de sopa", Porcoes.plural("colher de sopa"))
        assertEquals("500 g de polvilho", Porcoes.descrever(Ingrediente("polvilho", 500.0, "g"), 1, 1))
    }

    @Test
    fun tocarNoItem_abreDetalhesComIntentExplicita() {
        ActivityScenario.launch(ListaReceitasActivity::class.java).onActivity { activity ->
            val lista = activity.findViewById<RecyclerView>(R.id.lista) // só no teste, para achar a View
            lista.measure(0, 0)
            lista.layout(0, 0, 1080, 2000)
            lista.findViewHolderForAdapterPosition(0)!!.itemView.findViewById<View>(R.id.conteudo).performClick()

            val intent = shadowOf(activity).nextStartedActivity
            assertEquals(DetalheReceitaActivity::class.java.name, intent.component?.className)
            assertEquals("brigadeiro", intent.getStringExtra(DetalheReceitaActivity.EXTRA_ID))
        }
    }

    @Test
    fun detalhes_ajustarRendimentoEMarcarIngredientesAtualizaATela() {
        val intent = DetalheReceitaActivity.criarIntent(
            ApplicationProvider.getApplicationContext(),
            receitaId = "brigadeiro",
            favorita = false,
        )
        ActivityScenario.launch<DetalheReceitaActivity>(intent).onActivity { activity ->
            val binding = ActivityDetalheReceitaBinding.bind(activity.findViewById(R.id.raiz))
            val primeiro = { binding.listaIngredientes.getChildAt(0) as MaterialCheckBox }

            assertEquals("1 lata de leite condensado", primeiro().text)
            assertEquals("0 de 4 ingredientes separados", binding.progresso.text)

            // "+" no contador: 20 -> 21 unidades; as quantidades são recalculadas
            binding.contador.findViewById<View>(R.id.mais).performClick()
            assertEquals(21, binding.contador.valor)
            assertEquals("1,05 lata de leite condensado", primeiro().text)

            primeiro().performClick()
            assertEquals("1 de 4 ingrediente separado", binding.progresso.text)

            // Favoritar devolve o resultado para a lista
            binding.botaoFavoritar.performClick()
            assertEquals("Tirar das favoritas", binding.botaoFavoritar.text)
            val resultado = shadowOf(activity).resultIntent
            assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
            assertTrue(resultado.getBooleanExtra(DetalheReceitaActivity.EXTRA_FAVORITA, false))
        }
    }

    @Test
    fun detalhes_semIdValido_fechaATela() {
        val intent = DetalheReceitaActivity.criarIntent(
            ApplicationProvider.getApplicationContext(),
            receitaId = "nao-existe",
            favorita = false,
        )
        val cenario = ActivityScenario.launch<DetalheReceitaActivity>(intent)
        assertEquals(Lifecycle.State.DESTROYED, cenario.state)
    }
}
