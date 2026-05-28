package com.example.demo.service

import com.example.demo.dto.CreateTaskRequest
import com.example.demo.domain.Task
import com.example.demo.domain.TaskStatus
import com.example.demo.domain.TaskType
import com.example.demo.exception.InvalidTaskStatusException
import com.example.demo.exception.TaskNotFoundException
import com.example.demo.repository.TaskRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TaskService (val taskRepository: TaskRepository, val taskQueue: TaskQueue) {

    fun findById(id: Long): Task?{
        return taskRepository.findById(id).orElse(null)
    }

    fun findAll(
        status: TaskStatus?,
        type: TaskType?,
        createdFrom: Instant?,
        createdTo: Instant?,
        pageable: Pageable
    ): Page<Task>{
        validateSort(pageable)
        val specification = Specification<Task> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            status?.let {
                predicates.add(cb.equal(root.get<TaskStatus>("status"), it))
            }

            type?.let {
                predicates.add(cb.equal(root.get<TaskType>("type"), it))
            }

            createdFrom?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it))
            }

            createdTo?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it))
            }

            cb.and(*predicates.toTypedArray())
        }
        return taskRepository.findAll(specification, pageable)
    }

    private val sortFields = setOf(
        "id",
        "file",
        "status",
        "type",
        "createdAt"
    )
    private fun validateSort(pageable: Pageable) {
        pageable.sort.forEach { order ->
            if (order.property !in sortFields) {
                throw IllegalArgumentException("Invalid sort field: ${order.property}")
            }
        }
    }

    fun create(request: CreateTaskRequest): Task {
        val task = Task(
            file = request.file,
            status = TaskStatus.CREATED,
            type = request.type,
            createdAt = Instant.now()
        )
        return taskRepository.save(task)
    }

    fun addToQueue(id: Long): Task {
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status != TaskStatus.CREATED) {
            throw InvalidTaskStatusException("Task with id=$id is already ${task.status}")
        }

        task.status = TaskStatus.QUEUED
        val savedTask = taskRepository.save(task)
        taskQueue.addTask(id)
        return savedTask
    }


    fun cancelTask(id: Long){
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status == TaskStatus.DONE || task.status == TaskStatus.FAILED) {
            throw InvalidTaskStatusException("Task with id=$id cannot be canceled because status is ${task.status}")
        }
        if (task.status == TaskStatus.CANCELED) {
            throw InvalidTaskStatusException("Task with id=$id is already canceled")
        }
        task.status = TaskStatus.CANCELED
        taskRepository.save(task)
    }

    fun setProcessingStatus(id: Long){
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)
        if (task.status != TaskStatus.QUEUED) {
            throw InvalidTaskStatusException("Only QUEUED task can be marked as PROCESSING")
        }

        task.status = TaskStatus.PROCESSING
        taskRepository.save(task)
    }

    fun setDoneStatus(id: Long, result: String) {
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status == TaskStatus.CANCELED) {
            throw InvalidTaskStatusException("Canceled task cannot be marked as DONE")
        }

        if (task.status != TaskStatus.PROCESSING) {
            throw InvalidTaskStatusException("Only PROCESSING task can be marked as DONE")
        }

        task.status = TaskStatus.DONE
        task.result = result
        task.errorMessage = null
        taskRepository.save(task)
    }

    fun setFailedStatus(id: Long, errorMessage: String){
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status != TaskStatus.QUEUED && task.status != TaskStatus.PROCESSING) {
            throw InvalidTaskStatusException("Only QUEUED or PROCESSING task can be marked as FAILED")
        }

        task.status = TaskStatus.FAILED
        task.result = null
        task.errorMessage = errorMessage
        taskRepository.save(task)
    }
}