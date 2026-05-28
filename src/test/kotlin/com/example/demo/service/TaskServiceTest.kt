package com.example.demo.service

import com.example.demo.domain.Task
import com.example.demo.domain.TaskStatus
import com.example.demo.domain.TaskType
import com.example.demo.exception.InvalidTaskStatusException
import com.example.demo.exception.TaskNotFoundException
import com.example.demo.repository.TaskRepository
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskServiceTest {

    private val taskRepository = mock<TaskRepository>()

    private val taskQueue = mock<TaskQueue>()

    private val taskService = TaskService(taskRepository, taskQueue)

    @Test
    fun `addToQueue should change status to QUEUED`() {
        val taskId = 1L
        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.CREATED,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )
        whenever(taskRepository.findById(taskId))
            .thenReturn(Optional.of(task))

        whenever(taskRepository.save(task))
            .thenReturn(task)

        val result = taskService.addToQueue(taskId)
        assertEquals(TaskStatus.QUEUED, result.status)

        verify(taskRepository).findById(taskId)
        verify(taskRepository).save(task)
        verify(taskQueue).addTask(taskId)
    }

    @Test
    fun `addToQueue should throw exception when task is not CREATED`() {
        val taskId = 1L

        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.PROCESSING,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )

        whenever(taskRepository.findById(taskId))
            .thenReturn(Optional.of(task))

        assertThrows<InvalidTaskStatusException> {
            taskService.addToQueue(taskId)
        }

        verify(taskRepository).findById(taskId)
        verify(taskRepository, never()).save(any<Task>())
        verify(taskQueue, never()).addTask(any())
    }

    @Test
    fun `addToQueue should throw exception when task not found`() {
        val taskId = 999L

        whenever(taskRepository.findById(taskId))
            .thenReturn(Optional.empty())

        assertThrows<TaskNotFoundException> {
            taskService.addToQueue(taskId)
        }

        verify(taskRepository).findById(taskId)
        verify(taskRepository, never()).save(any<Task>())
        verify(taskQueue, never()).addTask(any())
    }

    @Test
    fun `cancelTask should throw exception when task is not CREATED or QUEUED`(){
        val taskId = 1L

        val task = Task(
            id = taskId,
            file = "test.txt",
            status = TaskStatus.FAILED,
            type = TaskType.VALIDATE,
            createdAt = Instant.now()
        )

        whenever(taskRepository.findById(taskId))
            .thenReturn(Optional.of(task))

        assertThrows<InvalidTaskStatusException> {
            taskService.cancelTask(taskId)
        }
        verify(taskRepository).findById(taskId)
        verify(taskRepository, never()).save(any<Task>())
    }

}