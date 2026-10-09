package br.com.caderno.parcial.componentes

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * A partir do Android 15 o app desenha atrás das barras do sistema (edge-to-edge).
 * Esta função dá à tela um espaçamento do tamanho das barras, para nada ficar escondido.
 */
fun View.respeitarBarrasDoSistema() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.updatePadding(left = barras.left, top = barras.top, right = barras.right, bottom = barras.bottom)
        WindowInsetsCompat.CONSUMED
    }
}
