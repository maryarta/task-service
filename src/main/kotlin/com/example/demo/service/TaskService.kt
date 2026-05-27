package com.example.demo.service

import com.example.demo.DTO.CreateTaskRequest
import com.example.demo.domain.Task
import com.example.demo.domain.TaskStatus
import com.example.demo.repository.TaskRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Optional

@Service
class TaskService (val taskRepository: TaskRepository, val taskQueue: TaskQueue) {

    fun findAll(): List<Task> = taskRepository.findAll()

    fun findById(id: Long): Optional<Task> = taskRepository.findById(id)

    fun create(request: CreateTaskRequest): Task {
        val task = Task(
            file = request.file,
            status = TaskStatus.CREATED,
            createdAt = Instant.now()
        )
        val createdTask = taskRepository.save(task)

//        taskQueue.addTask(createdTask.id!!)
        return createdTask
    }

    fun addToQueue(id: Long): Task {
        val task = taskRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Task with id=$id not found")

        if (task.status != TaskStatus.CREATED) {
            throw IllegalStateException("Task with id=$id is already ${task.status}")
        }

        task.status = TaskStatus.QUEUED
        val savedTask = taskRepository.save(task)
        taskQueue.addTask(id)
        return savedTask
    }



    fun cancelTask(id: Long): Task? {
        val task = taskRepository.findById(id).orElse(null) ?: return null
        task.status = TaskStatus.CANCELED
        return taskRepository.save(task)
    }

    fun setProcessingStatus(id: Long){
        val task = taskRepository.findById(id).orElse(null) ?: return
        task.status = TaskStatus.PROCESSING
        taskRepository.save(task)
    }

    fun setDoneStatus(id: Long){
        val task = taskRepository.findById(id).orElse(null) ?: return
        task.status = TaskStatus.DONE
        taskRepository.save(task)
    }

}