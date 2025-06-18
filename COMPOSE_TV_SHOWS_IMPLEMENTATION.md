## Compose TV Shows Screen Implementation

### Overview
This implements a new Compose-based TV Shows screen that replaces the Leanback components with Jetpack Compose for TV. The implementation follows the same pattern as the existing Compose Movies screen but is specifically designed for TV show navigation:

**TV Shows → Seasons → Episodes**

### Key Components Created

#### 1. `ComposeTvShowsScreen.kt`
- Main Compose UI screen with immersive list layout
- Loading, error, and empty states
- Uses `MultiSectionImmersiveList` for consistent TV experience

#### 2. `ComposeTvShowsViewModel.kt`
- Handles data loading for TV shows, seasons, and episodes
- Provides multiple sections:
  - **Continue Watching**: Episodes user has started but not finished
  - **Next Up**: Next episodes to watch for series in progress
  - **Latest Episodes**: Recently added episodes
  - **All TV Series**: Main grid showing all series (vertical grid layout)
  - **Favorite Series**: User's favorite TV series
- Proper navigation handling for Series → Season → Episode flow

#### 3. `ComposeTvShowsFragment.kt`
- Fragment wrapper that hosts the Compose screen
- Integrates with existing navigation system

### Integration Points

#### Feature Flag
- Added `ENABLE_COMPOSE_TVSHOWS = true` in `ComposeFeatureFlags.kt`
- Allows gradual rollout and easy toggle between old/new implementation

#### Navigation
- Added `composeTvShowsBrowser()` destination in `Destinations.kt`
- Updated `ItemLauncher.java` to use Compose TV shows screen when feature flag is enabled
- Falls back to existing Leanback implementation when disabled

#### Dependency Injection
- Registered `ComposeTvShowsViewModel` in `AppModule.kt`
- Uses Koin for dependency injection like other ViewModels

### Navigation Flow

1. **TV Shows Library View**: 
   - Displays series in a vertical grid
   - Shows horizontal rows for Continue Watching, Next Up, Latest Episodes, etc.

2. **Series Selection**: 
   - Clicking on a series navigates to item details (seasons view)
   - Uses existing `Destinations.itemDetails(item.id)` for consistency

3. **Season Selection**: 
   - From series details, user can navigate to seasons
   - Seasons also use `Destinations.itemDetails(season.id)`

4. **Episode Selection**: 
   - From season view, user can select episodes
   - Episodes navigate to playback or episode details

### Benefits

1. **Modern UI**: Uses Compose for TV with Material 3 theming
2. **Immersive Experience**: Backdrop images and smooth focus transitions
3. **Rich Content**: Multiple content sections with different layouts
4. **TV-Optimized**: Designed specifically for TV navigation patterns
5. **Consistent**: Follows same patterns as Compose Movies screen
6. **Backwards Compatible**: Feature flag allows fallback to Leanback

### Usage

When `ComposeFeatureFlags.ENABLE_COMPOSE_TVSHOWS = true`, the TV Shows library will automatically use the new Compose implementation. Users will see:

- A modern, immersive TV interface
- Multiple content sections (Continue Watching, Next Up, etc.)
- Smooth navigation between TV shows, seasons, and episodes
- Backdrop images that update based on focused content
- TV-optimized grid and list layouts

The implementation maintains full compatibility with the existing data layer and navigation system, ensuring a seamless transition for users.
