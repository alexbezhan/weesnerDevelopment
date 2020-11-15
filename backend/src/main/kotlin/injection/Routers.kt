package com.weesnerdevelopment.injection

import Path.*
import auth.UserRouter
import bills.BillsRouter
import breathOfTheWild.cookingPotFood.CookingPotFoodsRouter
import breathOfTheWild.critter.CrittersRouter
import breathOfTheWild.elixirs.ElixirsRouter
import breathOfTheWild.ingredients.IngredientsRouter
import categories.CategoriesRouter
import com.weesnerdevelopment.validator.ValidatorRouter
import com.weesnerdevelopment.validator.complex.ComplexValidatorRouter
import federalIncomeTax.FederalIncomeTaxRouter
import income.IncomeRouter
import incomeOccurrences.IncomeOccurrenceRouter
import logging.LoggingRouter
import medicare.MedicareRouter
import occurrences.BillOccurrenceRouter
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import socialSecurity.SocialSecurityRouter
import taxWithholding.TaxWithholdingRouter

val routers = Kodein.Module("routers") {
    bind<ValidatorRouter>() with singleton { ValidatorRouter(Server.validation, instance()) }
    bind<ComplexValidatorRouter>() with singleton { ComplexValidatorRouter(Server.complexValidation, instance()) }
    // user
    bind<UserRouter>() with singleton {
        UserRouter(User.base, instance(), instance(), User.account, User.login, User.signUp)
    }

    // taxFetcher
    bind<FederalIncomeTaxRouter>() with singleton {
        FederalIncomeTaxRouter(TaxFetcher.federalIncomeTax, instance(), instance(), instance())
    }
    bind<MedicareRouter>() with singleton {
        MedicareRouter(TaxFetcher.medicare, instance(), instance(), instance())
    }
    bind<SocialSecurityRouter>() with singleton {
        SocialSecurityRouter(TaxFetcher.socialSecurity, instance(), instance(), instance())
    }
    bind<TaxWithholdingRouter>() with singleton {
        TaxWithholdingRouter(TaxFetcher.taxWithholding, instance(), instance(), instance())
    }

    // billMan
    bind<BillsRouter>() with singleton { BillsRouter(BillMan.bills, instance()) }
    bind<CategoriesRouter>() with singleton { CategoriesRouter(BillMan.categories, instance()) }
    bind<IncomeRouter>() with singleton { IncomeRouter(BillMan.income, instance()) }
    bind<BillOccurrenceRouter>() with singleton { BillOccurrenceRouter(BillMan.occurrences, instance()) }
    bind<IncomeOccurrenceRouter>() with singleton { IncomeOccurrenceRouter(BillMan.incomeOccurrences, instance()) }
    bind<LoggingRouter>() with singleton { LoggingRouter(BillMan.logging, instance()) }

    // breathOfTheWild
    bind<CrittersRouter>() with singleton { CrittersRouter(BreathOfTheWild.critters, instance()) }
    bind<CookingPotFoodsRouter>() with singleton { CookingPotFoodsRouter(BreathOfTheWild.cookingPotFoods, instance()) }
    bind<IngredientsRouter>() with singleton { IngredientsRouter(BreathOfTheWild.ingredients, instance()) }
    bind<ElixirsRouter>() with singleton { ElixirsRouter(BreathOfTheWild.elixirs, instance()) }
}