# WorkoutSmartApp — Từ mã nguồn đến Docker Image

> **Phiên bản:** 1.0.0 | **Cập nhật:** 2026-08-20  
> Tài liệu này mô tả **một quy trình duy nhất**, tuyến tính: clone code → build image → chạy app trong Docker.

---

## Mục lục

1. [Bạn sẽ có gì sau khi làm xong?](#1-bạn-sẽ-có-gì-sau-khi-làm-xong)
2. [Yêu cầu](#2-yêu-cầu)
3. [Quy trình 4 bước (copy & chạy)](#3-quy-trình-4-bước-copy--chạy)
4. [Giải thích nhanh các file Docker trong repo](#4-giải-thích-nhanh-các-file-docker-trong-repo)
5. [Lệnh Docker thường dùng](#5-lệnh-docker-thường-dùng)
6. [Gỡ lỗi](#6-gỡ-lỗi)
7. [Tài liệu liên quan](#7-tài-liệu-liên-quan)

---

## 1. Bạn sẽ có gì sau khi làm xong?

Sau quy trình này, bạn có **3 container** chạy cùng lúc:

```
Trình duyệt → http://localhost
                    │
              [ web :80 ]        ← image workoutsmart-web (Nginx + React build)
                    │  /api, /media
              [ backend :8080 ]  ← image workoutsmart-backend (Spring Boot JAR)
                    │
              [ postgres :5432 ] ← image postgres:17 (official, không tự build)
```

| Thành phần | Image | Ai build? |
|---|---|---|
| Frontend React | `workoutsmart-web` | Bạn build từ `web/Dockerfile` |
| Backend Spring Boot | `workoutsmart-backend` | Bạn build từ `backend/Dockerfile` |
| PostgreSQL | `postgres:17` | Docker Hub (pull sẵn) |

**Không cần** cài JDK, Maven, Node.js hay PostgreSQL trên máy host — Docker lo toàn bộ build và runtime.

**Database migration:** Backend dùng Flyway (19 file `V1`–`V19` trong `backend/src/main/resources/db/migration/`). Lần đầu backend khởi động, Flyway **tự tạo schema** — bạn không chạy SQL thủ công.

---

## 2. Yêu cầu

| Phần mềm | Ghi chú |
|---|---|
| **Docker Desktop** (Windows/macOS) hoặc **Docker Engine + Compose** (Linux) | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) |
| **Git** (nếu clone repo) | Tùy chọn nếu đã có code trên máy |

Kiểm tra Docker đã sẵn sàng:

```powershell
docker --version
docker compose version
```

Kết quả mong đợi: Docker 24+ và Compose v2.

---

## 3. Quy trình 4 bước (copy & chạy)

> Thực hiện **đúng thứ tự** từ thư mục gốc repo `WorkoutSmartApp/`.

### Bước 1 — Tạo file biến môi trường

Sao chép template và điền `JWT_SECRET` (bắt buộc, ≥ 32 ký tự ngẫu nhiên):

```powershell
cd C:\Users\daizl\Desktop\Save\zOthers\WorkoutSmartApp
Copy-Item .env.docker.example .env.docker
notepad .env.docker
```

Nội dung tối thiểu trong `.env.docker`:

```ini
JWT_SECRET=thay-bang-chuoi-ngau-nhien-it-nhat-32-ky-tu
POSTGRES_PASSWORD=postgres
```

> **Không commit** `.env.docker` — file này chứa secret.

Tạo `JWT_SECRET` nhanh trên PowerShell:

```powershell
-join ((65..90)+(97..122)+(48..57) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
```

---

### Bước 2 — Build image

```powershell
docker compose --env-file .env.docker build
```

Lệnh này:

1. Build **backend image**: Maven compile JAR Spring Boot → image `workoutsmart-backend`
2. Build **web image**: `npm run build` → Nginx phục vụ file tĩnh → image `workoutsmart-web`
3. Pull **postgres:17** nếu chưa có

Lần đầu mất **5–15 phút** (tải base image + npm/maven dependencies). Các lần sau nhanh hơn nhờ Docker cache.

---

### Bước 3 — Chạy toàn bộ stack

```powershell
docker compose --env-file .env.docker up -d
```

Thứ tự khởi động tự động:

```
postgres (chờ healthy) → backend (Flyway migrate) → web
```

Xem log nếu cần theo dõi:

```powershell
docker compose --env-file .env.docker logs -f
```

Nhấn `Ctrl+C` để thoát log (container vẫn chạy).

---

### Bước 4 — Kiểm tra

| Kiểm tra | URL | Kết quả mong đợi |
|---|---|---|
| Frontend | http://localhost | Trang đăng nhập WorkoutSmartApp |
| Backend API docs | http://localhost:8080/swagger-ui.html | Swagger UI |
| API qua Nginx | http://localhost/api/v1/... | Proxy từ web → backend |

**Test đăng ký tài khoản:**

1. Mở http://localhost → **Đăng ký**
2. OTP in ra log backend (vì `EMAIL_PROVIDER=log`):

```powershell
docker compose --env-file .env.docker logs backend
```

3. Nhập OTP → đăng nhập thành công

**Test media bài tập:** Vào trang tìm bài tập — nếu thấy GIF/ảnh là volume `exercises-dataset/` mount đúng.

---

## 4. Giải thích nhanh các file Docker trong repo

```
WorkoutSmartApp/
├── docker-compose.yml       ← Orchestration: postgres + backend + web
├── .env.docker.example      ← Template biến môi trường (copy → .env.docker)
├── backend/
│   ├── Dockerfile           ← Multi-stage: Maven build → JRE chạy JAR
│   └── .dockerignore
└── web/
    ├── Dockerfile           ← Multi-stage: npm build → Nginx
    ├── nginx.conf           ← Proxy /api và /media sang backend
    └── .dockerignore
```

### `backend/Dockerfile`

- **Stage 1 (`build`):** Image `maven:3.9-eclipse-temurin-17` — chạy `mvn package`, tạo JAR
- **Stage 2 (runtime):** Image `eclipse-temurin:17-jre-alpine` — chỉ copy JAR, expose port 8080

### `web/Dockerfile`

- **Stage 1 (`build`):** Image `node:20-alpine` — `npm ci` + `npm run build` → thư mục `dist/`
- **Stage 2 (runtime):** Image `nginx:1.27-alpine` — phục vụ `dist/` + config proxy

### `web/nginx.conf`

Frontend gọi API qua `/api/v1` (xem `web/src/services/http.ts`). Nginx proxy:

- `/api/` → `http://backend:8080`
- `/media/` → `http://backend:8080`

`backend` là tên service trong Docker network — **không** dùng `localhost` giữa các container.

### `docker-compose.yml`

| Service | Port host | Volume quan trọng |
|---|---|---|
| `postgres` | 5432 | `pgdata` — dữ liệu DB persist |
| `backend` | 8080 | `./exercises-dataset` (read-only), `backend-uploads` |
| `web` | 80 | — |

Biến môi trường backend trong compose khớp `backend/src/main/resources/application.yml`:

- `DB_URL=jdbc:postgresql://postgres:5432/workoutsmart` — hostname `postgres` = tên service
- `EXERCISES_DATASET_PATH=/data/exercises-dataset/data/exercises.json`
- `JWT_SECRET` — lấy từ `.env.docker`

---

## 5. Lệnh Docker thường dùng

Tất cả lệnh chạy từ thư mục gốc repo, kèm `--env-file .env.docker`:

```powershell
# Xem container đang chạy
docker compose --env-file .env.docker ps

# Dừng (giữ data)
docker compose --env-file .env.docker stop

# Dừng và xóa container (giữ volume DB)
docker compose --env-file .env.docker down

# Dừng + xóa luôn data DB (reset sạch)
docker compose --env-file .env.docker down -v

# Rebuild sau khi sửa code
docker compose --env-file .env.docker up -d --build

# Chỉ rebuild một service
docker compose --env-file .env.docker build backend
docker compose --env-file .env.docker up -d backend

# Xem image đã build
docker images | findstr workoutsmart
```

### Build image riêng lẻ (không dùng compose)

```powershell
# Backend
docker build -t workoutsmart-backend:0.1.0 ./backend

# Web
docker build -t workoutsmart-web:0.1.0 ./web
```

---

## 6. Gỡ lỗi

### Backend không start — `JWT_SECRET` trống

```
Failed to bind properties under 'app.jwt.secret'
```

→ Kiểm tra `.env.docker` có `JWT_SECRET` và lệnh có `--env-file .env.docker`.

---

### Backend không kết nối DB — `Connection refused`

→ Postgres chưa sẵn sàng. Chờ vài giây hoặc xem log:

```powershell
docker compose --env-file .env.docker logs postgres
docker compose --env-file .env.docker logs backend
```

Compose đã cấu hình `depends_on` + healthcheck — thường tự hết sau lần chạy đầu.

---

### Flyway migration failed — schema cũ / lỗi

Reset database (xóa volume):

```powershell
docker compose --env-file .env.docker down -v
docker compose --env-file .env.docker up -d
```

Flyway chạy lại từ `V1` trên DB trống.

---

### Web vào được nhưng API lỗi 502

Backend chưa chạy hoặc đang crash:

```powershell
docker compose --env-file .env.docker ps
docker compose --env-file .env.docker logs backend
```

Kiểm tra trực tiếp: http://localhost:8080/swagger-ui.html

---

### Không load được ảnh/GIF bài tập

Volume `exercises-dataset` chưa mount hoặc thiếu file:

```powershell
Test-Path .\exercises-dataset\data\exercises.json
```

File phải tồn tại trên **máy host** — Docker mount vào container backend tại `/data/exercises-dataset/`.

---

### Port 80 hoặc 8080 đã bị chiếm

Sửa port trong `docker-compose.yml`:

```yaml
services:
  web:
    ports:
      - "8888:80"    # truy cập http://localhost:8888
  backend:
    ports:
      - "8081:8080"  # swagger http://localhost:8081/swagger-ui.html
```

Sau đó: `docker compose --env-file .env.docker up -d`

---

### Build backend chậm / fail mạng Maven

Chạy lại build:

```powershell
docker compose --env-file .env.docker build --no-cache backend
```

---

## 7. Tài liệu liên quan

| Tài liệu | Khi nào dùng |
|---|---|
| [SELF_HOSTING.md](./SELF_HOSTING.md) | Chạy dev local **không** Docker (Maven + npm + PostgreSQL cài trên máy) |
| [DEPLOY_PERSONAL_PC.md](./DEPLOY_PERSONAL_PC.md) | Mở app ra LAN/Internet (Nginx native, Cloudflare Tunnel, DuckDNS) |
| `backend/.env.example` | Giải thích đầy đủ biến môi trường backend |

---

## Phụ lục — So sánh Docker vs chạy native

| | Docker (tài liệu này) | Native ([SELF_HOSTING.md](./SELF_HOSTING.md)) |
|---|---|---|
| Cài JDK/Maven/Node/PostgreSQL | Không cần | Cần |
| Phù hợp | Demo, CI/CD, deploy đồng nhất | Dev hàng ngày (hot reload) |
| Hot reload frontend | Không (phải rebuild image) | Có (`npm run dev`) |
| Hot reload backend | Không | Có (`mvn spring-boot:run`) |

**Khuyến nghị:** Dev hàng ngày dùng native; Docker dùng khi cần đóng gói, chia sẻ môi trường, hoặc deploy.
