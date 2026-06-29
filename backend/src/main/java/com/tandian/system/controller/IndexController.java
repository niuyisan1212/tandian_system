package com.tandian.system.controller;

import com.tandian.system.service.ShopService;
import com.tandian.system.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页Controller
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping
public class IndexController {

    @Resource
    private ShopService shopService;

    /**
     * 系统首页
     */
    @GetMapping
    public Result<Map<String, Object>> index() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "探店路线规划系统");
        data.put("version", "1.0.0");
        data.put("description", "高效规划探店行程");
        return Result.success(data);
    }

    /**
     * 统计数据
     */
    @GetMapping("/statistics/overview")
    public Result<Map<String, Object>> getStatistics() {
        log.info("【接口调用】获取统计数据");
        
        Map<String, Object> data = new HashMap<>();
        
        // 统计店铺数据
        long totalShops = shopService.count();
        long validShops = shopService.getValidShops().size();
        long visitedShops = totalShops - validShops;
        
        data.put("totalShops", totalShops);
        data.put("validShops", validShops);
        data.put("visitedShops", visitedShops);
        data.put("completionRate", totalShops > 0 ? (visitedShops * 100 / totalShops) : 0);
        data.put("totalAvailableCount", shopService.getTotalAvailableCount());
        
        return Result.success(data);
    }
}
