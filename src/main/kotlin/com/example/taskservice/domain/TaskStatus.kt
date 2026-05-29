package com.example.taskservice.domain

enum class TaskStatus {
    CREATED,
    QUEUED,
    PROCESSING,
    DONE,
    FAILED,
    CANCELED,
}