package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.remote.FirestorePaymentSettings
import com.example.data.remote.PaymentMethod
import com.example.model.OfferModel
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOfferSemanticPalette
import com.example.viewmodel.PaymentCheckoutState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCheckoutSheet(
    state: PaymentCheckoutState,
    paymentSettings: FirestorePaymentSettings,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onTransactionIdChange: (String) -> Unit,
    onSubmitOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantics = LocalOfferSemanticPalette.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val adminNumber = state.selectedMethod.numberFrom(paymentSettings)
    val availableMethods = paymentSettings.availableMethods()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "অফার পেমেন্ট ও ট্রানজেকশন",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.quantity}× ${state.offer.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Payable Amount Card
            Surface(
                color = semantics.savingsContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "পেমেন্ট করার পরিমাণ",
                            style = MaterialTheme.typography.labelLarge,
                            color = semantics.onSavingsContainer
                        )
                        Text(
                            text = "${state.offer.discount} · ${state.quantity} প্যাক",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantics.onSavingsContainer.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = OfferModel.formatCurrency(state.payableAmount),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = semantics.savingsAccent,
                        modifier = Modifier.testTag("checkout_payable_amount")
                    )
                }
            }

            // Payment Method Selection
            Text(
                text = "পেমেন্ট মাধ্যম বেছে নিন",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    val hasNumber = method.numberFrom(paymentSettings).length >= 6
                    FilterChip(
                        selected = state.selectedMethod == method,
                        onClick = { onSelectMethod(method) },
                        label = {
                            Text(
                                text = if (hasNumber) method.label else "${method.label} (নেই)",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("checkout_method_${method.name.lowercase()}")
                    )
                }
            }

            // Admin Payment Number Box
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.large)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "অ্যাডমিন ${state.selectedMethod.label} পেমেন্ট নম্বর",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (adminNumber.length >= 6) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = adminNumber,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("checkout_admin_number")
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(adminNumber))
                                    Toast.makeText(
                                        context,
                                        "নম্বর কপি হয়েছে: $adminNumber",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("checkout_copy_number_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy payment number",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = paymentSettings.instructions.ifBlank {
                                "উপরের নম্বরে ${OfferModel.formatCurrency(state.payableAmount)} Send Money করে নিচে Transaction ID দিন।"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (availableMethods.isEmpty()) {
                                "অ্যাডমিন এখনো কোনো পেমেন্ট নম্বর সেট করেননি। অনুগ্রহ করে Admin ট্যাব থেকে পেমেন্ট নম্বর যোগ করুন।"
                            } else {
                                "${state.selectedMethod.label} নম্বর সেট করা নেই। অন্য পেমেন্ট মাধ্যম (${availableMethods.joinToString { it.label }}) সিলেক্ট করুন।"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.customerPhoneInput,
                onValueChange = onCustomerPhoneChange,
                label = { Text("আপনার মোবাইল নম্বর (যে নম্বরে অফার নেবেন)") },
                placeholder = { Text("017XXXXXXXX") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.PhoneAndroid, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_customer_phone_input")
            )

            OutlinedTextField(
                value = state.transactionIdInput,
                onValueChange = onTransactionIdChange,
                label = { Text("ট্রানজেকশন আইডি (Transaction ID / TrxID)") },
                placeholder = { Text("যেমন: 9K7A2BX4M1") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.ReceiptLong, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_trx_id_input")
            )

            if (state.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onSubmitOrder,
                enabled = !state.isSubmitting && adminNumber.length >= 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("checkout_submit_order_button")
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অর্ডার সাবমিট হচ্ছে…")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পেমেন্ট নিশ্চিত ও অর্ডার করুন (${OfferModel.formatCurrency(state.payableAmount)})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
