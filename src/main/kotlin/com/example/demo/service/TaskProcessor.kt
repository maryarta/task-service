package com.example.demo.service

import com.example.demo.domain.TaskStatus
import com.jetbrains.exported.JBRApi
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds

@Service
class TaskProcessor(private val taskService: TaskService, private val taskQueue: TaskQueue,
    @Value($$"${task.processing.max-concurrent}")
                    private val maxConcurrentTasks: Int) {


    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    @PostConstruct
    fun startWorkers() {
        repeat(maxConcurrentTasks) {
            scope.launch {
                for (taskId in taskQueue.tasks()) {
                    processTask(taskId)
                }
            }
        }
//        logger.info("Обработчики очереди запущены")
    }



    suspend fun processTask(id: Long) {
        val task = taskService.findById(id).orElse(null) ?: return
        if (task.status == TaskStatus.CANCELED) {
            return
        }
        taskService.setProcessingStatus(id)
        delay(10000L.milliseconds)
        taskService.setDoneStatus(id)
        //изменить статус
    }

    @PreDestroy
    fun shutdown() {
        taskQueue.close()        // сигнализируем, что новых элементов не будет
        scope.cancel()       // отменяем все корутины
//        logger.info("Сервис обработки очереди остановлен")
    }

}