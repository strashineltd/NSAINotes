package com.nsai.notes.data.local.license

import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseVerifier @Inject constructor() {

    // RSA 2048-bit public key (PEM format, PKCS8)
    // Private key is held offline by the developer for generating activation codes
    // RSA 2048-bit DER public key (base64)
    // Private key held offline for generating activation codes:
    // openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
    // openssl rsa -in private.pem -pubout -outform DER | base64 -w0
    private val publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsSaTE0Y5WHRLT/R6oPDlU1KkI0Qejf9QubUltg1CB30eS0mjRVRaO848ToSaTk3aYTzKdqZXiZDft0sr7jwXNdjsXTKvaGEc8Xykq1oQS9pYhJ9Ax1XhjdUCTRbkIvusx63OcyRfV9GcisMguE3Ku3pwvbYVo5KasTRvdv53LUuRCT7dbcJRINlTSTefhSEMZsUzLJv70+qkS9LShPb5a1z3K3XpYm9oy7SESoL1b5X6ILUsI33rAzACUK11pW+wx3wb4VQQykhayqsLptgVWMWtfViGF+Ve5ZJa9BYrkEm5WQcEZw+TCB6fDBTdE8VNV/3nFGQlYxk7brTftMJ8YQIDAQAB"

    fun verify(deviceId: String, expireTimestamp: Long, signature: ByteArray): Boolean {
        return try {
            val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update("$deviceId|$expireTimestamp".toByteArray(Charsets.UTF_8))
            sig.verify(signature)
        } catch (_: Exception) {
            false
        }
    }
}
