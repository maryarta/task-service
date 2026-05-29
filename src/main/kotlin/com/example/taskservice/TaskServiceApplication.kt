package com.example.taskservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class TaskServiceApplication

fun main(args: Array<String>) {
	runApplication<TaskServiceApplication>(*args)
}
