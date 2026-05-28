package com.example.demo.service

import com.example.demo.config.TaskProcessingProperties
import com.example.demo.domain.Task
import com.example.demo.domain.TaskStatus
import com.example.demo.domain.TaskType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class TaskProcessorServiceTest {

    private val taskService = mock<TaskService>()
    private val taskQueue = mock<TaskQueue>()

    private val taskProcessorService = TaskProcessorService (
        taskService = taskService,
        taskQueue = taskQueue,
        properties = TaskProcessingProperties(maxConcurrentTasks = 1)
    )

    @Test
    fun `processTask should return when task is CANCELLED`() = runTest{
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.CANCELED,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )
        whenever(taskService.findById(taskId))
            .thenReturn(task)

        taskProcessorService.processTask(taskId)

        verify(taskService).findById(taskId)

        verify(taskService, never()).setProcessingStatus(any())
        verify(taskService, never()).setDoneStatus(any(), any())
        verify(taskService, never()).setFailedStatus(any(), any())
    }

    @Test
    fun `processTask should process when task is QUEUED`() = runTest{
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.QUEUED,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )
        whenever(taskService.findById(taskId))
            .thenReturn(task)

        taskProcessorService.processTask(taskId)

        val inOrder = inOrder(taskService)
        inOrder.verify(taskService).findById(taskId)
        inOrder.verify(taskService).setProcessingStatus(taskId)
        inOrder.verify(taskService).setDoneStatus(taskId, "File test.txt is valid")

        verify(taskService, never()).setFailedStatus(any(), any())
    }

    @Test
    fun `processTask should process FAILED when task is QUEUED`() = runTest{
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test",
            status = TaskStatus.QUEUED,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )
        whenever(taskService.findById(taskId))
            .thenReturn(task)

        taskProcessorService.processTask(taskId)

        val inOrder = inOrder(taskService)
        inOrder.verify(taskService).findById(taskId)
        inOrder.verify(taskService).setProcessingStatus(taskId)
        inOrder.verify(taskService).setFailedStatus(taskId,"Invalid file name: extension is missing")

    }

}