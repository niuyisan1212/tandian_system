package com.tandian.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tandian.system.dto.ShoppingListDTO;
import com.tandian.system.entity.RoutePlan;
import com.tandian.system.service.ShoppingListService;
import com.tandian.system.vo.Result;
import com.tandian.system.vo.ShoppingListVO;


import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 探店清单Controller
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/lists")

public class ShoppingListController {

    @Resource
    private ShoppingListService shoppingListService;

    @PostMapping

    public Result<ShoppingListVO> create(@Validated @RequestBody ShoppingListDTO dto) {
        log.info("【接口调用】创建清单：{}", dto.getName());
        try {
            ShoppingListVO vo = shoppingListService.createList(dto);
            return Result.success("创建成功", vo);
        } catch (Exception e) {
            log.error("创建清单失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")

    public Result<ShoppingListVO> update(@PathVariable Long id, @Validated @RequestBody ShoppingListDTO dto) {
        log.info("【接口调用】更新清单：{}", id);
        try {
            ShoppingListVO vo = shoppingListService.updateList(id, dto);
            return Result.success("更新成功", vo);
        } catch (Exception e) {
            log.error("更新清单失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")

    public Result<Void> delete(@PathVariable Long id) {
        log.info("【接口调用】删除清单：{}", id);
        try {
            shoppingListService.deleteList(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除清单失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")

    public Result<ShoppingListVO> getById(@PathVariable Long id) {
        log.info("【接口调用】获取清单详情：{}", id);
        ShoppingListVO vo = shoppingListService.getListById(id);
        return vo != null ? Result.success(vo) : Result.error("清单不存在");
    }

    @GetMapping

    public Result<Page<ShoppingListVO>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        log.info("【接口调用】分页查询清单列表");
        Page<ShoppingListVO> page = shoppingListService.getListPage(pageNum, pageSize, status);
        return Result.success(page);
    }

    @PutMapping("/{id}/complete")

    public Result<Void> markAsCompleted(@PathVariable Long id) {
        log.info("【接口调用】标记清单为已完成：{}", id);
        try {
            shoppingListService.markAsCompleted(id);
            return Result.success("标记成功", null);
        } catch (Exception e) {
            log.error("标记失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")

    public Result<Void> markAsCancelled(@PathVariable Long id) {
        log.info("【接口调用】标记清单为已取消：{}", id);
        try {
            shoppingListService.markAsCancelled(id);
            return Result.success("标记成功", null);
        } catch (Exception e) {
            log.error("标记失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/replan")

    public Result<List<RoutePlan>> replanRoute(@PathVariable Long id) {
        log.info("【接口调用】重新规划路线：{}", id);
        try {
            List<RoutePlan> plans = shoppingListService.replanRoute(id);
            return Result.success("规划成功", plans);
        } catch (Exception e) {
            log.error("规划失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/select-plan")

    public Result<Void> selectPlan(@PathVariable Long id, @RequestParam String planType) {
        log.info("【接口调用】选择路线方案：{}, {}", id, planType);
        try {
            shoppingListService.selectPlan(id, planType);
            return Result.success("选择成功", null);
        } catch (Exception e) {
            log.error("选择失败", e);
            return Result.error(e.getMessage());
        }
    }
}
