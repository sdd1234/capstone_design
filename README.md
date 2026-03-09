# Campus Fit Workspace

This workspace now includes:

- `campus-fit-api`: Spring Boot backend
- `campus-fit-web`: React + Vite frontend
- existing `.NET test` files (left untouched)

## 1) Prerequisites

Install:

- JDK 21
- Maven 3.9+
- Node.js 20 LTS (includes npm)

## 2) Backend Run (Spring Boot)

```bash
cd campus-fit-api
mvn spring-boot:run
```

If `mvn` is not recognized, restart VS Code terminal once.

Backend default URL: `http://localhost:8080`

## 3) Frontend Run (React)

```bash
cd campus-fit-web
npm.cmd install
npm.cmd run dev
```

PowerShell execution policy can block `npm` script wrappers in some environments.
Use `npm.cmd` in that case.

Frontend default URL: `http://localhost:5173`

## 4) API Wiring

Frontend base URL is controlled by:

- `campus-fit-web/.env`
- `VITE_API_BASE_URL=http://localhost:8080`

You can copy from `.env.example`.

## 5) Included Starter Endpoints

- `POST /api/v1/auth/signup` (multipart: `payload` + `verificationFile`)
- `POST /api/v1/auth/login`
- `PATCH /api/v1/admin/student-verifications/{verificationId}`

These are scaffolding endpoints to help you start integration quickly.

## 6) VS Code One-Click Tasks

Use `Terminal -> Run Task`:

- `frontend: install`
- `backend: run`
- `frontend: dev`
- `dev: full stack`

Debug configurations are included in `.vscode/launch.json`:

- `Debug Campus Fit API`
- `Debug Campus Fit Web`
- `Debug Full Stack`

## 7) Local Setup Status (this machine)

- Java: Temurin 21 installed
- Maven: 3.9.11 installed in `%USERPROFILE%\\tools\\apache-maven-3.9.11`
- Node.js: LTS installed

If commands are still not found, close and reopen VS Code.
