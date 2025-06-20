#!/bin/bash

# Test script for cherry-pick conflict detection
# This demonstrates how to use the conflict detection tools

echo "🧪 Cherry-pick Conflict Detection Test Suite"
echo "============================================="
echo ""

# Check if scripts exist
if [ ! -f "check_cherry_pick_conflicts.sh" ]; then
    echo "❌ Basic script not found: check_cherry_pick_conflicts.sh"
    exit 1
fi

if [ ! -f "check_cherry_pick_conflicts_enhanced.sh" ]; then
    echo "❌ Enhanced script not found: check_cherry_pick_conflicts_enhanced.sh"
    exit 1
fi

# Make scripts executable
chmod +x check_cherry_pick_conflicts.sh
chmod +x check_cherry_pick_conflicts_enhanced.sh

echo "✅ Scripts found and made executable"
echo ""

# Test with latest commit
LATEST_COMMIT=$(git rev-parse HEAD)
echo "🔍 Testing with latest commit: $LATEST_COMMIT"
echo ""

echo "📋 Basic Analysis:"
echo "=================="
./check_cherry_pick_conflicts.sh "$LATEST_COMMIT" master
echo ""

echo "📊 Enhanced Analysis (Text):"
echo "============================"
./check_cherry_pick_conflicts_enhanced.sh "$LATEST_COMMIT" master
echo ""

echo "📊 Enhanced Analysis (JSON):"
echo "============================"
./check_cherry_pick_conflicts_enhanced.sh "$LATEST_COMMIT" master --json
echo ""

echo "✅ Test suite completed!"
echo ""
echo "💡 Usage examples:"
echo "  Basic: ./check_cherry_pick_conflicts.sh <commit> [branch]"
echo "  Enhanced: ./check_cherry_pick_conflicts_enhanced.sh <commit> [branch] [--json]"
echo "  GitHub comment: /check-cherry-pick <commit> [branch]"
