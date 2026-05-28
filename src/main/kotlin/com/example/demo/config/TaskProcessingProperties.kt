package com.example.demo.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "task.processing")
data class TaskProcessingProperties(
    val maxConcurrentTasks: Int = 3
)