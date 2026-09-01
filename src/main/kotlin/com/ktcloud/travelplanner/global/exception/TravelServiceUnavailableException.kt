package com.ktcloud.travelplanner.global.exception

// HttpTravelAccessAdapter가 Travel 서비스 호출에 실패(타임아웃/5xx/연결 실패 등)했을 때 던진다.
// 권한 확인 자체가 불가능한 상태를 "권한 없음"(false)으로 뭉개면 안 되므로 항상 이 예외를 던져
// 503으로 응답한다 — 404/403 같은 정상적인 "없음/권한없음" 응답과는 구분된다.
class TravelServiceUnavailableException(
	cause: Throwable? = null,
) : ExternalServiceException(ErrorCode.TRAVEL_SERVICE_UNAVAILABLE, cause = cause)
