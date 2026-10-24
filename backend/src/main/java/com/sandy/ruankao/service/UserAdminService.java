package com.sandy.ruankao.service;

import com.sandy.ruankao.dto.UserSummaryResponse;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员用户查询与账号状态管理服务。
 *
 * @author zoujunke
 */
@Service
public class UserAdminService {
    private final SysUserMapper userMapper;

    public UserAdminService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<UserSummaryResponse> list(String keyword, Boolean enabled) {
        String normalized = keyword == null ? null : keyword.trim();
        List<UserSummaryResponse> result = new ArrayList<UserSummaryResponse>();
        for (SysUser user : userMapper.findAll(normalized, enabled)) {
            result.add(new UserSummaryResponse(user));
        }
        return result;
    }

    public UserSummaryResponse updateStatus(Long operatorId, Long targetId, boolean enabled) {
        SysUser target = userMapper.findById(targetId);
        if (target == null) throw new IllegalArgumentException("用户不存在");
        if (operatorId.equals(targetId) && !enabled) {
            throw new IllegalArgumentException("不能停用当前管理员账号");
        }
        userMapper.updateStatus(targetId, enabled);
        return new UserSummaryResponse(userMapper.findById(targetId));
    }
}
