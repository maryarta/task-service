package com.example.taskservice.service

import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import com.example.taskservice.domain.TaskType
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds

@Service
class TaskProcessorService(
    private val taskService: TaskService,
    @Value($$"${task.processing.max-concurrent-tasks}")
    private val maxConcurrentTasks: Int,
    @Value($$"${task.processing.polling-delay-ms}")
    private val pollingDelayMs: Long
    ) {


    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    @PostConstruct
    fun startWorkers() {
        repeat(maxConcurrentTasks) {
            scope.launch {
                worker()
            }
        }
    }

    private suspend fun worker() {
        while (scope.isActive) {
            val task = taskService.takeNextQueuedTask()
            if (task == null) {
                delay(pollingDelayMs.milliseconds)
                continue
            }
            processTask(task)
        }
    }

    suspend fun processTask(task: Task) {
        val taskId = task.id ?: throw IllegalStateException("Task id is null")
        try{
            val result = processFile(task)
            taskService.setDoneStatus(taskId, result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception){
            taskService.setFailedStatus(taskId, errorMessage = e.message ?: "Unknown processing error")
        }
    }

    suspend fun processFile(task: Task): String {
        return when (task.type) {
            TaskType.CONVERT -> convertFile(task.file)
            TaskType.ARCHIVE -> archiveFile(task.file)
            TaskType.VALIDATE -> validateFile(task.file)
        }
    }

    suspend fun convertFile(file: String): String{
        delay(3000.milliseconds)
        return "File $file converted successfully"
    }

    suspend fun archiveFile(file: String): String{
        delay(4000.milliseconds)
        return "File $file archived successfully"
    }

    private suspend fun validateFile(file: String): String {
        delay(1000.milliseconds)
        if (!file.contains(".")) {
            throw IllegalArgumentException("Invalid file name: extension is missing")
        }
        return "File $file is valid"
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

}