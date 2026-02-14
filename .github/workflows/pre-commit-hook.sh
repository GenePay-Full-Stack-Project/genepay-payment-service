#!/bin/bash
# Pre-commit hook to run tests before committing
# To install: copy this file to .git/hooks/pre-commit and make it executable
#   cp .github/workflows/pre-commit-hook.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit

echo "Running tests before commit..."

# Get the repository root directory
cd "$(git rev-parse --show-toplevel)"

./mvnw test

if [ $? -ne 0 ]; then
    echo "❌ Tests failed! Commit aborted."
    echo "Fix the failing tests before committing."
    exit 1
fi

echo "✅ All tests passed! Proceeding with commit."
exit 0
