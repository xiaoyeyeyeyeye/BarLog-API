# Google Auth API (Backend)

Base URL follows `EXPO_PUBLIC_API_BASE_URL`.

PWA uses the redirect-based flow documented in the frontend `docs/API_GOOGLE_AUTH.md`.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/google/start` | Returns Google OAuth `authUrl` |
| GET | `/api/auth/google/callback` | Google redirects here; backend issues JWT and redirects to frontend |
| POST | `/api/auth/google/complete` | Validates callback tokens and returns `{ user, accessToken, refreshToken }` |

## Server env

```env
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GOOGLE_OAUTH_CALLBACK_URL=https://your-tunnel.trycloudflare.com/api/auth/google/callback
GOOGLE_OAUTH_ALLOWED_REDIRECT_ORIGINS=https://your-tunnel.trycloudflare.com
```

Register the callback URL in Google Cloud Console OAuth client.

**同事在 GCP 里怎么配？** 见 [GOOGLE_OAUTH_GCP_SETUP.md](./GOOGLE_OAUTH_GCP_SETUP.md)（可整份发给同事的 AI 助手）。

## Register welcome email

`POST /api/auth/register` sends a welcome email when `EMAIL_PROVIDER=aws` (AWS SES).
OTP SMS/email for verification uses `POST /api/auth/otp/send` on the native auth controller.
