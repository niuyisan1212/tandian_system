package com.tandian.system.controller;

import com.tandian.system.entity.SystemConfig;
import com.tandian.system.service.SystemConfigService;
import com.tandian.system.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 系统配置Controller
 */
@Slf4j
@RestController
@RequestMapping("/system-config")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping
    public Result<List<SystemConfig>> list() {
        log.info("【接口调用】获取系统配置");
        return Result.success(systemConfigService.listAll());
    }

    /**
     * 批量更新配置
     */
    @PutMapping
    public Result<Void> batchUpdate(@RequestBody Map<String, String> configMap) {
        log.info("【接口调用】批量更新系统配置");
        try {
            systemConfigService.batchUpdate(configMap);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            return Result.error(e.getMessage());
        }
    }
}
