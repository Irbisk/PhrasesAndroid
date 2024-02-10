package org.hyperskill.phrases

import org.hyperskill.phrases.internals.CustomAsyncDifferConfigShadow
import org.hyperskill.phrases.internals.PhrasesUnitTest
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// version 1.2
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [CustomAsyncDifferConfigShadow::class])
class Stage2BUnitTest : PhrasesUnitTest<MainActivity>(MainActivity::class.java){

    @Test
    fun test00_checkReminderTextViewExists() {
        testActivity {
            reminderTv
        }
    }

    @Test
    fun test01_checkFloatingButtonExists() {
        testActivity {
            floatingButton
        }
    }

    @Test
    fun test02_checkRecyclerViewExists() {
        testActivity {
            recyclerView
        }
    }

    @Test @Ignore("incompatible with stage4 requirements")
    fun test03_checkFirstListItemContainExpectedViews() {
        testActivity {
            recyclerView.assertItemViewsExistOnItemWithIndex(0)
        }
    }

    @Test @Ignore("incompatible with stage4 requirements")
    fun test04_checkRecyclerViewHasAtLeastThreeItems() {
        testActivity {
            recyclerView.assertAmountItems(3)
        }
    }

    @Test @Ignore("incompatible with stage4 requirements")
    fun test05_checkClickingDeleteDecreasesNumberOfItems() {
        testActivity {
            recyclerView.deleteLastItemAndAssertSizeDecreased()
        }
    }
}