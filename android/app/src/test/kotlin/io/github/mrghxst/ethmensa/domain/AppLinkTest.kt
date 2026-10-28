// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.Campus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLinkTest {
    @Test
    fun `https share links open the mensa`() {
        assertEquals(AppLink.OpenMensa("eth/9"), AppLink.parse("https://ethmensa.ch/s/eth/9"))
        assertEquals(AppLink.OpenMensa("uzh/12"), AppLink.parse("https://ethmensa.ch/s/uzh/12/"))
        assertEquals(AppLink.OpenMensa("eth/9"), AppLink.parse("https://www.ethmensa.ch/s/eth/9?utm=x"))
        assertEquals(AppLink.OpenMensa("eth/9"), AppLink.parse("https://ETHMENSA.CH/s/ETH/9"))
    }

    @Test
    fun `custom scheme links`() {
        assertEquals(AppLink.OpenMensa("eth/9"), AppLink.parse("ethmensa://s/eth/9"))
        assertEquals(AppLink.Legi, AppLink.parse("ethmensa://legi"))
        assertEquals(AppLink.Legi, AppLink.parse("ethmensa://action/legi"))
        assertEquals(AppLink.CurrentlyOpen, AppLink.parse("ethmensa://action/currently_open"))
        assertEquals(AppLink.ShowCampus(Campus.ZENTRUM), AppLink.parse("ethmensa://action/zentrum"))
        assertEquals(AppLink.ShowCampus(Campus.HONGG), AppLink.parse("ethmensa://action/hongg"))
        assertEquals(AppLink.ShowCampus(Campus.IRCHEL), AppLink.parse("ethmensa://action/irchel"))
    }

    @Test
    fun `everything else is ignored`() {
        listOf(
            null,
            "",
            "not a url",
            "http://ethmensa.ch/s/eth/9",
            "https://evil.example/s/eth/9",
            "https://ethmensa.ch.evil.example/s/eth/9",
            "https://ethmensa.ch/s/eth/abc",
            "https://ethmensa.ch/s/eth",
            "https://ethmensa.ch/s/eth/9/extra",
            "https://ethmensa.ch/",
            "ethmensa://s/eth",
            "ethmensa://action/unknown",
            "ethmensa://other",
            "intent://s/eth/9#Intent;scheme=ethmensa;end",
        ).forEach { assertNull(it, AppLink.parse(it)) }
    }
}
