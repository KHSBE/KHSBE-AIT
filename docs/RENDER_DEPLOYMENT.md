# Render + Neon test deployment

Status: deployment preparation only. No cloud resources have been created.
The EC2 Compose configuration and backend/Dockerfile remain unchanged.

## 1. Create a NEW Neon Free database

Do not use a production database for this initial experiment. Choose a region
close to the Render service where both providers offer availability.
Copy the hostname, database name, role and password from Neon's Connect dialog.
Store credentials only in Render's Environment settings, not Git or chat.
Use the direct database endpoint initially (small pool, schema bootstrap).

DB_JDBC_URL must be a JDBC URL, NOT a postgresql:// URL with embedded credentials:

    jdbc:postgresql://YOUR_NEON_HOST:5432/YOUR_DATABASE?sslmode=require

Set DB_USERNAME and DB_PASSWORD separately. Keep SSL enabled.
The render profile defaults to ddl-auto=update for a NEW disposable demo DB.
This is not a migration strategy. Back up data and introduce migrations before
using this deployment for important user data.

## 2. Create a Render Web Service

- Connect the intended GitHub repository and deployment branch.
- Runtime: Docker.
- Root Directory: backend.
- Dockerfile Path: ./Dockerfile.render (relative to Root Directory).
- Docker build context: . (the backend directory).
- Instance Type: Free. Do not select paid storage or a paid database.
- Health check: leave the HTTP path empty for the default TCP probe initially.
  TCP readiness does NOT establish that DB access, OAuth or AI works.
- Review workspace bandwidth/build limits and billing controls. Free compute
  does not imply that every associated resource or excess usage is free.
- Enable auto deploy if desired; CI checks are not automatically a deployment
  gate unless explicitly configured. Otherwise deploy manually after green CI.

## 3. Environment variables

Required:

| Key | Value |
| --- | --- |
| SPRING_PROFILES_ACTIVE | render |
| DB_JDBC_URL | Neon JDBC URL from step 1 |
| DB_USERNAME | Neon role |
| DB_PASSWORD | Neon password |
| GOOGLE_CLIENT_ID | Existing Google OAuth client ID |
| GOOGLE_CLIENT_SECRET | Existing Google OAuth client secret |
| JWT_SECRET | Strong random secret, at least 32 random bytes; do not use an example |
| GOOGLE_PROJECT_ID | Existing Google Cloud project ID |
| GOOGLE_CREDENTIALS_URI | file:/etc/secrets/gcp-sa.json |
| GOOGLE_GENAI_VERTEX_AI | true |
| GOOGLE_CLOUD_LOCATION | global |
| GOOGLE_PLACES_API_KEY | Server-side Places API key |
| CORS_ALLOWED_ORIGINS | Exact frontend origin, e.g. https://YOUR_SITE.vercel.app |
| OAUTH2_REDIRECT_URI | https://YOUR_SITE.vercel.app/oauth2/redirect |

Optional: WEATHER_API_KEY, ADMIN_EMAILS, PLAN_QUOTA_PER_DAY, CHAT_QUOTA_PER_DAY.
Start with small quotas for this test. Do not copy the entire EC2 .env: its DB
hostname and credential path refer to containers that do not exist on Render.
AI_MODEL_GENERAL and AI_MODEL_PLAN can override the repository defaults.
Verify access to the configured models in your Google project; a successful
build is not an AI availability check. Do not publish secrets in build logs.

In Render Secret Files, create gcp-sa.json using your service-account JSON.
It is mounted at /etc/secrets/gcp-sa.json at runtime. Do not commit it or pass it
as a Docker build argument. The Dockerfile uses explicit source COPY steps and
.dockerignore excludes the credential file from the image build context.
Use a least-privilege service account and inspect existing API-key restrictions:
old EC2 IP restrictions will not automatically allow Render egress.

## 4. Connect the frontend and Google login

Once Render assigns https://YOUR_SERVICE.onrender.com:

1. Add the exact Google OAuth authorized redirect URI:
   https://YOUR_SERVICE.onrender.com/login/oauth2/code/google
2. Set Vercel VITE_API_BASE_URL=https://YOUR_SERVICE.onrender.com and redeploy.
   Vite environment values are embedded at build time.
3. Confirm VITE_GOOGLE_MAPS_API_KEY exists in Vercel. Browser keys must have
   appropriate website/API restrictions. Never put the service-account key or
   server secrets in VITE_ variables.
4. Confirm backend CORS_ALLOWED_ORIGINS and OAUTH2_REDIRECT_URI match the actual
   frontend deployment. Preview URLs differ from the production origin.

Render supplies HTTPS, so no EC2, Caddy, or local Docker installation is needed.
Redis is disabled only in the render profile: current code has no direct Redis
consumer. Reassess this if Redis-backed functionality is added later.

## 5. Acceptance checklist

- Build succeeds and application starts without OOM or DB/schema errors.
- Google login returns to the intended frontend, not localhost or the old EC2.
- Chat and one short itinerary generation succeed with Google credits available.
- Save, reload, edit and delete a test itinerary; verify ownership checks.
- Restart/redeploy the backend: saved data remains in Neon.
- Test after idle sleep; show a retryable startup message rather than claiming
  login expiry for all network errors. This UI improvement is not implemented.
- Inspect memory during generation, not just startup. A 256 MB Java heap is an
  initial setting, not proof the process fits the Free instance's 512 MB.
- If OOM occurs, stop and reassess; do not repeatedly increase heap beyond the
  memory budget or upgrade to a paid instance without approval.

Known review findings (quota bypass, token handling, place validation and chat
session deletion) are NOT fixed by deployment. Keep this as a restricted test
or controlled demonstration, not a production-ready public release. Hiding a
URL alone is not access control. Do not weaken existing authentication.

## 6. Costs and backup

Neon Free and Render Free have usage limits. Render sleeps after inactivity.
Keep the database outside Render's ephemeral filesystem. Export important data
periodically; a free plan is not a permanent retention or availability promise.
Vertex AI, Maps/Places and domain registration are separate billing products.
Budget alerts are not hard spending caps; configure quotas where supported.
Do not delete the old deployment/configuration solely because the build passes.

## Official references

- https://render.com/docs/docker
- https://render.com/docs/docker-secrets
- https://render.com/docs/free
- https://render.com/docs/health-checks
- https://neon.com/pricing
