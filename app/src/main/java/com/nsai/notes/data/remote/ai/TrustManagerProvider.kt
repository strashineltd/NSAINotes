package com.nsai.notes.data.remote.ai

import android.util.Log
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class TrustManagerProvider(private val delegate: X509TrustManager) : X509TrustManager by delegate {

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        try {
            delegate.checkServerTrusted(chain, authType)
        } catch (e: Exception) {
            Log.e("TrustManager", "Certificate check failed", e)
            throw e
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        delegate.checkClientTrusted(chain, authType)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
}
