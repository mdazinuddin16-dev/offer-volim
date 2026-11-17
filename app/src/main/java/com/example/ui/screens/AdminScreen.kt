package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.remote.FirestoreOrder
import com.example.model.OfferCategory
import com.example.model.OfferModel
import com.example.ui.components.OfferCard
import com.example.ui.components.categoryIcon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOfferSemanticPalette
import com.example.viewmodel.AdminPanelState
import com.example.viewmodel.AdminPaymentSettingsState

private val AdminPlum = Color(0xFF4A148C)
private val AdminPlumContainer = Color(0xFFF3E5F5)

@Composable
fun AdminScreen(
    state: AdminPanelState,
    paymentFormState: AdminPaymentSettingsState,
    publishedOffers: List<OfferModel>,
    recentOrders: List<FirestoreOrder>,
    onBkashChange: (String) -> Unit,
    onNagadChange: (String) -> Unit,
    onRocketChange: (String) -> Unit,
    onPaymentInstructionsChange: (String) -> Unit,
    onSavePaymentSettings: () -> Unit,
    onTitleChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onBuyPriceChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onAutoFillDiscount: () -> Unit,
    onCategorySelected: (OfferCategory) -> Unit,
    onBadgeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onToggleFeatured: (Boolean) -> Unit,
    onPublishOffer: () -> Unit,
    onEditOffer: (OfferModel) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteOffer: (OfferModel) -> Unit,
    onOfferClick: (OfferModel) -> Unit,
    onCompleteOrder: (FirestoreOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalOfferSemanticPalette.current
    val adminCategories = OfferCategory.entries.filter { it != OfferCategory.ALL }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize()
                .testTag("admin_panel_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Royal Plum Admin Banner (#4A148C)
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = AdminPlum,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "অ্যাডমিন প্যানেল - নতুন অফার যোগ করুন",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Set payment numbers, publish offers, and verify customer TrxID orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Admin Payment Numbers Configuration Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.extraLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AdminPlum
                            )
                            Text(
                                text = "অ্যাডমিন পেমেন্ট নম্বর সেটিংস",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "কাস্টমার অফার কেনার সময় এখানে দেওয়া নম্বরগুলোতে টাকা পাঠিয়ে Transaction ID বসাবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = paymentFormState.bkashInput,
                                onValueChange = onBkashChange,
                                label = { Text("bKash নম্বর") },
                                placeholder = { Text("017XXXXXXXX") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_bkash_input")
                            )

                            OutlinedTextField(
                                value = paymentFormState.nagadInput,
                                onValueChange = onNagadChange,
                                label = { Text("Nagad নম্বর") },
                                placeholder = { Text("018XXXXXXXX") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_nagad_input")
                            )
                        }

                        OutlinedTextField(
                            value = paymentFormState.rocketInput,
                            onValueChange = onRocketChange,
                            label = { Text("Rocket নম্বর (ঐচ্ছিক)") },
                            placeholder = { Text("019XXXXXXXX") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_rocket_input")
                        )

                        OutlinedTextField(
                            value = paymentFormState.instructionsInput,
                            onValueChange = onPaymentInstructionsChange,
                            label = { Text("পেমেন্ট নির্দেশনা") },
                            singleLine = false,
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_payment_instructions_input")
                        )

                        if (paymentFormState.errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = paymentFormState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (paymentFormState.successMessage != null) {
                            Surface(
                                color = semantics.savingsContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = semantics.onSavingsContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = paymentFormState.successMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantics.onSavingsContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onSavePaymentSettings,
                            enabled = !paymentFormState.isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = semantics.savingsAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_save_payment_button")
                        ) {
                            if (paymentFormState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("সেভ হচ্ছে…")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "পেমেন্ট নম্বর সেভ করুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Admin Offer Creation Form
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.extraLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.isEditing) {
                            Surface(
                                color = AdminPlumContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "অফার এডিট মোড চালু আছে",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AdminPlum,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = onCancelEdit,
                                        modifier = Modifier.testTag("admin_cancel_edit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel")
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.titleInput,
                            onValueChange = onTitleChange,
                            label = { Text("অফারের বিবরণ (যেমন: H > 50 জিবি...)") },
                            placeholder = { Text("H > 150 জিবি 30 দিন 898৳ অল বিডি সবাই পাবে") },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_title_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = state.priceInput,
                                onValueChange = onPriceChange,
                                label = { Text("মূল দাম (যেমন: 898)") },
                                prefix = { Text("৳ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_price_input")
                            )

                            OutlinedTextField(
                                value = state.buyPriceInput,
                                onValueChange = onBuyPriceChange,
                                label = { Text("ছাড়ের পর দাম (যেমন: 748)") },
                                prefix = { Text("৳ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_buy_price_input")
                            )
                        }

                        OutlinedTextField(
                            value = state.discountInput,
                            onValueChange = onDiscountChange,
                            label = { Text("ডিসকাউন্ট ট্যাগ (যেমন: discount: ৳150)") },
                            placeholder = { Text("discount: ৳150") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                TextButton(
                                    onClick = onAutoFillDiscount,
                                    modifier = Modifier.testTag("admin_auto_discount_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto ৳")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_discount_input")
                        )

                        Text(
                            text = "ক্যাটাগরি",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(adminCategories) { category ->
                                FilterChip(
                                    selected = state.selectedCategory == category,
                                    onClick = { onCategorySelected(category) },
                                    label = { Text(category.label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = categoryIcon(category),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("admin_category_${category.name.lowercase()}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.badgeInput,
                            onValueChange = onBadgeChange,
                            label = { Text("রিজিওন / ব্যাজ (ঐচ্ছিক)") },
                            placeholder = { Text("All BD / Barishal / Dhaka") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_badge_input")
                        )

                        OutlinedTextField(
                            value = state.descriptionInput,
                            onValueChange = onDescriptionChange,
                            label = { Text("অতিরিক্ত নোট (ঐচ্ছিক)") },
                            singleLine = false,
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_description_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Spotlight in Hero Banner",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Feature this offer at the top of the Deals tab",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.featured,
                                onCheckedChange = onToggleFeatured,
                                modifier = Modifier.testTag("admin_featured_switch")
                            )
                        }

                        if (state.validationError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.validationError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (state.publishSuccessMessage != null) {
                            Surface(
                                color = semantics.savingsContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = semantics.onSavingsContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = state.publishSuccessMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantics.onSavingsContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onPublishOffer,
                            enabled = !state.isPublishing,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AdminPlum,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("admin_upload_offer_button")
                        ) {
                            if (state.isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("পাবলিশ হচ্ছে…")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isEditing) "অফার আপডেট করুন" else "অফার পাবলিশ করুন",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Live Offer Card Preview
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "লাইভ প্রিভিউ",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OfferCard(
                        offer = state.previewOffer(),
                        onCardClick = {},
                        onBookmarkClick = {},
                        onBuyClick = {},
                        onIncrementBag = {},
                        onDecrementBag = {},
                        showBagActions = false
                    )
                }
            }

            // Manage Published Offers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পাবলিশড অফারসমূহ (${publishedOffers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        color = AdminPlumContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Cloud Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminPlum,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(
                items = publishedOffers,
                key = { "admin_offer_${it.id}" }
            ) { offer ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.large),
                    onClick = { onOfferClick(offer) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = offer.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Buy ${offer.formattedBuyPrice}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = semantics.savingsAccent
                                )
                                Text(
                                    text = "· ${offer.discount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onEditOffer(offer) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_edit_offer_${offer.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit ${offer.title}",
                                    tint = AdminPlum
                                )
                            }
                            IconButton(
                                onClick = { onDeleteOffer(offer) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_delete_offer_${offer.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete ${offer.title}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Recent Orders Log with Payment Method & TrxID
            if (recentOrders.isNotEmpty()) {
                item {
                    Text(
                        text = "সাম্প্রতিক অর্ডার ও ট্রানজেকশন (${recentOrders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(
                    items = recentOrders.take(15),
                    key = { "admin_order_${it.id}" }
                ) { order ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.large)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = AdminPlum
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${order.quantity}× ${order.offerTitle}",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${order.paymentMethod} → ${order.adminPaymentNumber} · Customer: ${order.customerPhone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = OfferModel.formatCurrency(order.totalPaid),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = semantics.savingsAccent
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AdminPlumContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "TrxID: ${order.transactionId} · ${order.status}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AdminPlum,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (order.status != "COMPLETED") {
                                    TextButton(
                                        onClick = { onCompleteOrder(order) },
                                        modifier = Modifier.testTag("admin_complete_order_${order.id}")
                                    ) {
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
