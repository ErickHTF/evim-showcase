# Development guide

[← Back to README](../README.md)

## Prerequisites

- Java 21 for the backend.
- Node 20+ and npm for the frontend.
- Docker with Docker Compose for PostgreSQL.
- Keycloak and Jira Cloud configuration to run authenticated workflows.

## Build and verify

```bash
cd backend
./mvnw test

cd ../frontend
npm ci
npm run build
```

The code and tests run without external services. Starting the application requires an OIDC issuer and a Jira site for the inbox. Microsoft Graph polling is disabled by default (`graph.mock.enabled=true`), so incoming mail is not populated in this snapshot. See the [backend reference](backend.md) for environment variables and database settings, and the [frontend reference](frontend.md) for its Vite configuration.

## Local services

Start PostgreSQL from the backend directory:

```bash
cd backend
docker compose up -d
```

Copy `frontend/.env.example` to `frontend/.env` and configure the Keycloak issuer and client. The Vite development server runs on port `5173`; its proxy forwards `/deploy-api` to the backend on `8081`.

The screenshots in the repository were captured from a local instance with fictitious data and mocked Jira. They document the UI; they are not a runnable demo environment.
