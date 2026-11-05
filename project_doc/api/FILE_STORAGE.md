# 本地文件存储

头像和公告附件走 `POST /api/v1/files/upload`，删除走 `DELETE /api/v1/files/{fileId}`。

## 存什么、放哪

| 内容 | 位置 |
| --- | --- |
| 字节 | 进程工作目录下的 `camp.upload.dir`，默认 `data/uploads` |
| 元数据 | 已有表 `oa_files`。`name` 是原始文件名，`storage_key` 是 `purpose/uuid.ext`，`mime`、`size_bytes`、`owner_id`。`is_dir=0`，`parent_id` 为空 |
| 访问地址 | `url` 固定为 `/api/v1/files/{fileId}`，不把磁盘路径返回给前端 |

磁盘文件名是 UUID 加扩展名。`purpose` 没有单独的列，写在 `storage_key` 的第一段，响应里再拆出来。

删除会同时删行和磁盘文件。有分享记录的网盘文件不走这个删除（外键会挡住）。目录和 `oa_file_shares` 不在本接口创建。

H2 是内存库，重启后元数据清空，但 `data/uploads` 里的旧文件还在。不要把上传目录提交进 git。

## 限制

- 单个文件最大 5MB（`spring.servlet.multipart.max-file-size`）。
- `purpose=AVATAR` 只接受 `image/jpeg`、`image/png`、`image/gif`、`image/webp`。
- `purpose=ANNOUNCEMENT` 或 `OTHER` 再加 `application/pdf`、`text/plain`。
- 登录用户可上传，上传人必须是已有用户。删除限于拥有者或 `ADMIN`。下载需登录，响应 `Content-Disposition: inline`。

用户头像 `avatarUrl`、公告 `attachmentUrl` 如果以 `/api/v1/files/` 开头，写入时会核对 `fileId` 存在。外部 `http(s)` 地址原样保存，本服务不拉取。

## 和脚本的关系

表来自 `db/migration/V3__phase2_schema.sql`，H2 已转译在 `db/h2/schema.sql`。不要再跑一份 `sys_files` 迁移。
