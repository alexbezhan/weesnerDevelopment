import com.weesnerdevelopment.utils.Path
import federalIncomeTax.FederalIncomeTaxResponse
import io.kotlintest.shouldBe
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode
import shared.fromJson
import shared.taxFetcher.FederalIncomeTax
import shared.taxFetcher.MaritalStatus.Single
import shared.taxFetcher.PayPeriod.Weekly
import shared.toJson

class FederalIncomeTaxTest : BaseTest({ token ->
    fun newItem(year: Int) = FederalIncomeTax(
        year = year,
        percent = 10.0,
        payPeriod = Weekly,
        maritalStatus = Single,
        nonTaxable = 0.0,
        plus = 0.0,
        notOver = 5.0,
        over = 1.0
    )

    val path = Path.TaxFetcher.federalIncomeTax

    "verify getting base url returns ok" {
        with(engine) {
            request(Get, path, authToken = token).response.status() shouldBe HttpStatusCode.OK
        }
    }

    "verify getting base url returns all items in table" {
        with(engine) {
            bodyRequest(Post, path, newItem(2000).toJson(), token)
            bodyRequest(Post, path, newItem(2001).toJson(), token)
            with(request(Get, path, authToken = token)) {
                val responseItems = response.content?.fromJson<FederalIncomeTaxResponse>()?.items
                val item1 = responseItems!![responseItems.lastIndex - 1]
                val item2 = responseItems[responseItems.lastIndex]
                response.status() shouldBe HttpStatusCode.OK
                item1 shouldBe FederalIncomeTax(
                    item1.id,
                    2000,
                    Single,
                    Weekly,
                    1.0,
                    5.0,
                    0.0,
                    10.0,
                    0.0,
                    null,
                    item1.dateCreated,
                    item1.dateUpdated
                )
                item2 shouldBe FederalIncomeTax(
                    item2.id,
                    2001,
                    Single,
                    Weekly,
                    1.0,
                    5.0,
                    0.0,
                    10.0,
                    0.0,
                    null,
                    item2.dateCreated,
                    item2.dateUpdated
                )
            }
        }
    }

    "verify getting an added item" {
        with(engine) {
            val item = requestToObject<FederalIncomeTax>(Post, path, newItem(2002).toJson(), token)
            with(request(Get, path, item?.year?.toString(), authToken = token)) {
                val addedItem = response.content!!.fromJson<FederalIncomeTax>()!!
                response.status() shouldBe HttpStatusCode.OK
                addedItem shouldBe FederalIncomeTax(
                    item?.id,
                    2002,
                    Single,
                    Weekly,
                    1.0,
                    5.0,
                    0.0,
                    10.0,
                    0.0,
                    null,
                    addedItem.dateCreated,
                    addedItem.dateUpdated
                )
            }
        }
    }

    "verify getting an item that does not exist" {
        with(engine) {
            request(Get, path, "99", token).response.status() shouldBe HttpStatusCode.NotFound
        }
    }

    "verify adding a new item" {
        with(engine) {
            with(bodyRequest(Post, path, newItem(2003).toJson(), token)) {
                val addedItem = response.content!!.fromJson<FederalIncomeTax>()!!
                response.status() shouldBe HttpStatusCode.Created
                addedItem shouldBe FederalIncomeTax(
                    addedItem.id,
                    2003,
                    Single,
                    Weekly,
                    1.0,
                    5.0,
                    0.0,
                    10.0,
                    0.0,
                    null,
                    addedItem.dateCreated,
                    addedItem.dateUpdated
                )
            }
        }
    }

    "verify adding a duplicate item" {
        with(engine) {
            bodyRequest(Post, path, newItem(2008).toJson(), token)

            with(bodyRequest(Post, path, newItem(2008).toJson(), token)) {
                response.status() shouldBe HttpStatusCode.Conflict
            }
        }
    }

    "verify updating an added item" {
        with(engine) {
            bodyRequest(Post, path, newItem(2004).toJson(), authToken = token)
            with(
                bodyRequest(
                    Put,
                    path,
                    newItem(2004).copy(id = 1, percent = 1.4, over = 2.5).toJson(),
                    token
                )
            ) {
                val addedItem = response.content!!.fromJson<FederalIncomeTax>()!!
                response.status() shouldBe HttpStatusCode.OK
                addedItem shouldBe FederalIncomeTax(
                    1,
                    2004,
                    Single,
                    Weekly,
                    2.5,
                    5.0,
                    0.0,
                    1.4,
                    0.0,
                    null,
                    addedItem.dateCreated,
                    addedItem.dateUpdated
                )
            }
        }
    }

    "verify updating a non existent item" {
        with(engine) {
            bodyRequest(
                Put,
                path,
                newItem(2005).copy(99).toJson(),
                token
            ).response.status() shouldBe HttpStatusCode.BadRequest
        }
    }

    "verify updating without an id adds a new item" {
        with(engine) {
            bodyRequest(
                Put,
                path,
                newItem(2006).toJson(),
                token
            ).response.status() shouldBe HttpStatusCode.Created
        }
    }

    "verify deleting and item that has been added" {
        with(engine) {
            bodyRequest(Post, path, newItem(2007).toJson(), authToken = token)
            request(Delete, path, "2007", token).response.status() shouldBe HttpStatusCode.OK
        }
    }

    "verify deleting item that doesn't exist" {
        with(engine) {
            request(Delete, path, "2099", token).response.status() shouldBe HttpStatusCode.NotFound
        }
    }

    "verify adding new data where 'over' is between" {
        with(engine) {
            bodyRequest(
                Post,
                path,
                newItem(2006).copy(over = 2.0).toJson(),
                authToken = token
            ).response.status() shouldBe HttpStatusCode.Conflict
        }
    }

    "verify adding new data where 'notOver' is between" {
        with(engine) {
            bodyRequest(
                Post,
                path,
                newItem(2006).copy(notOver = 2.0).toJson(),
                authToken = token
            ).response.status() shouldBe HttpStatusCode.Conflict
        }
    }

    "verify adding new data where 'over' is between and 'notOver' is not" {
        with(engine) {
            bodyRequest(
                Post,
                path,
                newItem(2006).copy(over = 2.0, notOver = 10.0).toJson(),
                authToken = token
            ).response.status() shouldBe HttpStatusCode.Conflict
        }
    }
})
