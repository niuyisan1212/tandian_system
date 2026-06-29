package com.tandian.system.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tandian.system.dto.ShopDTO;
import com.tandian.system.entity.Shop;
import com.tandian.system.service.AmapService;
import com.tandian.system.service.ShopService;
import com.tandian.system.vo.Result;
import com.tandian.system.vo.ShopVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 店铺Controller
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/shops")
public class ShopController {

    @Resource
    private ShopService shopService;
    
    @Resource
    private AmapService amapService;

    /**
     * 根据店铺名称搜索店铺信息（地址、电话、营业时间等）
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> searchShopInfo(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "四川省") String city) {
        log.info("【接口调用】搜索店铺信息：{}", name);
        try {
            Map<String, Object> result = amapService.searchPOI(name, city);
            if (result != null) {
                return Result.success(result);
            }
            return Result.error("未找到该店铺信息");
        } catch (Exception e) {
            log.error("搜索店铺信息失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<ShopVO> create(@Validated @RequestBody ShopDTO dto) {
        log.info("【接口调用】创建店铺：{}", dto.getName());
        try {
            ShopVO vo = shopService.createShop(dto);
            return Result.success("创建成功", vo);
        } catch (Exception e) {
            log.error("创建店铺失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")

    public Result<ShopVO> update(@PathVariable Long id, @Validated @RequestBody ShopDTO dto) {
        log.info("【接口调用】更新店铺：{}", id);
        try {
            ShopVO vo = shopService.updateShop(id, dto);
            return Result.success("更新成功", vo);
        } catch (Exception e) {
            log.error("更新店铺失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")

    public Result<Void> delete(@PathVariable Long id) {
        log.info("【接口调用】删除店铺：{}", id);
        try {
            shopService.deleteShop(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除店铺失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")

    public Result<ShopVO> getById(@PathVariable Long id) {
        log.info("【接口调用】获取店铺详情：{}", id);
        ShopVO vo = shopService.getShopById(id);
        return vo != null ? Result.success(vo) : Result.error("店铺不存在");
    }

    @GetMapping

    public Result<Page<ShopVO>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer visitStatus,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isValid,
            @RequestParam(required = false) String expireTimeStart,
            @RequestParam(required = false) String expireTimeEnd,
            @RequestParam(required = false) Integer availableCount) {
        log.info("【接口调用】分页查询店铺列表，isValid：{}，过期时间范围：{} ~ {}，可用人数：{}", isValid, expireTimeStart, expireTimeEnd, availableCount);
        Page<ShopVO> page = shopService.getShopPage(pageNum, pageSize, visitStatus, category, keyword, isValid, expireTimeStart, expireTimeEnd, availableCount);
        return Result.success(page);
    }

    @GetMapping("/valid")

    public Result<List<ShopVO>> getValidShops() {
        log.info("【接口调用】获取所有有效待探店店铺");
        List<ShopVO> list = shopService.getValidShops();
        return Result.success(list);
    }

    @GetMapping("/available-count-options")

    public Result<List<Integer>> getAvailableCountOptions() {
        log.info("【接口调用】获取可用人数选项");
        List<Integer> options = shopService.getAvailableCountOptions();
        return Result.success(options);
    }

    @PutMapping("/{id}/visit")

    public Result<Void> markAsVisited(@PathVariable Long id) {
        log.info("【接口调用】标记店铺为已探店：{}", id);
        try {
            shopService.markAsVisited(id);
            return Result.success("标记成功", null);
        } catch (Exception e) {
            log.error("标记失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出店铺列表到Excel
     */
    @GetMapping("/export")
    public void exportShops(
            @RequestParam(required = false) Integer visitStatus,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isValid,
            @RequestParam(required = false) String expireTimeStart,
            @RequestParam(required = false) String expireTimeEnd,
            @RequestParam(required = false) Integer availableCount,
            HttpServletResponse response) {
        log.info("【接口调用】导出店铺列表");
        try {
            // 获取所有店铺（不分页）
            Page<ShopVO> page = shopService.getShopPage(1, 10000, visitStatus, category, keyword, isValid, expireTimeStart, expireTimeEnd, availableCount);
            List<ShopVO> shops = page.getRecords();

            // 创建Excel写入器
            ExcelWriter writer = ExcelUtil.getWriter(true);
            
            // 设置表头别名
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("name", "店铺名称");
            writer.addHeaderAlias("categoryText", "类别");
            writer.addHeaderAlias("address", "店铺地址");
            writer.addHeaderAlias("longitude", "经度");
            writer.addHeaderAlias("latitude", "纬度");
            writer.addHeaderAlias("phone", "联系电话");
            writer.addHeaderAlias("businessHours", "营业时间");
            writer.addHeaderAlias("visitStatusText", "探店状态");
            writer.addHeaderAlias("isValidText", "有效性");
            writer.addHeaderAlias("remark", "备注");
            writer.addHeaderAlias("availableCount", "可用人人数");
            writer.addHeaderAlias("expireTime", "过期时间");
            writer.addHeaderAlias("createTime", "创建时间");

            // 写入数据
            writer.write(shops, true);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("店铺列表_" + DateUtil.today(), "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            // 输出到响应流
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            out.close();

            log.info("【导出完成】共导出 {} 条数据", shops.size());
        } catch (Exception e) {
            log.error("导出店铺失败", e);
        }
    }

    /**
     * 导入店铺列表
     * 支持根据店铺名称自动查询地址和坐标（使用高德地图POI搜索）
     */
    @PostMapping("/import")
    public Result<String> importShops(@RequestParam("file") MultipartFile file) {
        log.info("【接口调用】导入店铺列表");
        try {
            // 读取Excel文件
            InputStream inputStream = file.getInputStream();
            ExcelReader reader = ExcelUtil.getReader(inputStream);

            // 读取数据（跳过表头）
            List<List<Object>> rows = reader.read();
            reader.close();

            if (rows.size() <= 1) {
                return Result.error("文件为空或只有表头");
            }

            // 解析数据并创建店铺
            int successCount = 0;
            int failCount = 0;
            int autoSearchCount = 0; // 自动查询地址的数量
            List<String> errors = new ArrayList<>();

            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    if (row.isEmpty() || (row.get(0) == null && row.get(1) == null)) {
                        continue;
                    }

                    ShopDTO dto = new ShopDTO();
                    String shopName = row.size() > 0 && row.get(0) != null ? row.get(0).toString().trim() : null;
                    String shopCategory = row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "美食";
                    String shopAddress = row.size() > 2 && row.get(2) != null ? row.get(2).toString().trim() : null;
                    
                    dto.setName(shopName);
                    dto.setCategory(shopCategory);

                    // 校验必填字段：店铺名称
                    if (shopName == null || shopName.isEmpty()) {
                        errors.add("第" + (i + 1) + "行：店铺名称不能为空");
                        failCount++;
                        continue;
                    }

                    // 处理地址和坐标
                    BigDecimal longitude = null;
                    BigDecimal latitude = null;
                    
                    // 解析Excel中的经纬度
                    if (row.size() > 3 && row.get(3) != null) {
                        try {
                            longitude = new BigDecimal(row.get(3).toString().trim());
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                    if (row.size() > 4 && row.get(4) != null) {
                        try {
                            latitude = new BigDecimal(row.get(4).toString().trim());
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                    
                    // 如果地址为空或坐标为空，尝试通过店铺名称自动搜索
                    boolean needAutoSearch = (shopAddress == null || shopAddress.isEmpty()) || 
                                             (longitude == null || latitude == null);
                    String phone = "";
                    String business_hour = "";
                    String remark = "";
                    if (needAutoSearch) {
                        log.info("【导入】第{}行：尝试根据店铺名称自动查询地址：{}", i + 1, shopName);
                        
                        // 使用高德地图POI搜索
                        Map<String, Object> poiResult = amapService.searchPOI(shopName, "四川省");
                        
                        if (poiResult != null) {
                            phone = poiResult.get("phone") == null ? "" : poiResult.get("phone").toString();
                            business_hour = poiResult.get("business_hour") == null ? "" : poiResult.get("business_hour").toString();
                            remark = poiResult.get("remark") == null ? "" : poiResult.get("remark").toString();
                            // 如果地址为空，使用搜索到的地址
                            if (shopAddress == null || shopAddress.isEmpty()) {
                                shopAddress = (String) poiResult.get("address");
                                log.info("【导入】自动获取地址成功：{} -> {}", shopName, shopAddress);
                            }
                            
                            // 如果坐标为空，使用搜索到的坐标
                            if (longitude == null && poiResult.get("longitude") != null) {
                                longitude = new BigDecimal(poiResult.get("longitude").toString());
                            }
                            if (latitude == null && poiResult.get("latitude") != null) {
                                latitude = new BigDecimal(poiResult.get("latitude").toString());
                            }
                            
                            autoSearchCount++;
                        } else {
                            log.warn("【导入】第{}行：未找到店铺地址信息：{}", i + 1, shopName);
                        }
                    }
                    
                    // 如果地址仍然为空，设置默认提示
                    if (shopAddress == null || shopAddress.isEmpty()) {
                        shopAddress = "地址待完善";
                    }
                    
                    // 如果坐标仍然为空，设置默认坐标
                    if (longitude == null) {
                        longitude = new BigDecimal("121.4737");
                    }
                    if (latitude == null) {
                        latitude = new BigDecimal("31.2304");
                    }
                    
                    dto.setAddress(shopAddress);
                    dto.setLongitude(longitude);
                    dto.setLatitude(latitude);
                    dto.setPhone(row.size() > 5 && row.get(5) != null ? row.get(5).toString().trim() : null);
                    dto.setBusinessHours(row.size() > 6 && row.get(6) != null ? row.get(6).toString().trim() : null);
                    
                    // 解析过期时间（格式：yyyy-MM-dd）
                    if (row.size() > 7 && row.get(7) != null) {
                        try {
                            String expireTimeStr = row.get(7).toString().trim();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime localDateTime = LocalDateTime.parse(expireTimeStr, formatter);
                            LocalDate date = localDateTime.toLocalDate();
                            dto.setExpireTime(date);
                        } catch (Exception e) {
                            // 尝试纯日期格式
                            try {
                                dto.setExpireTime(LocalDate.parse(row.get(7).toString().trim()));
                            } catch (Exception e2) {
                                log.warn("【导入】第{}行：过期时间格式错误", i + 1);
                            }
                        }
                    }
                    
                    // 解析可用人人数
                    if (row.size() > 8 && row.get(8) != null) {
                        try {
                            dto.setAvailableCount(Integer.parseInt(row.get(8).toString().trim()));
                        } catch (NumberFormatException e) {
                            log.warn("【导入】第{}行：可用人人数格式错误", i + 1);
                        }
                    }
                    
                    dto.setRemark(row.size() > 9 && row.get(9) != null ? row.get(9).toString().trim() : null);
                    dto.setVisitStatus(0); // 默认未探店

                    shopService.createShop(dto);
                    successCount++;

                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行：" + e.getMessage());
                    failCount++;
                }
            }

            String message = String.format("导入完成！成功 %d 条，失败 %d 条", successCount, failCount);
            if (autoSearchCount > 0) {
                message += String.format("，自动查询地址 %d 条", autoSearchCount);
            }
            if (!errors.isEmpty() && errors.size() <= 5) {
                message += "。错误：" + String.join("；", errors);
            } else if (errors.size() > 5) {
                message += "。前5个错误：" + String.join("；", errors.subList(0, 5));
            }

            log.info("【导入完成】成功 {} 条，失败 {} 条，自动查询地址 {} 条", successCount, failCount, autoSearchCount);
            return Result.success(message);

        } catch (Exception e) {
            log.error("导入店铺失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        log.info("【接口调用】下载导入模板");
        try {
            // 创建Excel写入器
            ExcelWriter writer = ExcelUtil.getWriter(true);

            // 设置表头
            List<String> headers = new ArrayList<>();
            headers.add("店铺名称");
            headers.add("类别");
            headers.add("店铺地址");
            headers.add("经度");
            headers.add("纬度");
            headers.add("联系电话");
            headers.add("营业时间");
            headers.add("过期时间");
            headers.add("可用人人数");
            headers.add("备注");

            writer.writeHeadRow(headers);

            // 添加示例数据
            List<List<Object>> exampleData = new ArrayList<>();
            List<Object> example1 = new ArrayList<>();
            example1.add("示例店铺1");
            example1.add("美食");
            example1.add("北京市朝阳区示例路123号");
            example1.add(116.397428);
            example1.add(39.90923);
            example1.add("010-12345678");
            example1.add("09:00-21:00");
            example1.add("2025-12-31");
            example1.add(2);
            example1.add("这是一个示例店铺");
            exampleData.add(example1);

            writer.write(exampleData);

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            String fileName = URLEncoder.encode("店铺导入模板", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            // 输出到响应流
            ServletOutputStream out = response.getOutputStream();
            writer.flush(out, true);
            writer.close();
            out.close();

        } catch (Exception e) {
            log.error("下载模板失败", e);
        }
    }
}
