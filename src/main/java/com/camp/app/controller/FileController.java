package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.model.FileVo;
import com.camp.app.security.AuthContext;
import com.camp.app.security.AuthPrincipal;
import com.camp.app.security.Phase1Roles;
import com.camp.app.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileVo> upload(@RequestPart("file") MultipartFile file,
                                       @RequestParam(value = "purpose", required = false) String purpose) {
        return BaseResponse.ok(fileStorageService.upload(file, AuthContext.userId(), purpose));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        Resource resource = fileStorageService.load(fileId);
        String type = fileStorageService.contentType(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type))
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    public BaseResponse<Void> delete(@PathVariable Long fileId) {
        AuthPrincipal principal = AuthContext.get();
        boolean admin = principal != null && principal.roles() != null && principal.roles().contains(Phase1Roles.ADMIN);
        fileStorageService.delete(fileId, AuthContext.userId(), admin);
        return BaseResponse.ok(null);
    }
}
