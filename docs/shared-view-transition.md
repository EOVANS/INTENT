# SharedViewKit article transition

Home article cards, individual articles in aggregate cards, and note-preview cards use SharedViewKit when the device reports protocol support. The visible portion is captured with PixelCopy and the reader or note detail opens in a separate activity. The return location is refreshed when available. If the SDK, capture or launch is unavailable, navigation falls back to the Compose shared-bounds transition. Reduced motion disables the SDK transition.

The SDK mask color matches the actual source page background. On the tested device it also affects the opening transition, despite the demo documentation describing it mainly in terms of the return animation.

The SDK reports capability, not market or ROM region. No assumptions are made from the manufacturer, locale or device model. Other article entry points continue to use the existing navigation.

The bundled `app/libs/sharedview-kit-1.0.0.5.aar` comes from the official [SharedViewDemo](https://gitee.com/itgsa_sharedviewkit/SharedViewDemo), commit `b449a426a340f1827d942592bddf0a416eae3ac4`. The demo's NOTICE identifies the SDK as an independent product whose licence is governed by its own release terms; the demo's MIT licence does not by itself establish the SDK licence. Check those terms before redistribution. This fixed local version avoids depending on the demo's HTTP Maven repository at build time.
