package com.example.model

import java.util.Locale
import kotlin.math.roundToInt

enum class OfferCategory(val label: String) {
    ALL("All"),
    INTERNET("Internet"),
    COMBO("Combo"),
    UNLIMITED("Unlimited"),
    MINUTES("Minutes"),
    REGIONAL("Regional")
}

/**
 * Core OfferModel preserving the 4-field contract (title, price, buyPrice, discount)
 * along with telecom drive offer metadata and computed savings helpers.
 */
data class OfferModel(
    val title: String,
    val price: String,
    val buyPrice: String,
    val discount: String,
    val id: String = title.hashCode().toUInt().toString(16),
    val category: OfferCategory = OfferCategory.INTERNET,
    val badge: String = "",
    val rating: Float = 4.9f,
    val description: String = "",
    val isFeatured: Boolean = false,
    val isBookmarked: Boolean = false,
    val bagQuantity: Int = 0
) {
    val numericPrice: Double
        get() = parseCurrency(price)

    val numericBuyPrice: Double
        get() = parseCurrency(buyPrice)

    val savingsAmount: Double
        get() = (numericPrice - numericBuyPrice).coerceAtLeast(0.0)

    val discountPercentValue: Int
        get() {
            val percentMatch = Regex("(\\d+)\\s*%").find(discount)
            if (percentMatch != null) {
                return percentMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 99) ?: 0
            }
            return if (numericPrice > 0.0) {
                (((numericPrice - numericBuyPrice) / numericPrice) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 99)
            } else {
                0
            }
        }

    val formattedSavings: String
        get() = formatCurrency(savingsAmount)

    val formattedBuyPrice: String
        get() {
            val trimmed = buyPrice.trim()
            return if (trimmed.startsWith("৳")) trimmed else "৳ ${trimmed.removePrefix("$")}"
        }

    val formattedOriginalPrice: String
        get() {
            val trimmed = price.trim()
            return if (trimmed.startsWith("৳")) trimmed else "৳ ${trimmed.removePrefix("$")}"
        }

    companion object {
        fun parseCurrency(raw: String): Double {
            val cleaned = raw.replace(Regex("[^0-9.]"), "")
            return cleaned.toDoubleOrNull() ?: 0.0
        }

        fun formatCurrency(amount: Double): String {
            return if (amount % 1.0 == 0.0) {
                String.format(Locale.US, "৳%,.0f", amount)
            } else {
                String.format(Locale.US, "৳%,.2f", amount)
            }
        }
    }
}
