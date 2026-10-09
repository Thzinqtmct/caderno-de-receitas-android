package br.com.caderno.receitas

import android.app.Application

/** Criada uma vez por processo: guarda as dependências que vivem enquanto o app está aberto. */
class CadernoApplication : Application() {
    val dependencias: Dependencias by lazy { DependenciasDoApp(this) }
}
