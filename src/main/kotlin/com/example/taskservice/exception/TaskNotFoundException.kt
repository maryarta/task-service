package com.example.taskservice.exception

class TaskNotFoundException(id: Long) :
    RuntimeException("Task with id=$id not found")