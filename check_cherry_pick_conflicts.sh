#!/bin/bash

# Cherry-pick Conflict Detection Script
# Usage: ./check_cherry_pick_conflicts.sh <commit_hash> [target_branch]

COMMIT_HASH=$1
TARGET_BRANCH=${2:-HEAD}

if [ -z "$COMMIT_HASH" ]; then
    echo "Usage: $0 <commit_hash> [target_branch]"
    echo "Example: $0 abc123def master"
    exit 1
fi

echo "=== Cherry-pick Conflict Analysis for commit: $COMMIT_HASH ==="
echo "Target branch: $TARGET_BRANCH"
echo ""

# Method 1: git merge-tree (most reliable)
echo "1. MERGE-TREE ANALYSIS (Most Reliable)"
echo "======================================"
MERGE_BASE=$(git merge-base $TARGET_BRANCH $COMMIT_HASH)
MERGE_RESULT=$(git merge-tree $MERGE_BASE $TARGET_BRANCH $COMMIT_HASH)

if echo "$MERGE_RESULT" | grep -q "<<<<<<< "; then
    echo "❌ CONFLICTS DETECTED!"
    echo "Conflicted files:"
    echo "$MERGE_RESULT" | grep -E "^(<<<<<<< |=======|>>>>>>> )" | head -10
else
    echo "✅ NO CONFLICTS - Safe to cherry-pick"
fi
echo ""

# Method 2: File modification analysis
echo "2. FILE MODIFICATION ANALYSIS"
echo "============================="
echo "Files that would be modified by this commit:"
git show --name-only --pretty=format: $COMMIT_HASH | grep -v '^$'
echo ""

# Method 3: Check if files were recently modified in target branch
echo "3. RECENT MODIFICATIONS CHECK"
echo "============================"
echo "Checking if any files were recently modified in target branch..."
MODIFIED_FILES=$(git show --name-only --pretty=format: $COMMIT_HASH | grep -v '^$')
for file in $MODIFIED_FILES; do
    if [ -f "$file" ]; then
        LAST_CHANGE=$(git log -1 --pretty=format:"%h %s" $TARGET_BRANCH -- "$file" 2>/dev/null)
        if [ ! -z "$LAST_CHANGE" ]; then
            echo "  $file: $LAST_CHANGE"
        fi
    fi
done
echo ""

# Method 4: Dry-run cherry-pick
echo "4. DRY-RUN CHERRY-PICK TEST"
echo "=========================="
echo "Attempting dry-run cherry-pick..."

# Create a temporary branch for testing
TEMP_BRANCH="temp-cherry-pick-test-$$"
git checkout -b $TEMP_BRANCH $TARGET_BRANCH >/dev/null 2>&1

if git cherry-pick --no-commit $COMMIT_HASH >/dev/null 2>&1; then
    echo "✅ DRY-RUN SUCCESSFUL - No conflicts detected"
    git reset --hard HEAD >/dev/null 2>&1
else
    echo "❌ DRY-RUN FAILED - Conflicts would occur"
    git cherry-pick --abort >/dev/null 2>&1
fi

# Clean up
git checkout - >/dev/null 2>&1
git branch -D $TEMP_BRANCH >/dev/null 2>&1

echo ""
echo "5. COMMIT DETAILS"
echo "================"
git show --stat $COMMIT_HASH

