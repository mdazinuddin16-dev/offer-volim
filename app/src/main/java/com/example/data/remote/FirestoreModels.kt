package com.example.data.remote

import androidx.annotation.Keep
import com.example.model.OfferCategory
import com.example.model.OfferModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PaymentMethod(val label: String) {
    BKASH("bKash"),
    NAGAD("Nagad"),
    ROCKET("Rocket");

    fun numberFrom(settings: FirestorePaymentSettings): String = when (this) {
        BKASH -> settings.bkashNumber.trim()
        NAGAD -> settings.nagadNumber.trim()
        ROCKET -> settings.rocketNumber.trim()
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreOffer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val price: String = "",
    val buyPrice: String = "",
    val discount: String = "",
    val category: String = OfferCategory.INTERNET.name,
    val badge: String = "",
    val rating: Double = 4.9,
    val description: String = "",
    val featured: Boolean = false,
    val bookmarked: Boolean = false,
    val bagQuantity: Long = 0L,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toOfferModel(): OfferModel {
        val safeCategory = enumValues<OfferCategory>()
            .firstOrNull { it.name == category && it != OfferCategory.ALL }
            ?: OfferCategory.INTERNET
        return OfferModel(
            title = title,
            price = price,
            buyPrice = buyPrice,
            discount = discount,
            id = id.ifBlank { title.hashCode().toUInt().toString(16) },
            category = safeCategory,
            badge = badge,
            rating = rating.toFloat().coerceIn(0f, 5f),
            description = description,
            isFeatured = featured,
            isBookmarked = bookmarked,
            bagQuantity = bagQuantity.toInt().coerceIn(0, 99)
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val offerId: String = "",
    val offerTitle: String = "",
    val price: String = "",
    val buyPrice: String = "",
    val discount: String = "",
    val quantity: Long = 1L,
    val totalPaid: Double = 0.0,
    val paymentMethod: String = PaymentMethod.BKASH.label,
    val adminPaymentNumber: String = "",
    val transactionId: String = "",
    val customerPhone: String = "",
    val status: String = "PLACED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestorePaymentSettings(
    @DocumentId val id: String = "",
    val bkashNumber: String = "",
    val nagadNumber: String = "",
    val rocketNumber: String = "",
    val instructions: String = "উপরের নম্বরে Send Money করে নিচে Transaction ID (TrxID) লিখুন।",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun availableMethods(): List<PaymentMethod> = buildList {
        if (bkashNumber.trim().length >= 6) add(PaymentMethod.BKASH)
        if (nagadNumber.trim().length >= 6) add(PaymentMethod.NAGAD)
        if (rocketNumber.trim().length >= 6) add(PaymentMethod.ROCKET)
    }
}

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val division: String = "",
    val email: String = "",
    val role: String = "USER",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
