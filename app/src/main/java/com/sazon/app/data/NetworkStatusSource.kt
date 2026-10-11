package com.sazon.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Costura minima de conectividad para el bloque 2 de la Home (Busqueda con IA): **siempre** hay
 * internet validado o no.
 *
 * Es la unica dependencia externa de la Home y la unica forma de cumplir el Scenario 4 (barra
 * deshabilitada sin conexion) con el generador falso, que nunca falla por red. No es una dependencia
 * nueva (API del framework) y su unica decision de UI (`canSubmit`) es pura y testeable
 * ([com.sazon.app.ui.search.AiSearchQuery]). Requiere el permiso `ACCESS_NETWORK_STATE`.
 *
 * [observe] emite el estado actual y luego los cambios via `registerDefaultNetworkCallback`; se
 * considera online solo si la red activa tiene `NET_CAPABILITY_INTERNET` **y**
 * `NET_CAPABILITY_VALIDATED` (evita dar por buena una red cautiva o sin salida real).
 */
class NetworkStatusSource(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isOnline())
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(isOnline())
            }
        }

        trySend(isOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
