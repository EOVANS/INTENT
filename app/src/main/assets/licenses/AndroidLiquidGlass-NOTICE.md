# AndroidLiquidGlass / Backdrop

Vendored from https://github.com/Kyant0/AndroidLiquidGlass, tag **1.0.6**, commit
`896a94a3ade1cc1a940b92365f942a34971fecda`. Copyright Kyant, Apache-2.0;
the upstream LICENSE is included alongside this file.

The Android source is compiled by the app without changing its Compose BOM or
Kotlin version, so the classic material keeps its existing dependencies.

Compatibility changes from upstream:

- Defer effect evaluation until measured bounds exist; register effect state
  observation on the first measured draw and on subsequent size changes. This
  prevents `Size is unspecified` on initial attachment and reattachment.

- Replace the Kotlin 2.3 context parameter in `recordLayer` with an explicit node
  argument, preserving the node's density during layer recording.
- Remove the optional Kyant Shapes branch from `lens`; Compose corner shapes,
  including the app's rounded capsule, remain supported.
- Remove the newer GraphicsLayerScope blendMode/colorFilter properties from the
  inverse transform helper. Neither is used by the app's transform path.

The upstream capture, native RenderEffect blur, lens shaders, highlight, and
coordinate mapping are retained. No bitmap downsampling is used for glass.

## Integration and regression checks (i-26-1022a)

`IntentNavHost` records the page only when advanced material is enabled on Android
13+, a top-level tab is visible, and the keyboard is hidden. `GlassTabBar` draws
that backdrop behind its labels/icons. Both modes now share a 64dp main capsule
(Home/Settings) and a separate 64dp Search button. The selection thumb follows
horizontal drags and snaps to a destination on release; taps and accessibility
selection use the same navigation callback. Search has its own selected state.
Advanced mode uses an exported surface backdrop for the glass thumb and uses
Backdrop's layerBlock for spring press scaling with anchored background samples.
Classic mode uses theme fills/borders and a restrained press response. Neither
mode adds another screen or changes the navigation destinations. Clear/regular,
adaptive tint and scroll-edge settings remain available.

Device acceptance checklist:

- Compare advanced OFF before/after in light and dark themes on all three tabs.
- Turn advanced ON; inspect curved edges over fine text/images while scrolling.
- Compare clear/regular and adaptive tint on/off over light/dark backgrounds.
- Press/release, cancel a gesture, switch tabs, open the keyboard, navigate to a
  detail screen and back; check alignment and absence of stale glass.
- Rotate/change display density; verify full-resolution, correctly aligned glass.
- On Android 11/12 verify the classic fallback and all navigation actions.

GPU appearance and frame timing require device verification; a successful JVM
build alone does not verify the visual result.
