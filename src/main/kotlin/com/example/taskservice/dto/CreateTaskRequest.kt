package com.example.taskservice.dto

import com.example.taskservice.domain.TaskType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateTaskRequest (
    @field:NotBlank
    val file: String,
    @field:NotNull
    val type: TaskType
)