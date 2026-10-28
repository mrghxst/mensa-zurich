// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui.legi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegiHostsTest {
    @Test
    fun `card and ETH login hosts stay in the WebView`() {
        listOf("eduapp.ethz.ch", "ethz.ch", "aai-logon.ethz.ch", "login.eduid.ch", "login.microsoftonline.com", "EDUAPP.ETHZ.CH")
            .forEach { assertTrue(it, isInAppHost(it)) }
    }

    @Test
    fun `other hosts open outside`() {
        listOf(null, "", "example.com", "notethz.ch", "ethz.ch.evil.example", "evilmicrosoft.com")
            .forEach { assertFalse(it.toString(), isInAppHost(it)) }
    }
}
