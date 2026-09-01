package com.ktcloud.travelplanner.community.adapter

import com.ktcloud.travelplanner.global.external.externalHttpRequestFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration

// HttpUserLookupAdapter/HttpTravelAccessAdapter 전용 RestClient. 타임아웃 없는 클라이언트를 만들지
// 않기 위해 반드시 externalHttpRequestFactory(connectTimeout, readTimeout)로 request factory를 준다.
@Configuration
class CommunityExternalClientsConfig {
	@Bean
	fun identityRestClient(
		@Value("\${app.services.identity.base-url}") baseUrl: String,
	): RestClient = RestClient.builder()
		.baseUrl(baseUrl)
		.requestFactory(externalHttpRequestFactory(CONNECT_TIMEOUT, READ_TIMEOUT))
		.build()

	@Bean
	fun travelRestClient(
		@Value("\${app.services.travel.base-url}") baseUrl: String,
	): RestClient = RestClient.builder()
		.baseUrl(baseUrl)
		.requestFactory(externalHttpRequestFactory(CONNECT_TIMEOUT, READ_TIMEOUT))
		.build()

	companion object {
		private val CONNECT_TIMEOUT = Duration.ofSeconds(2)
		private val READ_TIMEOUT = Duration.ofSeconds(3)
	}
}
