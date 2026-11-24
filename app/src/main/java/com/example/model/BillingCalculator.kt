package com.example.model

import kotlin.math.max
import kotlin.math.round

data class BillingBreakdown(
    val nights: Int,
    val roomCharges: Double,
    val taxableExtraCharges: Double,
    val exemptExtraCharges: Double,
    val extraChargesTotal: Double,
    val discount: Double,
    val taxableAmount: Double,
    val gstRate: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val grandTotal: Double,
    val balanceDue: Double
)

object BillingCalculator {

    /**
     * Computes Indian hospitality GST breakdown using configurable slabs from HotelSettings.
     * Supports exempt vs taxable extra charges and Intra-State (CGST+SGST) vs Inter-State (IGST).
     */
    fun calculate(
        nightlyRate: Double,
        nights: Int,
        roomCount: Int = 1,
        extraCharges: List<ExtraChargeItem> = emptyList(),
        discount: Double = 0.0,
        settings: HotelSettings,
        totalPaid: Double = 0.0
    ): BillingBreakdown {
        val safeNights = max(1, nights)
        val safeRooms = max(1, roomCount)
        val roomCharges = round2(nightlyRate * safeNights * safeRooms)
        val taxableExtras = round2(extraCharges.filter { it.taxable }.sumOf { it.amount })
        val exemptExtras = round2(extraCharges.filter { !it.taxable }.sumOf { it.amount })
        val totalExtras = round2(taxableExtras + exemptExtras)
        val safeDiscount = discount.coerceIn(0.0, roomCharges + taxableExtras)

        val taxableAmount = round2(max(0.0, roomCharges + taxableExtras - safeDiscount))

        val applicableGstRate = if (!settings.gstEnabled) {
            0.0
        } else if (nightlyRate > 7500.0) {
            settings.gstSlabAbove7500.coerceIn(0.0, 28.0)
        } else {
            settings.gstSlabUnder7500.coerceIn(0.0, 28.0)
        }

        val totalTax = round2(taxableAmount * (applicableGstRate / 100.0))
        val mode = GstMode.fromRaw(settings.gstMode)
        val cgst = if (mode == GstMode.INTRA_STATE) round2(totalTax / 2.0) else 0.0
        val sgst = if (mode == GstMode.INTRA_STATE) round2(totalTax - cgst) else 0.0
        val igst = if (mode == GstMode.INTER_STATE) totalTax else 0.0

        val grandTotal = round2(taxableAmount + exemptExtras + cgst + sgst + igst)
        val balanceDue = round2(max(0.0, grandTotal - totalPaid))

        return BillingBreakdown(
            nights = safeNights,
            roomCharges = roomCharges,
            taxableExtraCharges = taxableExtras,
            exemptExtraCharges = exemptExtras,
            extraChargesTotal = totalExtras,
            discount = round2(safeDiscount),
            taxableAmount = taxableAmount,
            gstRate = applicableGstRate,
            cgst = cgst,
            sgst = sgst,
            igst = igst,
            grandTotal = grandTotal,
            balanceDue = balanceDue
        )
    }

    fun buildUpiPaymentUri(
        upiId: String,
        merchantName: String,
        amount: Double,
        bookingCode: String
    ): String {
        val encodedMerchant = java.net.URLEncoder.encode(merchantName.ifBlank { "Smart Hotel" }, "UTF-8")
        val encodedNote = java.net.URLEncoder.encode("Hotel Booking $bookingCode", "UTF-8")
        val amtStr = String.format(java.util.Locale.ENGLISH, "%.2f", max(0.0, amount))
        return "upi://pay?pa=${upiId.trim()}&pn=$encodedMerchant&am=$amtStr&cu=INR&tn=$encodedNote"
    }

    fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
