package fr.skyost.bonsoir

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.annotation.NonNull
import fr.skyost.bonsoir.broadcast.BonsoirRegistrationListener
import fr.skyost.bonsoir.discovery.BonsoirDiscoveryListener
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.*


class MethodCallHandler(private val applicationContext: Context, private val messenger: BinaryMessenger) : MethodChannel.MethodCallHandler {
    private val registrationListeners: HashMap<Int, BonsoirRegistrationListener> = HashMap()
    private val discoveryListeners: HashMap<Int, BonsoirDiscoveryListener> = HashMap()

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        val nsdManager: NsdManager = applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val id: Int = call.argument("id")!!
        when (call.method) {
            "broadcast.initialize" -> {
                registrationListeners[id] = BonsoirRegistrationListener(id, call.argument("printLogs")!!, Runnable {
                    registrationListeners.remove(id)
                }, nsdManager, messenger)
                result.success(true)
            }
            "broadcast.start" -> {
                val serviceInfo = NsdServiceInfo()
                serviceInfo.serviceName = call.argument("service.name")
                serviceInfo.serviceType = call.argument("service.type")
                serviceInfo.port = call.argument("service.port")!!

                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListeners[id])
                result.success(true)
            }
            "broadcast.stop" -> {
                registrationListeners[id]?.dispose()
                result.success(true)
            }
            "discovery.initialize" -> {
                discoveryListeners[id] = BonsoirDiscoveryListener(id, call.argument("printLogs")!!, Runnable {
                    discoveryListeners.remove(id)
                }, nsdManager, messenger)
                result.success(true)
            }
            "discovery.start" -> {
                nsdManager.discoverServices(call.argument("type"), NsdManager.PROTOCOL_DNS_SD, discoveryListeners[id])
                result.success(true)
            }
            "discovery.stop" -> {
                discoveryListeners[id]?.dispose()
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    fun dispose() {
        for (registrationListener in ArrayList<BonsoirRegistrationListener>(registrationListeners.values)) {
            registrationListener.dispose()
        }
        for (discoveryListener in ArrayList<BonsoirDiscoveryListener>(discoveryListeners.values)) {
            discoveryListener.dispose()
        }
    }
}