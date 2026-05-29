package com.example.taskservice.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TaskRecoveryService(
    private val taskService: TaskService,
    @Value("\${task.recovery.processing-timeout-seconds}")
    private val processingTimeoutSeconds: Long) {

    @EventListener(ApplicationReadyEvent::class)
    fun recoverOnStartup() {
        taskService.recoverStaleProcessingTasks(processingTimeoutSeconds)

    }

    @Scheduled(fixedDelayString = "\${task.recovery.fixed-delay-ms}")
    fun recoverPeriodically() {
        taskService.recoverStaleProcessingTasks(processingTimeoutSeconds)
    }
}