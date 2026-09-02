package com.ktcloud.travelplanner.community.adapter

import com.ktcloud.travelplanner.global.logging.RequestIdGenerator
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

// 커뮤니티가 Identity/Travel을 호출할 때 들어온 요청의 Authorization과 X-Request-Id를 그대로
// 물려주는지 검증한다. X-Request-Id가 빠지면 호출받은 쪽이 새 ID를 발급해 버려서, 하나의 사용자
// 요청인데도 서비스별로 로그가 따로 놀게 된다.
class OutgoingRequestHeadersTest {
	@AfterEach
	fun clearRequestContext() {
		RequestContextHolder.resetRequestAttributes()
	}

	@Test
	fun `identity 호출에 들어온 Authorization과 X-Request-Id를 그대로 싣는다`() {
		givenIncomingRequest(authorization = BEARER_TOKEN, requestId = INCOMING_REQUEST_ID)

		val builder = RestClient.builder().baseUrl(IDENTITY_BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		val adapter = HttpUserLookupAdapter(builder.build())
		val userId = UUID.randomUUID()

		server.expect(requestTo("$IDENTITY_BASE_URL/api/v1/users/$userId/summary"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
			.andExpect(header(RequestIdGenerator.HEADER_NAME, INCOMING_REQUEST_ID))
			.andRespond(
				withSuccess(
					"""{"userId":"$userId","nickname":"작성자","profileImageUrl":null}""",
					MediaType.APPLICATION_JSON,
				),
			)

		val author = adapter.findAuthor(userId)

		server.verify()
		assertEquals("작성자", author?.nickname)
	}

	@Test
	fun `travel 호출에도 동일한 헤더가 실린다`() {
		givenIncomingRequest(authorization = BEARER_TOKEN, requestId = INCOMING_REQUEST_ID)

		val builder = RestClient.builder().baseUrl(TRAVEL_BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		val adapter = HttpTravelAccessAdapter(builder.build())
		val travelId = UUID.randomUUID()

		server.expect(requestTo("$TRAVEL_BASE_URL/api/v1/travels/$travelId/read-access"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
			.andExpect(header(RequestIdGenerator.HEADER_NAME, INCOMING_REQUEST_ID))
			.andRespond(withSuccess())

		adapter.exists(travelId)

		server.verify()
	}

	@Test
	fun `이어받을 값이 없으면 빈 헤더를 만들지 않는다`() {
		// 비로그인 요청(Authorization 없음) + 로깅 필터를 거치지 않은 호출(요청 ID 미설정) 상황.
		givenIncomingRequest(authorization = null, requestId = null)

		val builder = RestClient.builder().baseUrl(IDENTITY_BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		val adapter = HttpUserLookupAdapter(builder.build())
		val userId = UUID.randomUUID()

		server.expect(requestTo("$IDENTITY_BASE_URL/api/v1/users/$userId/summary"))
			.andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
			.andExpect(headerDoesNotExist(RequestIdGenerator.HEADER_NAME))
			.andRespond(
				withSuccess(
					"""{"userId":"$userId","nickname":null,"profileImageUrl":null}""",
					MediaType.APPLICATION_JSON,
				),
			)

		adapter.findAuthor(userId)

		server.verify()
	}

	private fun givenIncomingRequest(
		authorization: String?,
		requestId: String?,
	) {
		val request = MockHttpServletRequest()
		authorization?.let { request.addHeader(HttpHeaders.AUTHORIZATION, it) }
		// RequestLoggingFilter가 요청 시작 시 넣어두는 값과 같은 통로로 심는다.
		requestId?.let { RequestLoggingContext.setRequestId(request, it) }
		RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
	}

	private companion object {
		const val IDENTITY_BASE_URL = "http://identity:8080"
		const val TRAVEL_BASE_URL = "http://travel:8080"
		const val BEARER_TOKEN = "Bearer test-access-token"
		const val INCOMING_REQUEST_ID = "11111111-2222-3333-4444-555555555555"
	}
}
