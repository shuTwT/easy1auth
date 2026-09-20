package com.easy1auth.system.service;

import com.easy1auth.system.constant.SystemConfigKey;
import com.easy1auth.system.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 系统级配置服务。
 *
 * <p>为全局键值配置提供统一的读取、写入和基础类型转换入口。租户级配置不应写入
 * 此处，应继续由各租户领域自己的实体保存。</p>
 */
@Service
public class SystemConfigService {
    /** 配置键最大长度，与数据库约束一致。 */
    private static final int MAX_KEY_LENGTH = 150;
    /** 配置项说明最大长度，与数据库约束一致。 */
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    /** 系统配置仓储。 */
    private final SystemConfigRepository repository;

    SystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    /** 读取字符串配置。 */
    @Transactional(readOnly = true)
    public Optional<String> get(String key) {
        return repository.findValue(validateKey(key));
    }

    /** 读取字符串配置，不存在时返回默认值。 */
    @Transactional(readOnly = true)
    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /** 读取布尔配置，不存在时返回默认值。 */
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return booleanValue(key, value.get());
    }

    /** 新增或更新字符串配置。 */
    @Transactional
    public void set(String key, String value, String description) {
        String validatedKey = validateKey(key);
        if (value == null) {
            throw new IllegalArgumentException("System config value must not be null");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("System config description is too long");
        }
        repository.put(validatedKey, value, description);
    }

    /** 新增或更新布尔配置。 */
    @Transactional
    public void setBoolean(String key, boolean value, String description) {
        set(key, Boolean.toString(value), description);
    }

    /** 删除配置项。 */
    @Transactional
    public boolean remove(String key) {
        return repository.remove(validateKey(key)) > 0;
    }

    /** 判断系统初始化是否已完成；配置缺失时按未初始化处理。 */
    @Transactional(readOnly = true)
    public boolean isInitialized() {
        return getBoolean(SystemConfigKey.SYSTEM_INITIALIZED, false);
    }

    /** 加锁读取系统初始化状态，供一次性初始化事务串行化并发请求。 */
    @Transactional
    public boolean lockAndIsInitialized() {
        String value = repository.lockValue(SystemConfigKey.SYSTEM_INITIALIZED)
                .orElseThrow(() -> new IllegalStateException("System initialization config is missing"));
        return booleanValue(SystemConfigKey.SYSTEM_INITIALIZED, value);
    }

    /** 标记系统初始化已完成。 */
    @Transactional
    public void markInitialized() {
        setBoolean(SystemConfigKey.SYSTEM_INITIALIZED, true, "系统初始化是否已完成");
    }

    /** 校验并返回配置键。 */
    private static String validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("System config key must contain 1-150 characters");
        }
        return key;
    }

    /** 将配置值转换为布尔值，非法持久化值直接快速失败。 */
    private static boolean booleanValue(String key, String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException("System config '" + key + "' is not a boolean");
    }
}
