package br.com.caderno.receitas.dados.arquivos

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** Guarda as fotos dos pratos em arquivos do próprio app (persistência em arquivos). */
interface ArmazenamentoFotos {
    /** Copia a imagem escolhida no Photo Picker e devolve o caminho do arquivo salvo. */
    suspend fun guardar(uriOrigem: String): String
    suspend fun apagar(caminho: String)
}

/**
 * O Photo Picker só dá acesso temporário à imagem escolhida. Por isso copiamos o arquivo para
 * a pasta interna do app (`files/fotos`): assim a foto continua lá depois de reiniciar o celular.
 * A cópia é feita fora da thread principal (Dispatchers.IO) para não travar a tela.
 */
class ArmazenamentoFotosInterno(
    private val context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ArmazenamentoFotos {

    private val pasta: File get() = File(context.filesDir, PASTA).apply { mkdirs() }

    override suspend fun guardar(uriOrigem: String): String = withContext(io) {
        val destino = File(pasta, "prato-${UUID.randomUUID()}.jpg")
        val entrada = context.contentResolver.openInputStream(uriOrigem.toUri())
            ?: throw IOException("Não foi possível abrir a imagem escolhida")
        entrada.use { origem -> destino.outputStream().use { origem.copyTo(it) } }
        destino.absolutePath
    }

    override suspend fun apagar(caminho: String) {
        withContext(io) {
            val arquivo = File(caminho)
            // Segurança: só apaga arquivos que estão dentro da pasta de fotos do app.
            if (arquivo.canonicalPath.startsWith(pasta.canonicalPath)) arquivo.delete()
        }
    }

    private companion object {
        const val PASTA = "fotos"
    }
}
