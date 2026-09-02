package com.ktcloud.travelplanner.community.adapter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ktcloud.travelplanner.community.port.AuthorSummary
import com.ktcloud.travelplanner.community.port.UserLookupPort
import com.ktcloud.travelplanner.global.exception.IdentityServiceUnavailableException
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

// UserLookupPort의 HTTP 구현체 — Identity 서비스가 실제 프로세스로 분리된 뒤를 대비해 계약만
// 확정된 GET /api/v1/users/{userId}/summary 를 호출한다(Identity 쪽은 아직 미구현, 계약만 확정
// — GOAL_PROMPT_community_split.md 팀 확정 결정사항 3). 들어온 요청의 Authorization 헤더를
// 그대로 전달해 Identity가 자체적으로 검증하게 하고, X-Request-Id도 함께 넘겨 두 서비스의 로그가
// 하나의 사용자 요청으로 묶이게 한다(applyIncomingRequestContext).
@Component
class HttpUserLookupAdapter(
	private val identityRestClient: RestClient,
) : UserLookupPort {
	override fun findAuthor(userId: UUID): AuthorSummary? {
		return try {
			val response = identityRestClient.get()
				.uri("/api/v1/users/{userId}/summary", userId)
				.headers { headers -> headers.applyIncomingRequestContext() }
				.retrieve()
				.body(UserSummaryEnvelope::class.java)
			response?.data?.let {
				AuthorSummary(id = it.userId, nickname = it.nickname, profileImageUrl = it.profileImageUrl)
			}
		} catch (exception: HttpClientErrorException.NotFound) {
			// 작성자가 없는 것은 정상적인 조회 결과다(호출부가 CommunityPostAuthorNotFoundException 등으로 처리).
			null
		} catch (exception: RestClientException) {
			// 타임아웃/연결 실패/5xx 등 "확인 자체가 불가능한" 상태는 false/null로 뭉개지 않고 503으로 올린다.
			throw IdentityServiceUnavailableException(cause = exception)
		}
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UserSummaryEnvelope(
	val data: UserSummaryResponse?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class UserSummaryResponse(
	val userId: UUID,
	val nickname: String?,
	val profileImageUrl: String?,
)
