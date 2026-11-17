package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OfferRepository
import com.example.data.remote.FirestoreOrder
import com.example.data.remote.FirestorePaymentSettings
import com.example.data.remote.FirestoreUserProfile
import com.example.data.remote.PaymentMethod
import com.example.data.remote.unwrapFirestoreMessage
import com.example.model.OfferCategory
import com.example.model.OfferModel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface UserProfileGateState {
    data object Loading : UserProfileGateState
    data object NeedsRegistration : UserProfileGateState
    data class Registered(val profile: FirestoreUserProfile) : UserProfileGateState
    data class Error(val message: String) : UserProfileGateState
}

enum class OfferSortOption(val label: String) {
    HIGHEST_DISCOUNT("Highest Discount"),
    LOWEST_BUY_PRICE("Lowest Buy Price"),
    MOST_SAVINGS("Most Savings (৳)"),
    TITLE_ASC("A to Z")
}

enum class AnalyzerMode(val label: String) {
    CALCULATOR("Discount Calculator"),
    COMPARE("Compare Two Offers")
}

enum class CalculatorTarget(val label: String) {
    SOLVE_BUY_PRICE("Find Buy Price"),
    SOLVE_DISCOUNT("Find Discount %")
}

data class DealsScreenState(
    val isLoading: Boolean = true,
    val allOffers: List<OfferModel> = emptyList(),
    val filteredOffers: List<OfferModel> = emptyList(),
    val featuredOffer: OfferModel? = null,
    val searchQuery: String = "",
    val selectedCategory: OfferCategory = OfferCategory.ALL,
    val sortOption: OfferSortOption = OfferSortOption.HIGHEST_DISCOUNT,
    val savedOnly: Boolean = false,
    val selectedOfferForDetail: OfferModel? = null,
    val bagCount: Int = 0,
    val errorMessage: String? = null
)

data class BagSummaryState(
    val items: List<OfferModel> = emptyList(),
    val totalUnits: Int = 0,
    val totalOriginalPrice: Double = 0.0,
    val totalBuyPrice: Double = 0.0,
    val totalSavings: Double = 0.0,
    val effectiveDiscountPercent: Int = 0,
    val showCheckoutReceipt: Boolean = false
)

data class AnalyzerState(
    val mode: AnalyzerMode = AnalyzerMode.CALCULATOR,
    val calculatorTarget: CalculatorTarget = CalculatorTarget.SOLVE_BUY_PRICE,
    val customTitle: String = "150 জিবি 30 দিন অল বিডি",
    val priceInput: String = "898",
    val discountInput: String = "17",
    val buyPriceInput: String = "748",
    val compareOfferAId: String = "",
    val compareOfferBId: String = ""
) {
    fun buildPreviewOffer(): OfferModel {
        val original = OfferModel.parseCurrency(priceInput)
        return when (calculatorTarget) {
            CalculatorTarget.SOLVE_BUY_PRICE -> {
                val pct = (discountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 99.0)
                val computedBuy = (original * (1.0 - pct / 100.0)).coerceAtLeast(0.0)
                OfferModel(
                    title = customTitle.ifBlank { "Custom Offer Preview" },
                    price = OfferModel.formatCurrency(original),
                    buyPrice = OfferModel.formatCurrency(computedBuy),
                    discount = "${pct.roundToInt()}% OFF",
                    badge = "Live Preview"
                )
            }
            CalculatorTarget.SOLVE_DISCOUNT -> {
                val buy = OfferModel.parseCurrency(buyPriceInput).coerceIn(0.0, original.coerceAtLeast(0.0))
                val pct = if (original > 0.0) {
                    (((original - buy) / original) * 100.0).roundToInt().coerceIn(0, 99)
                } else {
                    0
                }
                OfferModel(
                    title = customTitle.ifBlank { "Custom Offer Preview" },
                    price = OfferModel.formatCurrency(original),
                    buyPrice = OfferModel.formatCurrency(buy),
                    discount = "$pct% OFF",
                    badge = "Live Preview"
                )
            }
        }
    }
}

data class AdminPanelState(
    val editingOfferId: String? = null,
    val titleInput: String = "",
    val priceInput: String = "",
    val buyPriceInput: String = "",
    val discountInput: String = "",
    val selectedCategory: OfferCategory = OfferCategory.INTERNET,
    val badgeInput: String = "All BD",
    val descriptionInput: String = "",
    val featured: Boolean = false,
    val isPublishing: Boolean = false,
    val validationError: String? = null,
    val publishSuccessMessage: String? = null
) {
    val isEditing: Boolean
        get() = !editingOfferId.isNullOrBlank()

    fun previewOffer(): OfferModel {
        val safeTitle = titleInput.trim().ifBlank { "H > 50 জিবি 30 দিন 698৳ অল বিডি" }
        val safePrice = priceInput.trim().ifBlank { "698" }
        val safeBuyPrice = buyPriceInput.trim().ifBlank { "598" }
        val safeDiscount = discountInput.trim().ifBlank {
            val diff = (OfferModel.parseCurrency(safePrice) - OfferModel.parseCurrency(safeBuyPrice))
                .roundToInt()
                .coerceAtLeast(0)
            "discount: ৳$diff"
        }
        return OfferModel(
            title = safeTitle,
            price = safePrice,
            buyPrice = safeBuyPrice,
            discount = safeDiscount,
            id = "admin_preview",
            category = selectedCategory,
            badge = badgeInput.trim(),
            description = descriptionInput.trim(),
            isFeatured = featured
        )
    }
}

data class AdminPaymentSettingsState(
    val bkashInput: String = "",
    val nagadInput: String = "",
    val rocketInput: String = "",
    val instructionsInput: String = "উপরের নম্বরে Send Money করে নিচে Transaction ID (TrxID) লিখুন।",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val hasLocalEdits: Boolean = false
)

data class PaymentCheckoutState(
    val offer: OfferModel,
    val quantity: Int = 1,
    val selectedMethod: PaymentMethod = PaymentMethod.BKASH,
    val customerPhoneInput: String = "",
    val transactionIdInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val payableAmount: Double
        get() = (offer.numericBuyPrice * quantity).coerceAtLeast(0.0)
}

private data class FilterCriteria(
    val searchQuery: String,
    val selectedCategory: OfferCategory,
    val sortOption: OfferSortOption,
    val savedOnly: Boolean,
    val selectedOfferId: String?
)

class OfferVaultViewModel(
    private val repository: OfferRepository,
    private val currentUserId: String
) : ViewModel() {

    private val filterCriteria = MutableStateFlow(
        FilterCriteria(
            searchQuery = "",
            selectedCategory = OfferCategory.ALL,
            sortOption = OfferSortOption.HIGHEST_DISCOUNT,
            savedOnly = false,
            selectedOfferId = null
        )
    )

    private val _isSeeded = MutableStateFlow(false)
    private val _showCheckoutReceipt = MutableStateFlow(false)
    private val _streamError = MutableStateFlow<String?>(null)
    private val _isRegisteringProfile = MutableStateFlow(false)
    private val _registrationError = MutableStateFlow<String?>(null)
    private val _analyzerState = MutableStateFlow(AnalyzerState())
    private val _adminState = MutableStateFlow(AdminPanelState())
    private val _paymentFormState = MutableStateFlow(AdminPaymentSettingsState())
    private val _checkoutState = MutableStateFlow<PaymentCheckoutState?>(null)

    val analyzerState: StateFlow<AnalyzerState> = _analyzerState
    val adminState: StateFlow<AdminPanelState> = _adminState
    val isRegisteringProfile: StateFlow<Boolean> = _isRegisteringProfile
    val registrationError: StateFlow<String?> = _registrationError
    val paymentFormState: StateFlow<AdminPaymentSettingsState> = _paymentFormState
    val checkoutState: StateFlow<PaymentCheckoutState?> = _checkoutState

    val paymentSettingsState: StateFlow<FirestorePaymentSettings> = repository.observePaymentSettings()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing payment settings", firestoreError ?: error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FirestorePaymentSettings()
        )

    val profileState: StateFlow<UserProfileGateState> = repository.observeUserProfile(currentUserId)
        .map<FirestoreUserProfile?, UserProfileGateState> { profile ->
            if (profile != null) {
                UserProfileGateState.Registered(profile)
            } else {
                UserProfileGateState.NeedsRegistration
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            emit(UserProfileGateState.Error(rootError.localizedMessage ?: "প্রোফাইল লোড করা যায়নি।"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserProfileGateState.Loading
        )

    private val offersStream: StateFlow<List<OfferModel>> = repository.observeOffers(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing offers", rootError)
            _streamError.value = rootError.localizedMessage ?: "Failed to load offers from Firestore."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val ordersState: StateFlow<List<FirestoreOrder>> = repository.observeOrders(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing orders", firestoreError ?: error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val dealsState: StateFlow<DealsScreenState> = combine(
        offersStream,
        filterCriteria,
        _isSeeded,
        _streamError
    ) { offers, criteria, seeded, streamErr ->
        val query = criteria.searchQuery.trim()
        val filtered = offers
            .asSequence()
            .filter { offer ->
                criteria.selectedCategory == OfferCategory.ALL || offer.category == criteria.selectedCategory
            }
            .filter { offer ->
                !criteria.savedOnly || offer.isBookmarked
            }
            .filter { offer ->
                if (query.isEmpty()) true
                else {
                    offer.title.contains(query, ignoreCase = true) ||
                        offer.category.label.contains(query, ignoreCase = true) ||
                        offer.discount.contains(query, ignoreCase = true) ||
                        offer.badge.contains(query, ignoreCase = true)
                }
            }
            .let { seq ->
                when (criteria.sortOption) {
                    OfferSortOption.HIGHEST_DISCOUNT -> seq.sortedByDescending { it.discountPercentValue }
                    OfferSortOption.LOWEST_BUY_PRICE -> seq.sortedBy { it.numericBuyPrice }
                    OfferSortOption.MOST_SAVINGS -> seq.sortedByDescending { it.savingsAmount }
                    OfferSortOption.TITLE_ASC -> seq.sortedBy { it.title.lowercase() }
                }
            }
            .toList()

        val featured = offers.firstOrNull { it.isFeatured } ?: offers.maxByOrNull { it.discountPercentValue }
        val selectedOffer = criteria.selectedOfferId?.let { id -> offers.firstOrNull { it.id == id } }
        val bagCount = offers.sumOf { it.bagQuantity }

        DealsScreenState(
            isLoading = !seeded && offers.isEmpty() && streamErr == null,
            allOffers = offers,
            filteredOffers = filtered,
            featuredOffer = featured,
            searchQuery = criteria.searchQuery,
            selectedCategory = criteria.selectedCategory,
            sortOption = criteria.sortOption,
            savedOnly = criteria.savedOnly,
            selectedOfferForDetail = selectedOffer,
            bagCount = bagCount,
            errorMessage = streamErr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DealsScreenState()
    )

    val bagState: StateFlow<BagSummaryState> = combine(
        offersStream,
        _showCheckoutReceipt
    ) { allOffers, showReceipt ->
        val items = allOffers.filter { it.bagQuantity > 0 }
        val totalUnits = items.sumOf { it.bagQuantity }
        val totalOriginal = items.sumOf { it.numericPrice * it.bagQuantity }
        val totalBuy = items.sumOf { it.numericBuyPrice * it.bagQuantity }
        val totalSavings = (totalOriginal - totalBuy).coerceAtLeast(0.0)
        val effectivePct = if (totalOriginal > 0.0) {
            ((totalSavings / totalOriginal) * 100.0).roundToInt().coerceIn(0, 99)
        } else {
            0
        }
        BagSummaryState(
            items = items,
            totalUnits = totalUnits,
            totalOriginalPrice = totalOriginal,
            totalBuyPrice = totalBuy,
            totalSavings = totalSavings,
            effectiveDiscountPercent = effectivePct,
            showCheckoutReceipt = showReceipt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = BagSummaryState()
    )

    init {
        viewModelScope.launch {
            val result = repository.ensureSeeded(currentUserId)
            result.onFailure { error ->
                _streamError.value = unwrapFirestoreMessage(error, "Failed to sync offer catalog.")
            }
            _isSeeded.value = true
        }
    }

    fun dismissError() {
        _streamError.value = null
    }

    fun clearRegistrationError() {
        _registrationError.value = null
    }

    fun submitUserRegistration(
        fullName: String,
        phone: String,
        division: String,
        email: String,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_isRegisteringProfile.value) return
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanDivision = division.trim().ifBlank { "All BD" }

        val validationMsg = when {
            cleanName.isEmpty() || cleanPhone.isEmpty() -> "সবগুলো ঘর পূরণ করুন!"
            cleanName.length < 2 -> "নাম কমপক্ষে ২ অক্ষরের হতে হবে।"
            cleanPhone.length < 6 -> "সঠিক মোবাইল নম্বর লিখুন।"
            else -> null
        }

        if (validationMsg != null) {
            _registrationError.value = validationMsg
            onError(validationMsg)
            return
        }

        _isRegisteringProfile.value = true
        _registrationError.value = null
        viewModelScope.launch {
            val result = repository.registerUserProfile(
                userId = currentUserId,
                fullName = cleanName,
                phone = cleanPhone,
                division = cleanDivision,
                email = email
            )
            _isRegisteringProfile.value = false
            result.onSuccess {
                onComplete()
            }.onFailure { err ->
                val message = unwrapFirestoreMessage(err, "রেজিস্ট্রেশন সম্পন্ন করা যায়নি।")
                _registrationError.value = message
                onError(message)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        filterCriteria.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: OfferCategory) {
        filterCriteria.update { it.copy(selectedCategory = category) }
    }

    fun onSortOptionSelected(option: OfferSortOption) {
        filterCriteria.update { it.copy(sortOption = option) }
    }

    fun onToggleSavedOnly() {
        filterCriteria.update { it.copy(savedOnly = !it.savedOnly) }
    }

    fun clearFilters() {
        filterCriteria.update {
            it.copy(
                searchQuery = "",
                selectedCategory = OfferCategory.ALL,
                savedOnly = false
            )
        }
    }

    fun openOfferDetail(offer: OfferModel) {
        filterCriteria.update { it.copy(selectedOfferId = offer.id) }
    }

    fun dismissOfferDetail() {
        filterCriteria.update { it.copy(selectedOfferId = null) }
    }

    fun toggleBookmark(offer: OfferModel) {
        viewModelScope.launch {
            repository.toggleBookmark(currentUserId, offer).onFailure { error ->
                _streamError.value = unwrapFirestoreMessage(error, "Could not update bookmark.")
            }
        }
    }

    fun incrementBagQuantity(offer: OfferModel) {
        viewModelScope.launch {
            repository.setBagQuantity(currentUserId, offer.id, offer.bagQuantity + 1).onFailure { error ->
                _streamError.value = unwrapFirestoreMessage(error, "Could not update bag.")
            }
        }
    }

    fun decrementBagQuantity(offer: OfferModel) {
        viewModelScope.launch {
            repository.setBagQuantity(currentUserId, offer.id, (offer.bagQuantity - 1).coerceAtLeast(0))
                .onFailure { error ->
                    _streamError.value = unwrapFirestoreMessage(error, "Could not update bag.")
                }
        }
    }

    fun addToBagWithQuantity(offer: OfferModel, quantityToAdd: Int) {
        viewModelScope.launch {
            repository.setBagQuantity(currentUserId, offer.id, offer.bagQuantity + quantityToAdd)
                .onFailure { error ->
                    _streamError.value = unwrapFirestoreMessage(error, "Could not update bag.")
                }
        }
    }

    fun clearBag() {
        val currentItems = bagState.value.items
        viewModelScope.launch {
            repository.clearBag(currentUserId, currentItems).onFailure { error ->
                _streamError.value = unwrapFirestoreMessage(error, "Could not clear bag.")
            }
            _showCheckoutReceipt.value = false
        }
    }

    fun openPaymentCheckout(offer: OfferModel, quantity: Int = 1) {
        val settings = paymentSettingsState.value
        val defaultMethod = settings.availableMethods().firstOrNull() ?: PaymentMethod.BKASH
        val registeredPhone = (profileState.value as? UserProfileGateState.Registered)?.profile?.phone.orEmpty()
        _checkoutState.value = PaymentCheckoutState(
            offer = offer,
            quantity = quantity.coerceIn(1, 99),
            selectedMethod = defaultMethod,
            customerPhoneInput = registeredPhone
        )
    }

    fun dismissPaymentCheckout() {
        _checkoutState.value = null
    }

    fun selectCheckoutPaymentMethod(method: PaymentMethod) {
        _checkoutState.update { it?.copy(selectedMethod = method, errorMessage = null) }
    }

    fun updateCheckoutCustomerPhone(phone: String) {
        val sanitized = phone.filter { it.isDigit() || it == '+' }.take(25)
        _checkoutState.update { it?.copy(customerPhoneInput = sanitized, errorMessage = null) }
    }

    fun updateCheckoutTransactionId(trxId: String) {
        val sanitized = trxId.trim().take(60)
        _checkoutState.update { it?.copy(transactionIdInput = sanitized, errorMessage = null) }
    }

    fun submitCheckoutOrder(
        onOrderSuccess: (String) -> Unit = {},
        onValidationFailed: (String) -> Unit = {}
    ) {
        val current = _checkoutState.value ?: return
        if (current.isSubmitting) return

        val settings = paymentSettingsState.value
        val adminNumber = current.selectedMethod.numberFrom(settings)
        val phone = current.customerPhoneInput.trim()
        val trxId = current.transactionIdInput.trim()

        val error = when {
            adminNumber.length < 6 -> "অ্যাডমিন এখনো ${current.selectedMethod.label} পেমেন্ট নম্বর সেট করেননি!"
            phone.isEmpty() || trxId.isEmpty() -> "সবগুলো ঘর পূরণ করুন!"
            phone.length < 6 -> "সঠিক মোবাইল নম্বর লিখুন!"
            trxId.length < 4 -> "সঠিক Transaction ID (কমপক্ষে ৪ অক্ষর) লিখুন!"
            else -> null
        }

        if (error != null) {
            _checkoutState.update { it?.copy(errorMessage = error) }
            onValidationFailed(error)
            return
        }

        _checkoutState.update { it?.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.placeOrder(
                userId = currentUserId,
                offer = current.offer,
                quantity = current.quantity,
                paymentMethod = current.selectedMethod,
                adminPaymentNumber = adminNumber,
                transactionId = trxId,
                customerPhone = phone
            )
            result.onSuccess {
                _checkoutState.value = null
                onOrderSuccess(
                    "অর্ডার কনফার্ম হয়েছে! (${OfferModel.formatCurrency(current.payableAmount)} · TrxID: $trxId)"
                )
            }.onFailure { err ->
                val message = unwrapFirestoreMessage(err, "অর্ডার সম্পন্ন করা যায়নি।")
                _checkoutState.update { it?.copy(isSubmitting = false, errorMessage = message) }
                onValidationFailed(message)
            }
        }
    }

    fun syncPaymentFormFromSettings(settings: FirestorePaymentSettings) {
        _paymentFormState.update { current ->
            if (current.hasLocalEdits) current
            else current.copy(
                bkashInput = settings.bkashNumber,
                nagadInput = settings.nagadNumber,
                rocketInput = settings.rocketNumber,
                instructionsInput = settings.instructions.ifBlank { current.instructionsInput }
            )
        }
    }

    fun updateAdminBkash(number: String) {
        val sanitized = number.filter { it.isDigit() || it == '+' }.take(25)
        _paymentFormState.update {
            it.copy(bkashInput = sanitized, hasLocalEdits = true, errorMessage = null, successMessage = null)
        }
    }

    fun updateAdminNagad(number: String) {
        val sanitized = number.filter { it.isDigit() || it == '+' }.take(25)
        _paymentFormState.update {
            it.copy(nagadInput = sanitized, hasLocalEdits = true, errorMessage = null, successMessage = null)
        }
    }

    fun updateAdminRocket(number: String) {
        val sanitized = number.filter { it.isDigit() || it == '+' }.take(25)
        _paymentFormState.update {
            it.copy(rocketInput = sanitized, hasLocalEdits = true, errorMessage = null, successMessage = null)
        }
    }

    fun updateAdminPaymentInstructions(instructions: String) {
        _paymentFormState.update {
            it.copy(instructionsInput = instructions.take(200), hasLocalEdits = true, errorMessage = null, successMessage = null)
        }
    }

    fun saveAdminPaymentSettings(
        onSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val form = _paymentFormState.value
        if (form.isSaving) return

        val bkash = form.bkashInput.trim()
        val nagad = form.nagadInput.trim()
        val rocket = form.rocketInput.trim()
        if (bkash.length < 6 && nagad.length < 6 && rocket.length < 6) {
            val msg = "কমপক্ষে একটি পেমেন্ট নম্বর (bKash / Nagad / Rocket) দিন!"
            _paymentFormState.update { it.copy(errorMessage = msg) }
            onError(msg)
            return
        }

        _paymentFormState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = repository.savePaymentSettings(
                userId = currentUserId,
                bkashNumber = bkash,
                nagadNumber = nagad,
                rocketNumber = rocket,
                instructions = form.instructionsInput
            )
            result.onSuccess {
                val msg = "পেমেন্ট নম্বর সফলভাবে সেভ হয়েছে!"
                _paymentFormState.update {
                    it.copy(isSaving = false, hasLocalEdits = false, successMessage = msg)
                }
                onSaved(msg)
            }.onFailure { err ->
                val msg = unwrapFirestoreMessage(err, "শুধুমাত্র অ্যাডমিন পেমেন্ট নম্বর পরিবর্তন করতে পারবেন।")
                _paymentFormState.update { it.copy(isSaving = false, errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun completeOrder(order: FirestoreOrder) {
        viewModelScope.launch {
            repository.completeOrder(currentUserId, order.id).onFailure { error ->
                _streamError.value = unwrapFirestoreMessage(error, "অর্ডার আপডেট করা যায়নি।")
            }
        }
    }

    fun openCheckoutReceipt() {
        _showCheckoutReceipt.value = true
    }

    fun dismissCheckoutReceipt() {
        _showCheckoutReceipt.value = false
    }

    // Analyzer controls
    fun setAnalyzerMode(mode: AnalyzerMode) {
        _analyzerState.update { it.copy(mode = mode) }
    }

    fun setCalculatorTarget(target: CalculatorTarget) {
        _analyzerState.update { it.copy(calculatorTarget = target) }
    }

    fun updateCustomTitle(title: String) {
        _analyzerState.update { it.copy(customTitle = title) }
    }

    fun updatePriceInput(price: String) {
        val sanitized = price.filter { it.isDigit() || it == '.' }
        _analyzerState.update { it.copy(priceInput = sanitized) }
    }

    fun updateDiscountInput(discount: String) {
        val sanitized = discount.filter { it.isDigit() || it == '.' }
        _analyzerState.update { it.copy(discountInput = sanitized) }
    }

    fun updateBuyPriceInput(buyPrice: String) {
        val sanitized = buyPrice.filter { it.isDigit() || it == '.' }
        _analyzerState.update { it.copy(buyPriceInput = sanitized) }
    }

    fun loadOfferIntoCalculator(offer: OfferModel) {
        _analyzerState.update {
            it.copy(
                mode = AnalyzerMode.CALCULATOR,
                customTitle = offer.title,
                priceInput = offer.numericPrice.toInt().toString(),
                discountInput = offer.discountPercentValue.toString(),
                buyPriceInput = offer.numericBuyPrice.toInt().toString()
            )
        }
    }

    fun selectCompareOfferA(offerId: String) {
        _analyzerState.update { it.copy(compareOfferAId = offerId) }
    }

    fun selectCompareOfferB(offerId: String) {
        _analyzerState.update { it.copy(compareOfferBId = offerId) }
    }

    // Admin Panel controls
    fun updateAdminTitle(title: String) {
        _adminState.update {
            it.copy(titleInput = title.take(200), validationError = null, publishSuccessMessage = null)
        }
    }

    fun updateAdminPrice(price: String) {
        val sanitized = price.filter { it.isDigit() || it == '.' }.take(30)
        _adminState.update {
            it.copy(priceInput = sanitized, validationError = null, publishSuccessMessage = null)
        }
    }

    fun updateAdminBuyPrice(buyPrice: String) {
        val sanitized = buyPrice.filter { it.isDigit() || it == '.' }.take(30)
        _adminState.update {
            it.copy(buyPriceInput = sanitized, validationError = null, publishSuccessMessage = null)
        }
    }

    fun updateAdminDiscount(discount: String) {
        _adminState.update {
            it.copy(discountInput = discount.take(60), validationError = null, publishSuccessMessage = null)
        }
    }

    fun autoFillAdminDiscountTag() {
        val current = _adminState.value
        val orig = OfferModel.parseCurrency(current.priceInput)
        val buy = OfferModel.parseCurrency(current.buyPriceInput)
        if (orig > 0.0 && buy >= 0.0 && orig >= buy) {
            val diff = (orig - buy).roundToInt()
            _adminState.update {
                it.copy(discountInput = "discount: ৳$diff", validationError = null)
            }
        } else {
            _adminState.update {
                it.copy(validationError = "অটো ডিসকাউন্ট হিসাব করতে সঠিক মূল দাম ও ছাড়ের দাম দিন।")
            }
        }
    }

    fun updateAdminCategory(category: OfferCategory) {
        if (category == OfferCategory.ALL) return
        _adminState.update {
            it.copy(selectedCategory = category, validationError = null, publishSuccessMessage = null)
        }
    }

    fun updateAdminBadge(badge: String) {
        _adminState.update {
            it.copy(badgeInput = badge.take(40), validationError = null, publishSuccessMessage = null)
        }
    }

    fun updateAdminDescription(description: String) {
        _adminState.update {
            it.copy(descriptionInput = description.take(500), validationError = null, publishSuccessMessage = null)
        }
    }

    fun toggleAdminFeatured(featured: Boolean) {
        _adminState.update { it.copy(featured = featured) }
    }

    fun dismissAdminFeedback() {
        _adminState.update { it.copy(validationError = null, publishSuccessMessage = null) }
    }

    fun loadOfferForAdminEdit(offer: OfferModel) {
        _adminState.update {
            AdminPanelState(
                editingOfferId = offer.id,
                titleInput = offer.title,
                priceInput = offer.price,
                buyPriceInput = offer.buyPrice,
                discountInput = offer.discount,
                selectedCategory = if (offer.category == OfferCategory.ALL) OfferCategory.INTERNET else offer.category,
                badgeInput = offer.badge,
                descriptionInput = offer.description,
                featured = offer.isFeatured
            )
        }
    }

    fun cancelAdminEdit() {
        _adminState.update { AdminPanelState(selectedCategory = it.selectedCategory) }
    }

    fun publishAdminOffer(
        onPublished: (String) -> Unit = {},
        onValidationFailed: (String) -> Unit = {}
    ) {
        val form = _adminState.value
        if (form.isPublishing) return

        val title = form.titleInput.trim()
        val price = form.priceInput.trim()
        val buyPrice = form.buyPriceInput.trim()
        val origVal = OfferModel.parseCurrency(price)
        val buyVal = OfferModel.parseCurrency(buyPrice)
        val discount = form.discountInput.trim().ifBlank {
            if (origVal > 0.0 && origVal >= buyVal) {
                "discount: ৳${(origVal - buyVal).roundToInt()}"
            } else {
                ""
            }
        }

        val error = when {
            title.isEmpty() || price.isEmpty() || buyPrice.isEmpty() -> "সবগুলো ঘর পূরণ করুন!"
            title.length < 2 -> "অফারের বিবরণ কমপক্ষে ২ অক্ষরের হতে হবে।"
            origVal <= 0.0 -> "সঠিক মূল দাম লিখুন (যেমন: 898)।"
            buyVal <= 0.0 -> "ছাড়ের পর সঠিক দাম লিখুন (যেমন: 748)।"
            buyVal > origVal -> "ছাড়ের পর দাম মূল দামের চেয়ে বেশি হতে পারবে না।"
            discount.isBlank() -> "ডিসকাউন্ট ট্যাগ লিখুন (যেমন: discount: ৳150)।"
            else -> null
        }

        if (error != null) {
            _adminState.update { it.copy(validationError = error) }
            onValidationFailed(error)
            return
        }

        val editingId = form.editingOfferId
        val badge = form.badgeInput.trim()
        val description = form.descriptionInput.trim().ifBlank {
            "Admin published telecom drive offer · Valid across ${badge.ifBlank { "Bangladesh" }}."
        }

        _adminState.update { it.copy(isPublishing = true, validationError = null, publishSuccessMessage = null) }
        viewModelScope.launch {
            val result = if (editingId != null) {
                repository.updateOffer(
                    userId = currentUserId,
                    offerId = editingId,
                    title = title,
                    price = price,
                    buyPrice = buyPrice,
                    discount = discount,
                    category = form.selectedCategory,
                    badge = badge,
                    description = description,
                    featured = form.featured
                )
            } else {
                repository.publishOffer(
                    userId = currentUserId,
                    title = title,
                    price = price,
                    buyPrice = buyPrice,
                    discount = discount,
                    category = form.selectedCategory,
                    badge = badge,
                    description = description,
                    featured = form.featured,
                    sortOrder = 0L
                )
            }
            result.onSuccess {
                val successMsg = if (editingId != null) {
                    "অফার সফলভাবে আপডেট হয়েছে!"
                } else {
                    "অফার সফলভাবে পাবলিশ হয়েছে!"
                }
                _adminState.update {
                    AdminPanelState(
                        selectedCategory = form.selectedCategory,
                        publishSuccessMessage = successMsg
                    )
                }
                onPublished(successMsg)
            }.onFailure { err ->
                val message = unwrapFirestoreMessage(err, "অফার পাবলিশ করা যায়নি।")
                _adminState.update {
                    it.copy(
                        isPublishing = false,
                        validationError = message
                    )
                }
                onValidationFailed(message)
            }
        }
    }

    fun deleteAdminOffer(offer: OfferModel) {
        viewModelScope.launch {
            repository.deleteOffer(currentUserId, offer.id).onFailure { err ->
                _adminState.update {
                    it.copy(validationError = unwrapFirestoreMessage(err, "অফার ডিলিট করা যায়নি।"))
                }
            }
        }
    }

    private companion object {
        const val TAG = "OfferVaultVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
