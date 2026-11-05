package com.camp.app.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件字节的存储。元数据在 oa_files，这里只按对象名读写。
 * 默认实现是本地目录；换成别的实现时把 camp.storage.type 指到对应的 Bean。
 */
public interface BlobStore {
    void write(String objectName, InputStream input) throws IOException;

    Resource read(String objectName);

    boolean exists(String objectName);

    void delete(String objectName) throws IOException;
}
