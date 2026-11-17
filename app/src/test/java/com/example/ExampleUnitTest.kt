package com.example

import com.example.model.OfferModel
import com.example.viewmodel.AnalyzerState
import com.example.viewmodel.CalculatorTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun offerModel_preservesTelecomDriveOfferContractAndComputesSavings() {
        val offer = OfferModel(
            "H > 150 জিবি 30 দিন 898৳ অল বিডি সবাই পাবে",
            "898",
            "748",
            "discount: ৳150"
        )
        assertEquals("H > 150 জিবি 30 দিন 898৳ অল বিডি সবাই পাবে", offer.title)
        assertEquals("898", offer.price)
        assertEquals("748", offer.buyPrice)
        assertEquals("discount: ৳150", offer.discount)
        assertEquals("৳ 748", offer.formattedBuyPrice)
        assertEquals("৳ 898", offer.formattedOriginalPrice)
        assertEquals(150.0, offer.savingsAmount, 0.001)
        assertEquals(17, offer.discountPercentValue)
        assertEquals("৳150", offer.formattedSavings)
    }

    @Test
    fun analyzerState_computesBuyPriceAndDiscountInTaka() {
        val solveBuy = AnalyzerState(
            calculatorTarget = CalculatorTarget.SOLVE_BUY_PRICE,
            customTitle = "50 জিবি ড্রাইভ প্যাক",
            priceInput = "1000",
            discountInput = "15"
        ).buildPreviewOffer()

        assertEquals("৳1,000", solveBuy.price)
        assertEquals("৳850", solveBuy.buyPrice)
        assertEquals("15% OFF", solveBuy.discount)

        val solveDiscount = AnalyzerState(
            calculatorTarget = CalculatorTarget.SOLVE_DISCOUNT,
            customTitle = "30 জিবি 700 মিনিট",
            priceInput = "818",
            buyPriceInput = "668"
        ).buildPreviewOffer()

        assertEquals("৳818", solveDiscount.price)
        assertEquals("৳668", solveDiscount.buyPrice)
        assertEquals("18% OFF", solveDiscount.discount)
    }
}
