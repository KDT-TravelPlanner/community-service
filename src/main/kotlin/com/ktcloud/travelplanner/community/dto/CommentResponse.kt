package com.ktcloud.travelplanner.community.dto

import com.ktcloud.travelplanner.community.model.CommunityComment
import com.ktcloud.travelplanner.community.port.AuthorSummary
import java.time.Instant
import java.util.UUID

// community-api-contract.md 1절 CommentResponse를 기반으로, 댓글 좋아요/수정 기능 추가에 맞춰
// updatedAt(수정 여부 표시용)·reactionCount·isReacted를 확장했다(계약 문서에는 아직 반영 안 됨).
data class CommentResponse(
	val commentId: UUID,
	val authorNickname: String?,
	val authorProfileImageUrl: String?,
	val content: String,
	val createdAt: Instant,
	val updatedAt: Instant?,
	val reactionCount: Long,
	val isReacted: Boolean,
	val isMine: Boolean,
) {
	companion object {
		// 목록 조회(getComments) 전용 — authorNickname/authorProfileImageUrl은 작성 시점에 저장된
		// 스냅샷 컬럼이라 Port를 거치지 않고 엔티티에서 바로 읽는다(Identity 호출 없음).
		fun from(
			comment: CommunityComment,
			isMine: Boolean,
			reactionCount: Long,
			isReacted: Boolean,
		): CommentResponse = CommentResponse(
			commentId = comment.id,
			authorNickname = comment.authorNickname,
			authorProfileImageUrl = comment.authorProfileImageUrl,
			content = comment.content,
			createdAt = comment.createdAt,
			updatedAt = comment.updatedAt,
			reactionCount = reactionCount,
			isReacted = isReacted,
			isMine = isMine,
		)

		// 단건 조회/변경(createComment/updateComment/toggleReaction) 전용 — UserLookupPort로 가져온
		// AuthorSummary를 쓴다. 단건이라 배치 문제가 없고, 이 경로가 실제 서비스 분리 시 HTTP
		// Adapter로 그대로 교체된다.
		fun from(
			comment: CommunityComment,
			author: AuthorSummary?,
			isMine: Boolean,
			reactionCount: Long,
			isReacted: Boolean,
		): CommentResponse = CommentResponse(
			commentId = comment.id,
			authorNickname = author?.nickname,
			authorProfileImageUrl = author?.profileImageUrl,
			content = comment.content,
			createdAt = comment.createdAt,
			updatedAt = comment.updatedAt,
			reactionCount = reactionCount,
			isReacted = isReacted,
			isMine = isMine,
		)
	}
}
