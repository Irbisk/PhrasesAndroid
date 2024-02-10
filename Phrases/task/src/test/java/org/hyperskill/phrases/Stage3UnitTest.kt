package org.hyperskill.phrases

import android.app.Notification
import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.os.SystemClock
import org.hyperskill.phrases.internals.CustomAsyncDifferConfigShadow
import org.hyperskill.phrases.internals.PhrasesUnitTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

// version 1.2
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [CustomAsyncDifferConfigShadow::class])
class Stage3UnitTest : PhrasesUnitTest<MainActivity>(MainActivity::class.java){


    @Before
    fun setUp() {
        SystemClock.setCurrentTimeMillis(System.currentTimeMillis())
        run { // compatibility with stage4
            val phrases = listOf("database should not be empty to set reminder")
            addToDatabase(phrases)
        }
    }

    @Test
    fun test00_checkNotificationChannelExists() {
        testActivity {
            notificationChannel
        }
    }

    @Test
    fun test01_checkTimePickerDialog() {
        testActivity {
            reminderTv.clickAndRun()
            val timePickerDialog = getLatestTimePickerDialog()
            timePickerDialog.pickTime(10, 15)

            val expectedTimeText = "Reminder set for 10:15"
            val actualTimeText = reminderTv.text.toString()

            assertEquals("The reminderTextView has a wrong text", expectedTimeText, actualTimeText)
        }
    }

    @Test
    fun test02_checkTimeFormatting() {


        testActivity {
            val testCases = listOf(
                0 to 0,
                2 to 22,
                12 to 6,
                17 to 25,
                23 to 59
            )

            val expectedTime = listOf(
                "00:00",
                "02:22",
                "12:06",
                "17:25",
                "23:59"
            )

            testCases.forEachIndexed { i, (pickHour, pickMinute)  ->
                reminderTv.clickAndRun()
                val timePickerDialog = getLatestTimePickerDialog()

                timePickerDialog.pickTime(pickHour, pickMinute)
                val timeText = expectedTime[i]

                val expectedText = "Reminder set for $timeText"
                val actualText = reminderTv.text.toString()
                assertEquals("Time is not formatted correctly", expectedText, actualText)
            }
        }
    }

    @Test
    fun test03_checkNotificationIsSent() {
        testActivity {
            val minutesToAdd = 10
            val (pickHour, pickMinute) = hourToMinutes(minutesFromNow = minutesToAdd)

            reminderTv.clickAndRun()
            val timePickerDialog = getLatestTimePickerDialog()
            timePickerDialog.pickTime(pickHour, pickMinute)

            runEnqueuedAlarms()
            val beforeTimeNotification: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            assertNull("Notification should not be sent before the time set", beforeTimeNotification)

            shadowLooper.idleFor(minutesToAdd + 2L, TimeUnit.MINUTES) // trigger alarm
            runEnqueuedAlarms()

            val notification: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            assertNotNull(messageNotificationWithIdNotFound, notification)
            notification!!

            val messageChannelId = "The notification channel id does not equals \"$CHANNEL_ID\""
            val actualChannelId = notification.channelId
            assertEquals(messageChannelId, CHANNEL_ID, actualChannelId)

            val messageTitle = "Have you set correct notification title?"
            val expectedTitle = "Your phrase of the day"
            val actualTitle = notification.extras.getCharSequence(EXTRA_TITLE)?.toString()
            assertEquals(messageTitle, expectedTitle, actualTitle)

            val messageContent = "Have you set the notification content?"
            val actualContent = notification.extras.getCharSequence(EXTRA_TEXT)?.toString()
            assertNotNull(messageContent, actualContent)
            assertTrue(messageContent, actualContent!!.isNotBlank())
        }
    }

    @Test
    fun test04_checkWhenTimeSetIsBeforeCurrentNotificationIsSentNextDay() {
        testActivity {
            val minutesToSubtract = 10
            val (pickHour, pickMinute) = hourToMinutes(minutesFromNow = -minutesToSubtract)

            reminderTv.clickAndRun()
            val timePickerDialog = getLatestTimePickerDialog()
            timePickerDialog.pickTime(pickHour, pickMinute)

            runEnqueuedAlarms()
            val beforeTimeNotification: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            val messageNotificationShouldBeNull =
                "If the notification is set to a time before the current day time " +
                        "send the notification only next day"
            assertNull(messageNotificationShouldBeNull, beforeTimeNotification)


            shadowLooper.idleFor(1, TimeUnit.DAYS) // trigger alarm
            runEnqueuedAlarms()

            val notification: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            assertNotNull(messageNotificationWithIdNotFound, notification)
            notification!!

            val messageChannelId = "The notification channel id does not equals \"$CHANNEL_ID\""
            val actualChannelId = notification.channelId
            assertEquals(messageChannelId, CHANNEL_ID, actualChannelId)

            val messageTitle = "Have you set correct notification title?"
            val expectedTitle = "Your phrase of the day"
            val actualTitle = notification.extras.getCharSequence(EXTRA_TITLE)?.toString()
            assertEquals(messageTitle, expectedTitle, actualTitle)

            val messageContent = "Have you set the notification content?"
            val actualContent = notification.extras.getCharSequence(EXTRA_TEXT)?.toString()
            assertNotNull(messageContent, actualContent)
            assertTrue(messageContent, actualContent!!.isNotBlank())
        }
    }

    @Test
    fun test05_checkNotificationIsRepeating() {

        testActivity {
            val minutesToAdd = 10
            val (pickHour, pickMinute) = hourToMinutes(minutesFromNow = minutesToAdd)

            reminderTv.clickAndRun()
            val timePickerDialog = getLatestTimePickerDialog()
            timePickerDialog.pickTime(pickHour, pickMinute)

            shadowLooper.idleFor(minutesToAdd + 2L, TimeUnit.MINUTES) // trigger alarm
            runEnqueuedAlarms()

            val notification: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            assertNotNull(messageNotificationWithIdNotFound, notification)
            notification!!

            shadowLooper.idleFor(1 , TimeUnit.DAYS)
            shadowLooper.idleFor(10, TimeUnit.MINUTES)  // trigger alarm on next day
            runEnqueuedAlarms()

            val notification2: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            assertNotNull(messageNotificationWithIdNotFound, notification2)
            notification2!!

            val messageSameNotificationError =
                "A new notification should be triggered on the next day"
            assertFalse(messageSameNotificationError, notification === notification2)
        }
    }
}