#!/bin/bash

# Enhanced Cherry-pick Conflict Detection Script with Smart Recommendations
# Usage: ./check_cherry_pick_conflicts_enhanced.sh <commit_hash> [target_branch] [--json]

COMMIT_HASH=""
TARGET_BRANCH="HEAD"
OUTPUT_FORMAT="text"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --json)
            OUTPUT_FORMAT="json"
            shift
            ;;
        --help)
            echo "Usage: $0 <commit_hash> [target_branch] [--json]"
            echo "Options:"
            echo "  --json    Output results in JSON format"
            echo "  --help    Show this help message"
            exit 0
            ;;
        *)
            if [ -z "$COMMIT_HASH" ]; then
                COMMIT_HASH=$1
            elif [ "$TARGET_BRANCH" = "HEAD" ]; then
                TARGET_BRANCH=$1
            fi
            shift
            ;;
    esac
done

if [ -z "$COMMIT_HASH" ]; then
    echo "Usage: $0 <commit_hash> [target_branch] [--json]"
    echo "Example: $0 abc123def master --json"
    exit 1
fi

# Initialize variables
CONFLICT_SCORE=0
RISK_LEVEL="LOW"
RECOMMENDATIONS=()
ANALYSIS_RESULTS=()

# Function to add recommendation
add_recommendation() {
    RECOMMENDATIONS+=("$1")
}

# Function to add analysis result
add_analysis() {
    ANALYSIS_RESULTS+=("$1")
}

# Function to calculate conflict score
calculate_conflict_score() {
    local score=0
    
    # Check merge-tree conflicts
    MERGE_BASE=$(git merge-base $TARGET_BRANCH $COMMIT_HASH 2>/dev/null)
    if [ $? -eq 0 ]; then
        MERGE_RESULT=$(git merge-tree $MERGE_BASE $TARGET_BRANCH $COMMIT_HASH 2>/dev/null)
        if echo "$MERGE_RESULT" | grep -q "<<<<<<< "; then
            score=$((score + 40))
            add_analysis "merge_tree_conflicts: true"
        else
            add_analysis "merge_tree_conflicts: false"
        fi
    fi
    
    # Check file modification recency
    MODIFIED_FILES=$(git show --name-only --pretty=format: $COMMIT_HASH 2>/dev/null | grep -v '^$')
    RECENT_MODIFICATIONS=0
    
    for file in $MODIFIED_FILES; do
        if [ -f "$file" ]; then
            LAST_CHANGE_DATE=$(git log -1 --pretty=format:"%ct" $TARGET_BRANCH -- "$file" 2>/dev/null)
            COMMIT_DATE=$(git show -s --pretty=format:"%ct" $COMMIT_HASH 2>/dev/null)
            
            if [ ! -z "$LAST_CHANGE_DATE" ] && [ ! -z "$COMMIT_DATE" ]; then
                DAYS_DIFF=$(( (COMMIT_DATE - LAST_CHANGE_DATE) / 86400 ))
                if [ $DAYS_DIFF -lt 7 ]; then
                    score=$((score + 15))
                    RECENT_MODIFICATIONS=$((RECENT_MODIFICATIONS + 1))
                elif [ $DAYS_DIFF -lt 30 ]; then
                    score=$((score + 5))
                fi
            fi
        fi
    done
    
    add_analysis "recent_modifications: $RECENT_MODIFICATIONS"
    
    # Check commit size
    LINES_CHANGED=$(git show --stat $COMMIT_HASH 2>/dev/null | tail -1 | grep -o '[0-9]\+ insertions\|[0-9]\+ deletions' | grep -o '[0-9]\+' | awk '{sum += $1} END {print sum}')
    if [ ! -z "$LINES_CHANGED" ]; then
        if [ $LINES_CHANGED -gt 100 ]; then
            score=$((score + 20))
            add_analysis "large_commit: true"
        elif [ $LINES_CHANGED -gt 50 ]; then
            score=$((score + 10))
            add_analysis "medium_commit: true"
        else
            add_analysis "small_commit: true"
        fi
    fi
    
    # Check file types
    if echo "$MODIFIED_FILES" | grep -q "\.java\|\.kt\|\.xml"; then
        score=$((score + 5))
        add_analysis "code_files_modified: true"
    fi
    
    if echo "$MODIFIED_FILES" | grep -q "build\.gradle\|pom\.xml\|package\.json"; then
        score=$((score + 15))
        add_analysis "build_files_modified: true"
    fi
    
    CONFLICT_SCORE=$score
}

# Function to determine risk level and generate recommendations
generate_recommendations() {
    if [ $CONFLICT_SCORE -lt 20 ]; then
        RISK_LEVEL="LOW"
        add_recommendation "✅ Safe to cherry-pick - Low conflict risk"
        add_recommendation "💡 Consider cherry-picking during off-peak hours"
        add_recommendation "📝 Document the cherry-pick in your commit message"
    elif [ $CONFLICT_SCORE -lt 50 ]; then
        RISK_LEVEL="MEDIUM"
        add_recommendation "⚠️ Moderate conflict risk - proceed with caution"
        add_recommendation "🔍 Review the modified files before cherry-picking"
        add_recommendation "🧪 Test in a separate branch first"
        add_recommendation "👥 Consider having a team member review the changes"
    else
        RISK_LEVEL="HIGH"
        add_recommendation "❌ High conflict risk - manual intervention likely needed"
        add_recommendation "🛠️ Prepare for manual conflict resolution"
        add_recommendation "📋 Create a backup branch before attempting"
        add_recommendation "🔄 Consider rebasing your branch first"
        add_recommendation "👨‍💻 Have an experienced developer handle this cherry-pick"
    fi
    
    # Additional context-specific recommendations
    if echo "${ANALYSIS_RESULTS[@]}" | grep -q "build_files_modified: true"; then
        add_recommendation "⚙️ Build files modified - verify dependencies after cherry-pick"
        add_recommendation "🧹 Clean and rebuild project after cherry-pick"
    fi
    
    if echo "${ANALYSIS_RESULTS[@]}" | grep -q "large_commit: true"; then
        add_recommendation "📦 Large commit detected - consider splitting into smaller changes"
        add_recommendation "🔍 Review each file change individually"
    fi
    
    if [ $RECENT_MODIFICATIONS -gt 3 ]; then
        add_recommendation "🔄 Multiple recent modifications - consider updating your branch first"
        add_recommendation "📅 Schedule cherry-pick after current development cycle"
    fi
}

# Function to output results
output_results() {
    if [ "$OUTPUT_FORMAT" = "json" ]; then
        # JSON output
        echo "{"
        echo "  \"commit_hash\": \"$COMMIT_HASH\","
        echo "  \"target_branch\": \"$TARGET_BRANCH\","
        echo "  \"conflict_score\": $CONFLICT_SCORE,"
        echo "  \"risk_level\": \"$RISK_LEVEL\","
        echo "  \"analysis\": {"
        
        # Parse analysis results
        for result in "${ANALYSIS_RESULTS[@]}"; do
            key=$(echo "$result" | cut -d: -f1)
            value=$(echo "$result" | cut -d: -f2- | sed 's/^ *//')
            if [[ "$value" =~ ^[0-9]+$ ]]; then
                echo "    \"$key\": $value,"
            elif [ "$value" = "true" ] || [ "$value" = "false" ]; then
                echo "    \"$key\": $value,"
            else
                echo "    \"$key\": \"$value\","
            fi
        done | sed '$ s/,$//'
        
        echo "  },"
        echo "  \"recommendations\": ["
        
        for i in "${!RECOMMENDATIONS[@]}"; do
            if [ $i -eq $((${#RECOMMENDATIONS[@]} - 1)) ]; then
                echo "    \"${RECOMMENDATIONS[$i]}\""
            else
                echo "    \"${RECOMMENDATIONS[$i]}\","
            fi
        done
        
        echo "  ],"
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\""
        echo "}"
    else
        # Text output
        echo "=== Enhanced Cherry-pick Conflict Analysis ==="
        echo "Commit: $COMMIT_HASH"
        echo "Target Branch: $TARGET_BRANCH"
        echo "Analysis Date: $(date)"
        echo ""
        echo "🎯 RISK ASSESSMENT"
        echo "=================="
        echo "Conflict Score: $CONFLICT_SCORE/100"
        echo "Risk Level: $RISK_LEVEL"
        echo ""
        echo "📊 ANALYSIS DETAILS"
        echo "=================="
        for result in "${ANALYSIS_RESULTS[@]}"; do
            echo "  $result"
        done
        echo ""
        echo "💡 SMART RECOMMENDATIONS"
        echo "======================="
        for rec in "${RECOMMENDATIONS[@]}"; do
            echo "  $rec"
        done
        echo ""
        
        # Run original analysis for detailed output
        echo "📋 DETAILED CONFLICT ANALYSIS"
        echo "============================"
        
        # Check if original script exists and run it
        if [ -f "check_cherry_pick_conflicts.sh" ]; then
            ./check_cherry_pick_conflicts.sh "$COMMIT_HASH" "$TARGET_BRANCH" | tail -n +4
        else
            echo "Original conflict detection script not found."
        fi
    fi
}

# Main execution
calculate_conflict_score
generate_recommendations
output_results
