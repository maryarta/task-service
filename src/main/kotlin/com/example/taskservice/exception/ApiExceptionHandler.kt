package com.example.taskservice.exception

import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ApiError(
    val message: String
)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(TaskNotFoundException::class)
    fun handleTaskNotFound(exception: TaskNotFoundException): ResponseEntity<ApiError> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiError(exception.message ?: "Task not found"))
    }

    @ExceptionHandler(InvalidTaskStatusException::class)
    fun handleInvalidTaskStatus(exception: InvalidTaskStatusException): ResponseEntity<ApiError> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiError(exception.message ?: "Invalid task status"))

    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidRequestBody(): ResponseEntity<ApiError> {
        return ResponseEntity
            .badRequest()
            .body(ApiError("Invalid request body"))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleInvalidRequestParameter(): ResponseEntity<ApiError> {
        return ResponseEntity
            .badRequest()
            .body(ApiError("Invalid request parameter"))
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException::class)
    fun handleInvalidSort(): ResponseEntity<ApiError> {
        return ResponseEntity
            .badRequest()
            .body(ApiError("Invalid sort parameter. Use format: sort=createdAt,desc"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ApiError> {
        return ResponseEntity
            .badRequest()
            .body(ApiError(exception.message ?: "Invalid request parameter"))
    }
}