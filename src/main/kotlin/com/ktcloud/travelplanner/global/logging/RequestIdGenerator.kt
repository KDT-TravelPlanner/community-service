package com.ktcloud.travelplanner.global.logging

import java.util.UUID

class RequestIdGenerator {
	fun generate(): String = UUID.randomUUID().toString()

	companion object {
		// 응답 헤더로 내보내고, 커뮤니티가 Identity/Travel을 호출할 때 그대로 실어 보내는 이름
		// (IncomingRequestHeaders.applyIncomingRequestContext). 반대로 들어오는 요청의 이 헤더는
		// 신뢰하지 않는다 — 클라이언트가 보낸 값을 그대로 쓰면 로그를 위조할 수 있어서, 요청 ID는
		// 언제나 이 서비스가 발급한다(RequestLoggingIntegrationTest가 이 규칙을 고정한다).
		const val HEADER_NAME = "X-Request-Id"
		const val MDC_KEY = "requestId"
	}
}
