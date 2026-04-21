# Render Deployment (Backend)

This backend is ready for Render using Docker + Blueprint.

## 1. Deploy with Blueprint

- Push this backend folder to GitHub.
- In Render: New + > Blueprint.
- Select the repository.
- Render will detect `render.yaml` and create:
  - `cuet-transport-backend` web service
  - `cuet-transport-db` PostgreSQL database

## 2. Important

- If your frontend is not hosted on Render, update `CORS_ALLOWED_ORIGINS` in Render dashboard after first deploy.
- Keep `/actuator/health` enabled for health checks (already configured).

## 3. Mail Settings

- Mail uses Gmail SMTP by default in `render.yaml`.
- If Gmail blocks login, use an App Password and keep 2FA enabled.

## 4. After Deploy

- Copy backend URL from Render service.
- Update frontend `VITE_API_BASE_URL` to that URL.
