# Deploying to Render

Place this `Dockerfile` in the same folder as `pom.xml` (root of the backend). If you use a monorepo, place it in a `backend/` subfolder and set that folder as the "Root Directory" in Render.

Dockerfile (Maven + Java 21) is a two-stage build: builds the app with Maven then runs the resulting `*-SNAPSHOT.jar` on Eclipse Temurin Java 21.

.dockerignore excludes git, build artifacts and environment files.

Render — two options

A. Build from your repo (recommended)

- Source: Public Git Repository
- Language: Docker
- Branch: main
- Root Directory: `backend` if monorepo, otherwise leave empty
- Instance: Free (for testing)
- Health Check Path: `/actuator/health`
- Environment Variables: paste your production `.env` values (not dev)

Note: `server.port` should already be mapped via `${PORT}` in `application.yml`.

B. Use an existing image

Build and push from the folder that contains the `Dockerfile`:

```powershell
docker build -t docker.io/USER/dispatchingbackend:1.0.0 .
docker push docker.io/USER/dispatchingbackend:1.0.0
```

On Render choose "Existing Image" and point to `docker.io/USER/dispatchingbackend:1.0.0`.

Health Check Path: `/actuator/health`
Environment Variables: same as above
