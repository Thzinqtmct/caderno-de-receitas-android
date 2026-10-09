package br.com.caderno.parcial.detalhe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import br.com.caderno.parcial.R
import br.com.caderno.parcial.componentes.respeitarBarrasDoSistema
import br.com.caderno.parcial.dados.ReceitasMock
import br.com.caderno.parcial.databinding.ActivityDetalheReceitaBinding
import br.com.caderno.parcial.databinding.ItemIngredienteBinding
import br.com.caderno.parcial.lista.textoDoTempo
import br.com.caderno.parcial.modelo.Porcoes
import br.com.caderno.parcial.modelo.Receita

/**
 * Tela 2: detalhes de uma receita. Recebe o id e o estado de favorita pela Intent,
 * deixa ajustar o rendimento (recalcula os ingredientes), marcar ingredientes separados
 * e favoritar — devolvendo o resultado para a lista.
 */
class DetalheReceitaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalheReceitaBinding
    private lateinit var receita: Receita

    private var favorita = false
    private var porcoes = 0
    private var separados: Set<Int> = emptySet() // posições dos ingredientes marcados

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // O id pode não vir (ou não existir): nesse caso avisamos e fechamos a tela.
        val encontrada = ReceitasMock.buscar(intent.getStringExtra(EXTRA_ID))
        if (encontrada == null) {
            Toast.makeText(this, R.string.receita_nao_encontrada, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        receita = encontrada

        binding = ActivityDetalheReceitaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.raiz.respeitarBarrasDoSistema()

        // Estado da tela: vem do Bundle salvo (rotação) ou, na primeira vez, da Intent/receita.
        favorita = savedInstanceState?.getBoolean(EXTRA_FAVORITA)
            ?: intent.getBooleanExtra(EXTRA_FAVORITA, false)
        porcoes = savedInstanceState?.getInt(ESTADO_PORCOES) ?: receita.rendimento
        separados = savedInstanceState?.getIntegerArrayList(ESTADO_SEPARADOS)?.toSet() ?: emptySet()

        mostrarReceita()

        // O Fragment só é criado na primeira vez; após rotação o FragmentManager o recria sozinho.
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(binding.containerPreparo.id, ModoPreparoFragment.novo(receita.id))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_FAVORITA, favorita)
        outState.putInt(ESTADO_PORCOES, porcoes)
        outState.putIntegerArrayList(ESTADO_SEPARADOS, ArrayList(separados))
    }

    private fun mostrarReceita() {
        binding.barra.title = receita.nome
        binding.barra.setNavigationOnClickListener { finish() }

        binding.emoji.text = receita.emoji
        binding.categoria.text = receita.categoria.rotulo
        binding.nome.text = receita.nome
        binding.tempo.text = textoDoTempo(this, receita.tempoPreparoMin)
        binding.dificuldade.text = getString(R.string.dificuldade, receita.dificuldade.rotulo)

        // Dica é opcional: se for nula, a View some (GONE) em vez de mostrar um espaço vazio.
        val dica = receita.dica
        binding.dica.visibility = if (dica == null) View.GONE else View.VISIBLE
        binding.dica.text = dica?.let { getString(R.string.dica, it) }

        binding.contador.unidade = receita.unidadeRendimento
        binding.contador.valor = porcoes
        binding.contador.aoMudar = { novo ->
            porcoes = novo
            mostrarIngredientes()
        }

        binding.botaoFavoritar.setOnClickListener {
            favorita = !favorita
            mostrarBotaoFavoritar()
            devolverResultado()
        }

        mostrarIngredientes()
        mostrarBotaoFavoritar()
    }

    /** Infla uma linha (item_ingrediente.xml) para cada ingrediente, com a quantidade ajustada. */
    private fun mostrarIngredientes() {
        val container = binding.listaIngredientes
        container.removeAllViews()
        receita.ingredientes.forEachIndexed { posicao, ingrediente ->
            val linha = ItemIngredienteBinding.inflate(layoutInflater, container, true)
            linha.caixa.text = Porcoes.descrever(ingrediente, receita.rendimento, porcoes)
            linha.caixa.isChecked = posicao in separados
            linha.caixa.setOnCheckedChangeListener { _, marcado ->
                separados = if (marcado) separados + posicao else separados - posicao
                mostrarProgresso()
            }
        }
        mostrarProgresso()
    }

    private fun mostrarProgresso() {
        val total = receita.ingredientes.size
        binding.progresso.text = if (separados.size == total) {
            getString(R.string.tudo_separado)
        } else {
            resources.getQuantityString(R.plurals.progresso_ingredientes, separados.size, separados.size, total)
        }
    }

    private fun mostrarBotaoFavoritar() {
        binding.botaoFavoritar.setText(if (favorita) R.string.desfavoritar else R.string.favoritar)
        binding.botaoFavoritar.setIconResource(if (favorita) R.drawable.ic_favorito else R.drawable.ic_favorito_borda)
    }

    /** Deixa pronto o resultado que a lista recebe quando esta tela fechar. */
    private fun devolverResultado() {
        val dados = Intent()
            .putExtra(EXTRA_ID, receita.id)
            .putExtra(EXTRA_FAVORITA, favorita)
        setResult(Activity.RESULT_OK, dados)
    }

    companion object {
        const val EXTRA_ID = "br.com.caderno.parcial.EXTRA_ID"
        const val EXTRA_FAVORITA = "br.com.caderno.parcial.EXTRA_FAVORITA"
        private const val ESTADO_PORCOES = "porcoes"
        private const val ESTADO_SEPARADOS = "separados"

        /** Cria a Intent explícita para abrir esta tela com os dados necessários. */
        fun criarIntent(contexto: Context, receitaId: String, favorita: Boolean): Intent =
            Intent(contexto, DetalheReceitaActivity::class.java)
                .putExtra(EXTRA_ID, receitaId)
                .putExtra(EXTRA_FAVORITA, favorita)
    }
}
