package com.anyi.memorial

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Splits a reply into utterances that fit one TTS request (the API caps 中文 at 150 字).
 * The first utterance stays short so playback starts while the rest is synthesized.
 */
internal fun splitSpeechSentences(text: String, limit: Int = 120, firstLimit: Int = 40): List<String> {
    val compact = text.replace(Regex("\\s+"), " ").trim()
    if (compact.isEmpty()) return emptyList()
    val sentences = Regex("[^。！？!?；;\\n]+[。！？!?；;]?").findAll(compact)
        .map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    fun flush() {
        if (current.isNotEmpty()) { chunks += current.toString(); current.clear() }
    }
    for (sentence in sentences) {
        // A single sentence longer than the limit is hard-wrapped on its own.
        if (sentence.length > limit) {
            flush()
            chunks += sentence.chunked(limit)
            continue
        }
        val cap = if (chunks.isEmpty()) firstLimit else limit
        if (current.isEmpty()) { current.append(sentence); continue }
        if (current.length + sentence.length <= cap) current.append(sentence) else { flush(); current.append(sentence) }
    }
    flush()
    // A trailing fragment this short is not worth its own request.
    if (chunks.size > 1 && chunks.last().length < 8 && chunks[chunks.size - 2].length + chunks.last().length <= limit) {
        chunks[chunks.size - 2] = chunks[chunks.size - 2] + chunks.last()
        chunks.removeAt(chunks.size - 1)
    }
    return chunks.map { it.take(150) }
}

/**
 * Plays synthesized sentences in order through one MediaPlayer, and reports a mouth
 * openness signal while audio is audible so the Live2D avatar moves with the voice.
 */
internal class AiSpeechQueue(
    private val cacheDir: File,
    private val scope: CoroutineScope,
    private val onPlayingChanged: (Boolean) -> Unit,
    private val onLipsync: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private var player: MediaPlayer? = null
    private var cancelled = false
    var playingMessageId: String? = null
        private set

    fun stop() {
        cancelled = true
        release()
        playingMessageId = null
        onLipsync(0f)
        onPlayingChanged(false)
    }

    /** Plays every utterance; [fetch] returns the MP3 bytes for one sentence. */
    suspend fun play(messageId: String, sentences: List<String>, fetch: suspend (String) -> ByteArray) {
        if (sentences.isEmpty()) return
        stop()
        cancelled = false
        playingMessageId = messageId
        onPlayingChanged(true)
        var prefetch: Job? = null
        var pending: ByteArray? = null
        try {
            for ((index, sentence) in sentences.withIndex()) {
                if (cancelled) return
                val bytes = pending ?: fetch(sentence)
                pending = null
                if (cancelled) return
                val file = File(cacheDir, "speech-${messageId.hashCode()}-$index.mp3")
                file.parentFile?.mkdirs()
                withContext(Dispatchers.IO) { file.writeBytes(bytes) }
                prefetch?.join()
                val next = sentences.getOrNull(index + 1)
                if (next != null) {
                    prefetch = scope.launch(Dispatchers.IO) {
                        pending = runCatching { fetch(next) }.getOrNull()
                    }
                }
                playFile(file)
            }
        } catch (cancelled_: CancellationException) {
            throw cancelled_
        } catch (failure: Throwable) {
            onError("语音生成失败，已显示文字")
        } finally {
            prefetch?.cancel()
            release()
            if (playingMessageId == messageId) {
                playingMessageId = null
                onPlayingChanged(false)
                onLipsync(0f)
            }
        }
    }

    private fun release() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
    }

    private suspend fun playFile(file: File) = suspendCancellableCoroutine { continuation ->
        val next = MediaPlayer()
        player = next
        var finished = false
        fun done() {
            if (finished) return
            finished = true
            release()
            if (continuation.isActive) continuation.resume(Unit)
        }
        try {
            next.setDataSource(file.absolutePath)
            next.setOnCompletionListener { if (player === next) done() }
            next.setOnErrorListener { _, _, _ -> if (player === next) done(); true }
            next.setOnPreparedListener { if (player === next && !cancelled) next.start() else done() }
            next.prepareAsync()
        } catch (failure: Throwable) {
            done()
        }
        continuation.invokeOnCancellation { release() }
    }
}
