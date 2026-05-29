package com.example.taskservice.service

import com.example.taskservice.dto.CreateTaskRequest
import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import com.example.taskservice.domain.TaskType
import com.example.taskservice.exception.InvalidTaskStatusException
import com.example.taskservice.exception.TaskNotFoundException
import com.example.taskservice.repository.TaskRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TaskService (private val taskRepository: TaskRepository) {

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
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw IllegalArgumentException("createdFrom must be before or equal to createdTo")
        }
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

    @Transactional
    fun addToQueue(id: Long): Task {
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status != TaskStatus.CREATED) {
            throw InvalidTaskStatusException("Task with id=$id is already ${task.status}")
        }

        task.status = TaskStatus.QUEUED
        return task
    }

    @Transactional
    fun takeNextQueuedTask(): Task? {
        val id = taskRepository.findNextQueuedTaskIdForUpdate() ?: return null
        val task = taskRepository.findById(id).orElseThrow { TaskNotFoundException(id) }

        task.status = TaskStatus.PROCESSING
        task.startedAt = Instant.now()
        task.errorMessage = null
        task.result = null

        return task
    }

    @Transactional
    fun cancelTask(id: Long){
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status == TaskStatus.CANCELED) {
            throw InvalidTaskStatusException("Task with id=$id is already canceled")
        }
        if (task.status != TaskStatus.CREATED && task.status != TaskStatus.QUEUED) {
            throw InvalidTaskStatusException("Only CREATED or QUEUED task can be canceled")
        }
        task.status = TaskStatus.CANCELED
    }

    @Transactional
    fun setDoneStatus(id: Long, result: String) {
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status != TaskStatus.PROCESSING) {
            throw InvalidTaskStatusException("Only PROCESSING task can be marked as DONE")
        }

        task.status = TaskStatus.DONE
        task.result = result
        task.startedAt = null
        task.errorMessage = null
    }

    @Transactional
    fun setFailedStatus(id: Long, errorMessage: String){
        val task = taskRepository.findById(id).orElse(null)
            ?: throw TaskNotFoundException(id)

        if (task.status != TaskStatus.PROCESSING) {
            throw InvalidTaskStatusException("Only PROCESSING task can be marked as FAILED")
        }

        task.status = TaskStatus.FAILED
        task.result = null
        task.startedAt = null
        task.errorMessage = errorMessage
    }

    @Transactional
    fun recoverStaleProcessingTasks(timeoutSeconds: Long): Int {
        val threshold = Instant.now().minusSeconds(timeoutSeconds)
        return taskRepository.resetStaleProcessingTasks(
            processingStatus = TaskStatus.PROCESSING,
            queuedStatus = TaskStatus.QUEUED,
            threshold = threshold
        )
    }
}