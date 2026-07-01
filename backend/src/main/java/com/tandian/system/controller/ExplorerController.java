package com.tandian.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tandian.system.entity.Explorer;
import com.tandian.system.service.ExplorerService;
import com.tandian.system.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 探店员Controller
 */
@Slf4j
@RestController
@RequestMapping("/explorers")
public class ExplorerController {

    @Resource
    private ExplorerService explorerService;

    /** 获取所有探店员 */
    @GetMapping
    public Result<List<Explorer>> list(@RequestParam(required = false) String keyword) {
        log.info("【接口调用】获取探店员列表");
        LambdaQueryWrapper<Explorer> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Explorer::getName, keyword)
                    .or().like(Explorer::getPhone, keyword));
        }
        wrapper.orderByDesc(Explorer::getCreatedAt);
        return Result.success(explorerService.list(wrapper));
    }

    /** 获取单个探店员 */
    @GetMapping("/{id}")
    public Result<Explorer> getById(@PathVariable Long id) {
        Explorer explorer = explorerService.getById(id);
        return explorer != null ? Result.success(explorer) : Result.error("探店员不存在");
    }

    /** 创建探店员 */
    @PostMapping
    public Result<Explorer> create(@RequestBody Explorer explorer) {
        log.info("【接口调用】创建探店员：{}", explorer.getName());
        try {
            return Result.success("创建成功", explorerService.createExplorer(explorer));
        } catch (Exception e) {
            log.error("创建探店员失败", e);
            return Result.error(e.getMessage());
        }
    }

    /** 更新探店员 */
    @PutMapping("/{id}")
    public Result<Explorer> update(@PathVariable Long id, @RequestBody Explorer explorer) {
        log.info("【接口调用】更新探店员：{}", id);
        try {
            return Result.success("更新成功", explorerService.updateExplorer(id, explorer));
        } catch (Exception e) {
            log.error("更新探店员失败", e);
            return Result.error(e.getMessage());
        }
    }

    /** 删除探店员 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("【接口调用】删除探店员：{}", id);
        try {
            explorerService.deleteExplorer(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除探店员失败", e);
            return Result.error(e.getMessage());
        }
    }
}
