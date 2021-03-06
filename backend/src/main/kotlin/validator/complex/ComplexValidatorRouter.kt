package com.weesnerdevelopment.validator.complex

import BaseRouter
import kotlin.reflect.full.createType

class ComplexValidatorRouter(
    override val basePath: String,
    override val service: ComplexValidatorService
) : BaseRouter<ComplexValidatorItem, ComplexValidatorService>(
    ComplexValidatorResponse(),
    service,
    ComplexValidatorItem::class.createType()
)
