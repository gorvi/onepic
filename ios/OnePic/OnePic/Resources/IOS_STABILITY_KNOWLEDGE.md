# iOS Stability Knowledge (OnePic)

## Purpose
This file records the root fixes, compatibility strategy, and temporary mitigations used to resolve Home screen vertical jitter across iOS versions.

## Single Source Of Runtime Policy
Use `AppRuntimePolicy` in:
- `/Users/ghw/AndroidStudioProjects/2026/onepic/ios/OnePic/OnePic/Utils/SwiftUICompatibility.swift`

Policy flags:
- `supportsNativeAds`
- `supportsRealtimeAnimationDriver`
- `supportsVisitorOverlay`
- `supportsFloatingBuffWindow`
- `useLazyHomeList`

Rule:
- Do not add scattered `#available(iOS 16.0, *)` checks for this issue path.
- Add/adjust behavior through `AppRuntimePolicy` first.

## Real Fixes (Keep)
1. CGPath correctness fix:
- In `CelestialVisitorView`, `Path.addCurve(...)` calls now have required `move(to:)`.
- Prevents CoreGraphics path warnings and unstable redraw behavior.

2. State update reduction:
- `LevelProgressManager.updateBuffState()` only writes when value changes.
- Reduces broad SwiftUI invalidations.

3. Compatibility API layer:
- `onChangeCompat`, navigation and scroll-content compatibility methods in `SwiftUICompatibility.swift`.
- Prevents API behavior drift across iOS 15/16/17.

## Compatibility Strategy (Keep)
iOS 15 stability mode uses reduced dynamic behavior:
- Disable native ads/webview-heavy paths.
- Disable realtime visitor timeline and overlay.
- Disable floating buff window.
- Use static galaxy background.
- Use `VStack` instead of `LazyVStack` for Home list.

iOS 16+ keeps full interactive/animated behavior, controlled by `AppRuntimePolicy`.

## Temporary Mitigations (Review Later)
1. Home initial positioning flow:
- Home content is hidden until initial scroll positioning completes.
- Session-level first-focus guard exists to avoid repeated initial jumps.

2. Jitter diagnostics:
- `StabilityDiagnostics.jitter(...)` emits `[JITTER]` logs for tracing.
- Safe to disable by setting `StabilityDiagnostics.enableJitterLogs = false`.

## Known Symptom Mapping
If you see:
- `"runInitialFocusSequence ..."` followed by visible jump:
  - check repeated Home re-creation and initial-focus triggers.
- `"onChange(of: Double) action tried to update multiple times per frame"`:
  - inspect high-frequency `onChange` write loops and timeline-driven state mutation.

## Rollback Order (When Stable)
1. Keep real fixes.
2. Remove session guard and hide-then-show positioning if not needed.
3. Re-enable iOS 15 features one-by-one using `AppRuntimePolicy`:
- `supportsRealtimeAnimationDriver`
- `supportsVisitorOverlay`
- `supportsFloatingBuffWindow`
- `supportsNativeAds`
- `useLazyHomeList`
4. Validate after each step.

