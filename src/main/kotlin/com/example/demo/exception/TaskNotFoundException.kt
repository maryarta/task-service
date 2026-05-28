package com.example.demo.exception

class TaskNotFoundException(id: Long) :
    RuntimeException("Task with id=$id not found")