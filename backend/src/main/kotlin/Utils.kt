package com.weesnerdevelopment

import auth.InvalidUserException
import auth.InvalidUserReason
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.request.uri
import io.ktor.response.respond
import java.lang.reflect.ParameterizedType

enum class Paths {
    user,
    socialSecurity, medicare, federalIncomeTax, taxWithholding
}

/**
 * A moshi instance that can take a [ParameterizedType] if needed.
 */
inline fun <reified T> moshi(type: ParameterizedType? = null) =
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<T>(type ?: T::class.java)

/**
 * Helper function that creates an object of type [T] from a json string.
 */
inline fun <reified T> String.fromJson(type: ParameterizedType? = null) = moshi<T>(type).fromJson(this)

/**
 * Helper function to convert an object to a json string.
 */
inline fun <reified T> T?.toJson() = moshi<T>().toJson(this)

data class ServerError(
    val status: String,
    val statusCode: Int,
    val message: String
)

/**
 * Helper function to [respond] with a [ServerError].
 */
internal suspend fun ApplicationCall.respondServerError(error: Throwable) {
    val httpStatus = HttpStatusCode.InternalServerError
    respond(httpStatus, ServerError(httpStatus.description, httpStatus.value, error.localizedMessage))
}

/**
 * Helper function to [respond] with an [InvalidUserException] with the given [reason].
 */
internal suspend fun ApplicationCall.respondAuthorizationIssue(reason: InvalidUserReason) {
    val httpStatus = HttpStatusCode.Unauthorized
    respond(httpStatus, InvalidUserException(request.uri, httpStatus.value, reason.code).toJson())
}
