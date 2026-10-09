package br.com.caderno.parcial.detalhe

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import br.com.caderno.parcial.R
import br.com.caderno.parcial.dados.ReceitasMock
import br.com.caderno.parcial.databinding.FragmentModoPreparoBinding
import br.com.caderno.parcial.databinding.ItemPassoBinding

/**
 * Fragment com o modo de preparo. Tocar num passo marca/desmarca como feito.
 *
 * Ciclo de vida + ViewBinding: a View do Fragment pode ser destruída antes do próprio Fragment,
 * por isso o binding é criado em onCreateView e zerado em onDestroyView (evita vazamento de memória).
 */
class ModoPreparoFragment : Fragment() {

    private var _binding: FragmentModoPreparoBinding? = null
    private val binding get() = _binding!!

    private var feitos: Set<Int> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feitos = savedInstanceState?.getIntegerArrayList(ESTADO_FEITOS)?.toSet() ?: emptySet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentModoPreparoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mostrarPassos()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntegerArrayList(ESTADO_FEITOS, ArrayList(feitos))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarPassos() {
        val passos = ReceitasMock.buscar(requireArguments().getString(ARG_RECEITA_ID))?.passos.orEmpty()
        binding.listaPassos.removeAllViews()
        passos.forEachIndexed { posicao, texto ->
            val item = ItemPassoBinding.inflate(layoutInflater, binding.listaPassos, true)
            item.texto.text = texto
            mostrarEstadoDoPasso(item, posicao)
            item.linha.setOnClickListener {
                feitos = if (posicao in feitos) feitos - posicao else feitos + posicao
                mostrarEstadoDoPasso(item, posicao)
            }
        }
    }

    private fun mostrarEstadoDoPasso(item: ItemPassoBinding, posicao: Int) {
        val feito = posicao in feitos
        item.numero.text = getString(if (feito) R.string.passo_feito else R.string.passo, posicao + 1)
        item.texto.paintFlags = if (feito) {
            item.texto.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            item.texto.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        item.texto.alpha = if (feito) 0.6f else 1f
    }

    companion object {
        private const val ARG_RECEITA_ID = "receita_id"
        private const val ESTADO_FEITOS = "feitos"

        /** Os argumentos vão num Bundle: assim o Fragment sobrevive à recriação. */
        fun novo(receitaId: String) = ModoPreparoFragment().apply {
            arguments = bundleOf(ARG_RECEITA_ID to receitaId)
        }
    }
}
