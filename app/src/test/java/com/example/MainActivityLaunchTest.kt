package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-420dpi")
class MainActivityLaunchTest {

    @Rule
    @JvmField
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity cold launch renders compose dashboard ui cleanly`() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Finance Tracker")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("Finance Tracker")).assertIsDisplayed()
        composeRule.onNodeWithTag("kpi_net_worth_card").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_dashboard").assertIsDisplayed()
    }
}
