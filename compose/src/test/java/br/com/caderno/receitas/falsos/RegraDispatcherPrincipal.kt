package br.com.caderno.receitas.falsos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * O viewModelScope roda no Dispatchers.Main, que não existe na JVM. Esta regra coloca no lugar
 * um dispatcher de teste com "tempo virtual" (dá para avançar 500 ms sem esperar de verdade).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegraDispatcherPrincipal(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
