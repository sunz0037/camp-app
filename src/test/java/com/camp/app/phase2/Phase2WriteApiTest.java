package com.camp.app.phase2;

import com.camp.app.entity.Class;
import com.camp.app.entity.Student;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.StudentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = "camp.upload.dir=build/phase2-uploads")
class Phase2WriteApiTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void writePathsFilesAndScores() throws Exception {
        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        long teacherUserId = userRepository.findByUsername("teacher01").orElseThrow().getId();
        long academicUserId = userRepository.findByUsername("academic01").orElseThrow().getId();
        Class classOne = classRepository.findByClassCode("JUNIOR_1_CLASS_1").orElseThrow();
        Class classTwo = classRepository.findByClassCode("JUNIOR_1_CLASS_2").orElseThrow();
        Student demoStudent = studentRepository.findByStudentNo("S20260001").orElseThrow();

        MvcResult createdUser = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"phase2t","realName":"联调教师","userType":2,"roleCodes":["TEACHER"],"password":"Demo@123456"}
                                """))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();
        long userId = objectMapper.readTree(createdUser.getResponse().getContentAsString()).get("data").get("id").asLong();
        String hash = userRepository.findById(userId).orElseThrow().getPassword();
        mockMvc.perform(put("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"联调教师乙\",\"password\":\"ShouldIgnore1\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.realName").value("联调教师乙"));
        assertEquals(hash, userRepository.findById(userId).orElseThrow().getPassword());

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentNo":"S2026P2","realName":"联调学生","classId":%d,"gender":1,"status":1}
                                """.formatted(classTwo.getId())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentNo").value("S2026P2"));

        mockMvc.perform(post("/api/v1/attendances/records")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"attendanceDate":"2026-11-04","exceptionType":"ABSENT"}
                                """.formatted(studentRepository.findByStudentNo("S2026P2").orElseThrow().getId())))
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/v1/attendances/records")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"attendanceDate":"2026-11-04","exceptionType":"ABSENT","confirmStatus":"CONFIRMED"}
                                """.formatted(demoStudent.getId())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentName").value("学生王小明"))
                .andExpect(jsonPath("$.data.exceptionType").value("ABSENT"));

        MvcResult course = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseCode\":\"ENG7\",\"courseName\":\"英语\",\"credit\":1.5}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.credit").value(1.5))
                .andReturn();
        long courseId = objectMapper.readTree(course.getResponse().getContentAsString()).get("data").get("id").asLong();

        long teacherId = teacherUserId;
        mockMvc.perform(post("/api/v1/class-courses")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":%d,"courseId":%d,"teacherId":%d,"academicYear":"2026-2027","semester":"1","sessions":[{"weekday":1,"periodStart":1,"periodEnd":1,"roomNo":"B1"}]}
                                """.formatted(classOne.getId(), courseId, teacherId)))
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/v1/class-courses")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classId":%d,"courseId":%d,"teacherId":%d,"academicYear":"2026-2027","semester":1,"sessions":[{"weekday":1,"periodStart":5,"periodEnd":6,"roomNo":"B1"}]}
                                """.formatted(classOne.getId(), courseId, teacherId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessions[0].periodStart").value(5));

        MvcResult announcement = mockMvc.perform(post("/api/v1/announcements")
                        .header("Authorization", "Bearer dev-ACADEMIC")
                        .header("X-Dev-User-Id", String.valueOf(academicUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"二期联调公告","content":"请查看课表","scopeType":"CLASS","scopeIds":[%d]}
                                """.formatted(classOne.getId())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.scopeType").value("CLASS"))
                .andReturn();
        long announcementId = objectMapper.readTree(announcement.getResponse().getContentAsString()).get("data").get("id").asLong();
        mockMvc.perform(delete("/api/v1/announcements/" + announcementId)
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200));

        MvcResult uploaded = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{(byte) 137, 80, 78, 71}))
                        .param("purpose", "AVATAR")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileId").isNumber())
                .andExpect(jsonPath("$.data.url").exists())
                .andReturn();
        JsonNode file = objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("data");
        mockMvc.perform(get(file.get("url").asText()).header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/files/" + file.get("fileId").asLong())
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200));

        long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        mockMvc.perform(post("/api/v1/exam-schedules")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"examName":"期中","examDate":"2026-11-20T08:00:00","targetGrade":"初中一年级","examType":"MIDTERM","isPublic":false}
                                """))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.examType").value("期中"));

        mockMvc.perform(post("/api/v1/scores")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":%d,"courseId":%d,"examType":"期中","semester":"1","academicYear":"2026-2027","score":92}
                                """.formatted(demoStudent.getId(), courseId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.gradePoint").value(4.0));

        mockMvc.perform(get("/api/v1/scores")
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(demoStudent.getUserId())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
