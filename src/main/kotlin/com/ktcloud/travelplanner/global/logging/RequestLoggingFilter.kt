package com.ktcloud.travelplanner.global.logging

import com.ktcloud.travelplanner.global.exception.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.math.max

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter(
	@Value("\${app.logging.error-frame-limit:5}") displayedFrameLimit: Int,
) : OncePerRequestFilter() {
	private val requestIdGenerator = RequestIdGenerator()
	private val safeExceptionSummaryFactory = SafeExceptionSummaryFactory(displayedFrameLimit)

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		// 요청 ID는 언제나 이 서비스가 발급한다 — 들어온 X-Request-Id는 신뢰하지 않는다. 클라이언트가
		// 보낸 값을 그대로 쓰면 로그를 위조할 수 있어서다(RequestLoggingIntegrationTest가 이 규칙을
		// 고정한다). 발급한 값은 Identity/Travel 호출에 그대로 실어 보내 서비스 간 로그를 묶는다
		// (IncomingRequestHeaders.applyIncomingRequestContext).
		val requestId = requestIdGenerator.generate()
		val startedAt = System.nanoTime()
		var hasUnexpectedFailure = false

		RequestLoggingContext.setRequestId(request, requestId)
		MDC.put(RequestIdGenerator.MDC_KEY, requestId)
		response.setHeader(RequestIdGenerator.HEADER_NAME, requestId)

		try {
			filterChain.doFilter(request, response)
		} catch (throwable: Throwable) {
			hasUnexpectedFailure = true
			RequestLoggingContext.setErrorCode(request, ErrorCode.INTERNAL_SERVER_ERROR)
			ApplicationLogger.logUnexpectedFailure(
				UnexpectedFailureLog(
					requestId = requestId,
					summary = safeExceptionSummaryFactory.create(throwable),
				),
			)
			throw throwable
		} finally {
			val httpStatus = resolveHttpStatus(response.status, hasUnexpectedFailure)
			val durationMilliseconds = max(
				0,
				(System.nanoTime() - startedAt) / NANOSECONDS_PER_MILLISECOND,
			)

			try {
				if (shouldLogCompletedRequest(request, httpStatus)) {
					logCompletedRequest(request, requestId, httpStatus, durationMilliseconds)
				}
			} finally {
				MDC.remove(RequestIdGenerator.MDC_KEY)
			}
		}
	}

	private fun logCompletedRequest(
		request: HttpServletRequest,
		requestId: String,
		httpStatus: Int,
		durationMilliseconds: Long,
	) {
		val errorCode = RequestLoggingContext.getErrorCode(request)
		val decision = RequestLogLevelPolicy.resolve(httpStatus, errorCode)

		ApplicationLogger.logRequestCompleted(
			requestLog = RequestCompletedLog(
				requestId = requestId,
				method = RequestRouteResolver.resolveMethod(request.method),
				route = RequestRouteResolver.resolveRoute(request),
				status = httpStatus,
				durationMs = durationMilliseconds,
				outcome = decision.outcome,
				errorCode = errorCode,
			),
			level = decision.level,
		)
	}

	private fun shouldLogCompletedRequest(request: HttpServletRequest, httpStatus: Int): Boolean =
		httpStatus !in 200..399 || request.requestURI !in successfulHealthRoutes

	private fun resolveHttpStatus(responseStatus: Int, hasUnexpectedFailure: Boolean): Int =
		if (hasUnexpectedFailure && responseStatus < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
			HttpServletResponse.SC_INTERNAL_SERVER_ERROR
		} else {
			responseStatus
		}

	companion object {
		private const val NANOSECONDS_PER_MILLISECOND = 1_000_000
		private val successfulHealthRoutes = setOf(
			"/actuator/health",
			"/actuator/health/liveness",
			"/actuator/health/readiness",
		)
	}
}
