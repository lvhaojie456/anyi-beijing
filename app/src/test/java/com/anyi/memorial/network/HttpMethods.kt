package com.anyi.memorial.network

import java.net.HttpURLConnection

/**
 * Test-only shim that lets local unit tests issue PATCH requests.
 *
 * `HttpURLConnection.setRequestMethod` validates against a private static
 * `methods` array. On the desktop JVM that array has no PATCH entry, so any
 * PATCH attempt throws `ProtocolException: Invalid HTTP method: PATCH` before a
 * socket is opened. Android does not share this limitation: its
 * `HttpURLConnection` is backed by OkHttp, which sends PATCH normally. So the
 * production client is correct on device, and only the test JVM needs widening.
 *
 * We overwrite the unused TRACE slot rather than growing the array, so the
 * array length and every other index stay untouched.
 *
 * Requires `--add-opens java.base/java.net=ALL-UNNAMED`, set for unit tests in
 * app/build.gradle.kts.
 */
internal object HttpMethods {
    private var patched = false

    fun allowPatch() {
        if (patched) return
        val field = HttpURLConnection::class.java.getDeclaredField("methods")
        field.isAccessible = true
        val methods = field.get(null) as Array<*>
        @Suppress("UNCHECKED_CAST")
        val mutable = methods as Array<String>
        if (mutable.none { it == "PATCH" }) {
            val slot = mutable.indexOfFirst { it == "TRACE" }
            check(slot >= 0) { "no TRACE slot to reuse for PATCH: ${mutable.joinToString()}" }
            mutable[slot] = "PATCH"
        }
        patched = true
    }
}
