package com.example.taskservice.repository

import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface TaskRepository: JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    @Query(
        value = """
            select id
            from tasks
            where status = 'QUEUED'
            order by created_at asc, id asc
            for update skip locked
            limit 1
        """,
        nativeQuery = true
    )
    fun findNextQueuedTaskIdForUpdate(): Long?


    @Modifying
    @Query(
        """
    update Task t
    set t.status = :queuedStatus,
        t.startedAt = null
    where t.status = :processingStatus
      and t.startedAt < :threshold
    """
    )
    fun resetStaleProcessingTasks(
        processingStatus: TaskStatus,
        queuedStatus: TaskStatus,
        threshold: Instant
    ): Int
}