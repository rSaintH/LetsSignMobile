package com.letssign.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.letssign.mobile.data.local.dao.DocumentDao
import com.letssign.mobile.data.local.dao.DocumentEventDao
import com.letssign.mobile.data.local.dao.DocumentSignerDao
import com.letssign.mobile.data.local.dao.SignatureFieldDao
import com.letssign.mobile.data.local.dao.UserDao
import com.letssign.mobile.data.local.entity.DocumentEntity
import com.letssign.mobile.data.local.entity.DocumentEventEntity
import com.letssign.mobile.data.local.entity.DocumentSignerEntity
import com.letssign.mobile.data.local.entity.SignatureFieldEntity
import com.letssign.mobile.data.local.entity.UserEntity
import com.letssign.mobile.data.local.entity.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

@Database(
    entities = [
        UserEntity::class,
        DocumentEntity::class,
        DocumentSignerEntity::class,
        SignatureFieldEntity::class,
        DocumentEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentSignerDao(): DocumentSignerDao
    abstract fun signatureFieldDao(): SignatureFieldDao
    abstract fun documentEventDao(): DocumentEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Credenciais do Admin padrão, usadas apenas para fins de demonstração. */
        const val SEED_ADMIN_EMAIL = "admin@letssign.com"
        const val SEED_ADMIN_PASSWORD = "admin123"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "letssign.db"
                )
                    .addCallback(seedCallback)
                    .build().also { INSTANCE = it }
            }
        }

        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Cria o usuário Admin padrão na primeira execução do app.
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    INSTANCE?.userDao()?.insert(
                        UserEntity(
                            fullName = "Administrador LetsSign",
                            email = SEED_ADMIN_EMAIL,
                            phone = "(11) 90000-0000",
                            passwordHash = BCrypt.hashpw(SEED_ADMIN_PASSWORD, BCrypt.gensalt()),
                            role = UserRole.ADMIN
                        )
                    )
                }
            }
        }
    }
}
