package com.sandy.ruankao;

import com.sandy.ruankao.entity.KnowledgeChapter;
import com.sandy.ruankao.entity.StudyPlan;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.entity.WrongQuestion;
import com.sandy.ruankao.mapper.KnowledgeChapterMapper;
import com.sandy.ruankao.mapper.StudyPlanMapper;
import com.sandy.ruankao.mapper.WrongQuestionMapper;
import com.sandy.ruankao.service.AuthService;
import com.sandy.ruankao.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户权限、JWT 失效和业务数据隔离集成测试。
 *
 * @author zoujunke
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserPermissionIntegrationTest {
    @Autowired private AuthService authService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private StudyPlanMapper planMapper;
    @Autowired private KnowledgeChapterMapper chapterMapper;
    @Autowired private WrongQuestionMapper wrongQuestionMapper;

    @Test
    void shouldEnforcePermissionsAndUserDataIsolation() {
        SysUser admin = authService.register("admin", "Admin123!", "管理员");
        SysUser userA = authService.register("integration_a", "User123!", "用户A");
        SysUser userB = authService.register("integration_b", "User123!", "用户B");
        assertEquals(SysUser.ROLE_ADMIN, admin.getRole());
        assertEquals(14, chapterMapper.count(userA.getId()));
        assertEquals(14, chapterMapper.count(userB.getId()));

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userA.getId());
        plan.setTitle("A的学习计划");
        plan.setCategory("项目管理");
        plan.setPlanDate(LocalDate.now());
        planMapper.insert(plan);
        assertEquals(1, planMapper.count(userA.getId()));
        assertEquals(0, planMapper.count(userB.getId()));
        assertNull(planMapper.findById(plan.getId(), userB.getId()));

        KnowledgeChapter userBChapter = chapterMapper.findAll(userB.getId()).get(0);
        WrongQuestion question = new WrongQuestion();
        question.setUserId(userB.getId());
        question.setTitle("用户B错题");
        question.setContent("测试内容");
        question.setErrorReason("知识点不熟");
        question.setChapter(userBChapter);
        wrongQuestionMapper.insert(question);
        assertNotNull(wrongQuestionMapper.findById(question.getId(), userB.getId()));
        assertNull(wrongQuestionMapper.findById(question.getId(), userA.getId()));

        Map<String, Object> login = authService.login("integration_a", "User123!");
        String oldToken = (String) login.get("token");
        assertNotNull(authService.authenticateToken(oldToken));

        userAdminService.updateStatus(admin.getId(), userA.getId(), false);
        assertNull(authService.authenticateToken(oldToken));
        userAdminService.updateStatus(admin.getId(), userA.getId(), true);

        String passwordToken = (String) authService.login("integration_a", "User123!").get("token");
        authService.changePassword(userA.getId(), "User123!", "User456!");
        assertNull(authService.authenticateToken(passwordToken));
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("integration_a", "User123!"));
        assertNotNull(authService.login("integration_a", "User456!").get("token"));
    }
}
