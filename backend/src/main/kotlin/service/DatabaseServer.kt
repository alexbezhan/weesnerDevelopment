package com.weesnerdevelopment.service

import auth.*
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.ryanharter.ktor.moshi.moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.weesnerdevelopment.AppConfig
import com.weesnerdevelopment.Path
import federalIncomeTax.FederalIncomeTaxRouter
import generics.route
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.websocket.WebSockets
import medicare.MedicareRouter
import respondAuthorizationIssue
import respondServerError
import socialSecurity.SocialSecurityRouter
import taxWithholding.TaxWithholdingRouter
import java.time.Duration

class DatabaseServer {
    fun Application.main() {
        val appConfig = AppConfig(environment.config)
        val jwtProvider =
            JwtProvider(appConfig.issuer, appConfig.audience, appConfig.expiresIn, Cipher(appConfig.secret))

        install(DefaultHeaders)
        install(CallLogging)
        install(WebSockets)
        install(CORS) {
            method(HttpMethod.Options)
            header(HttpHeaders.Authorization)
            host("${appConfig.baseUrl}:${appConfig.port}")
            host("localhost:3000")
            maxAge = Duration.ofDays(1)
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }

        install(ContentNegotiation) {
            moshi {
                add(KotlinJsonAdapterFactory())
            }
        }

        install(StatusPages) {
            exception<Throwable> { e ->
                when (e) {
                    is TokenExpiredException -> call.respondAuthorizationIssue(InvalidUserReason.Expired)
                    is JWTVerificationException -> call.respondAuthorizationIssue(InvalidUserReason.InvalidJwt)
                    else -> call.respondServerError(e)
                }
            }
            status(HttpStatusCode.Unauthorized) {
                try {
                    jwtProvider.decodeJWT((call.request.parseAuthorizationHeader() as HttpAuthHeader.Single).blob)
                } catch (e: Exception) {
                    return@status when (e) {
                        // usually happens when no token was passed...
                        is ClassCastException -> call.respondAuthorizationIssue(InvalidUserReason.InvalidJwt)
                        is TokenExpiredException -> call.respondAuthorizationIssue(InvalidUserReason.Expired)
                        is JWTVerificationException -> call.respondAuthorizationIssue(InvalidUserReason.InvalidJwt)
                        else -> call.respondServerError(Throwable(e))
                    }
                }

                call.respondAuthorizationIssue(InvalidUserReason.General)
            }
        }

        install(Authentication) {
            jwt {
                verifier(jwtProvider.verifier)
                this.realm = appConfig.realm
                validate { credential ->
                    if (credential.payload.audience.contains(appConfig.audience)) CustomPrincipal(credential.payload)
                    else null
                }
            }
        }

        DatabaseFactory.init()

        install(Routing) {
            route("/health") {
                get("/") {
                    call.respond(HttpStatusCode.OK, "Server is up and running")
                }
            }
            // user
            route(Path.User.base, UserRouter(jwtProvider, Path.User.account)) { router ->
                (router as UserRouter).apply {
                    login(Path.User.login)
                    signUp(Path.User.signUp)
                }
            }

            // tax fetcher
            authenticate {
                route(Path.TaxFetcher.socialSecurity, SocialSecurityRouter())
                route(Path.TaxFetcher.medicare, MedicareRouter())
                route(Path.TaxFetcher.taxWithholding, TaxWithholdingRouter())
                route(Path.TaxFetcher.federalIncomeTax, FederalIncomeTaxRouter())
            }
        }
    }
}
