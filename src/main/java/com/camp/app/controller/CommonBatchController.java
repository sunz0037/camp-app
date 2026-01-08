package com.camp.app.controller;

import com.camp.app.api.BaseResponse;
import com.camp.app.service.CommonBatchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonBatchController {

    private final CommonBatchService commonBatchService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<Void> uploadFile(@RequestPart("file") MultipartFile file,
                                         @RequestParam(required = false) String bizType) {
        commonBatchService.upload(bizType, file);
        return BaseResponse.ok(null);
    }

    @GetMapping("/template")
    public BaseResponse<Void> downloadTemplate(@RequestParam String bizType) {
        commonBatchService.downloadTemplate(bizType);
        return BaseResponse.ok(null);
    }

    @GetMapping("/export")
    public BaseResponse<Void> exportData(@RequestParam String bizType,
                                         @RequestParam(required = false) String filter) {
        commonBatchService.export(bizType, filter);
        return BaseResponse.ok(null);
    }
}

