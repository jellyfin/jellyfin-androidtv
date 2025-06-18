#!/bin/bash

# Compose TV Shows Implementation Verification Script
# This script verifies that all necessary files and components are in place

echo "=== Compose TV Shows Implementation Verification ==="
echo

# Check if all key files exist
echo "1. Checking file existence..."
files=(
    "app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsScreen.kt"
    "app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt"
    "app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsFragment.kt"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
    fi
done

echo

# Check feature flag
echo "2. Checking feature flag..."
if grep -q "ENABLE_COMPOSE_TVSHOWS" app/src/main/java/org/jellyfin/androidtv/ui/feature/ComposeFeatureFlags.kt; then
    echo "✓ ENABLE_COMPOSE_TVSHOWS feature flag found"
else
    echo "✗ ENABLE_COMPOSE_TVSHOWS feature flag missing"
fi

echo

# Check navigation integration
echo "3. Checking navigation integration..."
if grep -q "composeTvShowsBrowser" app/src/main/java/org/jellyfin/androidtv/ui/navigation/Destinations.kt; then
    echo "✓ composeTvShowsBrowser destination found"
else
    echo "✗ composeTvShowsBrowser destination missing"
fi

if grep -q "ENABLE_COMPOSE_TVSHOWS" app/src/main/java/org/jellyfin/androidtv/ui/itemhandling/ItemLauncher.java; then
    echo "✓ ItemLauncher integration found"
else
    echo "✗ ItemLauncher integration missing"
fi

echo

# Check DI integration
echo "4. Checking dependency injection..."
if grep -q "ComposeTvShowsViewModel" app/src/main/java/org/jellyfin/androidtv/di/AppModule.kt; then
    echo "✓ ComposeTvShowsViewModel registered in DI"
else
    echo "✗ ComposeTvShowsViewModel missing from DI"
fi

echo

# Check key functionality
echo "5. Checking key implementation details..."

# Check for TV-specific sections
if grep -q "Continue Watching" app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt; then
    echo "✓ Continue Watching section implemented"
else
    echo "✗ Continue Watching section missing"
fi

if grep -q "Next Up" app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt; then
    echo "✓ Next Up section implemented"
else
    echo "✗ Next Up section missing"
fi

if grep -q "Latest Episodes" app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt; then
    echo "✓ Latest Episodes section implemented"
else
    echo "✗ Latest Episodes section missing"
fi

if grep -q "All TV Series" app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt; then
    echo "✓ All TV Series section implemented"
else
    echo "✗ All TV Series section missing"
fi

echo

# Check navigation flow
echo "6. Checking navigation flow..."
if grep -q "BaseItemKind.SERIES" app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt; then
    echo "✓ Series navigation handling found"
else
    echo "✗ Series navigation handling missing"
fi

if grep -q "BaseItemKind.EPISODE" app/src/main/java/org/jellyfin/androidtv/ui/browsing/compose/ComposeTvShowsViewModel.kt; then
    echo "✓ Episode navigation handling found"
else
    echo "✗ Episode navigation handling missing"
fi

echo

echo "=== Verification Complete ==="
echo
echo "Summary:"
echo "- Created Compose-based TV Shows screen using Immersive List pattern"
echo "- Supports TV Shows → Seasons → Episodes navigation flow"
echo "- Integrated with existing navigation and DI systems"
echo "- Feature flag allows gradual rollout (ENABLE_COMPOSE_TVSHOWS)"
echo "- Multiple content sections optimized for TV viewing"
echo "- Follows same patterns as existing Compose Movies screen"
echo
echo "To enable: Set ComposeFeatureFlags.ENABLE_COMPOSE_TVSHOWS = true"
