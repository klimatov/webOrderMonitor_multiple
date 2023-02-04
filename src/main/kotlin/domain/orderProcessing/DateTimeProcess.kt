package domain.orderProcessing

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DateTimeProcess {
    fun dateFrom() : String {
        val result = LocalDate.now().minusDays(35)
        return result.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    fun dateFormat(docDate: String): LocalDateTime {
        val docDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        return LocalDateTime.parse(docDate, docDateFormat)
    }

    fun replaceDateTime(docDate: String): String {
        return dateFormat(docDate).format(DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy"))
    }

    class dateDiff(startDate: LocalDateTime, endDate: LocalDateTime = LocalDateTime.now()) {
        private val period = startDate.until(endDate, ChronoUnit.MINUTES)
        val days = period / 1440
        val hours = period % 1440 / 60
        val minutes = period % 1440 % 60
    }
}