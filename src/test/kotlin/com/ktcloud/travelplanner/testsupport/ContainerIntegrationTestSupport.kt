package com.ktcloud.travelplanner.testsupport

import com.ktcloud.travelplanner.CommunityServiceApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(
	classes = [CommunityServiceApplication::class],
	webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@Import(TestcontainersConfiguration::class)
abstract class ContainerIntegrationTestSupport
