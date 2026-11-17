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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.OfferModel
import com.example.ui.components.OfferCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalOfferSemanticPalette
import com.example.viewmodel.AnalyzerMode
import com.example.viewmodel.AnalyzerState
import com.example.viewmodel.CalculatorTarget
import kotlin.math.roundToInt

@Composable
fun AnalyzerScreen(
    state: AnalyzerState,
    allOffers: List<OfferModel>,
    onModeChange: (AnalyzerMode) -> Unit,
    onCalculatorTargetChange: (CalculatorTarget) -> Unit,
    onTitleChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onBuyPriceChange: (String) -> Unit,
    onSelectCompareA: (String) -> Unit,
    onSelectCompareB: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalOfferSemanticPalette.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.analyzer_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.analyzer_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnalyzerMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.mode == mode,
                            onClick = { onModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AnalyzerMode.entries.size
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (mode == AnalyzerMode.CALCULATOR) {
                                        Icons.Rounded.Calculate
                                    } else {
                                        Icons.Rounded.CompareArrows
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("analyzer_mode_${mode.name.lowercase()}")
                        ) {
                            Text(mode.label)
                        }
                    }
                }
            }

            if (state.mode == AnalyzerMode.CALCULATOR) {
                item {
                    val previewOffer = state.buildPreviewOffer()

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Target selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CalculatorTarget.entries.forEach { target ->
                                FilterChip(
                                    selected = state.calculatorTarget == target,
                                    onClick = { onCalculatorTargetChange(target) },
                                    label = { Text(target.label) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("calc_target_${target.name.lowercase()}")
                                )
                            }
                        }

                        // Inputs Card
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.large)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.customTitle,
                                    onValueChange = onTitleChange,
                                    label = { Text("Offer Title") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("calc_title_input")
                                )

                                OutlinedTextField(
                                    value = state.priceInput,
                                    onValueChange = onPriceChange,
                                    label = { Text("Original Price (৳)") },
                                    prefix = { Text("৳") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("calc_price_input")
                                )

                                if (state.calculatorTarget == CalculatorTarget.SOLVE_BUY_PRICE) {
                                    val sliderPct = (state.discountInput.toFloatOrNull() ?: 0f).coerceIn(0f, 90f)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Discount Percentage",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = "${sliderPct.roundToInt()}% OFF",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = sliderPct,
                                            onValueChange = { onDiscountChange(it.roundToInt().toString()) },
                                            valueRange = 0f..90f,
                                            modifier = Modifier.testTag("calc_discount_slider")
                                        )
                                        OutlinedTextField(
                                            value = state.discountInput,
                                            onValueChange = onDiscountChange,
                                            label = { Text("Discount (%)") },
                                            suffix = { Text("%") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("calc_discount_input")
                                        )
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = state.buyPriceInput,
                                        onValueChange = onBuyPriceChange,
                                        label = { Text("Target Buy Price (৳)") },
                                        prefix = { Text("৳") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("calc_buy_price_input")
                                    )
                                }
                            }
                        }

                        // Live OfferModel Preview
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live OfferModel Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        OfferCard(
                            offer = previewOffer,
                            onCardClick = {},
                            onBookmarkClick = {},
                            onBuyClick = {},
                            onIncrementBag = {},
                            onDecrementBag = {},
                            showBagActions = false
                        )

                        // Unit vs Bulk Breakdown
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = semantics.savingsContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BreakdownMetric(
                                    label = "UNIT SAVINGS",
                                    value = previewOffer.formattedSavings,
                                    color = semantics.onSavingsContainer
                                )
                                BreakdownMetric(
                                    label = "3x BUNDLE TOTAL",
                                    value = OfferModel.formatCurrency(previewOffer.numericBuyPrice * 3),
                                    color = semantics.onSavingsContainer
                                )
                                BreakdownMetric(
                                    label = "3x SAVED",
                                    value = OfferModel.formatCurrency(previewOffer.savingsAmount * 3),
                                    color = semantics.onSavingsContainer
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    CompareOffersSection(
                        allOffers = allOffers,
                        offerAId = state.compareOfferAId,
                        offerBId = state.compareOfferBId,
                        onSelectOfferA = onSelectCompareA,
                        onSelectOfferB = onSelectCompareB
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

@Composable
private fun CompareOffersSection(
    allOffers: List<OfferModel>,
    offerAId: String,
    offerBId: String,
    onSelectOfferA: (String) -> Unit,
    onSelectOfferB: (String) -> Unit
) {
    val semantics = LocalOfferSemanticPalette.current
    if (allOffers.size < 2) return

    val offerA = allOffers.firstOrNull { it.id == offerAId } ?: allOffers[0]
    val offerB = allOffers.firstOrNull { it.id == offerBId } ?: allOffers[1]

    val winner = when {
        offerA.discountPercentValue > offerB.discountPercentValue -> offerA
        offerB.discountPercentValue > offerA.discountPercentValue -> offerB
        offerA.savingsAmount >= offerB.savingsAmount -> offerA
        else -> offerB
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OfferPickerDropdown(
                label = "OFFER A",
                selectedOffer = offerA,
                offers = allOffers,
                onSelect = onSelectOfferA,
                modifier = Modifier
                    .weight(1f)
                    .testTag("compare_picker_a")
            )
            OfferPickerDropdown(
                label = "OFFER B",
                selectedOffer = offerB,
                offers = allOffers,
                onSelect = onSelectOfferB,
                modifier = Modifier
                    .weight(1f)
                    .testTag("compare_picker_b")
            )
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.large)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompareMetricRow("Original Price", offerA.formattedOriginalPrice, offerB.formattedOriginalPrice)
                HorizontalDivider(color = semantics.cardBorder)
                CompareMetricRow(
                    label = "Buy Price",
                    valueA = offerA.formattedBuyPrice,
                    valueB = offerB.formattedBuyPrice,
                    highlightA = offerA.numericBuyPrice <= offerB.numericBuyPrice,
                    highlightB = offerB.numericBuyPrice < offerA.numericBuyPrice
                )
                HorizontalDivider(color = semantics.cardBorder)
                CompareMetricRow(
                    label = "Discount",
                    valueA = offerA.discount,
                    valueB = offerB.discount,
                    highlightA = offerA.discountPercentValue >= offerB.discountPercentValue,
                    highlightB = offerB.discountPercentValue > offerA.discountPercentValue
                )
                HorizontalDivider(color = semantics.cardBorder)
                CompareMetricRow(
                    label = "Net Savings",
                    valueA = offerA.formattedSavings,
                    valueB = offerB.formattedSavings,
                    highlightA = offerA.savingsAmount >= offerB.savingsAmount,
                    highlightB = offerB.savingsAmount > offerA.savingsAmount
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = semantics.savingsContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = semantics.onSavingsContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "STRONGER DISCOUNT VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantics.onSavingsContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${winner.title} (${winner.discount}, save ${winner.formattedSavings})",
                        style = MaterialTheme.typography.titleSmall,
                        color = semantics.onSavingsContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferPickerDropdown(
    label: String,
    selectedOffer: OfferModel,
    offers: List<OfferModel>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val semantics = LocalOfferSemanticPalette.current

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.medium)
                .minimumInteractiveComponentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedOffer.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select $label"
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            offers.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.title} (${option.formattedBuyPrice})") },
                    onClick = {
                        onSelect(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CompareMetricRow(
    label: String,
    valueA: String,
    valueB: String,
    highlightA: Boolean = false,
    highlightB: Boolean = false
) {
    val semantics = LocalOfferSemanticPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueA,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = if (highlightA) semantics.savingsAccent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueB,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = if (highlightB) semantics.savingsAccent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
