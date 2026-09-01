package com.ktcloud.travelplanner.global.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ktcloud.travelplanner.CommunityServiceApplication
import com.ktcloud.travelplanner.testsupport.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest(
	classes = [CommunityServiceApplication::class],
	webEnvironment = SpringBootTest.WebEnvironment.MOCK,
)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class OpenApiDocumentationIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val objectMapper: ObjectMapper,
) {
	@Test
	fun `OpenAPI document exposes only versioned community API contracts with JWT security`() {
		val response = mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
			}
			.andReturn()
			.response

		val document = objectMapper.readTree(response.contentAsString)
		assertEquals(OpenApiConfiguration.API_TITLE, document.path("info").path("title").asText())
		assertEquals(OpenApiConfiguration.API_VERSION, document.path("info").path("version").asText())
		assertBearerSecurity(document)
		assertDocumentedPaths(document)
		assertRequestParameters(document)
	}

	@Test
	fun `OpenAPI YAML and Swagger UI are available without authentication`() {
		mockMvc.get("/v3/api-docs.yaml")
			.andExpect {
				status { isOk() }
			}

		mockMvc.get("/swagger-ui.html")
			.andExpect {
				status { is3xxRedirection() }
				redirectedUrl("/swagger-ui/index.html")
			}

		mockMvc.get("/swagger-ui/index.html")
			.andExpect {
				status { isOk() }
				content { string(org.hamcrest.Matchers.containsString("Swagger UI")) }
			}
	}

	@Test
	fun `business API remains protected when documentation paths are public`() {
		mockMvc.post("/api/v1/community/posts") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"categoryCode":"TRAVEL_REVIEW","title":"t","bodyJson":{"type":"doc"}}"""
		}
			.andExpect {
				status { isUnauthorized() }
			}
	}

	private fun assertBearerSecurity(document: JsonNode) {
		val bearerScheme = document.path("components").path("securitySchemes").path("bearerAuth")
		assertEquals("http", bearerScheme.path("type").asText())
		assertEquals("bearer", bearerScheme.path("scheme").asText())
		assertEquals("JWT", bearerScheme.path("bearerFormat").asText())
		assertTrue(document.path("security").single().has("bearerAuth"))
	}

	// community-api-contract.md 2절 — 커뮤니티 API 13개(카테고리/태그 목록 포함)가 전부 문서화됐는지.
	// SecurityConfig의 permitAll 판정(GET 목록/상세/댓글목록, 카테고리, 태그)은 springdoc이 정적으로
	// 반영하지 않는다 — 이 프로젝트의 어떤 컨트롤러도 @SecurityRequirements 같은 per-operation
	// 어노테이션을 쓰지 않아서, OpenAPI 문서상 security는 전역 bearerAuth 하나로 고정된다(실제
	// 인증 요구 여부는 SecurityConfig가 런타임에 판정 — 세 번째 테스트가 그 경로를 검증한다).
	private fun assertDocumentedPaths(document: JsonNode) {
		val paths = document.path("paths")
		val documentedPaths = paths.fieldNames().asSequence().toSet()
		assertTrue(documentedPaths.isNotEmpty())
		assertTrue(documentedPaths.all { it.startsWith("/api/v1/") })
		assertFalse("/api/ping" in documentedPaths)

		listOf(
			"/api/v1/community/categories",
			"/api/v1/community/tags",
			"/api/v1/community/posts",
			"/api/v1/community/posts/{postId}",
			"/api/v1/community/posts/{postId}/reactions/{type}",
			"/api/v1/community/posts/{postId}/comments",
			"/api/v1/community/comments/{commentId}",
			"/api/v1/community/comments/{commentId}/reactions/{type}",
			"/api/v1/community/me/posts",
			"/api/v1/community/me/comments",
		).forEach { path -> assertTrue(path in documentedPaths, "Missing documented path: $path") }

		assertTrue(document.path("components").path("schemas").has("CommunityPostCreateRequest"))
		assertTrue(document.path("components").path("schemas").has("CommentCreateRequest"))
	}

	private fun assertRequestParameters(document: JsonNode) {
		// AuthenticationPrincipalOperationCustomizer가 @AuthenticationPrincipal 파라미터를
		// 문서에서 걸러내는지(principal 노출 금지) 확인한다.
		val listParameters = document.path("paths")
			.path("/api/v1/community/posts")
			.path("get")
			.path("parameters")
			.mapNotNull { it.path("name").textValue() }
		assertFalse("principal" in listParameters)
		assertTrue("keyword" in listParameters)

		val detailParameters = document.path("paths")
			.path("/api/v1/community/posts/{postId}")
			.path("get")
			.path("parameters")
			.mapNotNull { it.path("name").textValue() }
		assertFalse("principal" in detailParameters)
		assertTrue("postId" in detailParameters)
	}
}
