# 🌐 WorkoutSmartApp — Hướng Dẫn Deploy Trên Máy Cá Nhân & Mở Cổng Cho Người Khác Truy Cập

> **Phiên bản tài liệu:** 1.0.0 | **Cập nhật:** 2026-08-19  
> Hướng dẫn này dành cho việc chạy WorkoutSmartApp trên máy tính cá nhân **và mở cho người khác truy cập** qua mạng LAN hoặc Internet.

---

## Mục Lục

- [Tổng quan kiến trúc](#tổng-quan-kiến-trúc)
- [Phương án A — Mạng LAN (nhanh, không cần cấu hình router)](#phương-án-a--mạng-lan-nhanh)
- [Phương án B — Internet qua Cloudflare Tunnel (dễ nhất, không cần IP tĩnh)](#phương-án-b--internet-qua-cloudflare-tunnel-dễ-nhất)
- [Phương án C — Mở cổng Router + DuckDNS (tự chủ, miễn phí)](#phương-án-c--mở-cổng-router--duckdns)
- [Bước chung: Build Production & Cài Nginx](#bước-chung-build-production--cài-nginx)
- [Gỡ lỗi thường gặp](#gỡ-lỗi-thường-gặp)

---

## Tổng Quan Kiến Trúc

Khi người khác truy cập, luồng dữ liệu như sau:

```
[ Người dùng - máy khác ]
         │ https://your-domain.com  (hoặc http://192.168.x.x)
         ▼
  [ Nginx :80/:443 ]  ← chạy trên máy của bạn
         │
         ├─ /         ──► [ React SPA (web/dist/) ]
         ├─ /api/**   ──► [ Spring Boot :8080 ]
         └─ /media/** ──► [ Spring Boot :8080 ]
```

**Nginx** đóng vai trò cửa ngõ duy nhất ra bên ngoài, backend không bao giờ lộ trực tiếp ra internet.

---

## Bước Chung: Build Production & Cài Nginx

> [!IMPORTANT]
> Thực hiện **mục này trước**, bất kể bạn chọn Phương án A, B hay C.

### Bước 1 — Build Frontend (React → tĩnh)

```bash
cd web
npm install
npm run build
# Kết quả: thư mục web/dist/ chứa toàn bộ file tĩnh
```

### Bước 2 — Build Backend (Spring Boot → JAR)

```bash
cd backend
mvn clean package -DskipTests
# Kết quả: backend/target/backend-0.1.0-SNAPSHOT.jar
```

### Bước 3 — Tạo file `.env` cho Backend

Tạo file `backend/.env` (nếu chưa có) với nội dung:

```ini
# === DATABASE ===
DB_URL=jdbc:postgresql://localhost:5432/workoutsmart
DB_USERNAME=postgres
DB_PASSWORD=your_db_password

# === JWT (BẮT BUỘC — chuỗi ngẫu nhiên >= 32 ký tự) ===
JWT_SECRET=replace-with-your-own-random-secret-string-here!

# === EMAIL ===
# Đổi thành smtp nếu muốn gửi OTP thật
EMAIL_PROVIDER=log
MAIL_USERNAME=
MAIL_PASSWORD=

# === MEDIA ===
EXERCISES_DATASET_PATH=../exercises-dataset/data/exercises.json
EXERCISES_MEDIA_PATH=../exercises-dataset/
USER_MEDIA_PATH=./uploads/media/
```

### Bước 4 — Cài Nginx trên Windows

1. Tải Nginx tại: https://nginx.org/en/download.html (chọn bản **Stable version** — file `.zip`)
2. Giải nén vào `C:\nginx\`
3. Thay toàn bộ nội dung file `C:\nginx\conf\nginx.conf` bằng:

```nginx
worker_processes 1;

events {
    worker_connections 1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    client_max_body_size 100M;

    # Bật gzip nén để tăng tốc độ tải trang
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    server {
        listen 80;
        server_name _;     # Thay bằng domain của bạn nếu có, VD: your-name.duckdns.org

        # Phục vụ file tĩnh React SPA
        root   C:/Users/your-username/WorkoutSmartApp/web/dist;
        index  index.html;

        # React Router — tránh 404 khi F5 trên các route như /profile, /training...
        location / {
            try_files $uri $uri/ /index.html;
        }

        # Proxy API sang Spring Boot Backend
        location /api/ {
            proxy_pass         http://127.0.0.1:8080/api/;
            proxy_set_header   Host $host;
            proxy_set_header   X-Real-IP $remote_addr;
            proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header   X-Forwarded-Proto $scheme;
            proxy_read_timeout 60s;
        }

        # Proxy Media (ảnh bài tập, upload)
        location /media/ {
            proxy_pass http://127.0.0.1:8080/media/;
            proxy_set_header Host $host;
        }
    }
}
```

> [!WARNING]
> Thay `C:/Users/your-username/WorkoutSmartApp/web/dist` thành **đường dẫn thực tế** trên máy bạn.  
> Dùng **dấu gạch chéo xuôi `/`** (không dùng `\`) trong file config Nginx.

### Bước 5 — Khởi động hệ thống

Mở **3 cửa sổ terminal riêng biệt**:

**Terminal 1 — Backend:**
```powershell
cd C:\Users\your-username\WorkoutSmartApp\backend
java -jar target/backend-0.1.0-SNAPSHOT.jar
# Chờ thấy: "Started WorkoutsmartBackendApplication in x.x seconds"
```

**Terminal 2 — Nginx:**
```powershell
cd C:\nginx
.\nginx.exe
# Nginx chạy ngầm, không thấy thông báo gì là bình thường
```

Kiểm tra Nginx đang chạy:
```powershell
# Xem process nginx
Get-Process nginx
```

Dừng / reload Nginx:
```powershell
.\nginx.exe -s stop    # Dừng hoàn toàn
.\nginx.exe -s reload  # Reload config (không cần dừng)
```

---

## Phương Án A — Mạng LAN (Nhanh)

> **Dùng khi:** Bạn và người dùng **cùng mạng WiFi / LAN** (gia đình, văn phòng, trường học).

### Bước A1 — Tìm địa chỉ IP LAN của máy bạn

```powershell
# Windows
ipconfig
# Tìm dòng "IPv4 Address" — thường là 192.168.x.x hoặc 10.x.x.x
```

Giả sử IP của bạn là `192.168.1.100`.

### Bước A2 — Mở tường lửa Windows cho cổng 80

```powershell
# Chạy PowerShell với quyền Administrator
New-NetFirewallRule -DisplayName "WorkoutSmartApp HTTP" `
  -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
```

### Bước A3 — Chia sẻ địa chỉ cho người dùng khác

Người dùng trong cùng mạng LAN truy cập:
```
http://192.168.1.100
```

> [!TIP]
> Để địa chỉ IP máy bạn không thay đổi mỗi khi restart, vào router và đặt **Static DHCP / IP Reservation** cho địa chỉ MAC của máy bạn.

---

## Phương Án B — Internet Qua Cloudflare Tunnel (Dễ Nhất)

> **Dùng khi:** Bạn muốn người ở **bất kỳ đâu trên internet** truy cập được, **không cần IP tĩnh**, **không cần mở port router**, có HTTPS miễn phí.

### Bước B1 — Tạo tài khoản Cloudflare miễn phí

Đăng ký tại: https://dash.cloudflare.com/sign-up  
*(Không cần có domain riêng — Cloudflare cấp miễn phí subdomain)*

### Bước B2 — Cài Cloudflare Tunnel (cloudflared)

```powershell
# Tải cloudflared cho Windows
# https://github.com/cloudflare/cloudflared/releases/latest
# Tải file cloudflared-windows-amd64.exe, đặt vào C:\cloudflared\cloudflared.exe

# Đăng nhập
C:\cloudflared\cloudflared.exe tunnel login
# Trình duyệt mở ra → chọn tài khoản Cloudflare → Authorize
```

### Bước B3 — Tạo và cấu hình Tunnel

```powershell
# Tạo tunnel tên "workoutsmart"
C:\cloudflared\cloudflared.exe tunnel create workoutsmart

# Xem UUID của tunnel vừa tạo (ghi nhớ để dùng bước sau)
C:\cloudflared\cloudflared.exe tunnel list
```

Tạo file cấu hình tại `C:\cloudflared\config.yml`:

```yaml
tunnel: <UUID-của-tunnel>        # Thay bằng UUID ở bước trên
credentials-file: C:\Users\your-username\.cloudflared\<UUID>.json

ingress:
  - hostname: workoutsmart.yourdomain.com   # Subdomain bạn muốn dùng
    service: http://localhost:80             # Nginx đang lắng nghe cổng 80
  - service: http_status:404
```

### Bước B4 — Trỏ DNS về Tunnel

```powershell
C:\cloudflared\cloudflared.exe tunnel route dns workoutsmart workoutsmart.yourdomain.com
```

### Bước B5 — Chạy Tunnel

```powershell
# Terminal 3 — Cloudflare Tunnel
C:\cloudflared\cloudflared.exe tunnel --config C:\cloudflared\config.yml run workoutsmart
```

Người dùng truy cập qua:
```
https://workoutsmart.yourdomain.com   # HTTPS tự động, miễn phí!
```

> [!NOTE]
> Cloudflare Tunnel **không yêu cầu** mở port trên router. Lưu lượng đi qua server của Cloudflare, máy bạn chủ động kết nối ra.

---

## Phương Án C — Mở Cổng Router + DuckDNS

> **Dùng khi:** Bạn muốn **tự chủ hoàn toàn**, không phụ thuộc dịch vụ bên ngoài. Yêu cầu có quyền truy cập vào router.

### Bước C1 — Đặt IP tĩnh trong LAN cho máy bạn

Vào router (thường ở `192.168.1.1`) → **DHCP / Address Reservation** → Gán IP cố định cho máy tính của bạn, ví dụ: `192.168.1.100`.

### Bước C2 — Mở Port Forwarding trên Router

Vào router → **Port Forwarding / Virtual Server** → Thêm rule:

| Trường | Giá trị |
|---|---|
| Tên | WorkoutSmartApp |
| Giao thức | TCP |
| Cổng ngoài (External Port) | `80` |
| Cổng trong (Internal Port) | `80` |
| IP nội bộ (Internal IP) | `192.168.1.100` (IP máy bạn) |

> [!CAUTION]
> Giao diện router khác nhau theo từng hãng. Tìm kiếm: `"[tên router] port forwarding"` để có hướng dẫn cụ thể.

### Bước C3 — Mở Tường Lửa Windows

```powershell
# Chạy PowerShell với quyền Administrator
New-NetFirewallRule -DisplayName "WorkoutSmartApp HTTP" `
  -Direction Inbound -Protocol TCP -LocalPort 80 -Action Allow
```

### Bước C4 — Đăng ký DuckDNS (Domain miễn phí)

1. Vào https://www.duckdns.org → đăng nhập bằng Google/GitHub
2. Tạo subdomain, ví dụ: `workoutsmart.duckdns.org`
3. DuckDNS tự trỏ về **IP công cộng hiện tại** của bạn

**Tự động cập nhật IP khi thay đổi (quan trọng!):**

Tạo file `C:\duckdns\update.ps1`:
```powershell
$domain = "workoutsmart"
$token  = "your-duckdns-token"   # Lấy từ trang duckdns.org
$url = "https://www.duckdns.org/update?domains=$domain&token=$token&ip="
Invoke-WebRequest -Uri $url -UseBasicParsing | Out-Null
```

Đặt lịch tự động chạy mỗi 5 phút (Windows Task Scheduler):
```powershell
# Chạy PowerShell với quyền Administrator
$action = New-ScheduledTaskAction -Execute "powershell.exe" `
  -Argument "-NonInteractive -File C:\duckdns\update.ps1"
$trigger = New-ScheduledTaskTrigger -RepetitionInterval (New-TimeSpan -Minutes 5) -Once -At (Get-Date)
Register-ScheduledTask -TaskName "DuckDNS Update" -Action $action -Trigger $trigger -RunLevel Highest
```

### Bước C5 — Truy cập

Người dùng ở bất kỳ đâu vào:
```
http://workoutsmart.duckdns.org
```

### Bước C6 (Tùy chọn) — Thêm HTTPS miễn phí với Certbot

```powershell
# Cài Certbot cho Windows: https://certbot.eff.org/instructions?ws=nginx&os=windows
# Sau khi cài:
certbot --nginx -d workoutsmart.duckdns.org
# Certbot tự cấu hình Nginx + cấp SSL/TLS từ Let's Encrypt (miễn phí, gia hạn tự động)
```

---

## So Sánh Các Phương Án

| Tiêu chí | Phương án A (LAN) | Phương án B (Cloudflare Tunnel) | Phương án C (Port Forwarding) |
|---|:---:|:---:|:---:|
| Dễ cài đặt | ✅ Rất dễ | ✅ Dễ | ⚠️ Trung bình |
| Truy cập từ internet | ❌ Không | ✅ Có | ✅ Có |
| Yêu cầu mở port router | ❌ Không | ❌ Không | ✅ Có |
| HTTPS / SSL | ❌ Không | ✅ Tự động | ⚠️ Cần cài thêm |
| Phụ thuộc dịch vụ ngoài | ❌ Không | ⚠️ Cloudflare | ⚠️ DuckDNS |
| Tự chủ hoàn toàn | ✅ Có | ❌ Không | ✅ Có |
| Chi phí | 🆓 Miễn phí | 🆓 Miễn phí | 🆓 Miễn phí |

---

## Gỡ Lỗi Thường Gặp

### ❌ Người ngoài vào được nhưng thấy lỗi 502 Bad Gateway

Backend chưa chạy. Kiểm tra:
```powershell
Invoke-WebRequest http://localhost:8080/swagger-ui.html
```
Nếu không kết nối được → khởi động lại backend.

### ❌ Nginx không start — lỗi `bind() to 0.0.0.0:80 failed`

Cổng 80 đang bị chiếm (thường do IIS hoặc Skype).
```powershell
# Tìm process đang dùng port 80
netstat -ano | findstr :80
# Kill theo PID
taskkill /PID <PID> /F
```

Hoặc đổi Nginx sang cổng `8888` và Port Forwarding `80 → 8888` trên router.

### ❌ Người ngoài không vào được, mình vào được

Kiểm tra theo thứ tự:
1. **Firewall Windows** đã mở cổng 80 chưa?
2. **Port Forwarding** trên router đã cấu hình đúng IP LAN máy bạn chưa?
3. **ISP** có chặn cổng 80 không? (Thử dùng cổng 8080 thay thế)

### ❌ Domain DuckDNS trỏ sai IP

Chạy thủ công script cập nhật:
```powershell
powershell -File C:\duckdns\update.ps1
```
Sau đó kiểm tra: https://www.duckdns.org — IP hiển thị phải khớp với IP công cộng của bạn.

### ❌ Cloudflare Tunnel không kết nối

```powershell
# Kiểm tra log
C:\cloudflared\cloudflared.exe tunnel --config C:\cloudflared\config.yml run workoutsmart
# Đọc thông báo lỗi trong output
```

---

## Lưu Ý Bảo Mật Khi Mở Ra Internet

> [!WARNING]
> Khi mở ứng dụng ra internet, hãy đảm bảo:

1. **Dùng `JWT_SECRET` đủ mạnh** — chuỗi ngẫu nhiên ít nhất 32 ký tự, không dùng giá trị mặc định.
2. **Đổi mật khẩu PostgreSQL** khỏi `postgres/postgres` mặc định.
3. **Không expose cổng 8080** (backend) hay `5432` (database) ra ngoài — chỉ mở cổng 80/443 của Nginx.
4. **Đổi `EMAIL_PROVIDER=smtp`** để người dùng nhận OTP thật (tránh lộ OTP qua log).
5. Nginx config đã chặn backend — chỉ cổng 80/443 được proxy. **Không thêm rule mở cổng 8080 trong firewall.**

---

*Tài liệu liên quan: `RUN_COMMANDS.txt` (chạy local dev), `SELF_HOSTING.md` (chạy local không mở internet).*
