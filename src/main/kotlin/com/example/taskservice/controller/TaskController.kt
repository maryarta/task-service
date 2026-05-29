package com.example.taskservice.controller

import com.example.taskservice.dto.CreateTaskRequest
import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import com.example.taskservice.domain.TaskType
import com.example.taskservice.service.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/task")
class TaskController (private val taskService: TaskService) {

    @Operation(summary = "Get task by id")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<Task> {
        return taskService.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }


    @Operation(summary = "Get tasks")
    @ApiResponse(responseCode = "200", description = "Tasks found")
    @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameter")
    @GetMapping
    fun findAll(
        @RequestParam(required = false) status: TaskStatus?,
        @RequestParam(required = false) type: TaskType?,
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
            type = type,
            createdFrom = createdFrom,
            createdTo = createdTo,
            pageable = pageable
        )
    }

    @Operation(summary = "Create task")
    @ApiResponse(responseCode = "201", description = "Task created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @PostMapping
    fun createTask(@Valid @RequestBody request: CreateTaskRequest): ResponseEntity<Task> {
        val task = taskService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(task)
    }


    @Operation(summary = "Run task processing")
    @ApiResponse(responseCode = "202", description = "Task queued for processing")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "409", description = "Task has invalid status")
    @PostMapping("/{id}/run")
    fun runTask(@PathVariable id: Long): ResponseEntity<Void>{
        taskService.addToQueue(id)
        return ResponseEntity.accepted().build()
    }


    @Operation(summary = "Cancel task")
    @ApiResponse(responseCode = "204", description = "Task canceled")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "409", description = "Task cannot be canceled")
    @PatchMapping("/{id}/cancel")
    fun cancelTask(@PathVariable id: Long): ResponseEntity<Void> {
        taskService.cancelTask(id)
        return ResponseEntity.noContent().build()
    }

}