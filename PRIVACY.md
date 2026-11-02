# Privacy

Mensa Zürich has no accounts, ads, analytics, crash reporting or tracking. The developer receives no data from the app.

## What stays on your device

- Settings, favorites, hidden mensas and how often you open each mensa (for smart sorting)
- Downloaded menus, cached for the current week
- Your location, if you allow it. The app uses it to sort mensas by distance and, if you turn on location-based reminders, to let Android tell it when you enter or leave a campus (geofencing). The location is never sent anywhere by the app.

Android's own backup may include the settings if you have device backup turned on.

## Servers the app contacts

| Server | Why |
| --- | --- |
| `idapps.ethz.ch`, `glyph.ethz.ch` | ETH mensa list, opening hours and menus |
| `api.zfv.ch` | UZH mensa menus (ZFV) |
| `ziuzhnowweb.uzh.ch` and the image hosts named by the ETH and ZFV data | Mensa and dish photos |
| `raw.githubusercontent.com` | Updated UZH opening hours and addresses from the [ETH Mensa for iOS](https://github.com/alexandrereol/ethmensa) repository, every three days |
| `tiles.openfreemap.org` | Map tiles around a mensa when you open it, cached on the device. The request reveals the area of the mensa, not your location |
| `eduapp.ethz.ch` and the ETH login pages | Only when you open the Legi card; you sign in directly with ETH |
| Android's geocoder | Looks up a mensa's address once if its coordinates aren't built in |

These requests carry the usual technical data (your IP address and the app's name and version in the `User-Agent`). Each service handles it under its own privacy policy.

Opening a mensa's website or the map hands the link to your browser or maps app.

## Permissions

| Permission | Used for |
| --- | --- |
| Internet, network state | Loading menus, showing when you're offline |
| Notifications | Menu reminders, if you turn them on |
| Location (approximate, precise, background) | Sorting by distance and campus-based reminders, only if you allow it |
| Run at startup | Re-scheduling reminders after a restart |

## Contact

Questions or concerns: open an issue in this repository.
