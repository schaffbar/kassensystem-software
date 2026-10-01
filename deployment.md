# Deployment

## Schaffbar

1. Join `Schaffbar` WLAN

2. Connect with server

```bash
ssh <user>@<server>
(enter password)
```

3. Pull updates or clone git repo

```bash
git clone ...
cd kassensystem/
git pull
```

4. Create `.env` file

```
cd schaffbar-backend
```

```
# Database configuration
POSTGRES_USER=<user>
POSTGRES_PASSWORD=<user-password>
POSTGRES_DB=<dbname>

# Backend configuration (choose environment)
# BACKEND_ENV=dev
# BACKEND_ENV=prod

# Frontend configuration (choose environment)
# FRONTEND_ENV=development
# FRONTEND_ENV=production
```

5. Run docker compose

```bash
docker compose up -d --build
```

6. Check that 3 containers run

```bash
docker ps -a 
```

```bash
CONTAINER ID   IMAGE                        COMMAND                  CREATED         STATUS                   PORTS                                         NAMES
992d0c80e35f   schaffbar-backend-backend    "java -jar app.jar"      7 minutes ago   Up 6 minutes             0.0.0.0:5000->5000/tcp, [::]:5000->5000/tcp   schaffbar-backend-backend-1
b85717c40832   postgres:16.3                "docker-entrypoint.s…"   7 minutes ago   Up 7 minutes (healthy)   0.0.0.0:5432->5432/tcp, [::]:5432->5432/tcp   schaffbar-backend-postgres-1
df03d27f1e27   schaffbar-backend-frontend   "nginx -c /etc/nginx…"   7 minutes ago   Up 7 minutes             0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp   schaffbar-backend-frontend-1
```

7. Got to your browser and type:

```bash
<your servername>:8080 
``` 

## Local (only Docker required)

Before proceeding, ensure that Docker and Docker Compose are installed on your system. These tools are required to build and run the application in isolated containers. You can download Docker from [https://www.docker.com/get-started](https://www.docker.com/get-started). Verify installation by running:

```bash
docker --version
docker compose --version
```

### 1. Clone the repository or download the source code

You can obtain the code by cloning the repository and switching to the desired branch, or by downloading it directly from the GitHub page.

- To clone and switch branch:

```bash
git clone https://github.com/schaffbar/kassensystem.git
cd kassensystem
git checkout piotr
```

- Alternatively, visit [https://github.com/schaffbar/kassensystem](https://github.com/schaffbar/kassensystem) and use the "Download ZIP" option if you prefer not to use Git.

### 2. Create `.env` file (in `schaffbar-backend` folder) with postgres credentials

```bash
cd schaffbar-backend
```

- `.env` file content

```bash
POSTGRES_USER=your_postgres_user
POSTGRES_PASSWORD=your_postgres_password
POSTGRES_DB=schaffbardb
```

### 3. Start all services with Docker Compose

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
