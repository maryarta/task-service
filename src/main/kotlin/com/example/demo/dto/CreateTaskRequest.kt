package com.example.demo.dto

import com.example.demo.domain.TaskType

data class CreateTaskRequest (
    val file: String,
    val type: TaskType
)