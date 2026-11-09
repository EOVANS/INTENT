# Split navigation bar — 2026-10-25

Both material settings use the same 64dp layout: Home/Settings in a capsule,
Search in a separate circle. The thumb follows horizontal drags and selects the
nearest destination on release. Clicks and accessibility selection remain
available. Page transition ordering now follows Home, Settings, Search.

Advanced material uses the existing vendored Backdrop implementation: a surface
export provides the thumb backdrop without capturing the thumb into itself;
press transforms use `layerBlock` so page sampling is not scaled along with the
control. Classic mode uses theme fills and restrained press feedback. The
previous unspecified-size initialization fix remains in place.

Validation on the connected phone:

- Build and 58 JVM tests passed, including Backdrop initialization regression.
- Advanced material enabled at startup: launched and rendered successfully.
- Drag Home to Settings and Settings to Home: correct destination and selected
  semantics after release.
- Held press: enlarged capsule/thumb captured; release restores resting bounds.
- Disable advanced material: same layout, classic surface, no process crash.
- Independent Search button: correct destination and selected state.
- Search bottom entry: full text remains visible.
- Keyboard open/close: navigation removed while open and restored on close.
- Re-enable advanced material: verified checked state and successful navigation.
- No crash entries for the tested application processes.

The final page-transition direction adjustment was build-checked after these
interaction checks. Dark theme, accessibility font extremes, Android 11/12 and
frame timing have not been exhaustively tested. Sharing the layout reduces
maintenance duplication; it does not promise a significant APK size reduction.

References:
- https://kyant.gitbook.io/backdrop/tutorials/interactive-glass-bottom-bar
- https://kyant.gitbook.io/backdrop/tutorials/glass-slider
