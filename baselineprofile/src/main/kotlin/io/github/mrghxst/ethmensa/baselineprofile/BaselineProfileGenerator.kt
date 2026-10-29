// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the startup and main journey: list → scroll → detail → scroll → back. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "io.github.mrghxst.ethmensa", includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()

        // A fresh install starts with the onboarding.
        device.findObject(By.text("Continue"))?.click()
        // The list is ready when a mensa row shows (menus come from the network the first time).
        device.wait(Until.hasObject(By.textContains("Mensa")), 30_000)

        val list = device.findObject(By.scrollable(true))
        list?.setGestureMargin(device.displayWidth / 5)
        list?.fling(Direction.DOWN)
        list?.fling(Direction.UP)
        device.waitForIdle()

        device.findObject(By.textContains("Polyterrasse"))?.click()
        device.wait(Until.hasObject(By.text("Mo")), 10_000)
        device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }
}
