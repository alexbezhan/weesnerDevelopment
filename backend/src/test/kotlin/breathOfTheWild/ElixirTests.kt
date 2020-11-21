package breathOfTheWild

import BaseTest
import Path
import breathOfTheWild.image.ImagesResponse
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.util.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import parseResponse
import shared.zelda.Elixir
import shared.zelda.Image
import shared.zelda.responses.ElixirsResponse
import shouldBe
import shouldNotBe

@KtorExperimentalAPI
class ElixirTests : BaseTest() {
    private lateinit var image: Image

    private fun newItem(addition: Int, id: Int? = null) = Elixir(
        id,
        addition.toString(),
        image,
        "effect",
        "description",
        listOf("ingredient")
    )

    private val path = Path.BreathOfTheWild.elixirs

    @BeforeAll
    fun start() {
        createUser()

        post(Path.BreathOfTheWild.images).send(Image(null, "anImage", "srcHere", 12, 12))
        image = get(Path.BreathOfTheWild.images).asObject<ImagesResponse>().items?.last()!!
    }

    @Test
    @Order(1)
    fun `verify getting base url`() {
        get(path).sendStatus<Unit>() shouldBe NoContent
    }

    @Test
    @Order(2)
    fun `verify getting base url returns all items in table`() {
        post(path).sendStatus(newItem(0)) shouldBe Created
        post(path).sendStatus(newItem(1)) shouldBe Created

        val request = get(path).send<Unit>()
        val responseItems = request.response.content.parseResponse<ElixirsResponse>()?.items

        val item1 = responseItems!![responseItems.lastIndex - 1]
        val item2 = responseItems[responseItems.lastIndex]
        request.response.status() shouldBe OK
        item1.name shouldBe "0"
        item2.name shouldBe "1"
    }

    @Test
    @Order(3)
    fun `verify getting an added item`() {
        post(path).sendStatus(newItem(2)) shouldBe Created

        val request = get(path).send<Elixir>()
        val addedItems = request.response.content.parseResponse<ElixirsResponse>()?.items?.last()

        request.response.status() shouldBe OK
        addedItems?.name shouldBe "2"
    }

    @Test
    @Order(4)
    fun `verify getting an item that does not exist`() {
        get(path, 99).sendStatus<Unit>() shouldBe NoContent
    }

    @Test
    @Order(5)
    fun `verify adding a new item`() {
        post(path).sendStatus(newItem(3)) shouldBe Created
    }

    @Test
    @Order(6)
    fun `verify adding a duplicate item`() {
        post(path).send(newItem(8))
        post(path).sendStatus((newItem(8, 8))) shouldBe Conflict
    }

    @Test
    @Order(7)
    fun `verify updating an added item`() {
        val updatedName = "elixir4"
        post(path).sendStatus(newItem(4)) shouldBe Created

        val elixir = get(path).asObject<ElixirsResponse>().items?.last()

        put(path).sendStatus(elixir?.copy(name = updatedName)) shouldBe OK

        val updatedElixir = get(path, elixir?.id).asObject<ElixirsResponse>().items?.first()

        updatedElixir shouldNotBe null
        updatedElixir?.name shouldBe updatedName
    }

    @Test
    @Order(8)
    fun `verify updating a non existent item`() {
        put(path).sendStatus(newItem(5, 99)) shouldBe BadRequest
    }

    @Test
    @Order(9)
    fun `verify updating without an id`() {
        put(path).sendStatus(newItem(6)) shouldBe BadRequest
    }

    @Test
    @Order(10)
    fun `verify deleting and item that has been added`() {
        post(path).sendStatus(newItem(7)) shouldBe Created

        val addedItem = get(path).asObject<ElixirsResponse>().items?.last()

        delete(path, addedItem?.id).sendStatus<Unit>() shouldBe OK
    }

    @Test
    @Order(11)
    fun `verify deleting item that doesn't exist`() {
        delete(path, 99).sendStatus<Unit>() shouldBe NotFound
    }
}
