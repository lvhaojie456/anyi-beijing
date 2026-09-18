package com.anyi.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSpeechSentenceTest {
    @Test
    fun longReplyIsSplitIntoPlayableUtterances() {
        val reply = "奶奶今天身体还好吗？我这边一切都好，就是有点想您。等周末我回去看您，您想吃什么我提前买好带回去。"
        val chunks = splitSpeechSentences(reply)
        assertTrue("expected several utterances, got ${chunks.size}", chunks.size >= 2)
        assertTrue("the first utterance must stay short", chunks.first().length <= 40)
        chunks.forEach { assertTrue("utterance too long: ${it.length}", it.length <= 150) }
        assertEquals(reply.replace(" ", ""), chunks.joinToString("").replace(" ", ""))
    }

    @Test
    fun shortRepliesBecomeOneUtteranceAndEmptyTextNone() {
        assertEquals(listOf("好的，我记住了。"), splitSpeechSentences("好的，我记住了。"))
        assertEquals(emptyList<String>(), splitSpeechSentences("   "))
        assertEquals(emptyList<String>(), splitSpeechSentences(""))
    }

    @Test
    fun veryLongSentenceIsHardWrappedAtTheApiLimit() {
        val paragraph = "嗯".repeat(400)
        val chunks = splitSpeechSentences(paragraph)
        assertTrue(chunks.size >= 3)
        chunks.forEach { assertTrue("limit exceeded: ${it.length}", it.length <= 150) }
        assertEquals(400, chunks.joinToString("").length)
    }

    @Test
    fun punctuationlessTextStillProducesOneChunk() {
        val chunks = splitSpeechSentences("今天天气不错")
        assertEquals(listOf("今天天气不错"), chunks)
    }
}
