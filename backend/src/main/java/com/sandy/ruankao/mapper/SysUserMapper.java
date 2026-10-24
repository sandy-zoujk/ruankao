package com.sandy.ruankao.mapper;

import com.sandy.ruankao.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户数据访问接口，SQL 定义在对应的 MyBatis XML 文件中。
 *
 * @author zoujunke
 */
@Mapper
public interface SysUserMapper {
    SysUser findByUsername(@Param("username") String username);

    SysUser findById(@Param("id") Long id);

    List<SysUser> findAll(@Param("keyword") String keyword, @Param("enabled") Boolean enabled);

    int insert(SysUser user);

    int updateStatus(@Param("id") Long id, @Param("enabled") boolean enabled);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int assignConfiguredAdmin(@Param("adminUsername") String adminUsername);
}
