# 🏋️ WorkoutSmartApp — Hướng Dẫn Tự Host Trên Máy Tính Cá Nhân

> **Phiên bản tài liệu:** 1.0.0 | **Cập nhật:** 2026-08-19  
> Hướng dẫn này dành cho người muốn chạy toàn bộ ứng dụng **WorkoutSmartApp** trên máy tính cá nhân (Windows / macOS / Linux) phục vụ mục đích phát triển hoặc dùng nội bộ.

---

## Mục Lục

1. [Yêu cầu phần mềm](#1-yêu-cầu-phần-mềm)
2. [Tải mã nguồn về máy](#2-tải-mã-nguồn-về-máy)
3. [Cài đặt & cấu hình PostgreSQL](#3-cài-đặt--cấu-hình-postgresql)
4. [Cấu hình biến môi trường Backend](#4-cấu-hình-biến-môi-trường-backend)
5. [Chạy Backend (Spring Boot)](#5-chạy-backend-spring-boot)
6. [Chạy Frontend (React + Vite)](#6-chạy-frontend-react--vite)
7. [Thứ tự khởi động đúng](#7-thứ-tự-khởi-động-đúng)
8. [Kiểm tra hệ thống hoạt động](#8-kiểm-tra-hệ-thống-hoạt-động)
9. [Gỡ lỗi thường gặp](#9-gỡ-lỗi-thường-gặp)
10. [Dừng ứng dụng](#10-dừng-ứng-dụng)

---

## 1. Yêu Cầu Phần Mềm

Cài đặt đầy đủ các phần mềm sau trước khi bắt đầu:

| Phần mềm | Phiên bản tối thiểu | Link tải |
|---|---|---|
| **JDK (Java Development Kit)** | Java 17 | https://adoptium.net |
| **Apache Maven** | 3.8+ | https://maven.apache.org/download.cgi |
| **Node.js** | 18+ (kèm npm) | https://nodejs.org |
| **PostgreSQL** | 16+ (khuyến nghị 18) | https://www.postgresql.org/download |

### Kiểm tra đã cài đúng chưa

Mở Terminal / PowerShell / Command Prompt rồi chạy từng lệnh sau:

```bash
java -version
# Kết quả mong đợi: openjdk version "17.x.x" ...

mvn -version
# Kết quả mong đợi: Apache Maven 3.x.x ...

node -version
# Kết quả mong đợi: v18.x.x hoặc cao hơn

npm -version
# Kết quả mong đợi: 9.x.x hoặc cao hơn

psql --version
# Kết quả mong đợi: psql (PostgreSQL) 16.x ...
```

> [!NOTE]
> Trên **Windows**, sau khi cài Maven cần thêm thư mục `bin` của Maven vào biến môi trường `PATH`.  
> Hướng dẫn chi tiết: Control Panel → System → Advanced → Environment Variables → chỉnh `Path`.

---

## 2. Tải Mã Nguồn Về Máy

### Nếu dùng Git (khuyến nghị)

```bash
git clone <url-repository> WorkoutSmartApp
cd WorkoutSmartApp
```

### Nếu tải file ZIP

Giải nén và đặt thư mục dự án ở nơi không có khoảng trắng trong đường dẫn.  
Ví dụ đúng: `C:\Projects\WorkoutSmartApp`  
Ví dụ sai: `C:\My Documents\Workout Smart App`

**Cấu trúc thư mục sau khi tải về:**

```
WorkoutSmartApp/
├── backend/               ← Mã nguồn Spring Boot
├── web/                   ← Mã nguồn React + Vite
├── exercises-dataset/     ← Dữ liệu bài tập (ảnh GIF, JSON)
├── docs/                  ← Tài liệu
├── specs/                 ← Đặc tả tính năng
└── RUN_COMMANDS.txt       ← Tóm tắt lệnh chạy nhanh
```

---

## 3. Cài Đặt & Cấu Hình PostgreSQL

### Bước 3.1 — Tạo Database

Sau khi cài PostgreSQL, mở **pgAdmin** hoặc công cụ dòng lệnh `psql`:

```sql
-- Kết nối vào psql với tài khoản postgres mặc định
-- Windows: mở "SQL Shell (psql)" từ Start Menu
-- macOS/Linux: psql -U postgres

-- Tạo database cho ứng dụng
CREATE DATABASE workoutsmart;

-- Xác nhận đã tạo thành công
\l
```

### Bước 3.2 — Kiểm tra thông tin kết nối mặc định

Backend mặc định sử dụng:

| Thông số | Giá trị mặc định |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `workoutsmart` |
| Username | `postgres` |
| Password | `postgres` |

Nếu PostgreSQL của bạn dùng mật khẩu khác, xem [Bước 4](#4-cấu-hình-biến-môi-trường-backend) để thay đổi.

> [!IMPORTANT]
> **Không cần chạy script SQL thủ công.** Backend dùng **Flyway** để tự động tạo toàn bộ bảng và dữ liệu mẫu khi khởi động lần đầu (19 migration scripts từ V1 đến V19).

---

## 4. Cấu Hình Biến Môi Trường Backend

Tạo file `.env` trong thư mục `backend/` (cùng cấp với `pom.xml`):

```bash
# Windows PowerShell
New-Item -Path "backend\.env" -ItemType File

# macOS / Linux
touch backend/.env
```

Mở file `backend/.env` bằng bất kỳ text editor nào và điền nội dung:

```ini
# ===== DATABASE =====
# Chỉ cần thay đổi nếu PostgreSQL của bạn dùng thông tin khác mặc định
DB_URL=jdbc:postgresql://localhost:5432/workoutsmart
DB_USERNAME=postgres
DB_PASSWORD=postgres

# ===== JWT (BẮT BUỘC) =====
# Chuỗi bí mật dùng để ký JWT — phải đủ dài (>= 32 ký tự ngẫu nhiên)
# Ví dụ tạo ngẫu nhiên (PowerShell):
#   -join ((65..90)+(97..122)+(48..57) | Get-Random -Count 48 | % {[char]$_})
JWT_SECRET=my-local-dev-secret-key-at-least-32-chars-long!

# ===== EMAIL (TÙY CHỌN cho dev) =====
# log  → OTP sẽ in ra terminal, KHÔNG gửi email thật (dùng khi dev)
# smtp → Gửi email thật qua Gmail SMTP (cần bật App Password)
EMAIL_PROVIDER=log

# Chỉ cần điền nếu EMAIL_PROVIDER=smtp
MAIL_USERNAME=
MAIL_PASSWORD=

# ===== ĐƯỜNG DẪN MEDIA (Giữ nguyên nếu cấu trúc thư mục chuẩn) =====
EXERCISES_DATASET_PATH=../exercises-dataset/data/exercises.json
EXERCISES_MEDIA_PATH=../exercises-dataset/
USER_MEDIA_PATH=./uploads/media/
```

> [!TIP]
> Khi để `EMAIL_PROVIDER=log`, mỗi lần đăng ký tài khoản, mã OTP sẽ xuất hiện ngay trong cửa sổ terminal đang chạy backend — tiện dụng cho môi trường phát triển mà không cần cấu hình email.

> [!CAUTION]
> **Không bao giờ commit file `.env` lên Git.** File `.gitignore` đã có rule loại trừ file này, hãy kiểm tra lại trước khi push.

---

## 5. Chạy Backend (Spring Boot)

Mở **Terminal 1**, di chuyển vào thư mục `backend/`:

```bash
cd backend
```

### Cách 1: Chạy trực tiếp (dành cho phát triển — tự động biên dịch)

```bash
mvn spring-boot:run
```

### Cách 2: Build JAR rồi chạy (dành cho production / test hiệu năng)

```bash
mvn clean package -DskipTests
java -jar target/backend-0.1.0-SNAPSHOT.jar
```

### Dấu hiệu backend khởi động thành công

Chờ đến khi thấy dòng tương tự trong terminal:

```
Started WorkoutsmartBackendApplication in 4.x seconds (process running for 5.x)
```

Hoặc:

```
Tomcat started on port 8080 (http) with context path ''
```

**Kiểm tra nhanh:** Mở trình duyệt vào `http://localhost:8080/swagger-ui.html` — nếu thấy trang Swagger UI là backend đang chạy tốt.

> [!NOTE]
> Lần chạy đầu tiên Flyway sẽ tự tạo 19 bảng trong database `workoutsmart`. Quá trình này mất khoảng 5–15 giây tùy tốc độ máy. Các lần sau sẽ khởi động nhanh hơn.

---

## 6. Chạy Frontend (React + Vite)

Mở **Terminal 2** (để Terminal 1 của backend vẫn chạy), di chuyển vào thư mục `web/`:

```bash
cd web
```

### Bước 6.1 — Cài dependencies (chỉ lần đầu)

```bash
npm install
```

> Quá trình này tải về ~300MB các gói Node.js, cần kết nối internet và mất 1–3 phút.

### Bước 6.2 — Khởi động dev server

```bash
npm run dev
```

**Dấu hiệu frontend khởi động thành công:**

```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: http://xxx.xxx.xxx.xxx:5173/
```

> [!NOTE]
> Vite tự động proxy `/api` và `/media` sang `http://localhost:8080` — bạn **không cần** cấu hình CORS hay thay đổi địa chỉ API trong code frontend.

---

## 7. Thứ Tự Khởi Động Đúng

> [!IMPORTANT]
> Phải khởi động theo đúng thứ tự để tránh lỗi kết nối:

```
1. ✅ Bật dịch vụ PostgreSQL
         ↓
2. ✅ Chạy Backend (Terminal 1) — chờ thấy "Started ... in x.x seconds"
         ↓
3. ✅ Chạy Frontend (Terminal 2)
         ↓
4. ✅ Mở trình duyệt: http://localhost:5173
```

---

## 8. Kiểm Tra Hệ Thống Hoạt Động

Sau khi cả hai service đã chạy, kiểm tra từng điểm sau:

### Kiểm tra Backend API

Mở trình duyệt vào `http://localhost:8080/swagger-ui.html` để xem toàn bộ API docs.

### Kiểm tra Frontend

1. Mở trình duyệt vào `http://localhost:5173`
2. Màn hình đăng nhập xuất hiện → nhấn **Đăng ký** để tạo tài khoản mới
3. Nhập email, mật khẩu → nhận mã OTP **từ terminal backend** (vì `EMAIL_PROVIDER=log`)
4. Nhập OTP → đăng nhập thành công

### Kiểm tra Media Bài Tập

Vào trang tìm kiếm bài tập (`/exercises`) — nếu danh sách bài tập hiển thị kèm ảnh GIF là `exercises-dataset` đang được đọc đúng.

---

## 9. Gỡ Lỗi Thường Gặp

### ❌ Lỗi: `Port 8080 is already in use`

Backend bị chặn vì cổng 8080 đang được dùng bởi chương trình khác.

```powershell
# Windows — tìm PID đang dùng cổng 8080
netstat -ano | findstr :8080
# Kill process theo PID (thay <PID> bằng số tìm được)
taskkill /PID <PID> /F
```

```bash
# macOS / Linux
lsof -ti:8080 | xargs kill
```

---

### ❌ Lỗi: `Connection to localhost:5432 refused`

PostgreSQL chưa được khởi động.

```powershell
# Windows — Kiểm tra dịch vụ
Get-Service -Name postgresql*

# Khởi động nếu chưa chạy (tên dịch vụ tùy phiên bản PostgreSQL)
Start-Service -Name "postgresql-x64-18"
```

```bash
# macOS (Homebrew)
brew services start postgresql@18

# Linux (systemd)
sudo systemctl start postgresql
```

---

### ❌ Lỗi: `Flyway migration failed` — `relation "xxx" already exists`

Database đang bị schema cũ/lỗi. Xóa và tạo lại database:

```sql
-- Trong psql
DROP DATABASE workoutsmart;
CREATE DATABASE workoutsmart;
```

Sau đó chạy lại backend — Flyway sẽ tạo lại từ đầu.

---

### ❌ Lỗi PowerShell: `running scripts is disabled`

```powershell
# Chạy một lần để cho phép script npm
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

# Hoặc dùng npm.cmd thay npm
npm.cmd run dev
```

---

### ❌ Lỗi: `npm install` thất bại — lỗi mạng / ECONNREFUSED

Thử xóa cache npm và cài lại:

```bash
npm cache clean --force
npm install
```

---

### ❌ Lỗi: Backend khởi động nhưng không load được bài tập

Kiểm tra đường dẫn file dataset trong `backend/.env`:

```ini
# Đường dẫn tương đối tính từ thư mục backend/
EXERCISES_DATASET_PATH=../exercises-dataset/data/exercises.json
```

Đảm bảo thư mục `exercises-dataset/data/exercises.json` tồn tại trong dự án.

---

### ❌ Refresh trang bị đăng xuất

Đây là hành vi bình thường khi backend đang **không chạy** — refresh token không thể được xác minh. Hãy đảm bảo backend đang chạy ở `http://localhost:8080` trước khi dùng ứng dụng.

---

## 10. Dừng Ứng Dụng

Để dừng ứng dụng, vào từng Terminal và nhấn `Ctrl + C`:

- **Terminal 1** (backend) → `Ctrl + C`
- **Terminal 2** (frontend) → `Ctrl + C`

PostgreSQL có thể để chạy ngầm hoặc dừng thủ công:

```powershell
# Windows
Stop-Service -Name "postgresql-x64-18"

# macOS
brew services stop postgresql@18

# Linux
sudo systemctl stop postgresql
```

---

## Phụ Lục — Script Khởi Động Nhanh (Windows)

Dự án đã có sẵn 2 file batch để khởi động nhanh. Tìm trong thư mục gốc dự án:

- **`start-backend.bat`** — double-click để khởi động backend trong cửa sổ mới
- **`start-frontend.bat`** — double-click để khởi động frontend trong cửa sổ mới

> [!TIP]
> Chạy `start-backend.bat` trước, chờ thấy thông báo "Started..." trong cửa sổ backend, rồi mới chạy `start-frontend.bat`.

---

*Nếu gặp vấn đề không có trong tài liệu này, hãy kiểm tra file `RUN_COMMANDS.txt` trong thư mục gốc dự án.*

*Muốn đóng gói bằng Docker thay vì cài JDK/Node/PostgreSQL trên máy: xem [DOCKER.md](./DOCKER.md).*
