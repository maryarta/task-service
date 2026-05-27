package com.example.demo.controller

import com.example.demo.DTO.CreateTaskRequest
import com.example.demo.domain.Task
import com.example.demo.domain.TaskStatus
import com.example.demo.service.TaskService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.Optional

@RestController
@RequestMapping("/task")
class TaskController (private val taskService: TaskService) {

    @GetMapping("/{id}")
    fun findTaskById(@PathVariable id: Long): ResponseEntity<Task> {
        return taskService.findById(id).toResponseEntity()
    }

    @GetMapping
    fun findAll(
        @RequestParam(required = false) status: TaskStatus?,
//        @RequestParam(required = false) type: TaskType?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        createdFrom: Instant?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        createdTo: Instant?,
        @PageableDefault(size = 10, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ):Page<Task>{
        return taskService.findAll(
            status = status,
//            type = type,
            createdFrom = createdFrom,
            createdTo = createdTo,
            pageable = pageable

        )
//        = ResponseEntity.ok(taskService.findAll())
    }



    private fun Optional<Task>.toResponseEntity(): ResponseEntity<Task> =
        if (this.isPresent) {
            ResponseEntity.ok(this.get())
        } else {
            ResponseEntity.notFound().build()
        }

    @PostMapping
    fun createTask(@RequestBody request: CreateTaskRequest): ResponseEntity<Task> {
        val savedTask = taskService.create(request)
        return ResponseEntity.ok(savedTask)
    }

    @PostMapping("{id}/run")
    fun runTask(@PathVariable id: Long):ResponseEntity<Task>{
        return try {
            val task = taskService.addToQueue(id)
            ResponseEntity.accepted().body(task)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PutMapping("/cancel/{id}")
    fun cancelTask(@PathVariable id: Long): ResponseEntity<Task>{
        return taskService.cancelTask(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }


}