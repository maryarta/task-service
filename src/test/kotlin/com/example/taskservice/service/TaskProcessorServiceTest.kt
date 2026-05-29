package com.example.taskservice.service

import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import com.example.taskservice.domain.TaskType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskProcessorServiceTest {

    private val taskService = mock<TaskService>()
    private val taskProcessorService = TaskProcessorService (
        taskService = taskService,
        maxConcurrentTasks = 1,
        pollingDelayMs = 1000L
    )

    @Test

    fun `processTask should set DONE status for valid file`() = runTest {
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.PROCESSING,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )

        taskProcessorService.processTask(task)

        verify(taskService).setDoneStatus(taskId, "File test.txt is valid")
        verify(taskService, never()).setFailedStatus(any(), any())
    }

    @Test

    fun `processTask should set FAILED status when validation fails`() = runTest {
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test",
            status = TaskStatus.PROCESSING,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )

        taskProcessorService.processTask(task)
        verify(taskService).setFailedStatus(taskId, "Invalid file name: extension is missing")
        verify(taskService, never()).setDoneStatus(any(), any())
    }

    @Test
    fun `processTask should set DONE status when convert succeeds`() = runTest {
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.PROCESSING,
            type = TaskType.CONVERT,
            createdAt = Instant.now()
        )

        taskProcessorService.processTask(task)
        verify(taskService).setDoneStatus(taskId, "File test.txt converted successfully")
        verify(taskService, never()).setFailedStatus(any(), any())
    }

    @Test
    fun `processTask should set DONE status when archive succeeds`() = runTest {
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.PROCESSING,
            type = TaskType.ARCHIVE,
            createdAt = Instant.now()
        )

        taskProcessorService.processTask(task)

        verify(taskService).setDoneStatus(taskId, "File test.txt archived successfully")
        verify(taskService, never()).setFailedStatus(any(), any())
    }

    @Test

    fun `processTask should throw exception when task id is null`() = runTest {
        val task = Task(
            id = null,
            file = "test.txt",
            status = TaskStatus.PROCESSING,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )

        assertFailsWith<IllegalStateException> { taskProcessorService.processTask(task)}

        verify(taskService, never()).setDoneStatus(any(), any())
        verify(taskService, never()).setFailedStatus(any(), any())

    }

}