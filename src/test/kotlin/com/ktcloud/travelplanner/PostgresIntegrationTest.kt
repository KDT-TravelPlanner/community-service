package com.ktcloud.travelplanner

import com.ktcloud.travelplanner.testsupport.ContainerIntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class PostgresIntegrationTest : ContainerIntegrationTestSupport() {

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Test
	fun `PostgreSQL accepts a real query`() {
		val result = jdbcTemplate.queryForObject("SELECT 1", Int::class.java)

		assertEquals(1, result)
	}
}
