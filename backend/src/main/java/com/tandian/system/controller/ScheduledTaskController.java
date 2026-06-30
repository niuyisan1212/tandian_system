package com.tandian.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tandian.system.entity.ScheduledTask;
import com.tandian.system.service.ScheduledTaskService;
import com.tandian.system.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 定时任务Controller
 */
@Slf4j
@RestController
@RequestMapping("/scheduled-tasks")
public class ScheduledTaskController {

    @Resource
    private ScheduledTaskService scheduledTaskService;

    /**
     * 获取所有定时任务列表
     */
    @GetMapping
    public Result<List<ScheduledTask>> list() {
        log.info("【接口调用】获取定时任务列表");
        List<ScheduledTask> tasks = scheduledTaskService.list(new LambdaQueryWrapper<ScheduledTask>()
                .orderByAsc(ScheduledTask::getId));
        return Result.success(tasks);
    }

    /**
     * 获取单个定时任务详情
     */
    @GetMapping("/{id}")
    public Result<ScheduledTask> getById(@PathVariable Long id) {
        log.info("【接口调用】获取定时任务详情：{}", id);
        ScheduledTask task = scheduledTaskService.getById(id);
        return task != null ? Result.success(task) : Result.error("任务不存在");
    }

    /**
     * 更新定时任务配置
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScheduledTask task) {
        log.info("【接口调用】更新定时任务：{}", id);
        try {
            task.setId(id);

            // 如果通知开启，校验邮箱
            if (task.getNotifyEnabled() != null && task.getNotifyEnabled() == 1) {
                if (task.getEmails() == null || task.getEmails().trim().isEmpty()) {
                    return Result.error("开启通知时必须填写至少一个邮箱地址");
                }
            }

            scheduledTaskService.updateAndReschedule(task);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            log.error("更新定时任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用定时任务
     */
    @PutMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        log.info("【接口调用】切换定时任务状态：{}", id);
        try {
            ScheduledTask task = scheduledTaskService.getById(id);
            if (task == null) {
                return Result.error("任务不存在");
            }

            task.setEnabled(task.getEnabled() == 1 ? 0 : 1);
            scheduledTaskService.updateAndReschedule(task);

            return Result.success(task.getEnabled() == 1 ? "已启用" : "已禁用", null);
        } catch (Exception e) {
            log.error("切换定时任务状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 手动触发定时任务
     */
    @PostMapping("/{id}/trigger")
    public Result<Void> trigger(@PathVariable Long id) {
        log.info("【接口调用】手动触发定时任务：{}", id);
        try {
            scheduledTaskService.manualTrigger(id);
            return Result.success("触发成功", null);
        } catch (Exception e) {
            log.error("手动触发定时任务失败", e);
            return Result.error(e.getMessage());
        }
    }
}
