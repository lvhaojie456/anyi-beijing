package com.anyi.memorial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConversationStateTest {
    @Test
    fun replacingOnePendingMessageDoesNotMoveLaterQueuedMessages() {
        val first = AiConversationMessage("local-1", "user", "第一条", 1L, pending = true)
        val second = AiConversationMessage("local-2", "user", "第二条", 2L, pending = true)
        val returned = listOf(
            AiConversationMessage("server-1", "user", "第一条", 3L),
            AiConversationMessage("server-2", "ai", "第一条的回复", 4L)
        )

        val result = replacePendingAiMessage(listOf(first, second), first.id, returned)

        assertEquals(listOf("server-1", "server-2", "local-2"), result.map { it.id })
        assertFalse(result[0].pending)
        assertFalse(result[1].pending)
        assertTrue(result[2].pending)
    }

    @Test
    fun mergingHistoryKeepsLocalMessagesThatHaveNotReachedTheServer() {
        val history = AiConversationMessage("server-history", "ai", "早上好", 1L)
        val pending = AiConversationMessage("local-1", "user", "你好", 2L, pending = true)

        val result = mergeAiMessages(listOf(history), listOf(history, pending))

        assertEquals(listOf("server-history", "local-1"), result.map { it.id })
        assertTrue(result.last().pending)
    }
}
