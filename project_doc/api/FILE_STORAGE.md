# 本地文件存储

网盘契约见 [phase2-files.md](./phase2-files.md)。这里只记落盘方式。

| 内容 | 位置 |
| --- | --- |
| 字节 | `camp.storage.type=local` 时写到 `camp.upload.dir`，默认 `data/uploads`。对象名是 UUID 加扩展名 |
| 元数据 | 已有表 `oa_files`。`name` 是原始文件名，`storage_key` 是 `purpose/uuid.ext`（目录是 `dir/{ownerId}/{uuid}`），另有 `mime`、`size_bytes`、`owner_id`、`parent_id`、`is_dir` |
| 访问地址 | 文件的 `url` 为 `/api/v1/files/{fileId}`。目录没有 url。不把磁盘路径返回给前端 |

`purpose` 没有单独的列，写在 `storage_key` 的第一段。

删除会同时删行和磁盘文件。目录非空，或 `oa_file_shares` 里还有分享时，返回 409，不删。

H2 是内存库，重启后元数据清空，但 `data/uploads` 里的旧文件还在。不要把上传目录提交进 git。

表来自 `db/migration/V3__phase2_schema.sql`，H2 已转译在 `db/h2/schema.sql`。Wave D 没有新的 migration。
