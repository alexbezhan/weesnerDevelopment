package auth

import BaseRouter
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.request.receive
import io.ktor.response.header
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import logRequest
import loggedUserData
import respond
import respondErrorAuthorizing
import shared.auth.HashedUser
import shared.auth.InvalidUserReason
import shared.auth.TokenResponse
import shared.auth.User
import shared.base.Response.Companion.BadRequest
import shared.base.Response.Companion.Conflict
import shared.base.Response.Companion.Created
import shared.base.Response.Companion.Ok
import java.util.*
import kotlin.reflect.full.createType

class UserRouter(
    override val basePath: String,
    override val service: UsersService,
    private val jwtProvider: JwtProvider,
    private val accountUrl: String,
    private val loginUrl: String,
    private val signUpUrl: String
) : BaseRouter<User, UsersService>(
    UsersResponse(),
    service,
    User::class.createType()
) {
    override fun Route.setupRoutes() {
        route(basePath) {
            getRequest()
            updateRequest()
            login(loginUrl)
            signUp(signUpUrl)
        }
    }

    override fun Route.getRequest() {
        authenticate {
            get("/$accountUrl") {
                logRequest<User>()

                val auth = call.loggedUserData()?.getData()
                    ?: return@get respondErrorAuthorizing(InvalidUserReason.General)

                val user = when {
                    auth.username != null && auth.password != null ->
                        service.getUserFromHash(HashedUser(auth.username, auth.password))
                    auth.uuid != null ->
                        service.getUserByUuid(auth.uuid)
                    else -> null
                }

                user?.redacted()?.let {
                    respond(Ok(it))
                } ?: return@get respondErrorAuthorizing(InvalidUserReason.NoUserFound)
            }
        }
    }

    override fun Route.updateRequest() {
        authenticate {
            put("/") {
                val authToken = call.loggedUserData()?.getData()

                val item = call.receive<User>()
                logRequest(item)

                val tokenToHash = authToken?.let {
                    if (it.username == null)
                        return@put respondErrorAuthorizing(InvalidUserReason.InvalidUserInfo)

                    if (it.password == null)
                        return@put respondErrorAuthorizing(InvalidUserReason.InvalidUserInfo)

                    HashedUser(it.username, it.password)
                } ?: return@put respondErrorAuthorizing(InvalidUserReason.NoUserFound)

                val hashedUser = service.getUserFromHash(tokenToHash)

                if (hashedUser?.uuid != item.uuid)
                    return@put respondErrorAuthorizing(InvalidUserReason.WrongUser)

                val updated = service.update(item.copy(id = hashedUser?.id)) {
                    service.table.id eq hashedUser?.id!!
                }

                val response = when {
                    updated == null -> BadRequest("Error occurred updated user information.")
                    updated != item.id -> Created(it)
                    else -> Ok(it)
                }

                respond(response)
            }
        }
    }

    private fun Route.login(pathParam: String) {
        post(pathParam) {
            val credentials = call.receive<HashedUser>()
            logRequest<User>()

            hashedUser(credentials.username, credentials.password) {
                it?.run {
                    val generatedToken = asHashed()?.asToken(jwtProvider) ?: throw Exception("Generate token was null")

                    call.response.header("x-auth-token", generatedToken)
                    respond(Ok(TokenResponse(generatedToken)))
                } ?: respondErrorAuthorizing(InvalidUserReason.NoUserFound)
            }
        }
    }

    private fun Route.signUp(pathParam: String) {
        post(pathParam) {
            val credentials = call.receive<User>()
            logRequest(credentials)

            hashedUser(credentials.username, credentials.password) {
                if (it != null) return@hashedUser respond(Conflict("Invalid user credentials."))

                val newUser = User(
                    uuid = UUID.nameUUIDFromBytes("${credentials.username}${credentials.password}".toByteArray())
                        .toString(),
                    email = credentials.email,
                    name = credentials.name,
                    photoUrl = credentials.photoUrl,
                    username = credentials.username,
                    password = credentials.password
                )

                val response = when (val added = service.addUser(newUser)) {
                    null -> Conflict("Unable to save user credentials.")
                    else -> Created(TokenResponse(added.asHashed()?.asToken(jwtProvider)))
                }

                respond(response)
            }
        }
    }

    /**
     * Helper function for getting the hashed user information from the request.
     */
    private suspend fun PipelineContext<Unit, ApplicationCall>.hashedUser(
        username: String?,
        password: String?,
        tryBlock: suspend (hashedUser: User?) -> Unit
    ) {
        if (username == null || password == null) {
            respond(BadRequest("Username and password must be provided."))
            return
        }

        val hashedUser = HashedUser(username, password)

        if (hashedUser.checkValidity() != null) {
            respondErrorAuthorizing(InvalidUserReason.InvalidUserInfo)
            return
        }

        try {
            val response = service.getUserFromHash(hashedUser)

            tryBlock(response)
        } catch (e: Exception) {
            respond(BadRequest(e.message ?: "Unknown reason for request failure."))
        }
    }
}
