# Wave D — 文件网盘

前缀 `/api/v1`，响应包是 `BaseResponse`。登录方式和首期一样：`h2` / `dev` 可用 `Authorization: Bearer dev-ADMIN|dev-ACADEMIC|dev-TEACHER|dev-STUDENT`，需要指到种子用户时加 `X-Dev-User-Id`。

不新建表。元数据用已合入的 `oa_files`，分享用 `oa_file_shares`（`db/migration/V3__phase2_schema.sql`，H2 在 `db/h2/schema.sql`）。实体仍是 `StoredFile`，没有 `stored_files` 这张表。

字节不进库。`camp.storage.type=local`（默认）写到 `camp.upload.dir`，默认 `data/uploads`。对象名是 `storage_key` 最后一段。换存储时实现 `com.camp.app.storage.BlobStore`，并把 `camp.storage.type` 设成别的值，让本地实现不再生效。

## 谁能看

| 文件 | 下载和读取 |
| --- | --- |
| `purpose=AVATAR` 或 `ANNOUNCEMENT` | 任意已登录用户。公告附件、头像靠这个地址给别人打开 |
| 其它文件（含 `OTHER`、`STUDENT_IMPORT`、`STUDENT_EXPORT`、种子里的 `dir/`、`obj/`） | 拥有者、ADMIN，或未过期的 `oa_file_shares`（`READ` / `WRITE` 都能读） |
| 目录 | 不能下载，返回 422 |

越权 403，记录不存在 404。有分享或子项时删除返回 409。

列表 `scope`：

| scope | 内容 |
| --- | --- |
| `mine`（默认） | 当前用户自己的。`parentId` 为空看根目录，有值看该目录的直接子项。父目录必须是自己的 |
| `shared` | 直接分享给当前用户，或分享给当前令牌角色的条目。不继承到子文件 |
| `all` | 仅 ADMIN。可按 `parentId` 看任意拥有者 |

## 接口

分页与首期相同：`page` 从 1，`pageSize` 默认 20、最大 100。`data` 为 `{ items, total, page, pageSize }`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/files` | 列表。查询参数 `parentId`、`purpose`、`scope`、`page`、`pageSize` |
| POST | `/files/upload` | `multipart` 字段 `file`，可选 `purpose`、`parentId`。单文件最大 5MB |
| POST | `/files/folders` | JSON `{ "name", "parentId" }`。只写元数据，不落字节 |
| GET | `/files/{fileId}` | 下载字节。`Content-Disposition: inline`，并带 `filename*` |
| DELETE | `/files/{fileId}` | 拥有者或 ADMIN。同时删行和字节 |

`purpose`：`AVATAR`、`ANNOUNCEMENT`、`OTHER`、`STUDENT_IMPORT`、`STUDENT_EXPORT`。不传按 `OTHER`。

| purpose | 允许的 Content-Type |
| --- | --- |
| AVATAR | `image/jpeg`、`image/png`、`image/gif`、`image/webp` |
| STUDENT_IMPORT / STUDENT_EXPORT | `text/csv`、`text/plain`、`application/csv` |
| ANNOUNCEMENT / OTHER | 上面的图片，再加上 `application/pdf` 和表格里的文本 |

类型不对返回 422。`Content-Type` 带 `; charset=utf-8` 时只看分号前的类型。

上传成功示例：

```json
{
  "fileId": 1,
  "url": "/api/v1/files/1",
  "originalName": "a.png",
  "contentType": "image/png",
  "sizeBytes": 3,
  "purpose": "AVATAR",
  "parentId": null,
  "directory": false,
  "ownerId": 1,
  "createdAt": "2026-11-05T10:00:00"
}
```

目录的 `url`、`purpose`、`contentType` 为空，`directory` 为 true。`storage_key` 不返回给前端。

种子里教师 `teacher01` 有根目录「数学资料」，其下两个文件。`期中测验说明.txt` 以 `READ` 分享给 `student01`，到期 `2027-06-30`。种子只有元数据，磁盘上没有对应字节，被分享人下载会 404，未分享的其他人 403。

## 和业务的挂钩

通用文件 API 是上传和下载的唯一入口。业务只保存 `/api/v1/files/{fileId}`，不另开存储。

| 挂钩 | 怎么用 |
| --- | --- |
| 头像 | `users.avatarUrl`。以 `/api/v1/files/` 开头时要求文件存在。请用 `purpose=AVATAR`，这样其他登录用户能打开 |
| 公告附件 | `announcements.attachmentUrl`，同样只核对存在。请用 `purpose=ANNOUNCEMENT`。外部 `http(s)` 地址原样保存，本服务不拉取 |
| 学生导入 | `POST /files/upload`，`purpose=STUDENT_IMPORT`，再 `POST /students/import-file`，body `{ "fileId" }`。权限 `student:import`。文件本身仍按网盘规则读取，所以要由上传者或 ADMIN 调用 |
| 学生导出 | 原来的 `GET /students/export` 仍直接返回 CSV。`GET /students/export-file` 把同一份 CSV 写入网盘并返回 `FileVo`，`purpose=STUDENT_EXPORT`。权限 `student:export`。导出里有手机号和证件号，只有拥有者和 ADMIN 能下载 |

`POST /students/batch-import` 的原始 CSV 体不变。

## 未做

- 创建、修改、撤销分享。只认库里已经有的 `oa_file_shares`
- 分享给目录时，子文件不自动获得权限
- `WRITE` 分享不能往别人的目录上传
- 没有 S3 / OSS 实现，只有 `BlobStore` 这一个插拔点
- 不做公开外链、秒传、在线预览
