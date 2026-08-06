# Implementation Plan - Animation and Transition System

Implement a high-performance animation system for WaterMark Pro, focusing on smooth transitions, Material Motion principles, and zero impact on backend processing logic.

## User Review Required

> [!IMPORTANT]
> **Backend Preservation**: This plan strictly avoids changing existing `WorkManager` workers, processors, or data layers to prevent regression. All changes are UI-level enhancements.

- **Shared Element Transitions**: Proposed for a premium feel (Morphing buttons, smooth image expansion). Will include a fallback for low-end devices.
- **Motion Tokens**: Centralized durations and easings for consistency.

## Proposed Changes

### 1. Build Configuration
- Add `androidx.compose.animation` dependencies.

### 2. Motion System (New Files)
- `com.zaidun.photozaidun.presentation.theme.motion.MotionTokens`: Durations and Easings.
- `com.zaidun.photozaidun.presentation.navigation.NavAnimations`: Reusable enter/exit/pop transitions.

### 3. Navigation Layer
- `com.zaidun.photozaidun.presentation.navigation.WaterMarkNavHost`: Main navigation entry point with global transitions.
- `com.zaidun.photozaidun.presentation.navigation.MainScaffold`: Bottom navigation with `saveState`/`restoreState` to prevent flickering.

### 4. Animated Components
- `com.zaidun.photozaidun.presentation.components.AnimatedDialog`: Scale + Fade entry.
- `com.zaidun.photozaidun.presentation.components.AnimatedCard`: Vertical expansion.
- `com.zaidun.photozaidun.presentation.components.PressAnimatedButton`: `graphicsLayer` scale effect.

### 5. Screen Optimizations
- `HistoryScreen`: Implement staggered loading with index capping.
- `ExportLoadingScreen`: Smooth progress interpolation (Linear).
- `WatermarkPreview`: Real-time gesture tracking with `snap()` vs `tween()` for transitions.

## Verification Plan

### Automated Tests
- `gradle_build` to ensure compilation.
- `analyze_file` on modified files to detect syntax or performance issues.

### Manual Verification
- Visual inspection of screen transitions.
- Verify Bottom Tab state persistence (Scroll position remains).
- Verify Export Progress smoothness during batch processing.
