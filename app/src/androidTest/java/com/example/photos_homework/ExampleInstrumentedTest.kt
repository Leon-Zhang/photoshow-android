package com.example.photos_homework

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.photos_homework", appContext.packageName)
    }

    @Test
    fun testActivityLaunch() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            assertNotNull(activity) // Check if activity is launched
        }
    }

    @Test
    fun addFavoritePhoto() = runTest {
        val viewModel = PhotoViewModel()
        val photoId=1
        viewModel.toggleFavorite(photoId)
        assertTrue(viewModel.favorites.value.contains(photoId))
        viewModel.toggleFavorite(photoId)
        assertFalse(viewModel.favorites.value.contains(photoId))
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    @Test
    fun testPhotoListAndDetailScreen() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.setContent {
                PhotoListScreen({}, PhotoViewModel())
            }
        }
        // Verify the search is displayed
        composeTestRule.onNodeWithText(SEARCH_LABEL)  // Adjust based on actual UI
            .assertIsDisplayed()
        // Click on the favorites button
        composeTestRule.onNodeWithText(FAVORITES_BTN)
            .performClick()
        composeTestRule.onNodeWithText(ERR_NOITEMSAVAIL)
            .assertIsDisplayed()

        activityScenarioRule.scenario.onActivity { activity ->
            // Load a different screen inside MainActivity
            activity.setContent {
                val photo = Photo(1, 1, "photo 1", "", "")
                PhotoDetailScreen(photo, onBack = {}, PhotoViewModel())
            }
        }
        composeTestRule.onNodeWithText(BACK_BTN)
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(IMAGE_CONTENTDESC)
            .assertIsDisplayed()
        assertTrue(composeTestRule.onNodeWithText(REMOVEFAV_BTN).isDisplayed() ||
                composeTestRule.onNodeWithText(ADDFAV_BTN).isDisplayed())
    }
}