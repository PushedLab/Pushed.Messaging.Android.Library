package ru.pushed.messaginglibrary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

class Connectivity(private val _context: Context): BroadcastReceiver() {
    private val connectivityAction = "android.net.conn.CONNECTIVITY_CHANGE"
    private val tag="Connectivity"
    enum class Status{NONE,WIFI,MOBILE,ETHERNET,BLUETOOTH,VPN}
    private val connectivityManager: ConnectivityManager =_context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var listener:(Status)->Unit=::baseListener
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    val currentStatus:Status
        get() {
            return getStatus()
        }
    @Suppress("DEPRECATION")
    private fun getStatus():Status {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
            val network= connectivityManager.activeNetwork
            val capabilities=connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return Status.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return Status.ETHERNET
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> return Status.VPN
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return Status.MOBILE
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> return Status.BLUETOOTH
                }
            }
        }
        else {
            val info=connectivityManager.activeNetworkInfo
            if (info != null) {
                when(info.type){
                    ConnectivityManager.TYPE_BLUETOOTH-> return Status.BLUETOOTH
                    ConnectivityManager.TYPE_ETHERNET-> return Status.ETHERNET
                    ConnectivityManager.TYPE_WIFI-> return Status.WIFI
                    ConnectivityManager.TYPE_WIMAX-> return Status.WIFI
                    ConnectivityManager.TYPE_VPN-> return Status.VPN
                    ConnectivityManager.TYPE_MOBILE-> return Status.MOBILE
                    ConnectivityManager.TYPE_MOBILE_DUN-> return Status.MOBILE
                    ConnectivityManager.TYPE_MOBILE_HIPRI-> return Status.MOBILE
                }
            }
        }

        return Status.NONE
    }
    private fun baseListener(_status:Status) {
        Log.d(tag,"Connection: $_status")
    }
    fun setListener(_listener:(Status)->Unit) {
        listener=_listener
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.N) {
            networkCallback=object :ConnectivityManager.NetworkCallback(){
                override fun onAvailable(network: Network) {
                    listener(currentStatus)
                }
                override fun onLost(network: Network) {
                    listener(Status.NONE)
                }
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
        else
            _context.registerReceiver(this, IntentFilter(connectivityAction))
    }
    override fun onReceive(p0: Context?, p1: Intent?) {
        listener(currentStatus)
    }

}