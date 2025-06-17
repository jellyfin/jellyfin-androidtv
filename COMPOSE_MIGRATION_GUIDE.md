# Jellyfin Android TV: Leanback to Compose TV Migration Guide

## Overview

This document outlines the step-by-step migration from Android Leanback Library to Jetpack Compose for TV with Material 3 Expressive theming.

## Migration Phases

### Phase 1: Foundation (COMPLETED) ✅
- [x] Added Jetpack Compose TV dependencies
- [x] Enhanced Material 3 theming with TV-specific colors
- [x] Created base TV Compose components (MediaCard, MediaRow, MediaGrid)
- [x] Set up JellyfinTvTheme with TV Material 3 support

### Phase 2: Component Migration Strategy

#### High Priority Components (Replace First)
1. **MediaCard Components**
   - Replace `CardPresenter` with `MediaCard` Compose component
   - Benefits: Better focus handling, Material 3 theming, customizable layouts

2. **Browse Fragments**
   - Convert `EnhancedBrowseFragment` to `HomeBrowseScreen` (Compose)
   - Replace `RowsSupportFragment` with `TvLazyColumn` + `MediaRow`
   - Maintain same data flow using existing ViewModels

3. **Grid Layouts**
   - Replace `HorizontalGridPresenter` with `MediaGrid`
   - Convert `BrowseGridFragment` to Compose grid layouts

#### Medium Priority Components
4. **Detail Screens**
   - Convert `FullDetailsFragment` to Compose
   - Implement TV-optimized detail layouts with Material 3

5. **Search Interface**
   - Migrate search fragments to Compose
   - Use TV-optimized input components

#### Low Priority Components (Keep for Stability)
6. **Playback Controls** (Keep Leanback for now)
   - Playback overlays are complex and stable
   - Migrate later once other components are stable

7. **Settings/Preferences** (Partially migrated)
   - Some preference screens already use Compose
   - Continue gradual migration

### Phase 3: Implementation Steps

#### Step 1: Start with Home Screen
```kotlin
// Replace HomeRowsFragment usage with:
@Composable
fun HomeScreen() {
    HomeBrowseScreen(navController = navController)
}
```

#### Step 2: Migrate Browse Views
```kotlin
// Replace EnhancedBrowseFragment with:
@Composable  
fun LibraryBrowseScreen(library: BaseItemDto) {
    val sections = remember { loadLibrarySections(library) }
    MediaBrowseLayout(
        sections = sections,
        onItemClick = { /* navigate */ },
        onItemFocus = { /* update background */ }
    )
}
```

#### Step 3: Convert Grid Views
```kotlin
// Replace BrowseGridFragment with:
@Composable
fun LibraryGridScreen(library: BaseItemDto) {
    val items = remember { loadLibraryItems(library) }
    MediaGrid(
        title = library.name ?: "",
        items = items,
        columns = 6,
        onItemClick = { /* navigate */ }
    )
}
```

### Phase 4: Navigation Integration

#### Current Navigation
- Fragment-based navigation using `NavigationRepository`
- Complex fragment transactions and back stack management

#### Target Navigation  
- Compose Navigation with TV optimizations
- Simplified navigation flows

```kotlin
@Composable
fun JellyfinTvNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeBrowseScreen(navController)
        }
        composable("library/{libraryId}") { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId")
            LibraryBrowseScreen(libraryId, navController)
        }
        // ... other destinations
    }
}
```

## Material 3 Expressive Theming

### Current Implementation
- Basic Material 3 colors defined in `JellyfinColors.kt`
- TV-specific focus states and container colors

### Enhanced Features
1. **Dynamic Color Theming**
   - Adapt colors based on currently focused content
   - Use poster/backdrop colors to influence theme

2. **TV-Optimized Components**
   - Focus rings and scaling animations
   - High contrast ratios for TV viewing distances
   - Larger touch targets adapted for D-pad navigation

3. **Expressive Typography**
   - TV-optimized font sizes and weights
   - Clear hierarchy for 10-foot UI

### Implementation Example
```kotlin
@Composable
fun DynamicJellyfinTheme(
    focusedItem: BaseItemDto?,
    content: @Composable () -> Unit
) {
    val dynamicColors = remember(focusedItem) {
        extractColorsFromItem(focusedItem)
    }
    
    JellyfinTvTheme(
        colorScheme = JellyfinTvColorScheme.Dark.copy(
            material3 = dynamicColors
        ),
        content = content
    )
}
```

## Migration Benefits

### Performance Improvements
- **Lazy composition**: Only compose visible items
- **Better memory management**: Automatic disposal of off-screen content
- **Optimized recomposition**: Fine-grained updates instead of full view refreshes

### User Experience Enhancements
- **Smoother animations**: Built-in Material 3 motion
- **Better focus handling**: Compose TV focus system
- **Responsive layouts**: Adaptive sizing for different screen sizes

### Developer Experience
- **Type safety**: Compose compile-time checks
- **Easier testing**: Compose testing framework
- **Better tooling**: Compose preview and inspection tools

## Implementation Timeline

### Week 1-2: Foundation
- [x] Set up dependencies and theming
- [x] Create base components
- [x] Implement example screens

### Week 3-4: Home Screen Migration
- [ ] Migrate `HomeRowsFragment` to `HomeBrowseScreen`
- [ ] Update navigation integration
- [ ] Test focus navigation and performance

### Week 5-6: Browse Screen Migration  
- [ ] Convert `EnhancedBrowseFragment` to Compose
- [ ] Migrate library browsing flows
- [ ] Implement grid layouts

### Week 7-8: Detail Screen Migration
- [ ] Convert `FullDetailsFragment` to Compose
- [ ] Implement TV-optimized detail layouts
- [ ] Add Material 3 interactive elements

### Week 9-10: Polish & Testing
- [ ] Performance optimization
- [ ] Accessibility improvements
- [ ] User testing and feedback

## Testing Strategy

### Focus Navigation Testing
```kotlin
@Test
fun testTvFocusNavigation() {
    composeTestRule.setContent {
        MediaBrowseLayout(sections = testSections)
    }
    
    composeTestRule.onNodeWithTag("media-card-0")
        .performKeyPress(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
        
    composeTestRule.onNodeWithTag("media-card-1")
        .assertIsFocused()
}
```

### Performance Testing
- Monitor composition performance with large datasets
- Test smooth scrolling with 60fps target
- Memory usage profiling during navigation

## Risk Mitigation

### Gradual Migration
- Keep existing Leanback components as fallback
- Feature flags to toggle between old/new implementations
- A/B testing for user acceptance

### Compatibility
- Maintain existing data layer and ViewModels
- Preserve navigation patterns during transition
- Ensure accessibility features continue to work

## Resources

### Documentation
- [Jetpack Compose for TV](https://developer.android.com/jetpack/compose/tv)
- [Material 3 for TV](https://material.io/blog/material-you-tv)
- [TV App Quality Guidelines](https://developer.android.com/docs/quality-guidelines/tv-app-quality)

### Code Examples
- See `ui/composable/tv/` for base components
- See `ui/browsing/compose/` for migration examples
- Existing Compose usage in search and next-up components

## Next Steps

1. **Review and test current implementation**
   - Test focus navigation in example components
   - Validate theming implementation
   - Performance test with sample data

2. **Plan first migration target**
   - Choose starting point (recommend Home Screen)
   - Set up feature flag system
   - Create migration timeline

3. **Team coordination**
   - Training on Compose TV best practices
   - Code review processes for Compose components
   - Testing protocols for TV-specific features
