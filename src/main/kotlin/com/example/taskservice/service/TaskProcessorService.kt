package com.example.demo.service

import com.example.demo.config.TaskProcessingProperties
import com.example.demo.domain.Task
import com.example.demo.domain.TaskStatus
import com.example.demo.domain.TaskType
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds

@Service
class TaskProcessorService(
    private val taskService: TaskService,
    private val taskQueue: TaskQueue,
    private val properties: TaskProcessingProperties
) {


    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    @PostConstruct
    fun startWorkers() {
        repeat(properties.maxConcurrentTasks) {
            scope.launch {
                for (taskId in taskQueue.tasks()) {
                    processTask(taskId)
                }
            }
        }
    }

    suspend fun processTask(id: Long) {
        try{
            val task = taskService.findById(id) ?: return
            if (task.status == TaskStatus.CANCELED) {
                return
            }
            taskService.setProcessingStatus(id)
            val result = processFile(task)

            taskService.setDoneStatus(id, result)
        } catch (e: Exception){
            taskService.setFailedStatus(id, errorMessage = e.message ?: "Unknown processing error")
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
        taskQueue.close()        // сигнализируем, что новых элементов не будет
        scope.cancel()       // отменяем все корутины
//        logger.info("Сервис обработки очереди остановлен")
    }

}