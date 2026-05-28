package com.example.taskservice

import com.example.taskservice.domain.Task
import com.example.taskservice.domain.TaskStatus
import com.example.taskservice.domain.TaskType
import com.example.taskservice.dto.CreateTaskRequest
import com.example.taskservice.repository.TaskRepository
import com.example.taskservice.service.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant

@SpringBootTest
@Testcontainers
class DemoApplicationTests(
	@Autowired
	private val taskService: TaskService,
	@Autowired
	private val taskRepository: TaskRepository
) {

	companion object {
		@Container
		@JvmField
		val postgresContainer = PostgreSQLContainer("postgres:16").apply {
			withDatabaseName("demo")
			withUsername("postgres")
			withPassword("postgres")
		}

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
			registry.add("spring.datasource.username", postgresContainer::getUsername)
			registry.add("spring.datasource.password", postgresContainer::getPassword)
		}

	}

	@BeforeEach
	fun cleanDb() {
		taskRepository.deleteAll()
	}

	private fun saveTask(
		file: String,
		status: TaskStatus = TaskStatus.CREATED,
		type: TaskType = TaskType.VALIDATE,
		createdAt: Instant = Instant.now()
	): Task {
		return taskRepository.save(
			Task(
				file = file,
				status = status,
				type = type,
				createdAt = createdAt
			)
		)
	}

	@Test
	fun `should save tasks to database`() {
		val savedIds = mutableListOf<Long>()
		for (i in 1..5) {
			val savedTask = taskService.create(
				CreateTaskRequest(
					file = "test$i.txt",
					type = TaskType.CONVERT
				)
			)
			val id = savedTask.id
			assertNotNull(id)
			savedIds.add(id)
		}

		assertEquals(5, savedIds.size)

		savedIds.forEachIndexed { index, id ->
			val taskFromDb = taskRepository.findById(id).orElseThrow()
			assertEquals("test${index + 1}.txt", taskFromDb.file)
			assertEquals(TaskType.CONVERT, taskFromDb.type)
			assertEquals(TaskStatus.CREATED, taskFromDb.status)
			assertNotNull(taskFromDb.createdAt)
		}

	}

	@Test
	fun `findById should return null when task not found`() {
		val foundTask = taskService.findById(999L)
		assertNull(foundTask)
	}

	@Test
	fun `findAll should filter by status type and createdAt`() {
		val oldTime = Instant.parse("2026-05-27T10:00:00Z")
		val middleTime = Instant.parse("2026-05-28T10:00:00Z")
		val newTime = Instant.parse("2026-05-29T10:00:00Z")

		saveTask(
			file = "old.txt",
			status = TaskStatus.DONE,
			type = TaskType.CONVERT,
			createdAt = oldTime
		)

		val expectedTask = saveTask(
			file = "expected.txt",
			status = TaskStatus.DONE,
			type = TaskType.VALIDATE,
			createdAt = middleTime
		)

		saveTask(
			file = "new.txt",
			status = TaskStatus.FAILED,
			type = TaskType.VALIDATE,
			createdAt = newTime
		)

		val page = taskService.findAll(
			status = TaskStatus.DONE,
			type = TaskType.VALIDATE,
			createdFrom = Instant.parse("2026-05-27T00:00:00Z"),
			createdTo = Instant.parse("2026-05-28T23:59:59Z"),
			pageable = PageRequest.of(0, 10)
		)

		assertEquals(1, page.totalElements)
		assertEquals(expectedTask.id, page.content[0].id)
		assertEquals("expected.txt", page.content[0].file)
	}

	@Test
	fun `findAll should return paginated result`() {
		for (i in 1..5) {
			saveTask(
				file = "test$i.txt",
				status = TaskStatus.CREATED,
				type = TaskType.VALIDATE
			)
		}

		val firstPage = taskService.findAll(
			status = null,
			type = null,
			createdFrom = null,
			createdTo = null,
			pageable = PageRequest.of(0, 2)
		)

		val secondPage = taskService.findAll(
			status = null,
			type = null,
			createdFrom = null,
			createdTo = null,
			pageable = PageRequest.of(1, 2)
		)

		assertEquals(5, firstPage.totalElements)
		assertEquals(3, firstPage.totalPages)

		assertEquals(2, firstPage.content.size)
		assertEquals(2, secondPage.content.size)
	}

	@Test
	fun `addToQueue should change status to QUEUED`(){
		val task = saveTask(
			file = "test.txt",
			status = TaskStatus.CREATED,
			type = TaskType.VALIDATE
		)

		val queuedTask = taskService.addToQueue(task.id!!)

		assertEquals(TaskStatus.QUEUED, queuedTask.status)

		val taskFromDb = taskRepository.findById(task.id!!).orElseThrow()

		assertEquals(TaskStatus.QUEUED, taskFromDb.status)

	}

	@Test
	fun `cancelTask should change status to CANCELED`() {
		val task = saveTask(
			file = "test.txt",
			status = TaskStatus.CREATED,
			type = TaskType.VALIDATE
		)

		taskService.cancelTask(task.id!!)
		val taskFromDb = taskRepository.findById(task.id!!).orElseThrow()
		assertEquals(TaskStatus.CANCELED, taskFromDb.status)
	}

}
