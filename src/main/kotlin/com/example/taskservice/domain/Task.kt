package com.example.demo.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tasks")
data class Task (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val file: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TaskType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TaskStatus,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "result")
    var result: String? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

)