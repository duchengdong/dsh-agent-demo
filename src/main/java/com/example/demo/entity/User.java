package com.example.demo.entity;

import java.util.Objects;

/**
 * 用户实体类。
 *
 * <p>对应演示项目中的用户数据模型，包含主键、登录名、显示名和启用状态。
 * 采用 JavaBean 约定（无参构造 + getter/setter），便于序列化和映射框架使用。</p>
 */
public class User {

    /** 主键。 */
    private Long id;

    /** 登录名，唯一标识一个用户。 */
    private String username;

    /** 展示名称。 */
    private String displayName;

    /** 是否启用。 */
    private boolean enabled;

    /** 供框架反射使用的无参构造方法。 */
    public User() {
    }

    /**
     * 创建完整用户对象。
     *
     * @param id          主键
     * @param username    登录名
     * @param displayName 展示名称
     * @param enabled     是否启用
     */
    public User(Long id, String username, String displayName, boolean enabled) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 实体按主键判断相等：同一主键的两条记录视为同一实体。
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User that = (User) other;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", displayName='" + displayName + '\''
                + ", enabled=" + enabled
                + '}';
    }
}
