package com.example.model

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

object IndianFormatters {
    val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val indianLocale = Locale("en", "IN")

    fun formatInr(amount: Double, showDecimals: Boolean = false): String {
        val negative = amount < 0
        val absVal = abs(amount)
        val whole = absVal.toLong()
        val decimals = ((absVal - whole) * 100.0).roundToLong()

        val wholeStr = formatIndianGrouping(whole)
        val sign = if (negative) "-" else ""
        return if (showDecimals || decimals > 0) {
            val decFmt = decimals.toString().padStart(2, '0')
            "$sign₹$wholeStr.$decFmt"
        } else {
            "$sign₹$wholeStr"
        }
    }

    private fun formatIndianGrouping(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val lastThree = s.takeLast(3)
        val rest = s.dropLast(3)
        val groupedRest = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            groupedRest.append(rest[i])
            count++
            if (count == 2 && i > 0) {
                groupedRest.append(',')
                count = 0
            }
        }
        return "${groupedRest.reverse()},$lastThree"
    }

    fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "--"
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply { timeZone = IST }
        return fmt.format(Date(epochMillis))
    }

    fun formatTime12h(epochMillis: Long): String {
        if (epochMillis <= 0L) return "--"
        val fmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = IST }
        return fmt.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long): String {
        if (epochMillis <= 0L) return "--"
        val fmt = SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = IST }
        return fmt.format(Date(epochMillis))
    }

    fun dateKey(epochMillis: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).apply { timeZone = IST }
        return fmt.format(Date(epochMillis))
    }

    fun parseDateDdMmYyyy(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.length != 10) return null
        return try {
            val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply {
                timeZone = IST
                isLenient = false
            }
            val date = fmt.parse(trimmed) ?: return null
            startOfDayIst(date.time)
        } catch (_: Exception) {
            null
        }
    }

    fun startOfDayIst(epochMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(IST).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun addDaysIst(baseDayEpoch: Long, days: Int): Long {
        val cal = Calendar.getInstance(IST).apply {
            timeInMillis = startOfDayIst(baseDayEpoch)
            add(Calendar.DAY_OF_MONTH, days)
        }
        return cal.timeInMillis
    }

    fun nightsBetween(checkInEpoch: Long, checkOutEpoch: Long): Int {
        val start = startOfDayIst(checkInEpoch)
        val end = startOfDayIst(checkOutEpoch)
        val diffDays = ((end - start) / 86_400_000L).toInt()
        return max(1, diffDays)
    }

    fun nightDateKeys(checkInEpoch: Long, checkOutEpoch: Long): List<String> {
        val nights = nightsBetween(checkInEpoch, checkOutEpoch)
        val start = startOfDayIst(checkInEpoch)
        return (0 until nights).map { offset ->
            dateKey(addDaysIst(start, offset))
        }
    }

    fun isSameDayIst(a: Long, b: Long): Boolean = dateKey(a) == dateKey(b)

    fun isSameMonthIst(epochMillis: Long, referenceEpoch: Long = System.currentTimeMillis()): Boolean {
        val calA = Calendar.getInstance(IST).apply { timeInMillis = epochMillis }
        val calB = Calendar.getInstance(IST).apply { timeInMillis = referenceEpoch }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.MONTH) == calB.get(Calendar.MONTH)
    }

    fun isSameYearIst(epochMillis: Long, referenceEpoch: Long = System.currentTimeMillis()): Boolean {
        val calA = Calendar.getInstance(IST).apply { timeInMillis = epochMillis }
        val calB = Calendar.getInstance(IST).apply { timeInMillis = referenceEpoch }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR)
    }

    fun normalizeIndianMobile(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length > 10 && digits.startsWith("91") -> digits.takeLast(10)
            digits.length > 10 && digits.startsWith("0") -> digits.takeLast(10)
            else -> digits
        }
    }

    fun maskSensitiveId(rawId: String, docType: IdDocumentType): String {
        val clean = rawId.trim().replace(" ", "")
        if (clean.isBlank()) return ""
        if (clean.startsWith("XXXX")) return clean
        val last4 = clean.takeLast(4)
        return when (docType) {
            IdDocumentType.AADHAAR_MASKED -> "XXXX-XXXX-$last4"
            IdDocumentType.PASSPORT -> "XXXX$last4"
            else -> if (clean.length > 4) "XXXX-$last4" else clean
        }
    }

    fun formatPercent(value: Double): String {
        return "${DecimalFormat("0.0").format(value)}%"
    }
}
