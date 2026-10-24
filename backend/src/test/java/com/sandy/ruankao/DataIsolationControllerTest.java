package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.sandy.ruankao.controller.KnowledgeChapterController;
import com.sandy.ruankao.controller.StudyPlanController;
import com.sandy.ruankao.controller.WrongQuestionController;
import com.sandy.ruankao.entity.WrongQuestion;
import com.sandy.ruankao.mapper.KnowledgeChapterMapper;
import com.sandy.ruankao.mapper.StudyPlanMapper;
import com.sandy.ruankao.mapper.WrongQuestionMapper;
import com.sandy.ruankao.service.AuthService;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 业务接口用户数据隔离测试。
 *
 * @author zoujunke
 */
class DataIsolationControllerTest {
  private final AuthService.UserSession session =
      new AuthService.UserSession(7L, "student", "学生", "USER", true, 0);

  @Test
  void shouldQueryPlansWithCurrentUserId() {
    StudyPlanMapper mapper = mock(StudyPlanMapper.class);
    when(mapper.findAll(7L)).thenReturn(Collections.emptyList());

    new StudyPlanController(mapper).list(session);

    verify(mapper).findAll(7L);
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void shouldRejectChapterOwnedByAnotherUser() {
    WrongQuestionMapper questionMapper = mock(WrongQuestionMapper.class);
    KnowledgeChapterMapper chapterMapper = mock(KnowledgeChapterMapper.class);
    when(chapterMapper.findById(99L, 7L)).thenReturn(null);

    WrongQuestionController controller = new WrongQuestionController(questionMapper, chapterMapper);
    WrongQuestion question = new WrongQuestion();

    assertThrows(IllegalArgumentException.class, () -> controller.create(session, question, 99L));
    verify(questionMapper, never()).insert(any());
  }

  @Test
  void shouldPageChaptersAndKeepOptionsScopedToCurrentUser() {
    KnowledgeChapterMapper mapper = mock(KnowledgeChapterMapper.class);
    when(mapper.findPage(7L, 100, 200L)).thenReturn(Collections.emptyList());
    when(mapper.count(7L)).thenReturn(14L);
    when(mapper.findAll(7L)).thenReturn(Collections.emptyList());
    KnowledgeChapterController controller = new KnowledgeChapterController(mapper);

    Map<String, Object> page = controller.list(session, 3, 500).getData();
    controller.options(session);

    assertEquals(14L, page.get("total"));
    assertEquals(3, page.get("page"));
    assertEquals(100, page.get("size"));
    verify(mapper).findPage(7L, 100, 200L);
    verify(mapper).findAll(7L);
  }
}
