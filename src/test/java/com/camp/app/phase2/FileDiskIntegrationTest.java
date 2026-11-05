package com.camp.app.phase2;

import com.camp.app.entity.Class;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = "camp.upload.dir=build/phase2-uploads")
class FileDiskIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClassRepository classRepository;

    @Test
    void uploadListDownloadAndBusinessHook() throws Exception {
        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        long teacherId = userRepository.findByUsername("teacher01").orElseThrow().getId();
        long studentId = userRepository.findByUsername("student01").orElseThrow().getId();
        long academicId = userRepository.findByUsername("academic01").orElseThrow().getId();

        byte[] png = new byte[]{(byte) 137, 80, 78, 71, 13, 10};
        MvcResult uploaded = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(new MockMultipartFile("file", "note.png", "image/png", png))
                        .param("purpose", "OTHER")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.purpose").value("OTHER"))
                .andExpect(jsonPath("$.data.directory").value(false))
                .andReturn();
        JsonNode uploadedNode = objectMapper.readTree(uploaded.getResponse().getContentAsByteArray()).get("data");
        long fileId = uploadedNode.get("fileId").asLong();
        String url = uploadedNode.get("url").asText();

        MvcResult listed = mockMvc.perform(get("/api/v1/files")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode adminItems = objectMapper.readTree(listed.getResponse().getContentAsByteArray()).get("data").get("items");
        boolean found = false;
        for (JsonNode item : adminItems) {
            if (item.get("fileId").asLong() == fileId) {
                found = true;
            }
        }
        assertTrue(found, new String(listed.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get(url)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(png));

        mockMvc.perform(get(url).header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(studentId)))
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/v1/files")
                        .param("scope", "all")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherId)))
                .andExpect(jsonPath("$.code").value(403));

        MvcResult teacherList = mockMvc.perform(get("/api/v1/files")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherId)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode teacherItems = objectMapper.readTree(teacherList.getResponse().getContentAsByteArray()).get("data").get("items");
        assertTrue(containsName(teacherItems, "数学资料"), new String(teacherList.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8));

        MvcResult shared = mockMvc.perform(get("/api/v1/files")
                        .param("scope", "shared")
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(studentId)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode sharedItems = objectMapper.readTree(shared.getResponse().getContentAsByteArray()).get("data").get("items");
        assertTrue(containsName(sharedItems, "期中测验说明.txt"), new String(shared.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8));
        long sharedId = findId(sharedItems, "期中测验说明.txt");
        mockMvc.perform(get("/api/v1/files/" + sharedId)
                        .header("Authorization", "Bearer dev-ACADEMIC")
                        .header("X-Dev-User-Id", String.valueOf(academicId)))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/api/v1/files/" + sharedId)
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(studentId)))
                .andExpect(jsonPath("$.code").value(404));

        MvcResult folder = mockMvc.perform(post("/api/v1/files/folders")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"二期资料\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.directory").value(true))
                .andReturn();
        long folderId = objectMapper.readTree(folder.getResponse().getContentAsByteArray()).get("data").get("fileId").asLong();
        MvcResult nested = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(new MockMultipartFile("file", "inside.txt", "text/plain", "hello".getBytes()))
                        .param("purpose", "OTHER")
                        .param("parentId", String.valueOf(folderId))
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.parentId").value(folderId))
                .andReturn();
        long nestedId = objectMapper.readTree(nested.getResponse().getContentAsByteArray()).get("data").get("fileId").asLong();
        mockMvc.perform(get("/api/v1/files")
                        .param("parentId", String.valueOf(folderId))
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].fileId").value(nestedId));
        mockMvc.perform(delete("/api/v1/files/" + folderId)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(delete("/api/v1/files/" + nestedId)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/v1/files/" + folderId)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200));

        Class classOne = classRepository.findByClassCode("JUNIOR_1_CLASS_1").orElseThrow();
        String csv = "studentNo,realName,classId,gender\nS2026WD01,网盘导入," + classOne.getId() + ",1\n";
        MvcResult importedFile = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(new MockMultipartFile("file", "students.csv", "text/csv", csv.getBytes()))
                        .param("purpose", "STUDENT_IMPORT")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.purpose").value("STUDENT_IMPORT"))
                .andReturn();
        long importId = objectMapper.readTree(importedFile.getResponse().getContentAsByteArray()).get("data").get("fileId").asLong();
        mockMvc.perform(post("/api/v1/students/import-file")
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(studentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":" + importId + "}"))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/api/v1/students/import-file")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":" + importId + "}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successCount").value(1));

        MvcResult exported = mockMvc.perform(get("/api/v1/students/export-file")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.purpose").value("STUDENT_EXPORT"))
                .andReturn();
        String exportUrl = objectMapper.readTree(exported.getResponse().getContentAsByteArray()).get("data").get("url").asText();
        MvcResult csvBody = mockMvc.perform(get(exportUrl)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId)))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(new String(csvBody.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8).contains("S2026WD01"));
        mockMvc.perform(get(exportUrl)
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(studentId)))
                .andExpect(jsonPath("$.code").value(403));

        MvcResult attachment = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(new MockMultipartFile("file", "notice.txt", "text/plain", "请看公告".getBytes()))
                        .param("purpose", "ANNOUNCEMENT")
                        .header("Authorization", "Bearer dev-ACADEMIC")
                        .header("X-Dev-User-Id", String.valueOf(academicId)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode attachmentNode = objectMapper.readTree(attachment.getResponse().getContentAsByteArray()).get("data");
        mockMvc.perform(post("/api/v1/announcements")
                        .header("Authorization", "Bearer dev-ACADEMIC")
                        .header("X-Dev-User-Id", String.valueOf(academicId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"网盘附件公告","content":"见附件","scopeType":"CLASS","scopeIds":[%d],"attachmentUrl":"%s"}
                                """.formatted(classOne.getId(), attachmentNode.get("url").asText())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.attachmentUrl").value(attachmentNode.get("url").asText()));
        mockMvc.perform(get(attachmentNode.get("url").asText())
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(content().bytes("请看公告".getBytes()));

        mockMvc.perform(delete("/api/v1/files/" + sharedId)
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherId)))
                .andExpect(jsonPath("$.code").value(409));

        assertEquals(fileId, uploadedNode.get("fileId").asLong());
    }

    private boolean containsName(JsonNode items, String name) {
        if (items == null) {
            return false;
        }
        for (JsonNode item : items) {
            if (item.get("originalName") != null && name.equals(item.get("originalName").asText())) {
                return true;
            }
        }
        return false;
    }

    private long findId(JsonNode items, String name) {
        for (JsonNode item : items) {
            if (name.equals(item.get("originalName").asText())) {
                return item.get("fileId").asLong();
            }
        }
        throw new AssertionError("列表里没有 " + name);
    }
}
