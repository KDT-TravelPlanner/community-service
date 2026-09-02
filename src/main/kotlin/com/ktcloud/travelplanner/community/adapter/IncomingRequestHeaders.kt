package com.ktcloud.travelplanner.community.adapter

import com.ktcloud.travelplanner.global.logging.RequestIdGenerator
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

// HttpUserLookupAdapter/HttpTravelAccessAdapter가 Identity/Travel을 호출할 때, 들어온 요청에서
// 그대로 이어 넘겨야 하는 값들을 모아둔다. 두 어댑터가 같은 헤더 집합을 붙이도록 한곳에서 관리한다.

private fun currentRequest(): HttpServletRequest? =
	(RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

// 두 서비스 모두 이 값으로 JWT를 직접 검증하거나(Identity) 요청자를 식별한다(Travel의 read-access
// 판정). 헤더가 없으면(비로그인 조회 등) null — 그 경우 아무것도 붙이지 않는다.
internal fun currentAuthorizationHeader(): String? =
	currentRequest()?.getHeader(HttpHeaders.AUTHORIZATION)

// RequestLoggingFilter가 요청 시작 시 넣어둔 값. 하나의 사용자 요청이 커뮤니티 → Identity/Travel로
// 이어질 때 같은 ID를 물려줘야 세 서비스의 로그를 한 요청으로 묶어 볼 수 있다. 이 값이 없으면
// 호출받은 쪽이 새 ID를 발급해 버려서 로그가 끊긴다.
//
// MDC가 아니라 요청 속성에서 읽는다 — MDC 접근은 RequestLoggingFilter로 한정하는 것이 이 레포의
// 로깅 경계 규칙이고(LoggingPolicyTest가 고정한다), RequestLoggingContext가 바로 그 값을 요청
// 스코프로 넘기기 위해 있는 통로다.
internal fun currentRequestId(): String? =
	currentRequest()?.let { RequestLoggingContext.getRequestId(it) }

// 외부 호출에 실을 헤더를 한 번에 적용한다. 값이 없는 헤더는 아예 붙이지 않는다(빈 문자열을 보내
// 받는 쪽이 그걸 유효한 값으로 오해하지 않도록).
internal fun HttpHeaders.applyIncomingRequestContext() {
	currentAuthorizationHeader()?.let { set(HttpHeaders.AUTHORIZATION, it) }
	currentRequestId()?.let { set(RequestIdGenerator.HEADER_NAME, it) }
}
