package com.example.taskservice.service

import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import com.example.taskservice.domain.TaskType
import com.example.taskservice.dto.CreateTaskRequest
import com.example.taskservice.exception.InvalidTaskStatusException
import com.example.taskservice.exception.TaskNotFoundException
import com.example.taskservice.repository.TaskRepository
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskServiceTest {

    private val taskRepository = mock<TaskRepository>()
    private val taskService = TaskService(taskRepository)

    @Test
    fun `findById should return task when task exists`() {
        val taskId = 1L
        val task = task(id = taskId)
        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        val result = taskService.findById(taskId)
        assertEquals(task, result)
    }

    @Test
    fun `findById should return null when task does not exist`() {
        val taskId = 1L
        whenever(taskRepository.findById(taskId)).thenReturn(Optional.empty())
        val result = taskService.findById(taskId)
        assertNull(result)
    }

    @Test

    fun `create should save task with CREATED status`() {
        val request = CreateTaskRequest(
            file = "test.txt",
            type = TaskType.VALIDATE
        )
        val savedTask = Task(
            id = 1L,
            file = "test.txt",
            type = TaskType.VALIDATE,
            status = TaskStatus.CREATED,
            createdAt = Instant.now()
        )

        whenever(taskRepository.save(any())).thenReturn(savedTask)
        val result = taskService.create(request)
        assertEquals(1L, result.id)
        assertEquals("test.txt", result.file)
        assertEquals(TaskType.VALIDATE, result.type)
        assertEquals(TaskStatus.CREATED, result.status)
        assertNotNull(result.createdAt)

        verify(taskRepository).save(any())
    }

    @Test

    fun `addToQueue should change CREATED task status to QUEUED`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            type = TaskType.VALIDATE,
            status = TaskStatus.CREATED,
            createdAt = Instant.now()
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))
        val result = taskService.addToQueue(taskId)
        assertEquals(TaskStatus.QUEUED, result.status)
        assertEquals(TaskStatus.QUEUED, task.status)
        verify(taskRepository).findById(taskId)
        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `addToQueue should throw exception when task not found`() {
        val taskId = 1L
        whenever(taskRepository.findById(taskId)).thenReturn(Optional.empty())

        assertFailsWith<TaskNotFoundException> {
            taskService.addToQueue(taskId)
        }

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `addToQueue should throw exception when task is not CREATED`() {
        val taskId = 1L

        val task = task(
            id = taskId,
            status = TaskStatus.PROCESSING
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        assertFailsWith<InvalidTaskStatusException> {
            taskService.addToQueue(taskId)
        }

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `takeNextQueuedTask should return null when there are no queued tasks`() {
        whenever(taskRepository.findNextQueuedTaskIdForUpdate()).thenReturn(null)

        val result = taskService.takeNextQueuedTask()

        assertNull(result)
        verify(taskRepository, never()).save(any())
    }

    @Test

    fun `takeNextQueuedTask should change QUEUED task status to PROCESSING`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.QUEUED,
            result = "result",
            errorMessage = "error"
        )

        whenever(taskRepository.findNextQueuedTaskIdForUpdate()).thenReturn(taskId)
        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))
        whenever(taskRepository.save(task)).thenReturn(task)

        val result = taskService.takeNextQueuedTask()

        assertEquals(task, result)
        assertEquals(TaskStatus.PROCESSING, task.status)
        assertNotNull(task.startedAt)
        assertNull(task.result)
        assertNull(task.errorMessage)
        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `cancelTask should change CREATED task status to CANCELED`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.CREATED
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        taskService.cancelTask(taskId)

        assertEquals(TaskStatus.CANCELED, task.status)

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `cancelTask should change QUEUED task status to CANCELED`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.QUEUED
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        taskService.cancelTask(taskId)

        assertEquals(TaskStatus.CANCELED, task.status)

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `cancelTask should throw exception when task is PROCESSING`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.PROCESSING
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))
        assertFailsWith<InvalidTaskStatusException> {
            taskService.cancelTask(taskId)
        }

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `cancelTask should throw exception when task is DONE`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.DONE
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))
        assertFailsWith<InvalidTaskStatusException> {
            taskService.cancelTask(taskId)
        }

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `setDoneStatus should change PROCESSING task status to DONE`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.PROCESSING,
            startedAt = Instant.now(),
            errorMessage = "old error"
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        taskService.setDoneStatus(taskId, "File processed successfully")

        assertEquals(TaskStatus.DONE, task.status)

        assertEquals("File processed successfully", task.result)

        assertNull(task.errorMessage)

        assertNull(task.startedAt)

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `setDoneStatus should throw exception when task is not PROCESSING`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.QUEUED
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        assertFailsWith<InvalidTaskStatusException> {
            taskService.setDoneStatus(taskId, "result")
        }

        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `setFailedStatus should change PROCESSING task status to FAILED`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.PROCESSING,
            startedAt = Instant.now(),
            result = "old result"
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        taskService.setFailedStatus(taskId, "Processing failed")

        assertEquals(TaskStatus.FAILED, task.status)
        assertEquals("Processing failed", task.errorMessage)
        assertNull(task.result)
        assertNull(task.startedAt)
        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `setFailedStatus should throw exception when task is not PROCESSING`() {
        val taskId = 1L
        val task = task(
            id = taskId,
            status = TaskStatus.QUEUED
        )

        whenever(taskRepository.findById(taskId)).thenReturn(Optional.of(task))

        assertFailsWith<InvalidTaskStatusException> {
            taskService.setFailedStatus(taskId, "error")
        }
        verify(taskRepository, never()).save(any())
    }

    @Test
    fun `recoverStaleProcessingTasks should return number of recovered tasks`() {
        whenever(
            taskRepository.resetStaleProcessingTasks(
                eq(TaskStatus.PROCESSING),
                eq(TaskStatus.QUEUED),
                any()
            )

        ).thenReturn(2)

        val result = taskService.recoverStaleProcessingTasks(timeoutSeconds = 60)

        assertEquals(2, result)

        verify(taskRepository).resetStaleProcessingTasks(

            eq(TaskStatus.PROCESSING),

            eq(TaskStatus.QUEUED),

            any()

        )

    }

    @Test
    fun `findAll should throw exception when createdFrom is after createdTo`() {
        val createdFrom = Instant.parse("2026-05-20T10:00:00Z")
        val createdTo = Instant.parse("2026-05-19T10:00:00Z")
        assertFailsWith<IllegalArgumentException> {
            taskService.findAll(
                status = null,
                type = null,
                createdFrom = createdFrom,
                createdTo = createdTo,
                pageable = PageRequest.of(0, 10)
            )
        }
    }

    @Test
    fun `findAll should throw exception when sort field is invalid`() {
        val pageable = PageRequest.of(
            0,
            10,
            Sort.by("unknownField").descending()
        )

        assertFailsWith<IllegalArgumentException> {
            taskService.findAll(
                status = null,
                type = null,
                createdFrom = null,
                createdTo = null,
                pageable = pageable
            )

        }

    }

    private fun task(
        id: Long = 1L,
        file: String = "test.txt",
        status: TaskStatus = TaskStatus.CREATED,
        type: TaskType = TaskType.VALIDATE,
        createdAt: Instant = Instant.now(),
        startedAt: Instant? = null,
        result: String? = null,
        errorMessage: String? = null
    ): Task {
        return Task(
            id = id,
            file = file,
            status = status,
            type = type,
            createdAt = createdAt,
            startedAt = startedAt,
            result = result,
            errorMessage = errorMessage
        )
    }
}