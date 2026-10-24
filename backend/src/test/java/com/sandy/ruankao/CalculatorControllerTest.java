package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.*;

import com.sandy.ruankao.controller.CalculatorController;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 挣值分析计算接口单元测试。
 *
 * @author zoujunke
 */
class CalculatorControllerTest {
  @Test
  void shouldCalculateEarnedValueIndicators() {
    CalculatorController controller = new CalculatorController();
    Map<String, Double> input = new HashMap<String, Double>();
    input.put("pv", 100.0);
    input.put("ev", 80.0);
    input.put("ac", 90.0);
    input.put("bac", 200.0);
    // 验证成本超支且进度落后场景下的偏差和状态判断。
    Map<String, Object> result = controller.earnedValue(input).getData();
    assertEquals(-10.0, result.get("cv"));
    assertEquals(-20.0, result.get("sv"));
    assertEquals("成本超支", result.get("costStatus"));
    assertEquals("进度落后", result.get("scheduleStatus"));
  }
}
