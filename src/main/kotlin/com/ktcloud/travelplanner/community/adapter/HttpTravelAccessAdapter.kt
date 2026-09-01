package com.ktcloud.travelplanner.community.adapter

import com.ktcloud.travelplanner.community.port.TravelAccessPort
import com.ktcloud.travelplanner.global.exception.TravelServiceUnavailableException
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

// TravelAccessPort의 HTTP 구현체 — GET /api/v1/travels/{travelId}/read-access 하나로 exists/
// hasReadAccess 둘 다 판정한다(Travel 쪽 계약: 200=존재+읽기권한 있음, 403=존재하지만 권한 없음,
// 404=존재하지 않음). requesterId는 URL에 싣지 않고, 들어온 요청의 Authorization 헤더를 그대로
// 전달해 Travel이 토큰에서 요청자를 직접 식별하게 한다 — 팀 확정 결정사항 4(구현 완료, 사용 가능).
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
		val authorization = currentAuthorizationHeader()
		return try {
			travelRestClient.get()
				.uri("/api/v1/travels/{travelId}/read-access", travelId)
				.headers { headers -> authorization?.let { headers.set(HttpHeaders.AUTHORIZATION, it) } }
				.retrieve()
				.toBodilessEntity()
			ReadAccessStatus.FOUND_WITH_ACCESS
		} catch (exception: HttpClientErrorException.NotFound) {
			ReadAccessStatus.NOT_FOUND
		} catch (exception: HttpClientErrorException.Forbidden) {
			ReadAccessStatus.FOUND_NO_ACCESS
		} catch (exception: RestClientException) {
			throw TravelServiceUnavailableException(cause = exception)
		}
	}

	private enum class ReadAccessStatus {
		FOUND_WITH_ACCESS,
		FOUND_NO_ACCESS,
		NOT_FOUND,
	}
}
