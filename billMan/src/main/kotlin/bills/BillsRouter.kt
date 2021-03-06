package bills

import BaseRouter
import shared.billMan.Bill
import shared.billMan.responses.BillsResponse
import kotlin.reflect.full.createType

class BillsRouter(
    override val basePath: String,
    override val service: BillsService
) : BaseRouter<Bill, BillsService>(
    BillsResponse(),
    service,
    Bill::class.createType()
)
