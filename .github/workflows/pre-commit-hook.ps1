# Pre-commit hook (PowerShell version) to run tests before committing
# To install: 
#   1. Create/edit .git\hooks\pre-commit (no extension)
#   2. Add this content:
#      #!/bin/sh
#      exec pwsh -File .github/workflows/pre-commit-hook.ps1

Write-Host "Running tests before commit..." -ForegroundColor Yellow

# Get the repository root directory (2 levels up from .github/workflows)
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent (Split-Path -Parent $scriptPath)
Push-Location $repoRoot

try {
    & .\mvnw.cmd test

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Tests failed! Commit aborted." -ForegroundColor Red
        Write-Host "Fix the failing tests before committing." -ForegroundColor Red
        exit 1
    }

    Write-Host "✅ All tests passed! Proceeding with commit." -ForegroundColor Green
    exit 0
}
finally {
    Pop-Location
}
