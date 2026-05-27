package com.example.demo.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.springframework.stereotype.Component

@Component
class TaskQueue {
    private val queue = Channel<Long>(Channel.UNLIMITED)

    fun addTask(id: Long) = queue.trySend(id)

    fun tasks(): ReceiveChannel<Long> {
        return queue
    }

    fun close() {
        queue.close()
    }
}