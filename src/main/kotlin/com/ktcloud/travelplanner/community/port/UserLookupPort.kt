package com.ktcloud.travelplanner.community.port

import java.util.UUID

// community가 Identity(user) 서비스를 직접(Repository/Entity) 참조하지 않고 이 인터페이스로만
// 접근하게 끊어두는 경계. 구현체는 HttpUserLookupAdapter — GET /api/v1/users/{userId}/summary를
// 호출한다. Identity API는 아직 미구현이라(계약만 확정) 실제 운영에서는 503이 날 수 있다.
interface UserLookupPort {
	fun findAuthor(userId: UUID): AuthorSummary?
}

// User 엔티티 전체가 아니라 community가 실제로 필요한 필드만 담는다(email/gender/생년 등
// 민감 정보는 애초에 노출하지 않음 — user/dto/UserProfileResponse.kt는 전체 프로필이라
// 이 용도로 그대로 못 쓴다).
data class AuthorSummary(
	val id: UUID,
	val nickname: String?,
	val profileImageUrl: String?,
)
