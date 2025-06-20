#!/bin/bash

# Enhanced Cherry-pick Conflict Detection Script with Smart Recommendations
# Usage: ./check_cherry_pick_conflicts_enhanced.sh <commit_hash> [target_branch] [--json]

# Load configuration if available
CONFIG_FILE=".cherry-pick-config"
if [ -f "$CONFIG_FILE" ]; then
    echo "📝 Loading configuration from $CONFIG_FILE"
    source "$CONFIG_FILE"
else
    # Default configuration values
    CONFLICT_THRESHOLD_LOW=20
    CONFLICT_THRESHOLD_MEDIUM=50
    SCORE_MERGE_CONFLICTS=40
    SCORE_RECENT_MODIFICATIONS=15
    SCORE_MEDIUM_MODIFICATION
