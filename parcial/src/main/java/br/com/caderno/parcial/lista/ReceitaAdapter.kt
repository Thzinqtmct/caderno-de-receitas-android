package br.com.caderno.parcial.lista

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.caderno.parcial.R
import br.com.caderno.parcial.databinding.ItemReceitaBinding
import br.com.caderno.parcial.modelo.Receita
import br.com.caderno.parcial.modelo.ReceitaNaLista

/**
 * Adapter da RecyclerView. Usa ListAdapter + DiffUtil: ao receber uma lista nova,
 * só os itens que mudaram (ex.: o coração de favorito) são redesenhados.
 */
class ReceitaAdapter(
    private val aoAbrir: (Receita) -> Unit,
    private val aoAlternarFavorita: (Receita) -> Unit,
) : ListAdapter<ReceitaNaLista, ReceitaAdapter.ReceitaViewHolder>(Diferenca) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceitaViewHolder {
        val binding = ItemReceitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReceitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceitaViewHolder, position: Int) {
        holder.mostrar(getItem(position))
    }

    inner class ReceitaViewHolder(
        private val binding: ItemReceitaBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun mostrar(item: ReceitaNaLista) {
            val receita = item.receita
            val contexto = binding.root.context
            binding.emoji.text = receita.emoji
            binding.nome.text = receita.nome
            binding.detalhes.text = contexto.getString(
                R.string.detalhes_item,
                receita.categoria.rotulo,
                textoDoTempo(contexto, receita.tempoPreparoMin),
                receita.dificuldade.rotulo,
            )

            binding.favorito.setImageResource(
                if (item.favorita) R.drawable.ic_favorito else R.drawable.ic_favorito_borda,
            )
            binding.favorito.contentDescription = contexto.getString(
                if (item.favorita) R.string.cd_desfavoritar else R.string.cd_favoritar,
                receita.nome,
            )

            binding.conteudo.setOnClickListener { aoAbrir(receita) }
            binding.favorito.setOnClickListener { aoAlternarFavorita(receita) }
        }
    }

    private object Diferenca : DiffUtil.ItemCallback<ReceitaNaLista>() {
        override fun areItemsTheSame(antigo: ReceitaNaLista, novo: ReceitaNaLista) =
            antigo.receita.id == novo.receita.id

        override fun areContentsTheSame(antigo: ReceitaNaLista, novo: ReceitaNaLista) = antigo == novo
    }
}

/** "20 min", "3 h 00 min" ou "Tempo não informado" quando o valor é nulo. */
fun textoDoTempo(contexto: Context, minutos: Int?): String = when {
    minutos == null -> contexto.getString(R.string.tempo_desconhecido)
    minutos < 60 -> contexto.getString(R.string.tempo_minutos, minutos)
    else -> contexto.getString(R.string.tempo_horas, minutos / 60, minutos % 60)
}
