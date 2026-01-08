package com.camp.app.service.impl;

import com.camp.app.service.CommonBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class CommonBatchServiceImpl implements CommonBatchService {
    
    @Override
    public void upload(String bizType, MultipartFile file) {
        // 根据业务类型路由到不同的导入处理器
        switch (bizType) {
            case "student":
                // TODO: 调用StudentService的批量导入方法
                break;
            case "score":
                // TODO: 调用ExamScoreService的批量导入方法
                break;
            case "attendance":
                // TODO: 调用AttendanceService的批量导入方法
                break;
            default:
                throw new IllegalArgumentException("Unsupported bizType: " + bizType);
        }
    }

    @Override
    public byte[] downloadTemplate(String bizType) {
        // 返回Excel模板内容
        String template = "";
        switch (bizType) {
            case "student":
                template = "学号,姓名,年级,班级,专业\n";
                break;
            case "score":
                template = "学号,课程代码,考试类型,成绩,学期,学年\n";
                break;
            case "attendance":
                template = "学号,日期,状态,缺勤原因\n";
                break;
            default:
                throw new IllegalArgumentException("Unsupported bizType: " + bizType);
        }
        return template.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] export(String bizType, String filter) {
        // 生成导出内容（CSV格式）
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            String header = "";
            switch (bizType) {
                case "student":
                    header = "学号,姓名,年级,班级,专业\n";
                    break;
                case "score":
                    header = "学号,课程代码,考试类型,成绩,学期,学年\n";
                    break;
                case "attendance":
                    header = "学号,日期,状态,缺勤原因\n";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported bizType: " + bizType);
            }
            outputStream.write(header.getBytes(StandardCharsets.UTF_8));
            // TODO: 根据filter参数查询数据并写入outputStream
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate export content", e);
        }
        return outputStream.toByteArray();
    }
}

