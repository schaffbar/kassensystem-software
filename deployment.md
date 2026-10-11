# Deployment

## Schaffbar

### 1. Join `Schaffbar` WLAN and connect with server

```bash
ssh <user>@<server_name>
(enter password)
```

### 2. Get the source code

- First installation: clone the repository

```bash
git clone https://github.com/schaffbar/kassensystem-software.git
cd kassensystem-software/
```

- Update: pull the latest changes

```bash
cd kassensystem-software/
git pull
```

### 3. Create `.env` file (only on first installation)

```bash
cd infrastructure
cp .env.example .env
nano .env
```

Set the credentials and choose the **production** environment on the server:

```dotenv
# Database configuration
POSTGRES_USER=schadmin
POSTGRES_PASSWORD=<user-password>
POSTGRES_DB=schaffbar

# Backend configuration: dev | prod (default: dev)
BACKEND_ENV=prod

# Frontend configuration: development | production (default: development)
FRONTEND_ENV=production
```

**Info:** If `BACKEND_ENV` / `FRONTEND_ENV` are missing, the `dev` / `development` configuration is used..

### 4. Adjust the server name

Adapt the host name under which the server is reachable (currently `ubuntu`) in these two files, if it differs:

- Backend CORS allowed origins (all hosts from which the frontend is opened, comma separated) in [application-prod.yaml](schaffbar-backend/core-pos/src/main/resources/application-prod.yaml):

  ```yaml
  application:
    cors:
      allowed-origins: http://<server_name>:8080
  ```

- Backend URL used by the frontend in [environment.production.ts](schaffbar-portal/src/environments/environment.production.ts):

  ```ts
  apiBaseUrl: 'http://<server_name>:5000',
  ```

The server name is part of the built images, so rebuild them after a change.

### 5. Deploy application

Run docker compose (in `infrastructure` folder)

```bash
docker compose up -d --build
```

Check that 3 containers run

```bash
docker ps -a
```

```bash
CONTAINER ID   IMAGE                COMMAND                  CREATED         STATUS                   PORTS                                         NAMES
992d0c80e35f   schaffbar-backend    "java -jar app.jar"      7 minutes ago   Up 6 minutes             0.0.0.0:5000->5000/tcp, [::]:5000->5000/tcp   schaffbar-backend-1
b85717c40832   postgres:16.3        "docker-entrypoint.s…"   7 minutes ago   Up 7 minutes (healthy)   0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp   schaffbar-postgres-1
df03d27f1e27   schaffbar-frontend   "nginx -c /etc/nginx…"   7 minutes ago   Up 7 minutes             0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp   schaffbar-frontend-1
```

Go to your browser and open:

```text
http://<server_name>:8080
```

## Local (only Docker required)

Before proceeding, ensure that Docker and Docker Compose are installed on your system. These tools are required to build and run the application in isolated containers. You can download Docker from [https://www.docker.com/get-started](https://www.docker.com/get-started). Verify installation by running:

```bash
docker --version
docker compose --version
```

### 1. Clone the repository or download the source code

You can obtain the code by cloning the repository or by downloading it directly from the GitHub page.

- To clone:

```bash
git clone https://github.com/schaffbar/kassensystem-software.git
cd kassensystem-software
```

- Alternatively, visit [https://github.com/schaffbar/kassensystem-software](https://github.com/schaffbar/kassensystem-software) and use the "Download ZIP" option if you prefer not to use Git.

### 2. Create `.env` file

```bash
cd infrastructure
cp .env.example .env
```

- Adjust the values in `.env` if needed. For local use keep `BACKEND_ENV=dev` and `FRONTEND_ENV=development`.

### 3. Start all services

```bash
docker compose up --build -d
```

- The `--build` parameter is required only the first time you run this command, or whenever you change the source code and need to rebuild the images. Subsequent runs can omit `--build` unless a rebuild is necessary.

- To start only two services (for example, `postgres` and `backend`), run:

```bash
docker compose up -d postgres backend
```

- Replace the service names with the ones you want to start, as defined in `docker-compose.yml`.

### 4. Stop all running containers

```bash
docker compose down
```

- Do **not** add `-v` unless you want to delete the database volume.
