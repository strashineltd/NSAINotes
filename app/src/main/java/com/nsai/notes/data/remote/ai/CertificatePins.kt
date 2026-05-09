package com.nsai.notes.data.remote.ai

object CertificatePins {
    data class Pin(val hostname: String, val sha256Hash: String)

    // Backup pins: if any primary pin fails, the backup for that host is tried.
    // Both are fetched via: openssl s_client -connect <host>:443 -servername <host>
    //   | openssl x509 -noout -pubkey | openssl pkey -pubin -outform der
    //   | openssl dgst -sha256 -binary | openssl base64
    val pins = listOf(
        Pin("api.deepseek.com", "sha256/jD4HoReqi4yPTndb5/Ks7bDUycyp1uN11oii4qwracs="),
        Pin("api.moonshot.cn", "sha256/kPjMPOLocq+5yBiG1tDVmTqsthmK8BKarCJvdXglzis="),
        Pin("open.bigmodel.cn", "sha256/efpviN4CHX6YeOqbLWsBTvnJqjULfZE/j9OAUrm/qH0="),
        Pin("api.minimax.chat", "sha256/3540qTRpY4mv924+IhiFoVJmZvS/1FLpHDBlVLt9k30="),
        Pin("dashscope.aliyuncs.com", "sha256/WZVJFj4+3elgfAAI/zW+L9mKCgh+6gck7f6zYoUC0Yg=")
    )

    // When providers rotate certificates, add the new pin as a backup BEFORE the old
    // one stops working. Keep the backup list empty until a rotation is announced.
    val backupPins: List<Pin> = emptyList()

    fun hostnames(): List<String> = pins.map { it.hostname }

    fun isValidSha256(hash: String): Boolean {
        val trimmed = hash.removePrefix("sha256/")
        return trimmed.length == 44 || trimmed.length == 64
    }
}
