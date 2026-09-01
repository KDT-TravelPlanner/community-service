package com.ktcloud.travelplanner.global.exception

// HttpUserLookupAdapter가 Identity 서비스 호출에 실패(타임아웃/5xx/연결 실패 등)했을 때 던진다.
// 404(작성자 없음)는 별개로 findAuthor가 null을 반환하는 정상 경로다 — 이 예외는 "확인 불가"만 의미한다.
class IdentityServiceUnavailableException(
	cause: Throwable? = null,
) : ExternalServiceException(ErrorCode.IDENTITY_SERVICE_UNAVAILABLE, cause = cause)
