# Data Model: 012 Exercise Library Filters

Không bảng mới. Dùng `exercises` hiện có.

## Exercise (reuse)

| Trường | Vai trò 012 |
|---|---|
| `category` | Filter OR; 10 giá trị dataset; form custom bắt buộc |
| `equipment` | Filter OR; 28 giá trị snake_case; form custom bắt buộc |
| `body_part` | Form custom gán = `category` |
| `muscle_group` | Không filter UI; form custom bắt buộc 6 giá trị |
| `name` | Search contains, case-insensitive |
| `source` / `created_by` / `deleted_at` | Visibility 011 không đổi |

## LibraryFilter (client state)

- `categories: string[]` — rỗng = không lọc
- `equipments: string[]` — rỗng = không lọc
- `q: string`
- `page`, `size`

## Validation form custom

- `name` not blank
- `category` ∈ 10 giá trị
- `equipment` ∈ 28 giá trị
- `muscleGroup` ∈ {chest, back, shoulders, arms, legs, core}
