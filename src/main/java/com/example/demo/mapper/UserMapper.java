package com.example.demo.mapper;

import com.example.demo.entity.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户数据访问（Mapper）层的内存实现。
 *
 * <p>演示项目没有引入数据库，这里用 {@link LinkedHashMap} 模拟一张用户表，
 * 对外提供与常见持久化框架 Mapper 一致的增删改查操作。主键由本类负责生成：
 * 插入未设置主键的实体时分配自增主键，插入显式主键时保证主键唯一。</p>
 *
 * <p>所有公开方法均在同一把锁下执行，因此本类可安全地被多个线程共享。</p>
 */
public class UserMapper {

    /** 模拟的用户表，按插入顺序保存，主键唯一。 */
    private final Map<Long, User> rows = new LinkedHashMap<>();

    /** 下一个可用的自增主键。 */
    private long nextId = 1L;

    /**
     * 插入一条用户记录。
     *
     * <p>若 {@code user} 未设置主键，则自动分配自增主键并回写到实体；
     * 若已设置主键，则校验该主键尚未存在。无论哪种情况都返回被保存的实体本身。</p>
     *
     * @param user 待插入的用户，不能为 {@code null}
     * @return 已保存的用户实体
     * @throws NullPointerException      {@code user} 为 {@code null}
     * @throws IllegalArgumentException  显式主键已存在
     */
    public synchronized User insert(User user) {
        Objects.requireNonNull(user, "user");

        Long id = user.getId();
        if (id == null) {
            id = nextId++;
            user.setId(id);
        } else {
            if (rows.containsKey(id)) {
                throw new IllegalArgumentException("duplicate user id: " + id);
            }
            nextId = Math.max(nextId, id + 1);
        }

        rows.put(id, user);
        return user;
    }

    /**
     * 按主键查询用户。
     *
     * @param id 主键
     * @return 命中的用户；不存在时返回空 {@link Optional}
     */
    public synchronized Optional<User> findById(long id) {
        return Optional.ofNullable(rows.get(id));
    }

    /**
     * 查询所有用户，按插入顺序返回。
     *
     * @return 用户列表的防御性副本，修改该列表不影响 Mapper 内部数据
     */
    public synchronized List<User> findAll() {
        return new ArrayList<>(rows.values());
    }

    /**
     * 按主键更新用户记录。
     *
     * @param user 待更新的用户，主键不能为 {@code null} 且必须已存在
     * @return 更新成功返回 {@code true}；主键为空或不存在返回 {@code false}
     * @throws NullPointerException {@code user} 为 {@code null}
     */
    public synchronized boolean update(User user) {
        Objects.requireNonNull(user, "user");

        Long id = user.getId();
        if (id == null || !rows.containsKey(id)) {
            return false;
        }

        rows.put(id, user);
        return true;
    }

    /**
     * 按主键删除用户。
     *
     * @param id 主键
     * @return 删除成功返回 {@code true}；主键不存在返回 {@code false}
     */
    public synchronized boolean deleteById(long id) {
        return rows.remove(id) != null;
    }

    /**
     * 返回当前用户记录数。
     *
     * @return 记录数
     */
    public synchronized int count() {
        return rows.size();
    }
}
