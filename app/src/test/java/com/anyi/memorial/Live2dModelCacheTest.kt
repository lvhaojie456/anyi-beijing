package com.anyi.memorial

import com.anyi.memorial.network.AnyiApiClient
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Live2dModelCacheTest {
    private val jobId = "0533712f-e198-4867-abcd-664a0f65fac6"
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** A fake API serving a manifest plus files; counts downloads so tests can prove the cache is hit. */
    private class FakeServer(files: Map<String, ByteArray>, jobId: String, corruptName: String? = null) : AutoCloseable {
        val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var downloads = 0
        val api: AnyiApiClient
        init {
            val manifest = files.entries.joinToString(",") { (name, bytes) ->
                val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                """{"name":"$name","sha256":"$sha","size":${bytes.size}}"""
            }
            server.createContext("/ai/live2d/jobs/$jobId/manifest") { exchange ->
                val body = """{"jobId":"$jobId","files":[$manifest]}""".toByteArray()
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong()); exchange.responseBody.use { it.write(body) }
            }
            server.createContext("/ai/live2d/jobs/$jobId/files/") { exchange ->
                val name = exchange.requestURI.path.substringAfter("/files/")
                val bytes = files[name]
                if (bytes == null) { exchange.sendResponseHeaders(404, -1); exchange.close(); return@createContext }
                downloads += 1
                val payload = if (name == corruptName) bytes.copyOf().also { it[0] = (it[0] + 1).toByte() } else bytes
                exchange.sendResponseHeaders(200, payload.size.toLong()); exchange.responseBody.use { it.write(payload) }
            }
            server.start()
            api = AnyiApiClient("http://127.0.0.1:${server.address.port}") { "test-token" }
        }
        override fun close() = server.stop(0)
    }

    private fun tempRoot(): File = Files.createTempDirectory("anyi-live2d-cache").toFile()

    @Test
    fun prefetchDownloadsOnceThenServesFromDisk() {
        val files = mapOf(
            "runtime/model.model3.json" to """{"Version":3}""".toByteArray(),
            "runtime/model.moc3" to "MOC3fixture".toByteArray(),
            "runtime/texture.png" to ByteArray(5000) { (it % 251).toByte() },
            "preview.png" to ByteArray(300) { 7 }
        )
        FakeServer(files, jobId).use { fake ->
            val cache = Live2dModelCache(tempRoot())
            val progress = mutableListOf<Pair<Int, Int>>()
            val first = runBlocking { cache.prefetch(fake.api, jobId) { d, t -> progress += d to t } }
            assertEquals(4, first.total); assertEquals(4, first.downloaded); assertEquals(4, fake.downloads)
            assertEquals(4 to 4, progress.last())
            for ((name, bytes) in files) {
                val local = cache.read(jobId, name)
                assertNotNull(name, local); assertTrue(bytes.contentEquals(local!!.readBytes()))
            }
            assertTrue(cache.isComplete(jobId, cache.fetchManifest(fake.api, jobId)))
            // Second prefetch: everything is local, nothing is downloaded again.
            val second = runBlocking { cache.prefetch(fake.api, jobId) }
            assertEquals(0, second.downloaded); assertEquals(4, fake.downloads)
            // fetch() of a cached file also does not touch the network.
            runBlocking { cache.fetch(fake.api, jobId, "runtime/model.moc3") }
            assertEquals(4, fake.downloads)
            // No stray temp files are left behind.
            assertTrue(cache.jobDir(jobId).walkBottomUp().none { it.name.endsWith(".part") })
        }
    }

    @Test
    fun corruptDownloadIsRejectedAndNotKept() {
        val files = mapOf("runtime/model.moc3" to "MOC3fixture".toByteArray(), "runtime/ok.json" to "{}".toByteArray())
        FakeServer(files, jobId, corruptName = "runtime/model.moc3").use { fake ->
            val cache = Live2dModelCache(tempRoot())
            val failure = assertThrows(Exception::class.java) { runBlocking { cache.prefetch(fake.api, jobId) } }
            assertTrue(failure.message.orEmpty(), failure.message?.contains("live2d_file_corrupt") == true)
            assertNull(cache.read(jobId, "runtime/model.moc3"))
            assertTrue(cache.jobDir(jobId).walkBottomUp().none { it.name.endsWith(".part") })
            // The good file may or may not have landed depending on ordering; either way the store stays consistent.
            cache.read(jobId, "runtime/ok.json")?.let { assertEquals("{}", it.readText()) }
        }
    }

    @Test
    fun removeClearAndPruneManageTheStore() {
        val root = tempRoot()
        val cache = Live2dModelCache(root, maxBytes = 1000, targetBytes = 900)
        fun seed(id: String, size: Int, age: Long) {
            val dir = File(root, id).apply { mkdirs() }
            File(dir, "runtime/blob.bin").apply { parentFile!!.mkdirs(); writeBytes(ByteArray(size)) }
            dir.setLastModified(age)
        }
        val old = "11111111-1111-4111-8111-111111111111"; val mid = "22222222-2222-4222-8222-222222222222"; val new = "33333333-3333-4333-8333-333333333333"
        // Realistic timestamps: some filesystems clamp or round values near the epoch.
        val now = System.currentTimeMillis()
        seed(old, 400, now - 3 * 86_400_000L); seed(mid, 400, now - 2 * 86_400_000L); seed(new, 400, now - 86_400_000L)
        assertTrue(File(root, old).lastModified() < File(root, mid).lastModified())
        assertTrue(File(root, mid).lastModified() < File(root, new).lastModified())
        assertEquals(1200L, cache.totalBytes())
        cache.prune()
        // Oldest job goes first, and pruning stops once under target.
        val remaining = root.listFiles()!!.filter { it.isDirectory }.map { it.name }.sorted()
        assertEquals("remaining after prune: $remaining (total ${cache.totalBytes()})", listOf(mid, new), remaining)
        assertEquals(800L, cache.totalBytes())
        cache.remove(mid)
        assertFalse(File(root, mid).exists()); assertEquals(400L, cache.totalBytes())
        cache.clear()
        assertFalse(root.exists())
    }

    @Test
    fun rejectsUnsafeIdsNamesAndManifests() {
        val cache = Live2dModelCache(tempRoot())
        assertThrows(IllegalArgumentException::class.java) { cache.read("../etc", "runtime/x") }
        assertThrows(IllegalArgumentException::class.java) { cache.read(jobId, "../secret") }
        assertThrows(IllegalArgumentException::class.java) { cache.read(jobId, "/abs") }
        assertThrows(IllegalArgumentException::class.java) {
            Live2dModelCache.parseManifest(org.json.JSONObject("""{"files":[{"name":"runtime/a","sha256":"nothex","size":1}]}"""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Live2dModelCache.parseManifest(org.json.JSONObject("""{"files":[{"name":"a/../b","sha256":"${"0".repeat(64)}","size":1}]}"""))
        }
        assertEquals(emptyList<Live2dModelCache.Entry>(), Live2dModelCache.parseManifest(org.json.JSONObject("{}")))
    }
}
