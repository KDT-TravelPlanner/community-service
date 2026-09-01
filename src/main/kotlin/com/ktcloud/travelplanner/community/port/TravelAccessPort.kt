package com.ktcloud.travelplanner.community.port

import java.util.UUID

// community가 Travel 서비스를 직접(Repository/Entity) 참조하지 않고 이 인터페이스로만 접근하게
// 끊어두는 경계. 구현체는 HttpTravelAccessAdapter — GET /api/v1/travels/{travelId}/read-access를
// 호출한다(Travel 쪽 구현 완료, 팀 확정 결정사항 4). 실패·타임아웃 시 false가 아니라 예외를
// 던져 503으로 응답한다 — false를 반환하면 "권한 없음"과 "확인 불가"가 구분되지 않는다.
interface TravelAccessPort {
	fun exists(travelId: UUID): Boolean

	fun hasReadAccess(travelId: UUID, requesterId: UUID): Boolean
}
