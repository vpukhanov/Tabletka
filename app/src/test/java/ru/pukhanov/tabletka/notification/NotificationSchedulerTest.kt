package ru.pukhanov.tabletka.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class NotificationSchedulerTest {

    @Test
    fun getRequestCode_isDeterministicAndUnique() {
        val scheduleMonday800 = NotificationScheduler.UniqueSchedule(DayOfWeek.MONDAY, 8, 0)
        val scheduleMonday830 = NotificationScheduler.UniqueSchedule(DayOfWeek.MONDAY, 8, 30)
        val scheduleSunday2359 = NotificationScheduler.UniqueSchedule(DayOfWeek.SUNDAY, 23, 59)

        // Monday = 1
        assertEquals(10800, NotificationScheduler.getRequestCode(scheduleMonday800))
        assertEquals(10830, NotificationScheduler.getRequestCode(scheduleMonday830))
        
        // Sunday = 7
        assertEquals(72359, NotificationScheduler.getRequestCode(scheduleSunday2359))
    }

    @Test
    fun getNextTriggerTime_laterToday_returnsToday() {
        // Monday May 18, 2026 at 10:00 AM
        val now = LocalDateTime.of(2026, 5, 18, 10, 0)
        val schedule = NotificationScheduler.UniqueSchedule(DayOfWeek.MONDAY, 14, 30) // Monday at 14:30

        val nextTrigger = NotificationScheduler.getNextTriggerTime(schedule, now)

        assertEquals(2026, nextTrigger.year)
        assertEquals(5, nextTrigger.monthValue)
        assertEquals(18, nextTrigger.dayOfMonth) // Monday (same day)
        assertEquals(14, nextTrigger.hour)
        assertEquals(30, nextTrigger.minute)
    }

    @Test
    fun getNextTriggerTime_earlierToday_returnsNextWeek() {
        // Monday May 18, 2026 at 16:00 PM
        val now = LocalDateTime.of(2026, 5, 18, 16, 0)
        val schedule = NotificationScheduler.UniqueSchedule(DayOfWeek.MONDAY, 14, 30) // Monday at 14:30

        val nextTrigger = NotificationScheduler.getNextTriggerTime(schedule, now)

        // Should return next Monday, May 25, 2026
        assertEquals(2026, nextTrigger.year)
        assertEquals(5, nextTrigger.monthValue)
        assertEquals(25, nextTrigger.dayOfMonth) // Next Monday
        assertEquals(14, nextTrigger.hour)
        assertEquals(30, nextTrigger.minute)
    }

    @Test
    fun getNextTriggerTime_differentDayOfWeekLater_returnsThatDay() {
        // Monday May 18, 2026 at 10:00 AM
        val now = LocalDateTime.of(2026, 5, 18, 10, 0)
        val schedule = NotificationScheduler.UniqueSchedule(DayOfWeek.FRIDAY, 9, 0) // Friday at 9:00 AM

        val nextTrigger = NotificationScheduler.getNextTriggerTime(schedule, now)

        // Friday of same week is May 22, 2026
        assertEquals(2026, nextTrigger.year)
        assertEquals(5, nextTrigger.monthValue)
        assertEquals(22, nextTrigger.dayOfMonth)
        assertEquals(9, nextTrigger.hour)
        assertEquals(0, nextTrigger.minute)
    }

    @Test
    fun getNextTriggerTime_differentDayOfWeekEarlier_returnsThatDayNextWeek() {
        // Saturday May 23, 2026 at 12:00 PM
        val now = LocalDateTime.of(2026, 5, 23, 12, 0)
        val schedule = NotificationScheduler.UniqueSchedule(DayOfWeek.FRIDAY, 9, 0) // Friday at 9:00 AM

        val nextTrigger = NotificationScheduler.getNextTriggerTime(schedule, now)

        // Friday of next week is May 29, 2026
        assertEquals(2026, nextTrigger.year)
        assertEquals(5, nextTrigger.monthValue)
        assertEquals(29, nextTrigger.dayOfMonth)
        assertEquals(9, nextTrigger.hour)
        assertEquals(0, nextTrigger.minute)
    }
}
