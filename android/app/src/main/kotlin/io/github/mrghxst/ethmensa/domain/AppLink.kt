// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.domain

import io.github.mrghxst.ethmensa.domain.model.Campus
import java.net.URI

/** Something a link, an app shortcut or the Quick Settings tile asks the app to show. */
sealed interface AppLink {
    /** `https://ethmensa.ch/s/{provider}/{id}` or `ethmensa://s/{provider}/{id}`. */
    data class OpenMensa(val mensaId: String) : AppLink

    /** `ethmensa://legi` and the `legi` shortcut. */
    data object Legi : AppLink

    /** `ethmensa://action/currently_open`: open mensas only, other filters cleared. */
    data object CurrentlyOpen : AppLink

    /** `ethmensa://action/{zentrum|hongg|irchel}`: filters reset, then the campus set. */
    data class ShowCampus(val campus: Campus) : AppLink

    companion object {
        private val HOSTS = setOf("ethmensa.ch", "www.ethmensa.ch")

        // iOS OpenURLViewModifier: "/s/" + word chars + "/" + digits, optional trailing slash.
        private val MENSA_PATH = Regex("""^/s/(\w+/\d+)/?$""")

        /** Parses a link, or returns null for anything the app doesn't handle. */
        fun parse(link: String?): AppLink? {
            val uri = runCatching { URI(link ?: return null) }.getOrNull() ?: return null
            val host = uri.host?.lowercase()
            val path = uri.path.orEmpty()
            return when (uri.scheme?.lowercase()) {
                "https" -> if (host in HOSTS) mensa(path) else null
                "ethmensa" -> when (host) {
                    "s" -> mensa("/s$path")
                    "legi" -> Legi
                    "action" -> action(path.trim('/'))
                    else -> null
                }
                else -> null
            }
        }

        private fun mensa(path: String): AppLink? =
            MENSA_PATH.find(path)?.groupValues?.get(1)?.let { OpenMensa(it.lowercase()) }

        private fun action(id: String): AppLink? = when (id) {
            "currently_open" -> CurrentlyOpen
            "legi" -> Legi
            else -> Campus.fromId(id)?.let(::ShowCampus)
        }
    }
}
