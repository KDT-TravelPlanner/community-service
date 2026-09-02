package com.ktcloud.travelplanner.community.adapter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ktcloud.travelplanner.community.port.TravelAccessPort
import com.ktcloud.travelplanner.global.exception.TravelServiceUnavailableException
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

// TravelAccessPort의 HTTP 구현체 — GET /api/v1/travels/{travelId}/read-access 하나로 exists/
// hasReadAccess 둘 다 판정한다.
//
// Travel 쪽 계약(SERVICE_COMMUNICATION_BOUNDARIES.md 2절)은 두 표현을 모두 허용한다:
//   - 본문: 200 + {"data":{exists, hasReadAccess}}  ← travel-service 현재 구현은 항상 이 형태
//     (존재하지 않는 travel도 404가 아니라 200 + exists=false 로 응답한다)
//   - 상태코드: 404(없음) / 403(권한 없음)
// 따라서 200 본문의 exists/hasReadAccess를 먼저 읽고, 404/403 상태코드도 같은 의미로 받아준다.
// (예전 구현은 toBodilessEntity()로 본문을 버리고 상태코드만 봐서, travel이 200 + exists=false로
//  응답하면 "존재+읽기가능"으로 오판했다 — 없는 여행/권한 없는 여행으로 글쓰기가 뚫렸다.)
//
// requesterId는 URL에 싣지 않고, 들어온 요청의 Authorization 헤더를 그대로 전달해 Travel이
// 토큰에서 요청자를 직접 식별하게 한다. X-Request-Id도 함께 넘겨 두 서비스의 로그가 하나의
// 사용자 요청으로 묶이게 한다(applyIncomingRequestContext).
//
// 실패·타임아웃 시 절대 false를 반환하지 않는다. 권한 확인 자체가 불가능한 상태를 "권한 없음"으로
// 뭉개면, 실제로는 접근 가능한 사용자가 부당하게 403을 받게 된다 — 그 대신 예외를 던져 503으로
// 응답해서 "다시 시도하면 될 수도 있는 문제"라는 걸 클라이언트가 구분할 수 있게 한다.
@Component
class HttpTravelAccessAdapter(
	private val travelRestClient: RestClient,
) : TravelAccessPort {
	override fun exists(travelId: UUID): Boolean = fetchReadAccessStatus(travelId) != ReadAccessStatus.NOT_FOUND

	override fun hasReadAccess(
		travelId: UUID,
		requesterId: UUID,
	): Boolean = fetchReadAccessStatus(travelId) == ReadAccessStatus.FOUND_WITH_ACCESS

	private fun fetchReadAccessStatus(travelId: UUID): ReadAccessStatus {
		val body = try {
			travelRestClient.get()
				.uri("/api/v1/travels/{travelId}/read-access", travelId)
				.headers { headers -> headers.applyIncomingRequestContext() }
				.retrieve()
				.body(ReadAccessEnvelope::class.java)
		} catch (exception: HttpClientErrorException.NotFound) {
			return ReadAccessStatus.NOT_FOUND
		} catch (exception: HttpClientErrorException.Forbidden) {
			return ReadAccessStatus.FOUND_NO_ACCESS
		} catch (exception: RestClientException) {
			throw TravelServiceUnavailableException(cause = exception)
		}

		// 200인데 본문이 계약과 다르면(누락/파싱 실패) "권한 있음"으로 넘기지 않고 장애로 처리한다.
		val result = body?.data ?: throw TravelServiceUnavailableException()
		return when {
			!result.exists -> ReadAccessStatus.NOT_FOUND
			result.hasReadAccess -> ReadAccessStatus.FOUND_WITH_ACCESS
			else -> ReadAccessStatus.FOUND_NO_ACCESS
		}
	}

	private enum class ReadAccessStatus {
		FOUND_WITH_ACCESS,
		FOUND_NO_ACCESS,
		NOT_FOUND,
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ReadAccessEnvelope(
	val data: TravelReadAccess?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TravelReadAccess(
	val exists: Boolean = false,
	val hasReadAccess: Boolean = false,
)
