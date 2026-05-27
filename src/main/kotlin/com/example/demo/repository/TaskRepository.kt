package com.example.demo.repository

import com.example.demo.domain.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface TaskRepository: JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
}