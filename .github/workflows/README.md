# CI/CD Workflows

This directory contains GitHub Actions workflows for the GenePay Payment Service.

## Workflows

### 1. CI - Run Tests (`ci.yml`)

**Triggers:**
- Pull requests to `develop` and `main` branches
- Direct pushes to `develop` and `main` branches

**Jobs:**

#### Test Job
- Checks out the code
- Sets up JDK 17 with Maven caching
- Runs `./mvnw clean test`
- Generates test reports
- Uploads test results as artifacts (retained for 30 days)

#### Build Job
- Runs after successful test job
- Builds the application with `./mvnw clean package -DskipTests`
- Uploads the built JAR as an artifact (retained for 7 days)

### 2. Code Quality Check (`code-quality.yml`)

**Triggers:**
- Pull requests to `develop` and `main` branches

**Jobs:**
- Runs Maven verify
- Checks code formatting (if spotless is configured)

## Branch Protection Rules

To enforce CI checks before merging, configure branch protection rules in your repository:

1. Go to **Settings** → **Branches**
2. Add branch protection rule for `develop`:
   - Require status checks to pass before merging
   - Select: `Run Tests`, `Build Application`
   - Require branches to be up to date before merging
3. Repeat for `main` branch

## Local Testing

Before pushing, you can run the same checks locally:

```bash
# Run tests
./mvnw clean test

# Run full build
./mvnw clean package

# Run verification
./mvnw clean verify
```

## Requirements

- Java 17
- Maven 3.6+
- All dependencies defined in `pom.xml`

## Artifacts

- **Test Results**: Available for 30 days after workflow run
- **Application JAR**: Available for 7 days after workflow run

## Notes

- Tests must pass before the build job runs
- Failed tests will block the merge
- Artifacts are automatically cleaned up after retention period
