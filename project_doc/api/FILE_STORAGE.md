# 本地文件存储

头像和公告附件走 `POST /api/v1/files/upload`，删除走 `DELETE /api/v1/files/{fileId}`。

## 存什么、放哪

| 内容 | 位置 |
| --- | --- |
| 字节 | 进程工作目录下的 `camp.upload.dir`，默认 `data/uploads` |
| 元数据 | 表 `sys_files`（H2 与 MySQL 都有）。字段：`stored_name`、`original_name`、`content_type`、`size_bytes`、`purpose`、`uploader_id` |
| 访问地址 | `url` 固定为 `/api/v1/files/{fileId}`，不把磁盘路径返回给前端 |

磁盘文件名是 UUID 加扩展名，不用用户原始文件名落盘，避免路径穿越。`sys_files` 不设外键，用户被删后审计和文件行仍可留着；删除接口会同时删行和磁盘文件。

H2 是内存库，重启后 `sys_files` 清空，但 `data/uploads` 里的旧文件还在。这些孤儿文件不会再被接口引用。不要把上传目录提交进 git。

## 限制

- 单个文件最大 5MB（`spring.servlet.multipart.max-file-size`）。
- `purpose=AVATAR` 只接受 `image/jpeg`、`image/png`、`image/gif`、`image/webp`。
- `purpose=ANNOUNCEMENT` 或 `OTHER` 再加 `application/pdf`、`text/plain`。
- 登录用户可上传。删除限于上传者或 `ADMIN`。下载需登录，响应 `Content-Disposition: inline`。

用户头像 `avatarUrl`、公告 `attachmentUrl` 如果以 `/api/v1/files/` 开头，写入时会核对 `fileId` 存在。外部 `http(s)` 地址原样保存，本服务不拉取。

## 和首期脚本的关系

MySQL 增量是 `db/migration/V3__phase2_files_and_exams.sql`。H2 启动只执行 `db/h2/schema.sql`，同表已经写在该文件末尾，不要把 V3 直接喂给 H2。
