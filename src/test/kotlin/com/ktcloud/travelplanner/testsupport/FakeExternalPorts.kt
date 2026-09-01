package com.ktcloud.travelplanner.testsupport

import com.ktcloud.travelplanner.community.port.AuthorSummary
import com.ktcloud.travelplanner.community.port.TravelAccessPort
import com.ktcloud.travelplanner.community.port.UserLookupPort
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// HttpUserLookupAdapter/HttpTravelAccessAdapter는 실제로 Identity/Travel 서비스에 HTTP 호출을
// 하므로 통합 테스트에서 그대로 쓸 수 없다. @Primary로 실제 어댑터 빈을 덮어써서, 테스트가
// register*()로 미리 등록해둔 값만 돌려주는 인메모리 대역으로 대체한다.
class FakeUserLookupPort : UserLookupPort {
	private val authors = ConcurrentHashMap<UUID, AuthorSummary>()

	fun register(
		id: UUID,
		nickname: String?,
		profileImageUrl: String? = null,
	): AuthorSummary = AuthorSummary(id, nickname, profileImageUrl).also { authors[id] = it }

	override fun findAuthor(userId: UUID): AuthorSummary? = authors[userId]
}

class FakeTravelAccessPort : TravelAccessPort {
	private val existingTravelIds = ConcurrentHashMap.newKeySet<UUID>()
	private val readAccessByTravelId = ConcurrentHashMap<UUID, MutableSet<UUID>>()

	fun registerTravel(
		travelId: UUID,
		vararg readerIds: UUID,
	) {
		existingTravelIds.add(travelId)
		readAccessByTravelId.getOrPut(travelId) { mutableSetOf() }.addAll(readerIds.toList())
	}

	override fun exists(travelId: UUID): Boolean = existingTravelIds.contains(travelId)

	override fun hasReadAccess(
		travelId: UUID,
		requesterId: UUID,
	): Boolean = readAccessByTravelId[travelId]?.contains(requesterId) ?: false
}

@TestConfiguration(proxyBeanMethods = false)
class FakeExternalPortsConfiguration {
	@Bean
	@Primary
	fun fakeUserLookupPort(): FakeUserLookupPort = FakeUserLookupPort()

	@Bean
	@Primary
	fun fakeTravelAccessPort(): FakeTravelAccessPort = FakeTravelAccessPort()
}
