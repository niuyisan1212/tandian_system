package com.tandian.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tandian.system.entity.SystemConfig;
import com.tandian.system.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 */
@Slf4j
@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    /**
     * 获取所有配置（脱敏密码）
     */
    public List<SystemConfig> listAll() {
        List<SystemConfig> configs = this.list(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getId));
        // 脱敏：密码显示为掩码
        configs.forEach(c -> {
            if ("smtp_password".equals(c.getConfigKey()) && c.getConfigValue() != null && !c.getConfigValue().isEmpty()) {
                c.setConfigValue("******");
            }
        });
        return configs;
    }

    /**
     * 根据key获取配置值
     */
    public String getValue(String key) {
        SystemConfig config = this.getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 根据key获取配置值，带默认值
     */
    public String getValue(String key, String defaultValue) {
        String value = getValue(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 批量更新配置
     */
    public void batchUpdate(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 密码掩码不更新
            if ("smtp_password".equals(key) && "******".equals(value)) {
                continue;
            }

            SystemConfig config = this.getOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigKey, key));
            if (config != null) {
                config.setConfigValue(value);
                this.updateById(config);
            }
        }
        log.info("【系统配置】批量更新完成，共更新 {} 项", configMap.size());
    }

    /**
     * 获取邮件配置Map（不脱敏）
     */
    public Map<String, String> getMailConfig() {
        List<SystemConfig> configs = this.list(new LambdaQueryWrapper<SystemConfig>()
                .likeRight(SystemConfig::getConfigKey, "smtp_"));
        return configs.stream().collect(Collectors.toMap(
                SystemConfig::getConfigKey,
                c -> c.getConfigValue() != null ? c.getConfigValue() : ""
        ));
    }
}
