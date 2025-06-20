#!/bin/bash

# Test workflow locally to debug commit hash issues
# This script simulates the GitHub Actions workflow steps

echo "🧪 Local Workflow Test"
echo "======================"
echo ""

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    echo "❌ Not in a git repository!"
    exit 1
fi

echo "📋 Repository Information:"
echo "Current branch: $(git branch --show-current)"
echo "Repository root: $(git rev-parse --show-toplevel)"
echo ""

# Test commit hash validation
echo "🔍 Testing Commit Hash Validation:"
echo "=================================="

# Get the latest commit for testing
LATEST_COMMIT=$(git rev-parse HEAD)
echo "Testing with latest commit: $LATEST_COMMIT"

# Test various validation methods
echo ""
echo "Method 1 - git cat-file -e:"
if git cat-file -e "$LATEST_COMMIT" 2>/dev/null; then
    echo "✅ Valid"
else
    echo "❌ Invalid"
fi

echo ""
echo "Method 2 - git cat-file -e with ^{commit}:"
if git cat-file -e "$LATEST_COMMIT^{commit}" 2>/dev/null; then
    echo "✅ Valid"
else
    echo "❌ Invalid"
fi

echo ""
echo "Method 3 - git rev-parse:"
if git rev-parse "$LATEST_COMMIT" >/dev/null 2>&1; then
    echo "✅ Valid"
else
    echo "❌ Invalid"
fi

echo ""
echo "📊 Testing Cherry-pick Scripts:"
echo "==============================="

# Check if scripts exist
if [ ! -f "check_cherry_pick_conflicts.sh" ]; then
    echo "❌ Basic script missing: check_cherry_pick_conflicts.sh"
else
    echo "✅ Basic script found"
    chmod +x check_cherry_pick_conflicts.sh
fi

if [ ! -f "check_cherry_pick_conflicts_enhanced.sh" ]; then
    echo "❌ Enhanced script missing: check_cherry_pick_conflicts_enhanced.sh"
else
    echo "✅ Enhanced script found"
    chmod +x check_cherry_pick_conflicts_enhanced.sh
fi

echo ""
echo "🎯 Testing with Real Commit:"
echo "============================"
echo "Commit: $LATEST_COMMIT"
echo "Target: master"

if [ -f "check_cherry_pick_conflicts_enhanced.sh" ]; then
    echo ""
    echo "Enhanced script output:"
    echo "----------------------"
    ./check_cherry_pick_conflicts_enhanced.sh "$LATEST_COMMIT" master --json | head -20
fi

echo ""
echo "🔍 Repository State Debug Info:"
echo "==============================="
echo "Recent commits:"
git log --oneline -5

echo ""
echo "Remote branches:"
git branch -r | head -10

echo ""
echo "Git status:"
git status --porcelain

echo ""
echo "Git config:"
git config --list | grep -E "(user\.|remote\.)" | head -5

echo ""
echo "✅ Local test completed!"
echo ""
echo "💡 To test with a specific commit:"
echo "   $0 <commit_hash>"

# If a commit hash was provided as argument, test with that
if [ ! -z "$1" ]; then
    echo ""
    echo "🎯 Testing with provided commit: $1"
    echo "=================================="
    
    if git cat-file -e "$1" 2>/dev/null; then
        echo "✅ Commit $1 is valid"
        if [ -f "check_cherry_pick_conflicts_enhanced.sh" ]; then
            echo "Running analysis..."
            ./check_cherry_pick_conflicts_enhanced.sh "$1" master
        fi
    else
        echo "❌ Commit $1 is invalid or not found"
        echo "Trying to resolve..."
        git rev-parse "$1" 2>&1 || echo "Failed to resolve commit"
    fi
fi
