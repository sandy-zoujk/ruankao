package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供界面和申报材料共同使用的软件身份信息。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/software")
public class SoftwareInfoController {
  private final String name;
  private final String version;
  private final String purpose;
  private final String developer;
  private final String copyright;

  public SoftwareInfoController(
      @Value("${app.software.name}") String name,
      @Value("${app.software.version}") String version,
      @Value("${app.software.purpose}") String purpose,
      @Value("${app.software.developer}") String developer,
      @Value("${app.software.copyright}") String copyright) {
    this.name = name;
    this.version = version;
    this.purpose = purpose;
    this.developer = developer;
    this.copyright = copyright;
  }

  @GetMapping("/info")
  public ApiResponse<Map<String, String>> info() {
    Map<String, String> result = new LinkedHashMap<String, String>();
    result.put("name", name);
    result.put("version", version);
    result.put("fullName", name + " " + version);
    result.put("purpose", purpose);
    result.put("developer", developer);
    result.put("copyright", copyright);
    return ApiResponse.ok(result);
  }
}
