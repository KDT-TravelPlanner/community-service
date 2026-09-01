package com.ktcloud.travelplanner.global.exception

import com.ktcloud.travelplanner.global.logging.RequestIdGenerator
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

typealias FieldErrorResponse = com.ktcloud.travelplanner.common.response.FieldErrorResponse
typealias ApiErrorResponse = com.ktcloud.travelplanner.common.response.ApiErrorResponse

internal object ApiErrorResponseFactory {
	private val requestIdGenerator = RequestIdGenerator()

	fun create(
		errorCode: ErrorCode,
		requestId: String,
		message: String = errorCode.defaultMessage,
		fieldErrors: List<FieldErrorResponse>? = null,
	): ApiErrorResponse = ApiErrorResponse(
		code = errorCode.name,
		message = message,
		requestId = requestId,
		fieldErrors = fieldErrors?.sortedBy(FieldErrorResponse::field),
	)

	fun resolveRequestId(request: HttpServletRequest, response: HttpServletResponse): String {
		val requestId = RequestLoggingContext.getRequestId(request)
			?: requestIdGenerator.generate().also { RequestLoggingContext.setRequestId(request, it) }

		response.setHeader(RequestIdGenerator.HEADER_NAME, requestId)
		return requestId
	}
}
