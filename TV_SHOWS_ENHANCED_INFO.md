# TV Shows Enhanced Information Display

## Overview
Enhanced the Jellyfin Android TV app's Compose implementation to display comprehensive metadata when browsing TV shows, seasons, and episodes.

## Changes Made

### 1. Enhanced Information Overlay (`ImmersiveList.kt`)

**Added detailed metadata display including:**
- **Status indicator**: Shows "Continuing", "Ended", or "Cancelled" with color coding (green/red)
- **Runtime information**: Shows episode/movie duration in minutes
- **Video quality badges**: Displays 4K, HD, 720p, or SD based on video stream height
- **Audio format indicators**: Shows audio codec (e.g., AC3, DTS, AAC)
- **Enhanced rating display**: Existing star ratings and Rotten Tomatoes scores

**Color coding:**
- Continuing shows: Green
- Ended/Cancelled shows: Red
- Video quality: Jellyfin primary color with dark background
- Audio format: White text with dark background

### 2. Enhanced Series Detail Screen (`ComposeSeriesScreen.kt`)

**Created `SeriesDetailImmersiveList` component:**
- Custom information overlay specifically for TV series
- Shows series-specific metadata when no item is focused
- Switches to focused item metadata when browsing seasons/episodes

**Added `SeriesInformationOverlay` component:**
- **Series year range**: "2020 - 2025" or "2020 - Present" for continuing shows
- **Episode count**: Total number of episodes in the series
- **Genres display**: Up to 4 genre tags with styled backgrounds
- **Status badge**: Prominently displays whether series is continuing or ended
- **Enhanced description**: More lines for series overview when not focused on specific item

### 3. Updated Item Repository (`ItemRepository.kt`)

**Added metadata fields:**
- `ItemFields.STATUS` - For series status (continuing/ended)
- `ItemFields.AIR_TIME` - For air time information
- `ItemFields.PRODUCTION_YEAR` - For production year
- `ItemFields.END_DATE` - For series end date

## User Experience Improvements

### When browsing TV Shows:
1. **Series Information**: Shows comprehensive series details including status, year range, total episodes, and genres
2. **Focus behavior**: Series information displayed when no specific season is focused
3. **Smooth transitions**: When focusing on seasons, overlay switches to season-specific information

### When browsing Seasons:
1. **Episode details**: Shows runtime, video quality, and audio format for individual episodes
2. **Technical information**: Video resolution (4K/HD/720p/SD) and audio codec clearly displayed
3. **Enhanced metadata**: Production year, ratings, and descriptions for focused items

### Visual Enhancements:
- **Color-coded status**: Immediately see if a show is continuing (green) or ended (red)
- **Quality badges**: Quick identification of video quality with prominent badges
- **Genre tags**: Easy genre identification with styled background tags
- **Organized layout**: Information logically grouped and well-spaced

## Implementation Details

### Components Added:
- `SeriesDetailImmersiveList`: Main container for series detail view
- `SeriesInformationOverlay`: Custom overlay showing series-specific metadata

### Enhanced Components:
- `ContentInformationOverlay`: Added status, runtime, video quality, and audio format indicators
- All existing functionality preserved while adding new features

### Metadata Handling:
- Automatically detects and displays available metadata
- Graceful handling of missing information
- Consistent styling across all information types

## Testing Recommendations

When testing on actual Android TV:
1. **Navigate to TV Shows** → Select a show → Verify series information display
2. **Check status indicators** for continuing vs ended shows
3. **Browse seasons** → Verify episode information shows video quality and runtime
4. **Test focus behavior** → Ensure smooth transitions between series and item information
5. **Verify genre display** for shows with multiple genres
6. **Check video quality badges** for different resolution content

## Future Enhancements

Potential additional improvements:
- **Next episode to watch**: Highlight next unwatched episode
- **Season progress indicators**: Show completion percentage
- **Cast and crew information**: Display main actors
- **Related shows**: Show similar series recommendations
- **Detailed technical specs**: Bitrate, file size, container format
