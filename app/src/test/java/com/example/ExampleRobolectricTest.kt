package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BlockerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("YT Focus", appName)
  }

  @Test
  fun `test blocker preferences default values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = BlockerPreferences.getInstance(context)
    val settings = prefs.settings.value
    assertTrue(settings.isMasterEnabled)
    assertTrue(settings.redirectSubscriptions)
    assertTrue(settings.blockShorts)
  }
}
