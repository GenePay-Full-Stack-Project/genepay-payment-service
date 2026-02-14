# GitHub Repository Secrets Configuration

This document lists all the secrets that need to be configured in your GitHub repository for the CI/CD workflows to run successfully.

## How to Add Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret below with its corresponding value

## Required Secrets

### Mail Configuration

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `MAIL_HOST` | SMTP server hostname | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP server port | `587` |
| `MAIL_USERNAME` | Email account username | `your-email@gmail.com` |
| `MAIL_PASSWORD` | Email account password or app password | `your-app-password` |
| `EMAIL_FROM` | Sender email address | `noreply@genepay.com` |
| `EMAIL_FROM_NAME` | Sender display name | `GenePay` |
| `SUPPORT_EMAIL` | Support email address | `support@genepay.com` |

### Platform Integration

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `BIOPAY_PLATFORM_TOKEN` | BioPay platform API token | `7d2f44f9-21fb-47c5-8b73-228556f8bfb7` |

### Blockchain Configuration

| Secret Name | Description | Example Value |
|------------|-------------|---------------|
| `BLOCKCHAIN_ENABLED` | Enable/disable blockchain integration | `false` (for CI) or `true` |
| `BLOCKCHAIN_RELAY_URL` | Blockchain relay service URL | `http://localhost:3001` |

## Database Configuration

> **Note:** Database credentials for PostgreSQL are configured directly in the workflow as they use the service container. These don't need to be added as secrets:
> - `DB_HOST: localhost`
> - `DB_PORT: 5432`
> - `DB_NAME: genepay_db`
> - `DB_USERNAME: postgres`
> - `DB_PASSWORD: postgres`

## Test/CI Recommended Values

For CI/CD workflows, you can use test values that don't require real services:

```yaml
MAIL_HOST: smtp.gmail.com
MAIL_PORT: 587
MAIL_USERNAME: test@example.com
MAIL_PASSWORD: test-password-for-ci
EMAIL_FROM: noreply@genepay.com
EMAIL_FROM_NAME: GenePay
SUPPORT_EMAIL: support@genepay.com
BIOPAY_PLATFORM_TOKEN: test-token-for-ci
BLOCKCHAIN_ENABLED: false
BLOCKCHAIN_RELAY_URL: http://localhost:3001
```

## Security Best Practices

1. ✅ Never commit secrets to your repository
2. ✅ Use different values for CI and production
3. ✅ Rotate secrets periodically
4. ✅ Use app-specific passwords for email (not your main password)
5. ✅ Limit secret access to necessary workflows only

## Verifying Secrets

After adding secrets, you can verify they're configured correctly by:

1. Triggering a workflow run (push to develop/main or create a PR)
2. Check the workflow logs in the **Actions** tab
3. Secrets will show as `***` in logs for security

## Troubleshooting

If your workflow fails with environment variable errors:

1. Verify all secrets are added in GitHub Settings
2. Check secret names match exactly (case-sensitive)
3. Ensure there are no trailing spaces in secret values
4. Re-run the workflow after adding/updating secrets
