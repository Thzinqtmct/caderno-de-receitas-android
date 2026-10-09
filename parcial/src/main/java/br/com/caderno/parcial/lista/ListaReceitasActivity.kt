package br.com.caderno.parcial.lista

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.caderno.parcial.R
import br.com.caderno.parcial.componentes.respeitarBarrasDoSistema
import br.com.caderno.parcial.dados.ReceitasMock
import br.com.caderno.parcial.databinding.ActivityListaReceitasBinding
import br.com.caderno.parcial.detalhe.DetalheReceitaActivity
import br.com.caderno.parcial.modelo.Receita
import br.com.caderno.parcial.modelo.ReceitaNaLista

/** Tela 1: lista de receitas (RecyclerView) com filtro de favoritas. */
class ListaReceitasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaReceitasBinding

    private val adapter = ReceitaAdapter(
        aoAbrir = ::abrirDetalhes,
        aoAlternarFavorita = { receita -> definirFavorita(receita.id, receita.id !in favoritas) },
    )

    /** Ids das receitas favoritas. É trocado por um conjunto novo a cada mudança (imutável). */
    private var favoritas: Set<String> = emptySet()

    /**
     * Abre os detalhes esperando uma resposta: se a pessoa favoritar lá,
     * a tela de detalhes devolve o novo estado e a lista se atualiza ao voltar.
     */
    private val abrirDetalhesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            val dados = resultado.data
            if (resultado.resultCode == Activity.RESULT_OK && dados != null) {
                val id = dados.getStringExtra(DetalheReceitaActivity.EXTRA_ID) ?: return@registerForActivityResult
                definirFavorita(id, dados.getBooleanExtra(DetalheReceitaActivity.EXTRA_FAVORITA, false))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityListaReceitasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respeitarBarrasDoSistema()

        // Ao girar a tela a Activity é recriada: recuperamos as favoritas salvas.
        favoritas = savedInstanceState?.getStringArrayList(ESTADO_FAVORITAS)?.toSet() ?: emptySet()

        binding.lista.adapter = adapter
        binding.filtroFavoritas.setOnCheckedChangeListener { _, _ -> atualizarTela() }
        atualizarTela()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(ESTADO_FAVORITAS, ArrayList(favoritas))
    }

    private fun abrirDetalhes(receita: Receita) {
        // Intent explícita: dizemos exatamente qual Activity abrir e mandamos os dados como extras.
        val intent = DetalheReceitaActivity.criarIntent(this, receita.id, receita.id in favoritas)
        abrirDetalhesLauncher.launch(intent)
    }

    private fun definirFavorita(id: String, favorita: Boolean) {
        favoritas = if (favorita) favoritas + id else favoritas - id
        atualizarTela()
    }

    /** Monta a lista a partir do estado atual e atualiza resumo, lista e mensagem de vazio. */
    private fun atualizarTela() {
        val todas = ReceitasMock.receitas.map { ReceitaNaLista(it, favorita = it.id in favoritas) }
        val visiveis = if (binding.filtroFavoritas.isChecked) todas.filter { it.favorita } else todas

        adapter.submitList(visiveis)
        binding.resumo.text = resources.getQuantityString(
            R.plurals.resumo_lista,
            todas.size,
            todas.size,
            favoritas.size,
        )
        val vazia = visiveis.isEmpty()
        binding.vazio.visibility = if (vazia) View.VISIBLE else View.GONE
        binding.lista.visibility = if (vazia) View.GONE else View.VISIBLE
    }

    private companion object {
        const val ESTADO_FAVORITAS = "favoritas"
    }
}
