package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * 项目管理计算工具接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/calculators")
public class CalculatorController {
    @PostMapping("/earned-value")
    public ApiResponse<Map<String, Object>> earnedValue(@RequestBody Map<String, Double> input) {
        double pv = value(input, "pv");
        double ev = value(input, "ev");
        double ac = value(input, "ac");
        double bac = value(input, "bac");

        // 挣值分析：CV/SV 判断成本和进度偏差，CPI/SPI 衡量执行效率。
        double cv = ev - ac;
        double sv = ev - pv;
        // 分母为 0 时返回 0，避免出现 Infinity 或 NaN 导致 JSON 序列化失败。
        double cpi = ac == 0 ? 0 : ev / ac;
        double spi = pv == 0 ? 0 : ev / pv;
        // 假设后续成本绩效保持不变，使用 BAC/CPI 估算完工成本。
        double eac = cpi == 0 ? 0 : bac / cpi;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("cv", round(cv));
        result.put("sv", round(sv));
        result.put("cpi", round(cpi));
        result.put("spi", round(spi));
        result.put("eac", round(eac));
        result.put("vac", round(bac - eac));
        result.put("costStatus", cv >= 0 ? "成本节约" : "成本超支");
        result.put("scheduleStatus", sv >= 0 ? "进度提前" : "进度落后");
        return ApiResponse.ok(result);
    }

    private double value(Map<String, Double> input, String key) {
        Double value = input.get(key);
        return value == null ? 0 : value;
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
