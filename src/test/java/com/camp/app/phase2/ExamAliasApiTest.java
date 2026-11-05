package com.camp.app.phase2;

import com.camp.app.entity.Class;
import com.camp.app.entity.Student;
import com.camp.app.repository.ClassRepository;
import com.camp.app.repository.CourseRepository;
import com.camp.app.repository.StudentRepository;
import com.camp.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = "camp.upload.dir=build/phase2-uploads")
class ExamAliasApiTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void examsAndScoresUseExistingTables() throws Exception {
        mockMvc.perform(get("/api/v1/exams")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items").isArray());
        mockMvc.perform(get("/api/v1/exam-scores")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/v1/exams")
                        .param("keyword", "数学期中测验")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].examName").value("【演示】数学期中测验"))
                .andExpect(jsonPath("$.data.items[0].courseName").value("数学"))
                .andExpect(jsonPath("$.data.items[0].className").value("初一1班"))
                .andExpect(jsonPath("$.data.items[0].gradeName").value("初中一年级"));

        mockMvc.perform(get("/api/v1/exam-scores")
                        .param("keyword", "王小明")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].studentName").value("学生王小明"))
                .andExpect(jsonPath("$.data.items[0].studentNo").value("S20260001"))
                .andExpect(jsonPath("$.data.items[0].courseName").value("数学"))
                .andExpect(jsonPath("$.data.items[0].score").value(92.5))
                .andExpect(jsonPath("$.data.items[0].gradeLevel").value("A"));

        mockMvc.perform(get("/api/v1/exams")
                        .param("examType", "期中")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/api/v1/exams")
                        .param("isPublic", "true")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/exams")
                        .param("status", "CANCELLED")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/exams")
                        .param("grade", "高一")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/exams")
                        .param("status", "NORMAL")
                        .param("isPublic", "false")
                        .param("keyword", "数学期中测验")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/api/v1/exam-schedules")
                        .param("status", "0")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(400));

        long teacherUserId = userRepository.findByUsername("teacher01").orElseThrow().getId();
        mockMvc.perform(get("/api/v1/exams")
                        .param("keyword", "数学期中测验")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherUserId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/api/v1/exams")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-Class-Ids", ""))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));

        Class classOne = classRepository.findByClassCode("JUNIOR_1_CLASS_1").orElseThrow();
        long courseId = courseRepository.findByCourseCode("MATH7").orElseThrow().getId();
        long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        Student demoStudent = studentRepository.findByStudentNo("S20260001").orElseThrow();

        mockMvc.perform(post("/api/v1/exam-schedules")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"examName":"缺学年","examDate":"2026-12-01T08:00:00","classId":%d,"courseId":%d}
                                """.formatted(classOne.getId(), courseId)))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("学年")));

        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"教师不能建","subject":"数学","classId":%d,"startTime":"2026-12-18T08:00:00"}
                                """.formatted(classOne.getId())))
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"无班","subject":"数学","startTime":"2026-12-19T08:00:00","examType":"月考","isPublic":true,"status":"NORMAL"}
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("班级")));

        MvcResult created = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"名称班","subject":"数学","className":"初一1班","gradeName":"初中一年级","startTime":"2026-12-21T08:00:00","endTime":"2026-12-21T10:00:00","examType":"期末","isPublic":true,"status":"NORMAL","remark":"不入库"}
                                """))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.examName").value("名称班"))
                .andExpect(jsonPath("$.data.classId").value(classOne.getId()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.academicYear").value("2026-2027"))
                .andExpect(jsonPath("$.data.semester").value(1))
                .andReturn();
        long examId = objectMapper.readTree(created.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/exam-schedules")
                        .param("keyword", "名称班")
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].examName").value("名称班"));

        mockMvc.perform(put("/api/v1/exams/" + examId)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"名称班","subject":"数学","className":"初一1班","status":"CANCELLED"}
                                """))
                .andExpect(jsonPath("$.code").value(422));

        MvcResult score = mockMvc.perform(post("/api/v1/exam-scores")
                        .header("Authorization", "Bearer dev-ADMIN")
                        .header("X-Dev-User-Id", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"examId":%d,"studentId":%d,"score":88,"gradeLevel":"A"}
                                """.formatted(examId, demoStudent.getId())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.examScheduleId").value(examId))
                .andExpect(jsonPath("$.data.gradePoint").value(3.0))
                .andExpect(jsonPath("$.data.gradeLevel").value("B"))
                .andReturn();
        long scoreId = objectMapper.readTree(score.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/scores")
                        .param("examScheduleId", String.valueOf(examId))
                        .header("Authorization", "Bearer dev-ADMIN"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/v1/exam-scores")
                        .param("examId", String.valueOf(examId))
                        .header("Authorization", "Bearer dev-STUDENT")
                        .header("X-Dev-User-Id", String.valueOf(demoStudent.getUserId())))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[*].studentNo", everyItem(org.hamcrest.Matchers.is("S20260001"))));

        mockMvc.perform(put("/api/v1/exam-scores/" + scoreId)
                        .header("Authorization", "Bearer dev-ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":61}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.gradeLevel").value("D"));
    }

    @Test
    void visitorReasonAliasDoesNotResolveHostName() throws Exception {
        long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        long teacherUserId = userRepository.findByUsername("teacher01").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/visitors")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visitorName":"别名访客","phone":"13800002222","reason":"送文件","hostName":"班主任李想","appointmentTime":"2026-12-03T09:00:00"}
                                """))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(not(containsString("事由不能为空"))))
                .andExpect(jsonPath("$.message").value(containsString("hostUserId")));

        mockMvc.perform(post("/api/v1/visitors")
                        .header("Authorization", "Bearer dev-TEACHER")
                        .header("X-Dev-User-Id", String.valueOf(teacherUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visitorName":"别名访客","phone":"13800002222","reason":"送文件","idNo":"110101199001011234","hostUserId":%d,"appointmentTime":"2026-12-03T09:00:00"}
                                """.formatted(adminId)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.purpose").value("送文件"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.idCard").value("110101199001011234"));
    }
}
