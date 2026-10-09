package br.com.caderno.parcial.componentes

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import br.com.caderno.parcial.databinding.ViewContadorPorcoesBinding
import br.com.caderno.parcial.modelo.Porcoes
import java.util.Locale

/**
 * Componente reutilizável: um contador com botões de menos/mais.
 * O layout vem de `view_contador_porcoes.xml` e é inflado aqui dentro com ViewBinding.
 * Quem usa só define [unidade], [valor] e escuta [aoMudar].
 */
class ContadorPorcoesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val binding = ViewContadorPorcoesBinding.inflate(LayoutInflater.from(context), this)

    /** Chamado sempre que o usuário muda o valor pelos botões. */
    var aoMudar: ((Int) -> Unit)? = null

    var valor: Int = Porcoes.MINIMO
        set(novo) {
            field = novo.coerceIn(Porcoes.MINIMO, Porcoes.MAXIMO)
            atualizarTela()
        }

    var unidade: String = ""
        set(nova) {
            field = nova
            binding.unidade.text = nova
            atualizarTela()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        binding.menos.setOnClickListener { mudarPara(valor - 1) }
        binding.mais.setOnClickListener { mudarPara(valor + 1) }
        atualizarTela()
    }

    private fun mudarPara(novo: Int) {
        val anterior = valor
        valor = novo
        if (valor != anterior) aoMudar?.invoke(valor)
    }

    private fun atualizarTela() {
        binding.valor.text = String.format(Locale.getDefault(), "%d", valor)
        binding.valor.contentDescription = "$valor $unidade"
        // Desabilita o botão no limite, em vez de deixar o toque "não fazer nada".
        binding.menos.isEnabled = valor > Porcoes.MINIMO
        binding.mais.isEnabled = valor < Porcoes.MAXIMO
        binding.menos.alpha = if (binding.menos.isEnabled) 1f else 0.38f
        binding.mais.alpha = if (binding.mais.isEnabled) 1f else 0.38f
    }
}
