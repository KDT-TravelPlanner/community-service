package com.ktcloud.travelplanner.community.adapter

import com.ktcloud.travelplanner.global.exception.TravelServiceUnavailableException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

// Travel read-access 응답 해석. travel-service는 존재/권한 여부를 HTTP 상태코드가 아니라
// 200 본문의 {exists, hasReadAccess}로 알려준다 — 그걸 제대로 읽는지 고정한다.
class HttpTravelAccessAdapterTest {
	private val travelId: UUID = UUID.randomUUID()
	private val requesterId: UUID = UUID.randomUUID()

	@AfterEach
	fun clearRequestContext() {
		RequestContextHolder.resetRequestAttributes()
	}

	private fun adapterFor(
		count: ExpectedCount,
		respond: (org.springframework.test.web.client.ResponseActions) -> Unit,
	): HttpTravelAccessAdapter {
		val builder = RestClient.builder().baseUrl(BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		respond(server.expect(count, requestTo("$BASE_URL/api/v1/travels/$travelId/read-access")))
		return HttpTravelAccessAdapter(builder.build())
	}

	private fun jsonBody(exists: Boolean, hasReadAccess: Boolean) =
		withSuccess(
			"""{"data":{"travelId":"$travelId","exists":$exists,"hasReadAccess":$hasReadAccess}}""",
			MediaType.APPLICATION_JSON,
		)

	@Test
	fun `200 exists=true hasReadAccess=true 이면 존재하고 읽기 가능`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(jsonBody(exists = true, hasReadAccess = true)) }
		assertTrue(adapter.hasReadAccess(travelId, requesterId))
	}

	@Test
	fun `200 exists=true hasReadAccess=false 이면 존재하지만 읽기 불가`() {
		val existsAdapter = adapterFor(ExpectedCount.once()) { it.andRespond(jsonBody(exists = true, hasReadAccess = false)) }
		assertTrue(existsAdapter.exists(travelId))

		val accessAdapter = adapterFor(ExpectedCount.once()) { it.andRespond(jsonBody(exists = true, hasReadAccess = false)) }
		assertFalse(accessAdapter.hasReadAccess(travelId, requesterId))
	}

	@Test
	fun `200 exists=false 이면 존재하지 않음 (상태코드가 200이어도)`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(jsonBody(exists = false, hasReadAccess = false)) }
		assertFalse(adapter.exists(travelId))
	}

	@Test
	fun `404 상태코드도 존재하지 않음으로 받는다`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(withStatus(HttpStatus.NOT_FOUND)) }
		assertFalse(adapter.exists(travelId))
	}

	@Test
	fun `403 상태코드는 존재하지만 읽기 불가로 받는다`() {
		val existsAdapter = adapterFor(ExpectedCount.once()) { it.andRespond(withStatus(HttpStatus.FORBIDDEN)) }
		assertTrue(existsAdapter.exists(travelId))

		val accessAdapter = adapterFor(ExpectedCount.once()) { it.andRespond(withStatus(HttpStatus.FORBIDDEN)) }
		assertFalse(accessAdapter.hasReadAccess(travelId, requesterId))
	}

	@Test
	fun `5xx는 장애로 간주해 예외를 던진다`() {
		val adapter = adapterFor(ExpectedCount.once()) { it.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)) }
		assertThrows<TravelServiceUnavailableException> { adapter.exists(travelId) }
	}

	@Test
	fun `200인데 본문이 계약과 다르면 권한 있음으로 넘기지 않고 예외를 던진다`() {
		val adapter = adapterFor(ExpectedCount.once()) {
			it.andRespond(withSuccess("""{"data":null}""", MediaType.APPLICATION_JSON))
		}
		assertThrows<TravelServiceUnavailableException> { adapter.hasReadAccess(travelId, requesterId) }
	}

	private companion object {
		const val BASE_URL = "http://travel:8080"
	}
}
