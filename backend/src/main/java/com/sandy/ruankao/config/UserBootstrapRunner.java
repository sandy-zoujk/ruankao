package com.sandy.ruankao.config;

import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import com.sandy.ruankao.service.UserDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 确保配置账号拥有管理员角色，并初始化其历史学习数据。
 *
 * @author zoujunke
 */
@Component
@Order(1)
public class UserBootstrapRunner implements ApplicationRunner {
  private final SysUserMapper userMapper;
  private final UserDataService userDataService;
  private final String adminUsername;

  public UserBootstrapRunner(
      SysUserMapper userMapper,
      UserDataService userDataService,
      @Value("${app.admin.username:admin}") String adminUsername) {
    this.userMapper = userMapper;
    this.userDataService = userDataService;
    this.adminUsername = adminUsername.trim();
  }

  @Override
  public void run(ApplicationArguments args) {
    // 仅提升配置账号，不覆盖其他由管理员授权的管理员角色。
    userMapper.assignConfiguredAdmin(adminUsername);
    SysUser admin = userMapper.findByUsername(adminUsername);
    if (admin != null) userDataService.provision(admin.getId(), true);
  }
}
