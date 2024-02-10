package org.hyperskill.phrases

import android.app.Dialog
import android.app.Notification
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
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.util.*
import java.util.concurrent.TimeUnit

// version 1.2
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [CustomAsyncDifferConfigShadow::class])
class Stage4UnitTest : PhrasesUnitTest<MainActivity>(MainActivity::class.java) {


    @Before
    fun setUp() {
        SystemClock.setCurrentTimeMillis(System.currentTimeMillis())
    }


    @Test
    fun test00_checkRecyclerViewIsUsingDatabase() {

        addToDatabase(fakePhrases)

        testActivity {
            assertDatabaseContentMatchesList(
                messageWrongDatabaseContent = messageWrongDatabaseContent,
                expectedDatabaseContent = fakePhrases
            )
        }
    }

    @Test
    fun test01_checkRecyclerViewIsUsingDatabase2() {

        val phrases = fakePhrases + "one more test phrase"
        addToDatabase(phrases)

        testActivity {
            assertDatabaseContentMatchesList(
                messageWrongDatabaseContent = messageWrongDatabaseContent,
                expectedDatabaseContent = phrases
            )
        }
    }

    @Test
    fun test02_checkAddDialog() {
        val phrases = listOf("A text for test")

        testActivity {
            addPhrase(phrases[0])

            assertDatabaseContentMatchesList(
                messageWrongDatabaseContent = "Database content should contain added phrase",
                expectedDatabaseContent = phrases
            )
        }
    }



    @Test
    fun test03_checkPhrasesAreDeleted() {

        addToDatabase(fakePhrases)

        testActivity {
            deletePhraseAtIndex(0)
            assertDatabaseContentMatchesList(
                messageWrongDatabaseContent = "After deleting database content should be updated",
                expectedDatabaseContent = fakePhrases.drop(1)
            )
        }
    }

    @Test
    fun test04_checkNotificationContainsPhraseFromDb() {

        addToDatabase(fakePhrases)

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

            val messageContent = messagePhraseNotInDatabase
            val actualContent = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            assertTrue(messageContent, actualContent in fakePhrases)
        }
    }

    @Test
    fun test05_checkRemindersNotAllowedWithEmptyDatabase() {

        testActivity {
            reminderTv.clickAndRun()
            val maybeDialog: Dialog? = ShadowDialog.getLatestDialog()
            assertNull("No dialog should be shown if database is empty", maybeDialog)

            val toast = ShadowToast.getLatestToast()
            assertNotNull("Toast is not shown after trying to set reminder with empty database", toast)

            val expectedText = "No reminder set"
            val actualText = reminderTv.text.toString()
            assertEquals("Seems like reminder is still set with empty database", expectedText, actualText)
        }
    }

    @Test
    fun test06_checkEmptyAfterDeleteUnsetsReminder() {

        val fakePhrases = listOf("single item")
        addToDatabase(fakePhrases)

        testActivity {
            val minutesToAdd = 10
            val (pickHour, pickMinute) = hourToMinutes(minutesFromNow = minutesToAdd)


            reminderTv.clickAndRun()
            val timePickerDialog = getLatestTimePickerDialog()
            timePickerDialog.pickTime(pickHour, pickMinute)

            val unexpectedTextReminders = "No reminder set"
            val actualTextReminders = reminderTv.text.toString()
            assertNotEquals(
                "If reminder is set reminderTv should change",
                unexpectedTextReminders,
                actualTextReminders
            )

            deletePhraseAtIndex(0)

            assertDatabaseContentMatchesList(
                messageWrongDatabaseContent = "Database should be empty after deleting its only element",
                expectedDatabaseContent = emptyList()
            )

            val expectedTextNoReminders = "No reminder set"
            val actualTextNoReminders = reminderTv.text.toString()
            assertEquals(
                "If list becomes empty after deletion reminderTv should change",
                expectedTextNoReminders,
                actualTextNoReminders
            )

            val messageNotificationId =
                "No notification should be triggered if the list is empty"

            try{
                shadowLooper.idleFor(minutesToAdd + 2L, TimeUnit.MINUTES) // trigger alarm
                runEnqueuedAlarms()
                val notification: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
                assertNull(messageNotificationId, notification)
            } catch (noSuchElementException : NoSuchElementException) {
                val exceptionThrownMessage = "Make sure you don't trigger alarm if the list is empty. " +
                        "$noSuchElementException"
                throw AssertionError(exceptionThrownMessage, noSuchElementException)
            }
        }
    }

    @Test
    fun test07_checkNotificationSentOnNextDay() {
        addToDatabase(fakePhrases)

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

            val messageContent = messagePhraseNotInDatabase
            val actualContent = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            assertTrue(messageContent, actualContent in fakePhrases)

            shadowLooper.idleFor(1 , TimeUnit.DAYS)
            shadowLooper.idleFor(10, TimeUnit.MINUTES)  // trigger alarm on next day
            runEnqueuedAlarms()

            val notification2: Notification? = notificationManager.getNotification(NOTIFICATION_ID)
            assertNotNull(messageNotificationWithIdNotFound, notification2)
            notification2!!

            val messageSameNotificationError =
                "A new notification should be triggered on the next day"
            assertFalse(messageSameNotificationError, notification === notification2)

            val actualContent2 =
                notification2.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            assertTrue(messagePhraseNotInDatabase, actualContent2 in fakePhrases)
        }
    }
}