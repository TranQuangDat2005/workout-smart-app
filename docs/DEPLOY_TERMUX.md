# WorkoutSmartApp trên Termux

Hướng dẫn này chạy toàn bộ web app native trong Termux, không dùng Docker hoặc Nginx:

```text
Cloudflare Tunnel -> cloudflared
                      ├── React SPA :5173
                      └── Spring Boot :8080 -> PostgreSQL :5432
```

Termux không cần quyền root cho các cổng `5173`, `8080` và `5432`. Nên cài Termux từ F-Droid hoặc GitHub Releases chính thức, không trộn package repository giữa các nguồn.

## 1. Cài công cụ

Trong Termux:

```bash
pkg update && pkg upgrade
pkg install git openjdk-17 nodejs-lts maven postgresql termux-services
```

Nếu package `openjdk-17` hoặc `nodejs-lts` không tồn tại trên mirror hiện tại, chạy `pkg search openjdk` hoặc `pkg search nodejs` và chọn bản Java 17/Node.js LTS tương ứng.

Kiểm tra:

```bash
java -version
node --version
mvn --version
```

## 2. Lấy mã nguồn

```bash
cd "$HOME"
git clone <URL_REPOSITORY> workout-smart-app
cd "$HOME/workout-smart-app"
```

Nếu mã nguồn đã được chép vào máy, chỉ cần `cd` tới thư mục đó. Bộ dữ liệu bài tập phải tồn tại tại `exercises-dataset/`.

## 3. Khởi tạo PostgreSQL

Chỉ chạy `initdb` một lần:

```bash
initdb -D "$PREFIX/var/lib/postgresql" -U postgres --auth=trust
pg_ctl -D "$PREFIX/var/lib/postgresql" -l "$PREFIX/var/log/postgresql.log" start
createdb -U postgres workoutsmart 2>/dev/null || true
```

`--auth=trust` chỉ phù hợp cho PostgreSQL local trong Termux. Không mở cổng `5432` ra Internet. Kiểm tra database:

```bash
pg_isready -h 127.0.0.1 -p 5432
psql -U postgres -d workoutsmart -c '\\conninfo'
```

Muốn PostgreSQL tự chạy cùng Termux:

```bash
mkdir -p "$PREFIX/var/service/postgresql/log"
cat > "$PREFIX/var/service/postgresql/run" <<'EOF'
#!/data/data/com.termux/files/usr/bin/sh
exec postgres -D "$PREFIX/var/lib/postgresql"
EOF
chmod +x "$PREFIX/var/service/postgresql/run"
sv-enable postgresql
```

Nếu service script không phù hợp với phiên bản Termux, dùng lệnh `pg_ctl ... start` thủ công sau mỗi lần khởi động app.

## 4. Cấu hình và build backend

Chạy trong thư mục gốc project:

```bash
export DB_URL='jdbc:postgresql://127.0.0.1:5432/workoutsmart'
export DB_USERNAME='postgres'
export DB_PASSWORD='postgres'
export JWT_SECRET="$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48)"
export EMAIL_PROVIDER='log'
export EXERCISES_DATASET_PATH="$PWD/exercises-dataset/data/exercises.json"
export EXERCISES_MEDIA_PATH="$PWD/exercises-dataset/"
export USER_MEDIA_PATH="$PWD/backend/uploads/media/"
mkdir -p "$USER_MEDIA_PATH"

cd backend
./mvnw clean package -DskipTests
```

Nếu shell báo `Permission denied`, chạy `bash mvnw clean package -DskipTests`. Giữ các biến `export` trong cùng phiên Termux khi khởi động JAR.

Khởi động backend ở terminal thứ nhất:

```bash
cd "$HOME/workout-smart-app/backend"
java -jar target/backend-0.1.0-SNAPSHOT.jar
```

Backend chạy tại `http://127.0.0.1:8080`. Flyway tự chạy migration khi khởi động. OTP đăng ký được ghi trong log vì `EMAIL_PROVIDER=log`.

## 5. Build frontend và cài Cloudflare Tunnel

Ở terminal thứ hai:

```bash
cd "$HOME/workout-smart-app/web"
npm install
npm run build
```

### Cài `cloudflared`

Kiểm tra package Termux trước:

```bash
pkg search cloudflared
```

Nếu mirror có package, cài bằng:

```bash
pkg install cloudflared
```

Nếu không có, tải binary ARM64 tương ứng từ trang Releases chính thức của Cloudflare, đặt tên là `cloudflared`, rồi chạy:

```bash
chmod +x cloudflared
mv cloudflared "$PREFIX/bin/cloudflared"
cloudflared --version
```

### Tạo tunnel và gắn domain

Thay `app.example.com` bằng domain của bạn. Domain phải được quản lý trong tài khoản Cloudflare:

```bash
cloudflared tunnel login
cloudflared tunnel create workoutsmart
cloudflared tunnel list
cloudflared tunnel route dns workoutsmart app.example.com
```

Ghi lại UUID tunnel được in ra từ lệnh `tunnel create`, sau đó tạo `$HOME/.cloudflared/config.yml`:

```bash
mkdir -p "$HOME/.cloudflared"
cat > "$HOME/.cloudflared/config.yml" <<'EOF'
tunnel: YOUR_TUNNEL_UUID
credentials-file: /data/data/com.termux/files/home/.cloudflared/YOUR_TUNNEL_UUID.json

ingress:
    - hostname: app.example.com
        path: ^/api/.*
        service: http://127.0.0.1:8080

    - hostname: app.example.com
        path: ^/media/.*
        service: http://127.0.0.1:8080

    - hostname: app.example.com
        service: http://127.0.0.1:5173

    - service: http_status:404
EOF
```

Thay cả `YOUR_TUNNEL_UUID` và `app.example.com` trong file. Kiểm tra cấu hình:

```bash
cloudflared tunnel ingress validate
```

## 6. Khởi động app và tunnel

Ở terminal frontend:

```bash
cd "$HOME/workout-smart-app/web"
npm run preview -- --host 127.0.0.1 --port 5173
```

Ở terminal tunnel:

```bash
cloudflared tunnel run workoutsmart
```

Mở ứng dụng tại:

```text
https://app.example.com
```

Cloudflare Tunnel thay thế Nginx ở lớp public: không cần mở port router, không cần bind cổng `80/443`, và HTTPS được Cloudflare xử lý.

## 7. Truy cập local và chạy nền

Lấy IP Wi-Fi của điện thoại:

```bash
ip route get 1.1.1.1
```

Không cần truy cập bằng IP LAN khi dùng Cloudflare Tunnel. Nếu cần kiểm tra riêng frontend trên điện thoại, mở `http://127.0.0.1:5173`.

Android có thể dừng tiến trình nền; giữ Termux chạy, tắt battery optimization cho Termux, và dùng `termux-wake-lock` khi cần. Cần giữ PostgreSQL, backend và frontend preview chạy trước khi khởi động tunnel.

## 7. Dừng và khởi động lại

```bash
pg_ctl -D "$PREFIX/var/lib/postgresql" stop
```

Khởi động lại theo thứ tự PostgreSQL, backend, frontend preview, rồi Cloudflare Tunnel. Dừng tunnel bằng `Ctrl+C` trong terminal đang chạy `cloudflared`. Xem log backend ở terminal đang chạy JAR và log PostgreSQL tại `$PREFIX/var/log/postgresql.log`.

## Gỡ lỗi nhanh

- `JWT_SECRET` trống: export lại biến trong đúng terminal chạy `java`.
- `Connection refused` ở `5432`: chạy `pg_ctl ... start`, rồi kiểm tra `pg_isready`.
- Cloudflare báo 502: kiểm tra frontend đang nghe ở `127.0.0.1:5173` và backend đang nghe ở `127.0.0.1:8080`.
- Route React trả 404: kiểm tra `npm run preview` đang chạy và ingress catch-all trỏ tới port `5173`.
- Tunnel không nhận domain: chạy lại `cloudflared tunnel route dns workoutsmart app.example.com` và kiểm tra DNS record trong Cloudflare.
- Thiếu bộ dữ liệu: kiểm tra `exercises-dataset/data/exercises.json` và các thư mục media.