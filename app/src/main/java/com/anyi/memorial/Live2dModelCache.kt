package com.anyi.memorial

import com.anyi.memorial.network.AnyiApiClient
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * On-device store for generated Live2D models.
 *
 * A succeeded generation never changes (the server only allows retrying failed
 * or cancelled jobs), so every file is kept forever under
 * `<root>/<jobId>/<name>` and verified once against the SHA-256 the server
 * publishes in the job manifest. Nothing here expires; eviction only happens
 * when the whole store outgrows [maxBytes], oldest-used job first.
 *
 * The root must be app-private storage (`filesDir`): model files are the
 * user's own private assets and must not be readable by other apps.
 */
internal class Live2dModelCache(
    private val root: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
    private val targetBytes: Long = DEFAULT_TARGET_BYTES,
    private val parallelism: Int = 4
) {
    /** One entry of the server manifest. */
    data class Entry(val name: String, val sha256: String, val size: Long)

    /** Result of [prefetch]: how many files were already local vs downloaded. */
    data class PrefetchResult(val jobId: String, val total: Int, val downloaded: Int)

    fun jobDir(jobId: String): File = File(root, requireJobId(jobId))

    /** Local file for [name] if it exists and is complete; null otherwise. Touches the job for eviction order. */
    fun read(jobId: String, name: String): File? {
        val file = File(jobDir(jobId), requireName(name))
        if (!file.isFile || file.length() <= 0L) return null
        jobDir(jobId).setLastModified(System.currentTimeMillis())
        return file
    }

    /** True when every file in [manifest] is present locally. */
    fun isComplete(jobId: String, manifest: List<Entry>): Boolean =
        manifest.isNotEmpty() && manifest.all { entry ->
            val file = File(jobDir(jobId), requireName(entry.name))
            file.isFile && file.length() == entry.size
        }

    /**
     * Fetch the manifest and download every missing file, [parallelism] at a
     * time. [onProgress] receives (done, total) after each file. Files that
     * fail verification are discarded; the caller sees the exception.
     */
    suspend fun prefetch(
        api: AnyiApiClient,
        jobId: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): PrefetchResult = withContext(Dispatchers.IO) {
        val id = requireJobId(jobId)
        val manifest = fetchManifest(api, id)
        val missing = manifest.filter { entry ->
            val file = File(jobDir(id), entry.name)
            !(file.isFile && file.length() == entry.size)
        }
        var done = manifest.size - missing.size
        onProgress(done, manifest.size)
        if (missing.isEmpty()) {
            jobDir(id).setLastModified(System.currentTimeMillis())
            return@withContext PrefetchResult(id, manifest.size, 0)
        }
        val gate = Semaphore(parallelism)
        coroutineScope {
            missing.map { entry ->
                async {
                    gate.withPermit { download(api, id, entry) }
                    synchronized(this@Live2dModelCache) { done += 1; onProgress(done, manifest.size) }
                }
            }.forEach { it.await() }
        }
        jobDir(id).setLastModified(System.currentTimeMillis())
        prune()
        PrefetchResult(id, manifest.size, missing.size)
    }

    /**
     * Return the local file for [name], downloading and verifying it first if
     * needed. Used by the WebView interceptor for any file the prefetch did
     * not cover (or when prefetch was skipped).
     */
    suspend fun fetch(api: AnyiApiClient, jobId: String, name: String): File = withContext(Dispatchers.IO) {
        read(jobId, name)?.let { return@withContext it }
        val id = requireJobId(jobId)
        val entry = fetchManifest(api, id).firstOrNull { it.name == name }
            ?: throw IOException("live2d_file_not_in_manifest")
        download(api, id, entry)
        prune()
        File(jobDir(id), entry.name)
    }

    fun fetchManifest(api: AnyiApiClient, jobId: String): List<Entry> =
        parseManifest(api.live2dManifest(requireJobId(jobId)))

    /** Remove one job's files (companion deleted, or the job vanished server-side). */
    fun remove(jobId: String) {
        jobDir(jobId).deleteRecursively()
    }

    /** Remove everything (logout / account switch: never show one account's avatars to another). */
    fun clear() {
        root.deleteRecursively()
    }

    fun totalBytes(): Long = root.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** Evict least-recently-used jobs until under [targetBytes]; only runs when over [maxBytes]. */
    fun prune() {
        var total = totalBytes()
        if (total <= maxBytes) return
        val jobs = root.listFiles()?.filter { it.isDirectory }?.sortedBy { it.lastModified() } ?: return
        for (dir in jobs) {
            if (total <= targetBytes) break
            val size = dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            if (dir.deleteRecursively()) total -= size
        }
    }

    private fun download(api: AnyiApiClient, jobId: String, entry: Entry) {
        val target = File(jobDir(jobId), entry.name)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.${UUID.randomUUID()}.part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            tmp.outputStream().use { out ->
                api.downloadLive2dFile(jobId, entry.name, DigestingOutput(out, digest), entry.size.coerceAtLeast(1L))
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (tmp.length() != entry.size || actual != entry.sha256) {
                throw IOException("live2d_file_corrupt:${entry.name}")
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        } catch (failure: Exception) {
            tmp.delete()
            throw failure
        }
    }

    /** Streams bytes through to [inner] while feeding [digest]. */
    private class DigestingOutput(private val inner: java.io.OutputStream, private val digest: MessageDigest) : java.io.OutputStream() {
        override fun write(b: Int) { digest.update(b.toByte()); inner.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { digest.update(b, off, len); inner.write(b, off, len) }
        override fun flush() = inner.flush()
        override fun close() = inner.close()
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 200L * 1024L * 1024L
        const val DEFAULT_TARGET_BYTES = 150L * 1024L * 1024L
        private val jobIdPattern = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
        private val namePattern = Regex("[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*")

        fun requireJobId(jobId: String): String {
            require(jobIdPattern.matches(jobId)) { "live2d_invalid_id" }
            return jobId
        }

        fun requireName(name: String): String {
            require(name.length <= 200 && namePattern.matches(name) && name.split('/').none { it == "." || it == ".." }) { "live2d_invalid_path" }
            return name
        }

        /** Parses `GET /ai/live2d/jobs/:id/manifest`; rejects anything the store would not accept. */
        fun parseManifest(json: JSONObject): List<Entry> {
            val files = json.optJSONArray("files") ?: return emptyList()
            return List(files.length()) { index ->
                val item = files.getJSONObject(index)
                val name = requireName(item.getString("name"))
                val sha = item.getString("sha256").lowercase()
                require(sha.matches(Regex("[a-f0-9]{64}"))) { "live2d_invalid_manifest" }
                val size = item.getLong("size")
                require(size > 0L) { "live2d_invalid_manifest" }
                Entry(name, sha, size)
            }
        }
    }
}
