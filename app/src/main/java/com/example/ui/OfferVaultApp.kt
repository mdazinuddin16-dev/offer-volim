package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.PendingRegistrationStore
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.OfferRepository
import com.example.model.OfferModel
import com.example.ui.components.OfferDetailSheet
import com.example.ui.components.PaymentCheckoutSheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BagScreen
import com.example.ui.screens.CompleteRegistrationScreen
import com.example.ui.screens.DealsScreen
import com.example.viewmodel.OfferVaultViewModel
import com.example.viewmodel.UserProfileGateState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_DEALS = "deals"
const val ROUTE_ADMIN = "admin"
const val ROUTE_ANALYZER = "analyzer"
const val ROUTE_BAG = "bag"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    NavDestination(
        route = ROUTE_DEALS,
        labelRes = R.string.nav_deals,
        selectedIcon = Icons.Rounded.LocalOffer,
        unselectedIcon = Icons.Outlined.LocalOffer,
        testTag = "nav_deals_tab"
    ),
    NavDestination(
        route = ROUTE_ADMIN,
        labelRes = R.string.nav_admin,
        selectedIcon = Icons.Rounded.AdminPanelSettings,
        unselectedIcon = Icons.Outlined.AdminPanelSettings,
        testTag = "nav_admin_tab"
    ),
    NavDestination(
        route = ROUTE_ANALYZER,
        labelRes = R.string.nav_analyzer,
        selectedIcon = Icons.Rounded.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_analyzer_tab"
    ),
    NavDestination(
        route = ROUTE_BAG,
        labelRes = R.string.nav_bag,
        selectedIcon = Icons.Rounded.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        testTag = "nav_bag_tab"
    )
)

@Composable
fun OfferVaultApp() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedOfferVaultApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName.orEmpty(),
                userEmail = user.email.orEmpty()
            )
        }
    }
}

@Composable
private fun AuthenticatedOfferVaultApp(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String
) {
    val viewModel: OfferVaultViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                OfferVaultViewModel(OfferRepository(db), currentUserId)
            }
        }
    )

    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val isRegistering by viewModel.isRegisteringProfile.collectAsStateWithLifecycle()
    val registrationError by viewModel.registrationError.collectAsStateWithLifecycle()

    LaunchedEffect(profileState) {
        if (profileState is UserProfileGateState.NeedsRegistration) {
            val pending = PendingRegistrationStore.consume()
            if (pending != null) {
                viewModel.submitUserRegistration(
                    fullName = pending.fullName,
                    phone = pending.phone,
                    division = pending.division,
                    email = userEmail,
                    onComplete = {
                        Toast.makeText(context, "রেজিস্ট্রেশন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    when (val gate = profileState) {
        UserProfileGateState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        UserProfileGateState.NeedsRegistration -> {
            CompleteRegistrationScreen(
                initialName = userDisplayName,
                email = userEmail,
                isSubmitting = isRegistering,
                errorMessage = registrationError,
                onSubmitRegistration = { fullName, phone, division ->
                    viewModel.submitUserRegistration(
                        fullName = fullName,
                        phone = phone,
                        division = division,
                        email = userEmail,
                        onComplete = {
                            Toast.makeText(context, "রেজিস্ট্রেশন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onSignOut = { signOutUser(credentialManager, scope) }
            )
            return
        }

        is UserProfileGateState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = gate.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { signOutUser(credentialManager, scope) }) {
                        Text("Back to Login")
                    }
                }
            }
            return
        }

        is UserProfileGateState.Registered -> Unit
    }

    val dealsState by viewModel.dealsState.collectAsStateWithLifecycle()
    val bagState by viewModel.bagState.collectAsStateWithLifecycle()
    val analyzerState by viewModel.analyzerState.collectAsStateWithLifecycle()
    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val paymentSettings by viewModel.paymentSettingsState.collectAsStateWithLifecycle()
    val paymentFormState by viewModel.paymentFormState.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()

    LaunchedEffect(paymentSettings) {
        viewModel.syncPaymentFormFromSettings(paymentSettings)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DEALS

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val startPaymentCheckout: (OfferModel, Int) -> Unit = { offer, quantity ->
        viewModel.openPaymentCheckout(offer, quantity)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToRoute(dest.route) },
                                icon = {
                                    NavTabIcon(
                                        destination = dest,
                                        selected = selected,
                                        bagCount = dealsState.bagCount
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(dest.route) },
                                icon = {
                                    NavTabIcon(
                                        destination = dest,
                                        selected = selected,
                                        bagCount = dealsState.bagCount
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DEALS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DEALS) {
                        DealsScreen(
                            state = dealsState,
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onCategorySelected = viewModel::onCategorySelected,
                            onSortOptionSelected = viewModel::onSortOptionSelected,
                            onToggleSavedOnly = viewModel::onToggleSavedOnly,
                            onClearFilters = viewModel::clearFilters,
                            onOfferClick = viewModel::openOfferDetail,
                            onBookmarkClick = viewModel::toggleBookmark,
                            onBuyClick = { offer -> startPaymentCheckout(offer, 1) },
                            onIncrementBag = viewModel::incrementBagQuantity,
                            onDecrementBag = viewModel::decrementBagQuantity,
                            onDismissError = viewModel::dismissError,
                            onSignOut = { signOutUser(credentialManager, scope) }
                        )
                    }

                    composable(ROUTE_ADMIN) {
                        AdminScreen(
                            state = adminState,
                            paymentFormState = paymentFormState,
                            publishedOffers = dealsState.allOffers,
                            recentOrders = ordersState,
                            onBkashChange = viewModel::updateAdminBkash,
                            onNagadChange = viewModel::updateAdminNagad,
                            onRocketChange = viewModel::updateAdminRocket,
                            onPaymentInstructionsChange = viewModel::updateAdminPaymentInstructions,
                            onSavePaymentSettings = {
                                viewModel.saveAdminPaymentSettings(
                                    onSaved = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onTitleChange = viewModel::updateAdminTitle,
                            onPriceChange = viewModel::updateAdminPrice,
                            onBuyPriceChange = viewModel::updateAdminBuyPrice,
                            onDiscountChange = viewModel::updateAdminDiscount,
                            onAutoFillDiscount = viewModel::autoFillAdminDiscountTag,
                            onCategorySelected = viewModel::updateAdminCategory,
                            onBadgeChange = viewModel::updateAdminBadge,
                            onDescriptionChange = viewModel::updateAdminDescription,
                            onToggleFeatured = viewModel::toggleAdminFeatured,
                            onPublishOffer = {
                                viewModel.publishAdminOffer(
                                    onPublished = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onValidationFailed = { errMsg ->
                                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onEditOffer = viewModel::loadOfferForAdminEdit,
                            onCancelEdit = viewModel::cancelAdminEdit,
                            onDeleteOffer = viewModel::deleteAdminOffer,
                            onOfferClick = viewModel::openOfferDetail,
                            onCompleteOrder = viewModel::completeOrder
                        )
                    }

                    composable(ROUTE_ANALYZER) {
                        AnalyzerScreen(
                            state = analyzerState,
                            allOffers = dealsState.allOffers,
                            onModeChange = viewModel::setAnalyzerMode,
                            onCalculatorTargetChange = viewModel::setCalculatorTarget,
                            onTitleChange = viewModel::updateCustomTitle,
                            onPriceChange = viewModel::updatePriceInput,
                            onDiscountChange = viewModel::updateDiscountInput,
                            onBuyPriceChange = viewModel::updateBuyPriceInput,
                            onSelectCompareA = viewModel::selectCompareOfferA,
                            onSelectCompareB = viewModel::selectCompareOfferB
                        )
                    }

                    composable(ROUTE_BAG) {
                        BagScreen(
                            state = bagState,
                            onBrowseOffers = { navigateToRoute(ROUTE_DEALS) },
                            onOfferClick = viewModel::openOfferDetail,
                            onBookmarkClick = viewModel::toggleBookmark,
                            onBuyClick = { offer -> startPaymentCheckout(offer, offer.bagQuantity.coerceAtLeast(1)) },
                            onIncrementBag = viewModel::incrementBagQuantity,
                            onDecrementBag = viewModel::decrementBagQuantity,
                            onClearBag = viewModel::clearBag,
                            onOpenCheckoutReceipt = viewModel::openCheckoutReceipt,
                            onDismissCheckoutReceipt = viewModel::dismissCheckoutReceipt
                        )
                    }
                }
            }
        }
    }

    dealsState.selectedOfferForDetail?.let { offer ->
        OfferDetailSheet(
            offer = offer,
            onDismiss = viewModel::dismissOfferDetail,
            onToggleBookmark = { viewModel.toggleBookmark(offer) },
            onAddToBag = { qty -> viewModel.addToBagWithQuantity(offer, qty) },
            onBuyNow = { qty -> startPaymentCheckout(offer, qty) },
            onAnalyzeOffer = {
                viewModel.loadOfferIntoCalculator(offer)
                viewModel.dismissOfferDetail()
                navigateToRoute(ROUTE_ANALYZER)
            }
        )
    }

    checkoutState?.let { activeCheckout ->
        PaymentCheckoutSheet(
            state = activeCheckout,
            paymentSettings = paymentSettings,
            onSelectMethod = viewModel::selectCheckoutPaymentMethod,
            onCustomerPhoneChange = viewModel::updateCheckoutCustomerPhone,
            onTransactionIdChange = viewModel::updateCheckoutTransactionId,
            onSubmitOrder = {
                viewModel.submitCheckoutOrder(
                    onOrderSuccess = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    onValidationFailed = { errMsg ->
                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDismiss = viewModel::dismissPaymentCheckout
        )
    }
}

@Composable
private fun NavTabIcon(
    destination: NavDestination,
    selected: Boolean,
    bagCount: Int
) {
    val label = stringResource(destination.labelRes)
    val iconVector = if (selected) destination.selectedIcon else destination.unselectedIcon
    if (destination.route == ROUTE_BAG && bagCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (bagCount > 99) "99+" else bagCount.toString())
                }
            }
        ) {
            Icon(imageVector = iconVector, contentDescription = label)
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = label)
    }
}
