package com.ktcloud.travelplanner.community.adapter

import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

// HttpUserLookupAdapter/HttpTravelAccessAdapter가 Identity/Travel에 그대로 넘길 Authorization
// 헤더 — 두 서비스 모두 이 값으로 JWT를 직접 검증하거나(Identity) 요청자를 식별한다(Travel의
// read-access 판정). 헤더가 없으면(비로그인 조회 등) null — 호출부가 그대로 전달하지 않으면 된다.
internal fun currentAuthorizationHeader(): String? =
	(RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
		?.request
		?.getHeader(HttpHeaders.AUTHORIZATION)
