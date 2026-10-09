package br.com.caderno.receitas.dados.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ReceitaEntity::class,
        BuscaEntity::class,
        CategoriaEntity::class,
        RegistroEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class BancoCaderno : RoomDatabase() {
    abstract fun receitaDao(): ReceitaDao
    abstract fun registroDao(): RegistroDao

    companion object {
        const val NOME_ARQUIVO = "caderno.db"

        fun criar(context: Context): BancoCaderno =
            Room.databaseBuilder(context, BancoCaderno::class.java, NOME_ARQUIVO).build()
    }
}
