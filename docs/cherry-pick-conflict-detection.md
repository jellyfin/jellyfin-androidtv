# Cherry-pick Conflict Detection System

This repository includes automated tools for detecting potential conflicts when cherry-picking commits between branches.

## 🚀 Quick Start

### Manual Script Usage

1. **Basic conflict check:**
   ```bash
   ./check_cherry_pick_conflicts.sh <commit_hash> [target_branch]
   ```

2. **Enhanced analysis with smart recommendations:**
   ```bash
   ./check_cherry_pick_conflicts_enhanced.sh <commit_hash> [target_branch] [--json]
   ```

### GitHub Actions Integration

The system automatically runs conflict analysis in several scenarios:

#### 1. Manual Trigger
Go to Actions → Cherry-pick Conflict Check → Run workflow
- Specify the commit hash to analyze
- Choose target branch (default: master)
- Select output format (text/json)

#### 2. Pull Request Analysis
- Automatically runs on PRs with "cherry-pick" in the title
- Posts analysis results as a comment on the PR

#### 3. Comment-triggered Analysis
Comment on any issue or PR with:
```
/check-cherry-pick <commit_hash> [target_branch]
```

Examples:
- `/check-cherry-pick abc123def`
- `/check-cherry-pick abc123def master`
- `/check-cherry-pick abc123def develop`

## 📊 Understanding the Analysis

### Risk Levels

| Risk Level | Score Range | Description |
|------------|-------------|-------------|
| **LOW** | 0-19 | ✅ Safe to cherry-pick |
| **MEDIUM** | 20-49 | ⚠️ Proceed with caution |
| **HIGH** | 50+ | ❌ Manual intervention likely needed |

### Scoring Factors

The conflict score is calculated based on:

- **Merge-tree conflicts** (+40 points): Direct conflicts detected
- **Recent file modifications** (+15 points per file): Files modified in last 7 days
- **Medium-term modifications** (+5 points per file): Files modified in last 30 days
- **Large commits** (+20 points): Changes affecting 100+ lines
- **Medium commits** (+10 points): Changes affecting 50+ lines
- **Code file changes** (+5 points): Java, Kotlin, XML files
- **Build file changes** (+15 points): Gradle, Maven, package files

### Analysis Methods

The system uses multiple detection methods:

1. **Git merge-tree analysis**: Most reliable conflict detection
2. **File modification history**: Identifies potential conflict areas
3. **Dry-run cherry-pick**: Actual test in a temporary branch
4. **Commit impact analysis**: Size and type of changes

## 🎯 Smart Recommendations

Based on the analysis, the system provides context-aware recommendations:

### Low Risk (Score < 20)
- ✅ Safe to cherry-pick
- 💡 Consider off-peak hours
- 📝 Document in commit message

### Medium Risk (Score 20-49)
- ⚠️ Moderate conflict risk
- 🔍 Review modified files
- 🧪 Test in separate branch
- 👥 Get team review

### High Risk (Score 50+)
- ❌ High conflict risk
- 🛠️ Prepare for manual resolution
- 📋 Create backup branch
- 🔄 Consider rebasing first
- 👨‍💻 Use experienced developer

### Special Situations
- **Build files modified**: Clean rebuild required
- **Large commits**: Consider splitting changes
- **Multiple recent modifications**: Update branch first

## 🔧 Script Features

### Basic Script (`check_cherry_pick_conflicts.sh`)

- Git merge-tree analysis
- File modification tracking
- Recent changes detection
- Dry-run cherry-pick test
- Detailed commit information

### Enhanced Script (`check_cherry_pick_conflicts_enhanced.sh`)

All basic features plus:
- Risk scoring algorithm
- Smart recommendations
- JSON output support
- Context-aware analysis
- Comprehensive reporting

### Usage Examples

```bash
# Basic analysis
./check_cherry_pick_conflicts.sh abc123def master

# Enhanced analysis with text output
./check_cherry_pick_conflicts_enhanced.sh abc123def master

# Enhanced analysis with JSON output
./check_cherry_pick_conflicts_enhanced.sh abc123def master --json

# Help information
./check_cherry_pick_conflicts_enhanced.sh --help
```

## 🤖 GitHub Actions Features

### Automatic Triggers
- ✅ Manual workflow dispatch
- ✅ Pull requests with "cherry-pick" in title
- ✅ Comment commands (`/check-cherry-pick`)
- ✅ Optional scheduled monitoring

### Output Options
- 📝 Formatted comments on PRs/issues
- 📊 JSON artifacts for automation
- 📈 Risk-based job status
- 🔔 Configurable notifications

### Integration Points
- Works with existing CI/CD workflows
- Integrates with PR review process
- Supports automation and scripting
- Provides actionable feedback

## 📋 Best Practices

### Before Cherry-picking
1. Run conflict analysis first
2. Review recommendations carefully
3. Create backup branches for high-risk changes
4. Test in isolated environment

### During Cherry-picking
1. Follow risk-appropriate procedures
2. Document any manual resolutions
3. Test thoroughly after conflicts
4. Update related documentation

### After Cherry-picking
1. Verify build integrity
2. Run full test suites
3. Update tracking systems
4. Notify relevant team members

## 🔍 Troubleshooting

### Common Issues

**Invalid commit hash:**
- Ensure commit exists in repository
- Use full SHA or minimum 7 characters
- Check if commit is accessible from current branch

**Permission errors:**
- Make scripts executable: `chmod +x *.sh`
- Ensure Git repository access
- Check GitHub token permissions

**Unexpected results:**
- Verify target branch exists
- Check repository state
- Ensure clean working directory

### Getting Help

1. Check script help: `./check_cherry_pick_conflicts_enhanced.sh --help`
2. Review GitHub Actions logs
3. Examine detailed analysis output
4. Consult team documentation

## 📈 Monitoring and Metrics

The system can be extended to track:
- Cherry-pick success rates
- Conflict resolution times
- Risk assessment accuracy
- Team productivity metrics

Consider integrating with monitoring tools for production use.
