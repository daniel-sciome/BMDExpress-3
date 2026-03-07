#!/usr/bin/env bash
# sync-from-upstream.sh — Keep bmdx-core in sync with a parent branch.
#
# The bmdx-core branch is a curated subset of the full BMDExpress 3 source
# tree.  When the parent branch (e.g., experiment-metadata) gets changes to
# model classes, services, or utilities that bmdx-core depends on, this
# script merges those changes and verifies compilation.
#
# How it works:
#   1. Merge the upstream branch into bmdx-core (preserving our deletions).
#   2. Try to compile.  If it succeeds, we're done.
#   3. If compilation fails with "cannot find symbol" errors, extract the
#      missing class names, find them in the upstream branch, restore them,
#      and retry.  Repeat up to 5 rounds (transitive deps).
#   4. If it still fails after 5 rounds, print the remaining errors for
#      manual resolution.
#
# Usage:
#   ./scripts/sync-from-upstream.sh [upstream-branch]
#
# Defaults to "experiment-metadata" if no branch is specified.
# Must be run from the BMDExpress-3 repo root, on the bmdx-core branch.

set -euo pipefail

UPSTREAM="${1:-experiment-metadata}"
MAX_ROUNDS=5

# --- Sanity checks ---

CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "bmdx-core" ]; then
    echo "ERROR: Must be on the bmdx-core branch (currently on '$CURRENT_BRANCH')"
    exit 1
fi

if ! git rev-parse --verify "$UPSTREAM" >/dev/null 2>&1; then
    echo "ERROR: Upstream branch '$UPSTREAM' does not exist"
    exit 1
fi

# --- Step 1: Merge upstream ---

echo "=== Merging $UPSTREAM into bmdx-core ==="
# Use --no-edit to accept the default merge message.
# Merge will respect bmdx-core's deletions — files we removed won't
# reappear unless the upstream explicitly modified them (conflict).
if ! git merge "$UPSTREAM" --no-edit; then
    echo ""
    echo "Merge conflicts detected.  Resolve them manually, then re-run this script"
    echo "with no arguments to just do the compile-and-restore cycle:"
    echo "  ./scripts/sync-from-upstream.sh --compile-only"
    exit 1
fi

# --- Step 2: Build classpath ---

# Ensure dependencies are downloaded
if [ ! -d target/deps ] || [ -z "$(ls target/deps/*.jar 2>/dev/null)" ]; then
    echo "=== Downloading dependencies ==="
    mvn dependency:copy-dependencies -DoutputDirectory=target/deps -DincludeScope=compile -q
fi

CP=$(echo target/deps/*.jar | tr ' ' ':')

# --- Step 3: Iterative compile-and-restore ---

for round in $(seq 1 $MAX_ROUNDS); do
    echo ""
    echo "=== Compile round $round ==="

    # Build source list from whatever .java files are present
    find src -name '*.java' | sort > /tmp/bmdx-sources.txt
    SOURCE_COUNT=$(wc -l < /tmp/bmdx-sources.txt)
    echo "  $SOURCE_COUNT source files"

    mkdir -p target/classes

    # Try compilation, capture errors
    ERRORS=$(javac -cp "$CP" -d target/classes @/tmp/bmdx-sources.txt 2>&1) || true

    if echo "$ERRORS" | grep -q '^src.*error:'; then
        # Extract missing class names from "cannot find symbol" errors
        # Pattern: "symbol:   class Foo" or "symbol: class Foo"
        MISSING=$(echo "$ERRORS" \
            | grep -A2 'cannot find symbol' \
            | grep 'symbol:' \
            | grep -oP 'class \K\w+' \
            | sort -u)

        if [ -z "$MISSING" ]; then
            # Errors exist but aren't missing-class errors — bail out
            echo "$ERRORS"
            echo ""
            echo "=== Compilation errors are not missing-class issues.  Manual fix needed. ==="
            exit 1
        fi

        echo "  Missing classes: $MISSING"

        RESTORED=0
        for cls in $MISSING; do
            # Find the class file in the upstream branch
            FILE=$(git ls-tree -r "$UPSTREAM" --name-only | grep "/${cls}.java$" | head -1)
            if [ -n "$FILE" ]; then
                echo "  Restoring: $FILE"
                git checkout "$UPSTREAM" -- "$FILE"
                RESTORED=$((RESTORED + 1))
            else
                echo "  WARNING: $cls not found in $UPSTREAM"
            fi
        done

        if [ "$RESTORED" -eq 0 ]; then
            echo ""
            echo "=== No restorable classes found.  Manual fix needed. ==="
            echo "$ERRORS"
            exit 1
        fi

        # Stage restored files and continue to next round
        git add -A src/
    else
        # Clean compilation
        echo "  Compilation successful!"

        # Rebuild JAR
        echo ""
        echo "=== Packaging bmdx-core.jar ==="
        cp src/main/resources/vocabulary.yml target/classes/ 2>/dev/null || true
        jar cf target/bmdx-core.jar -C target/classes .
        echo "  $(wc -l < /tmp/bmdx-sources.txt) sources → $(find target/classes -name '*.class' | wc -l) classes → $(ls -lh target/bmdx-core.jar | awk '{print $5}')"

        # Check if there are restored files to commit
        if ! git diff --cached --quiet 2>/dev/null; then
            echo ""
            echo "=== Restored files staged but not committed ==="
            echo "  Review with: git diff --cached --name-only"
            echo "  Commit with: git commit -m 'Restore upstream deps for bmdx-core compilation'"
        fi

        echo ""
        echo "=== Sync complete ==="
        exit 0
    fi
done

echo ""
echo "=== Still failing after $MAX_ROUNDS rounds.  Remaining errors: ==="
javac -cp "$CP" -d target/classes @/tmp/bmdx-sources.txt 2>&1 | head -40
exit 1
