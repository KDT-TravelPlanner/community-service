package com.ktcloud.travelplanner.community.adapter

import com.ktcloud.travelplanner.global.exception.IdentityServiceUnavailableException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import java.util.UUID

// Identity user summary 응답 해석. identity-service는 요약 정보를 {"data":{...}} 한 겹으로
// 감싸서 주기 때문에, 봉투를 벗기지 않으면 필드가 통째로 null이 된다 — 실제로 그 버그가 한 번
// 났고(c6067c1 unwrap Identity user summary response) 통합 테스트는 Fake 대역을 쓰느라 잡지
// 못했다. 여기서 봉투 해석과 장애 승격 규칙을 고정한다.
class HttpUserLookupAdapterTest {
	private val userId: UUID = UUID.randomUUID()

	@AfterEach
	fun clearRequestContext() {
		RequestContextHolder.resetRequestAttributes()
	}

	private fun adapterFor(
		count: ExpectedCount,
		respond: (org.springframework.test.web.client.ResponseActions) -> Unit,
	): HttpUserLookupAdapter {
		val builder = RestClient.builder().baseUrl(BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		respond(server.expect(count, requestTo("$BASE_URL/api/v1/users/$userId/summary")))
		return HttpUserLookupAdapter(builder.build())
	}

	private fun jsonBody(body: String) = withSuccess(body, MediaType.APPLICATION_JSON)

	@Test
	fun `200 응답의 data 봉투를 벗겨 AuthorSummary로 만든다`() {
		val adapter = adapterFor(ExpectedCount.once()) {
			it.andRespond(
				jsonBody(
					"""{"data":{"userId":"$userId","nickname":"규","profileImageUrl":"https://cdn.example/1.png"}}""",
				),
			)
		}

		val author = adapter.findAuthor(userId)

		assertEquals(userId, author?.id)
		assertEquals("규", author?.nickname)
		assertEquals("https://cdn.example/1.png", author?.profileImageUrl)
	}

	@Test
	fun `nickname과 profileImageUrl이 null이어도 조회에 성공한다`() {
		val adapter = adapterFor(ExpectedCount.once()) {
			it.andRespond(jsonBody("""{"data":{"userId":"$userId","nickname":null,"profileImageUrl":null}}"""))
		}

		val author = adapter.findAuthor(userId)

		assertEquals(userId, author?.id)
		assertNull(author?.nickname)
		assertNull(author?.profileImageUrl)
	}

	@Test
	fun `계약에 없는 필드가 늘어나도 무시하고 읽는다`() {
		val adapter = adapterFor(ExpectedCount.once()) {
			it.andRespond(
				jsonBody(
					"""{"message":"ok","data":{"userId":"$userId","nickname":"규","profileImageUrl":null,"email":"x@y.z"}}""",
				),
			)
		}

		assertEquals("규", adapter.findAuthor(userId)?.nickname)
	}

	@Test
	fun `200인데 data가 비어 있으면 작성자 없음으로 본다`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(jsonBody("""{"data":null}""")) }

		assertNull(adapter.findAuthor(userId))
	}

	@Test
	fun `404는 작성자 없음이라는 정상 결과다`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(withStatus(HttpStatus.NOT_FOUND)) }

		assertNull(adapter.findAuthor(userId))
	}

	@Test
	fun `5xx는 장애로 간주해 예외를 던진다`() {
		val adapter = adapterFor(ExpectedCount.once()) {
			it.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
		}

		assertThrows<IdentityServiceUnavailableException> { adapter.findAuthor(userId) }
	}

	@Test
	fun `401은 작성자 없음으로 뭉개지 않고 장애로 올린다`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(withStatus(HttpStatus.UNAUTHORIZED)) }

		assertThrows<IdentityServiceUnavailableException> { adapter.findAuthor(userId) }
	}

	private companion object {
		const val BASE_URL = "http://identity:8080"
	}
}
