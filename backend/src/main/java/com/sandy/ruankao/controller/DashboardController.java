package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.entity.KnowledgeChapter;
import com.sandy.ruankao.entity.StudyPlan;
import com.sandy.ruankao.mapper.KnowledgeChapterMapper;
import com.sandy.ruankao.mapper.StudyPlanMapper;
import com.sandy.ruankao.mapper.WrongQuestionMapper;
import com.sandy.ruankao.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

/**
 * 学习概览与统计数据接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final StudyPlanMapper planMapper;
    private final KnowledgeChapterMapper chapterMapper;
    private final WrongQuestionMapper wrongMapper;

    public DashboardController(StudyPlanMapper planMapper,
                               KnowledgeChapterMapper chapterMapper,
                               WrongQuestionMapper wrongMapper) {
        this.planMapper = planMapper;
        this.chapterMapper = chapterMapper;
        this.wrongMapper = wrongMapper;
    }

    @GetMapping public ApiResponse<Map<String, Object>> dashboard(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session) {
        Long userId = session.getUserId();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        long totalPlans = planMapper.count(userId);
        long completedPlans = planMapper.countByCompleted(userId, true);

        // 没有学习计划时完成率按 0 处理，避免除零错误。
        result.put("totalPlans", totalPlans);
        result.put("completedPlans", completedPlans);
        result.put("completionRate", totalPlans == 0 ? 0 : Math.round(completedPlans * 100.0 / totalPlans));
        result.put("wrongQuestions", wrongMapper.count(userId));
        result.put("unmasteredQuestions", wrongMapper.countByMastered(userId, false));

        // 查询从今天起七天内的计划，供首页近期任务区域展示。
        List<StudyPlan> weekPlans = planMapper.findBetween(
                userId, LocalDate.now(), LocalDate.now().plusDays(7));
        result.put("weekPlans", weekPlans);

        // 只组装图表需要的字段，避免把无关章节数据传输给前端。
        List<Map<String, Object>> mastery = new ArrayList<Map<String, Object>>();
        for (KnowledgeChapter chapter : chapterMapper.findAll(userId)) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", chapter.getName());
            item.put("mastery", chapter.getMastery());
            item.put("priority", chapter.getPriority());
            mastery.add(item);
        }
        result.put("mastery", mastery);
        return ApiResponse.ok(result);
    }
}
