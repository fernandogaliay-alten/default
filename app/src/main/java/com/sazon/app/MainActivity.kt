package com.sazon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sazon.app.data.AssetTaxonomySource
import com.sazon.app.data.profile.ProfileDatabase
import com.sazon.app.data.profile.RoomProfileRepository
import com.sazon.app.ui.SazonApp
import com.sazon.app.ui.theme.SazonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Composicion de dependencias a mano (sin DI): base de datos -> repositorio -> raiz de la app.
        val database = ProfileDatabase.build(applicationContext)
        val profileRepository = RoomProfileRepository(database.profileDao())
        val taxonomySource = AssetTaxonomySource(applicationContext)

        setContent {
            SazonTheme {
                SazonApp(
                    profileRepository = profileRepository,
                    taxonomySource = taxonomySource,
                )
            }
        }
    }
}
