package com.example.data

import android.content.Context
import com.example.R
import com.example.data.remote.FirestoreOffer
import com.example.data.remote.FirestoreOrder
import com.example.data.remote.FirestorePaymentSettings
import com.example.data.remote.FirestoreUserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.PaymentMethod
import com.example.data.remote.handleFirestoreError
import com.example.model.OfferCategory
import com.example.model.OfferModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OfferRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    private fun offersCollection(userId: String) =
        db.collection("users").document(userId).collection("offers")

    private fun ordersCollection(userId: String) =
        db.collection("users").document(userId).collection("orders")

    fun observeOffers(userId: String): Flow<List<OfferModel>> = flow {
        require(userId.isNotBlank()) { "Authenticated userId is required." }
        val path = "users/$userId/offers"
        emitAll(
            offersCollection(userId)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreOffer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toOfferModel() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOrders(userId: String): Flow<List<FirestoreOrder>> = flow {
        require(userId.isNotBlank()) { "Authenticated userId is required." }
        val path = "users/$userId/orders"
        emitAll(
            ordersCollection(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePaymentSettings(): Flow<FirestorePaymentSettings> {
        val docRef = db.collection("app_config").document("payment_settings")
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        FirestorePaymentSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: FirestorePaymentSettings()
                } else {
                    FirestorePaymentSettings()
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun savePaymentSettings(
        userId: String,
        bkashNumber: String,
        nagadNumber: String,
        rocketNumber: String,
        instructions: String
    ): Result<Unit> {
        val docRef = db.collection("app_config").document("payment_settings")
        return try {
            val uid = requireUserId(userId)
            val cleanBkash = bkashNumber.trim().take(25)
            val cleanNagad = nagadNumber.trim().take(25)
            val cleanRocket = rocketNumber.trim().take(25)
            val cleanInstructions = instructions.trim()
                .ifBlank { "উপরের নম্বরে Send Money করে নিচে Transaction ID (TrxID) লিখুন।" }
                .take(200)

            require(cleanBkash.length >= 6 || cleanNagad.length >= 6 || cleanRocket.length >= 6) {
                "কমপক্ষে একটি পেমেন্ট নম্বর (bKash / Nagad / Rocket) দিন।"
            }

            docRef.set(
                mapOf(
                    "bkashNumber" to cleanBkash,
                    "nagadNumber" to cleanNagad,
                    "rocketNumber" to cleanRocket,
                    "instructions" to cleanInstructions,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = flow {
        require(userId.isNotBlank()) { "Authenticated userId is required." }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FirestoreUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun registerUserProfile(
        userId: String,
        fullName: String,
        phone: String,
        division: String,
        email: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanName = fullName.trim()
            val cleanPhone = phone.trim()
            val cleanDivision = division.trim().ifBlank { "All BD" }
            val cleanEmail = email.trim().take(120)

            require(cleanName.length in 2..80) { "নাম কমপক্ষে ২ অক্ষরের হতে হবে।" }
            require(cleanPhone.length in 6..20) { "সঠিক মোবাইল নম্বর লিখুন।" }
            require(cleanDivision.length in 2..40) { "সঠিক বিভাগ নির্বাচন করুন।" }

            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "phone" to cleanPhone,
                        "division" to cleanDivision,
                        "email" to cleanEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "fullName" to cleanName,
                        "phone" to cleanPhone,
                        "division" to cleanDivision,
                        "email" to cleanEmail,
                        "role" to "USER",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureSeeded(userId: String): Result<Unit> {
        val collectionPath = "users/$userId/offers"
        return try {
            val uid = requireUserId(userId)
            val existing = offersCollection(uid).limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                OfferCatalogSeed.curatedOffers().forEachIndexed { index, offer ->
                    val docRef = offersCollection(uid).document(offer.id)
                    val safeCat = if (offer.category == OfferCategory.ALL) {
                        OfferCategory.INTERNET.name
                    } else {
                        offer.category.name
                    }
                    val payload = mapOf(
                        "userId" to uid,
                        "title" to offer.title.take(200),
                        "price" to offer.price.take(30),
                        "buyPrice" to offer.buyPrice.take(30),
                        "discount" to offer.discount.take(60),
                        "category" to safeCat,
                        "badge" to offer.badge.take(40),
                        "rating" to offer.rating.toDouble().coerceIn(0.0, 5.0),
                        "description" to offer.description.take(500),
                        "featured" to offer.isFeatured,
                        "bookmarked" to false,
                        "bagQuantity" to 0L,
                        "sortOrder" to (index + 1).toLong(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, collectionPath)
            Result.failure(e)
        }
    }

    suspend fun publishOffer(
        userId: String,
        title: String,
        price: String,
        buyPrice: String,
        discount: String,
        category: OfferCategory,
        badge: String,
        description: String,
        featured: Boolean,
        sortOrder: Long = 0L
    ): Result<Unit> {
        var docPath = "users/$userId/offers"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanPrice = price.trim()
            val cleanBuyPrice = buyPrice.trim()
            val cleanDiscount = discount.trim()
            val cleanBadge = badge.trim()
            val cleanDesc = description.trim()
            val safeCategory = if (category == OfferCategory.ALL) OfferCategory.INTERNET else category

            require(cleanTitle.length in 2..200) { "Offer title must be 2 to 200 characters." }
            require(cleanPrice.length in 1..30) { "Original price must be 1 to 30 characters." }
            require(cleanBuyPrice.length in 1..30) { "Buy price must be 1 to 30 characters." }
            require(cleanDiscount.length in 1..60) { "Discount tag must be 1 to 60 characters." }
            require(cleanBadge.length <= 40) { "Badge must be 40 characters or fewer." }
            require(cleanDesc.length <= 500) { "Description must be 500 characters or fewer." }

            val docRef = offersCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "price" to cleanPrice,
                "buyPrice" to cleanBuyPrice,
                "discount" to cleanDiscount,
                "category" to safeCategory.name,
                "badge" to cleanBadge,
                "rating" to 4.9,
                "description" to cleanDesc,
                "featured" to featured,
                "bookmarked" to false,
                "bagQuantity" to 0L,
                "sortOrder" to sortOrder.coerceIn(0L, 100000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOffer(
        userId: String,
        offerId: String,
        title: String,
        price: String,
        buyPrice: String,
        discount: String,
        category: OfferCategory,
        badge: String,
        description: String,
        featured: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/offers/$offerId"
        return try {
            val uid = requireUserId(userId)
            require(offerId.isNotBlank()) { "Offer ID must not be blank for update." }
            val cleanTitle = title.trim()
            val cleanPrice = price.trim()
            val cleanBuyPrice = buyPrice.trim()
            val cleanDiscount = discount.trim()
            val cleanBadge = badge.trim()
            val cleanDesc = description.trim()
            val safeCategory = if (category == OfferCategory.ALL) OfferCategory.INTERNET else category

            require(cleanTitle.length in 2..200) { "Offer title must be 2 to 200 characters." }
            require(cleanPrice.length in 1..30) { "Original price must be 1 to 30 characters." }
            require(cleanBuyPrice.length in 1..30) { "Buy price must be 1 to 30 characters." }
            require(cleanDiscount.length in 1..60) { "Discount tag must be 1 to 60 characters." }
            require(cleanBadge.length <= 40) { "Badge must be 40 characters or fewer." }
            require(cleanDesc.length <= 500) { "Description must be 500 characters or fewer." }

            offersCollection(uid).document(offerId).update(
                mapOf(
                    "title" to cleanTitle,
                    "price" to cleanPrice,
                    "buyPrice" to cleanBuyPrice,
                    "discount" to cleanDiscount,
                    "category" to safeCategory.name,
                    "badge" to cleanBadge,
                    "description" to cleanDesc,
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteOffer(userId: String, offerId: String): Result<Unit> {
        val docPath = "users/$userId/offers/$offerId"
        return try {
            val uid = requireUserId(userId)
            require(offerId.isNotBlank()) { "Offer ID must not be blank." }
            offersCollection(uid).document(offerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(userId: String, offer: OfferModel): Result<Unit> {
        val docPath = "users/$userId/offers/${offer.id}"
        return try {
            val uid = requireUserId(userId)
            require(offer.id.isNotBlank()) { "Offer ID must not be blank." }
            offersCollection(uid).document(offer.id).update(
                mapOf(
                    "bookmarked" to !offer.isBookmarked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setBagQuantity(userId: String, offerId: String, quantity: Int): Result<Unit> {
        val docPath = "users/$userId/offers/$offerId"
        return try {
            val uid = requireUserId(userId)
            require(offerId.isNotBlank()) { "Offer ID must not be blank." }
            offersCollection(uid).document(offerId).update(
                mapOf(
                    "bagQuantity" to quantity.coerceIn(0, 99).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearBag(userId: String, bagItems: List<OfferModel>): Result<Unit> {
        val collectionPath = "users/$userId/offers"
        return try {
            val uid = requireUserId(userId)
            if (bagItems.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            bagItems.forEach { item ->
                if (item.id.isNotBlank()) {
                    batch.update(
                        offersCollection(uid).document(item.id),
                        mapOf(
                            "bagQuantity" to 0L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, collectionPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        userId: String,
        offer: OfferModel,
        quantity: Int,
        paymentMethod: PaymentMethod,
        adminPaymentNumber: String,
        transactionId: String,
        customerPhone: String
    ): Result<Unit> {
        var docPath = "users/$userId/orders"
        return try {
            val uid = requireUserId(userId)
            val safeQty = quantity.coerceIn(1, 99)
            val safeOfferId = offer.id.ifBlank { "custom_offer" }.take(128)
            val safeTitle = offer.title.trim().ifBlank { "Telecom Offer" }.take(200)
            val safePrice = offer.price.trim().ifBlank { "0" }.take(30)
            val safeBuyPrice = offer.buyPrice.trim().ifBlank { "0" }.take(30)
            val safeDiscount = offer.discount.trim().ifBlank { "৳0" }.take(60)
            val totalPaid = (offer.numericBuyPrice * safeQty).coerceIn(0.0, 10_000_000.0)
            val cleanAdminNumber = adminPaymentNumber.trim().take(25)
            val cleanTrxId = transactionId.trim().take(60)
            val cleanCustomerPhone = customerPhone.trim().take(25)

            require(cleanAdminNumber.length >= 6) { "অ্যাডমিন পেমেন্ট নম্বর পাওয়া যায়নি।" }
            require(cleanTrxId.length >= 4) { "সঠিক Transaction ID (কমপক্ষে ৪ অক্ষর) লিখুন।" }
            require(cleanCustomerPhone.length >= 6) { "আপনার সঠিক মোবাইল নম্বর লিখুন।" }

            val docRef = ordersCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "offerId" to safeOfferId,
                "offerTitle" to safeTitle,
                "price" to safePrice,
                "buyPrice" to safeBuyPrice,
                "discount" to safeDiscount,
                "quantity" to safeQty.toLong(),
                "totalPaid" to totalPaid,
                "paymentMethod" to paymentMethod.label,
                "adminPaymentNumber" to cleanAdminNumber,
                "transactionId" to cleanTrxId,
                "customerPhone" to cleanCustomerPhone,
                "status" to "PLACED",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun completeOrder(userId: String, orderId: String): Result<Unit> {
        val docPath = "users/$userId/orders/$orderId"
        return try {
            val uid = requireUserId(userId)
            require(orderId.isNotBlank()) { "Order ID must not be blank." }
            ordersCollection(uid).document(orderId).update(
                mapOf(
                    "status" to "COMPLETED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
