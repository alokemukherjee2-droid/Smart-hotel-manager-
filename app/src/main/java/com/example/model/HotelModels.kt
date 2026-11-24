package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RoomType(val label: String, val defaultCapacity: Int, val defaultRate: Double) {
    SINGLE("Single", 1, 1800.0),
    DOUBLE("Double", 2, 2600.0),
    DELUXE("Deluxe", 2, 3800.0),
    SUPER_DELUXE("Super Deluxe", 3, 5200.0),
    FAMILY_ROOM("Family Room", 4, 6500.0),
    SUITE("Suite", 3, 9500.0),
    DORMITORY("Dormitory", 6, 900.0);

    companion object {
        fun fromRaw(raw: String?): RoomType = entries.firstOrNull { it.name == raw } ?: DELUXE
    }
}

enum class RoomStatus(val label: String) {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    RESERVED("Reserved"),
    CLEANING_REQUIRED("Cleaning Required"),
    CLEANING_IN_PROGRESS("Cleaning In Progress"),
    UNDER_MAINTENANCE("Under Maintenance"),
    BLOCKED("Blocked");

    companion object {
        fun fromRaw(raw: String?): RoomStatus = entries.firstOrNull { it.name == raw } ?: AVAILABLE
    }
}

enum class BookingSource(val label: String) {
    DIRECT("Direct Booking"),
    PHONE("Phone Booking"),
    WHATSAPP("WhatsApp"),
    WALK_IN("Walk-in"),
    WEBSITE("Website"),
    OTA("Online Travel Agency (OTA)"),
    TRAVEL_AGENT("Travel Agent");

    companion object {
        fun fromRaw(raw: String?): BookingSource = entries.firstOrNull { it.name == raw } ?: DIRECT
    }
}

enum class BookingStatus(val label: String) {
    CONFIRMED("Confirmed"),
    PENDING("Pending"),
    CHECKED_IN("Checked-in"),
    CHECKED_OUT("Checked-out"),
    CANCELLED("Cancelled"),
    NO_SHOW("No-show");

    companion object {
        fun fromRaw(raw: String?): BookingStatus = entries.firstOrNull { it.name == raw } ?: CONFIRMED
    }
}

enum class IdDocumentType(val label: String) {
    AADHAAR_MASKED("Masked Aadhaar (Optional)"),
    VOTER_ID("Voter ID"),
    DRIVING_LICENCE("Driving Licence"),
    PASSPORT("Passport"),
    OTHER_GOVT_ID("Other Govt. ID"),
    NONE("Not Provided Yet");

    companion object {
        fun fromRaw(raw: String?): IdDocumentType = entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class PaymentMode(val label: String, val isUpiGroup: Boolean = false, val isCardGroup: Boolean = false) {
    CASH("Cash"),
    UPI("UPI", isUpiGroup = true),
    GOOGLE_PAY("Google Pay", isUpiGroup = true),
    PHONEPE("PhonePe", isUpiGroup = true),
    PAYTM("Paytm", isUpiGroup = true),
    DEBIT_CARD("Debit Card", isCardGroup = true),
    CREDIT_CARD("Credit Card", isCardGroup = true),
    BANK_TRANSFER("Bank Transfer"),
    PAYMENT_GATEWAY("Payment Gateway"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): PaymentMode = entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class PaymentStatus(val label: String) {
    VERIFIED("Verified"),
    PENDING_VERIFICATION("Verification Pending"),
    REFUNDED("Refunded"),
    FAILED("Failed");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus = entries.firstOrNull { it.name == raw } ?: VERIFIED
    }
}

enum class GstMode(val label: String) {
    INTRA_STATE("Intra-State (CGST + SGST)"),
    INTER_STATE("Inter-State (IGST)");

    companion object {
        fun fromRaw(raw: String?): GstMode = entries.firstOrNull { it.name == raw } ?: INTRA_STATE
    }
}

enum class StaffRole(val label: String) {
    OWNER("Owner / Administrator"),
    MANAGER("Hotel Manager"),
    RECEPTIONIST("Receptionist"),
    HOUSEKEEPING("Housekeeping Staff");

    fun canApproveCreditException(): Boolean = this == OWNER || this == MANAGER
    fun canViewFinancialReports(): Boolean = this == OWNER || this == MANAGER
    fun canManageSettingsAndStaff(): Boolean = this == OWNER
    fun canManageBookingsAndPayments(): Boolean = this != HOUSEKEEPING
    fun canEditRoomInventory(): Boolean = this == OWNER || this == MANAGER

    companion object {
        fun fromRaw(raw: String?): StaffRole = entries.firstOrNull { it.name == raw } ?: OWNER
    }
}

@Keep
@IgnoreExtraProperties
data class ExtraChargeItem(
    val title: String = "",
    val category: String = "Food",
    val amount: Double = 0.0,
    val taxable: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "title" to title,
        "category" to category,
        "amount" to amount,
        "taxable" to taxable
    )
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val activeHotelId: String = "hotel_primary",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HotelSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "The Grand Heritage Residency",
    val address: String = "42 M.G. Road, Civil Lines",
    val city: String = "Jaipur",
    val state: String = "Rajasthan",
    val pincode: String = "302001",
    val phone: String = "+91 98290 45120",
    val email: String = "desk@grandheritageresidency.in",
    val gstin: String = "08AABCG4521K1Z5",
    val upiId: String = "grandheritage@icici",
    val upiMerchantName: String = "The Grand Heritage Residency",
    val bankName: String = "HDFC Bank Ltd.",
    val bankAccountNumber: String = "50200045891234",
    val bankIfsc: String = "HDFC0000142",
    val checkInTime: String = "02:00 PM",
    val checkOutTime: String = "11:00 AM",
    val cancellationPolicy: String = "Free cancellation up to 24 hours before check-in. 1 night tariff applies for late cancellations or no-shows.",
    val gstEnabled: Boolean = true,
    val gstSlabUnder7500: Double = 12.0,
    val gstSlabAbove7500: Double = 18.0,
    val gstMode: String = GstMode.INTRA_STATE.name,
    val invoicePrefix: String = "GHR-26",
    val nextInvoiceNumber: Long = 101L,
    val nextBookingSeq: Long = 1001L,
    val demoSeeded: Boolean = false,
    val logoUri: String? = null,
    val whatsappTemplateBooking: String = "Namaste {{guestName}}, your booking {{bookingCode}} at {{hotelName}} for Room {{roomNumber}} ({{checkIn}} to {{checkOut}}) is confirmed. Total: {{total}}. Balance: {{balance}}.",
    val whatsappTemplateAdvance: String = "Namaste {{guestName}}, we have received your advance payment of {{amount}} for booking {{bookingCode}}. Remaining balance: {{balance}}.",
    val whatsappTemplateCheckIn: String = "Welcome to {{hotelName}}, {{guestName}}! Check-in starts at {{checkInTime}}. Please carry a valid Govt. photo ID (Aadhaar optional).",
    val whatsappTemplateCheckOut: String = "Namaste {{guestName}}, friendly reminder that checkout for Room {{roomNumber}} is at {{checkOutTime}}. Let reception know if you need assistance.",
    val whatsappTemplateReceipt: String = "Thank you for staying at {{hotelName}}, {{guestName}}! Invoice {{invoiceNumber}} for {{total}} has been generated. We look forward to welcoming you again.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Room(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val roomNumber: String = "",
    val floor: String = "1st Floor",
    val roomType: String = RoomType.DELUXE.name,
    val capacity: Long = 2L,
    val basePrice: Double = 3500.0,
    val extraPersonCharge: Double = 750.0,
    val seasonalPrice: Double = 4200.0,
    val seasonalActive: Boolean = false,
    val status: String = RoomStatus.AVAILABLE.name,
    val amenities: List<String> = emptyList(),
    val photoUri: String? = null,
    val assignedStaffName: String? = null,
    val housekeepingNotes: String? = null,
    val maintenanceIssue: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveNightlyRate(): Double = if (seasonalActive && seasonalPrice > 0.0) seasonalPrice else basePrice
    fun roomTypeEnum(): RoomType = RoomType.fromRaw(roomType)
    fun statusEnum(): RoomStatus = RoomStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class Guest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val fullName: String = "",
    val mobile: String = "",
    val whatsapp: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = "India",
    val nationality: String? = "Indian",
    val idDocumentType: String? = IdDocumentType.NONE.name,
    val maskedIdNumber: String? = null,
    val vehicleNumber: String? = null,
    val emergencyContact: String? = null,
    val foreignGuest: Boolean = false,
    val passportNumber: String? = null,
    val visaNumber: String? = null,
    val visaExpiry: String? = null,
    val cFormSubmitted: Boolean = false,
    val cFormChecklistNotes: String? = null,
    val lifetimeBookingCount: Long = 0L,
    val completedStaysCount: Long = 0L,
    val totalLifetimeSpend: Double = 0.0,
    val lastStayDate: String? = null,
    val preferredRoomType: String? = null,
    val hospitalityNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isReturningGuest(): Boolean = lifetimeBookingCount >= 1L
    fun loyaltyTierLabel(): String = when {
        lifetimeBookingCount >= 5L -> "Platinum Returning Guest"
        lifetimeBookingCount >= 2L -> "Gold Returning Guest"
        lifetimeBookingCount >= 1L -> "Returning Guest"
        else -> "First-Time Guest"
    }
}

@Keep
@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val bookingCode: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val guestMobile: String = "",
    val guestWhatsapp: String? = null,
    val guestEmail: String? = null,
    val returningGuest: Boolean = false,
    val guestLifetimeBookingsAtBooking: Long = 0L,
    val adults: Long = 2L,
    val children: Long = 0L,
    val roomCount: Long = 1L,
    val roomId: String = "",
    val roomNumber: String = "",
    val roomType: String = RoomType.DELUXE.name,
    val bookingDateEpoch: Long = 0L,
    val checkInEpoch: Long = 0L,
    val checkOutEpoch: Long = 0L,
    val checkInTimeStr: String = "02:00 PM",
    val checkOutTimeStr: String = "11:00 AM",
    val nights: Long = 1L,
    val nightlyRate: Double = 0.0,
    val roomCharges: Double = 0.0,
    val extraChargesTotal: Double = 0.0,
    val extraCharges: List<Map<String, Any>> = emptyList(),
    val discount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstRate: Double = 12.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val refundAmount: Double = 0.0,
    val bookingSource: String = BookingSource.DIRECT.name,
    val status: String = BookingStatus.CONFIRMED.name,
    val specialRequests: String? = null,
    val internalNotes: String? = null,
    val guestSignatureName: String? = null,
    val actualCheckInEpoch: Long? = null,
    val actualCheckOutEpoch: Long? = null,
    val creditApprovedBy: String? = null,
    val creditExceptionReason: String? = null,
    val invoiceId: String? = null,
    val invoiceNumber: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): BookingStatus = BookingStatus.fromRaw(status)
    fun sourceEnum(): BookingSource = BookingSource.fromRaw(bookingSource)
    fun roomTypeEnum(): RoomType = RoomType.fromRaw(roomType)

    fun parsedExtraCharges(): List<ExtraChargeItem> = extraCharges.map { map ->
        ExtraChargeItem(
            title = map["title"] as? String ?: "Service",
            category = map["category"] as? String ?: "Other",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            taxable = map["taxable"] as? Boolean ?: true
        )
    }
}

@Keep
@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val transactionCode: String = "",
    val bookingId: String = "",
    val bookingCode: String = "",
    val guestName: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = PaymentMode.CASH.name,
    val paymentStatus: String = PaymentStatus.VERIFIED.name,
    val referenceNumber: String? = null,
    val upiIdUsed: String? = null,
    val receivedBy: String = "",
    val paymentEpoch: Long = 0L,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun modeEnum(): PaymentMode = PaymentMode.fromRaw(paymentMode)
    fun statusEnum(): PaymentStatus = PaymentStatus.fromRaw(paymentStatus)
}

@Keep
@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val invoiceNumber: String = "",
    val invoiceType: String = "TAX_INVOICE",
    val bookingId: String = "",
    val bookingCode: String = "",
    val guestName: String = "",
    val guestAddress: String? = null,
    val guestMobile: String? = null,
    val roomNumber: String = "",
    val checkInDateStr: String = "",
    val checkOutDateStr: String = "",
    val nights: Long = 1L,
    val roomCharges: Double = 0.0,
    val additionalServicesTotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstRate: Double = 12.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val grandTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentMethod: String = "Cash",
    val hotelName: String = "",
    val hotelAddress: String = "",
    val hotelPhone: String = "",
    val hotelGstin: String? = null,
    val issuedEpoch: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val fullName: String = "",
    val phone: String = "",
    val email: String? = null,
    val role: String = StaffRole.RECEPTIONIST.name,
    val active: Boolean = true,
    val shift: String = "Morning (07:00 AM - 03:00 PM)",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): StaffRole = StaffRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hotelId: String = "hotel_primary",
    val actorName: String = "",
    val actorRole: String = "",
    val actionType: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val summary: String = "",
    val timestampEpoch: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class HotelNotificationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val urgency: String,
    val targetBookingId: String? = null,
    val targetRoomId: String? = null
)
