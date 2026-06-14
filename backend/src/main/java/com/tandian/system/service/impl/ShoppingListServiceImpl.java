package com.tandian.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tandian.system.dto.ShoppingListDTO;
import com.tandian.system.entity.ListShop;
import com.tandian.system.entity.RoutePlan;
import com.tandian.system.entity.Shop;
import com.tandian.system.entity.ShoppingList;
import com.tandian.system.mapper.ListShopMapper;
import com.tandian.system.mapper.RoutePlanMapper;
import com.tandian.system.mapper.ShopMapper;
import com.tandian.system.mapper.ShoppingListMapper;
import com.tandian.system.service.AmapService;
import com.tandian.system.service.ShoppingListService;
import com.tandian.system.vo.ShoppingListVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 探店清单Service实现类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Slf4j
@Service
public class ShoppingListServiceImpl implements ShoppingListService {

    @Resource
    private ShoppingListMapper shoppingListMapper;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private ListShopMapper listShopMapper;

    @Resource
    private RoutePlanMapper routePlanMapper;

    @Resource
    private AmapService amapService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShoppingListVO createList(ShoppingListDTO dto) {
        log.info("创建清单：{}", dto.getName());

        // 创建清单
        ShoppingList list = new ShoppingList();
        BeanUtils.copyProperties(dto, list);
        list.setStatus(0); // 待执行
        shoppingListMapper.insert(list);

        // 创建清单-店铺关联
        if (CollUtil.isNotEmpty(dto.getShopIds())) {
            int order = 1;
            for (Long shopId : dto.getShopIds()) {
                ListShop listShop = new ListShop();
                listShop.setListId(list.getId());
                listShop.setShopId(shopId);
                listShop.setVisitOrder(order++);
                listShop.setIsSkipped(0);
                listShopMapper.insert(listShop);
            }
        }

        // 自动规划路线
        List<RoutePlan> plans = replanRoute(list.getId());

        log.info("清单创建成功，ID：{}，生成{}个路线方案", list.getId(), plans.size());
        return getListById(list.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShoppingListVO updateList(Long id, ShoppingListDTO dto) {
        log.info("更新清单：{}", id);

        ShoppingList list = shoppingListMapper.selectById(id);
        if (list == null) {
            throw new RuntimeException("清单不存在");
        }

        // 更新基本信息
        BeanUtils.copyProperties(dto, list, "id", "status");
        shoppingListMapper.updateById(list);

        // 更新店铺关联
        if (dto.getShopIds() != null) {
            // 删除旧关联
            LambdaQueryWrapper<ListShop> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ListShop::getListId, id);
            listShopMapper.delete(wrapper);

            // 添加新关联
            int order = 1;
            for (Long shopId : dto.getShopIds()) {
                ListShop listShop = new ListShop();
                listShop.setListId(id);
                listShop.setShopId(shopId);
                listShop.setVisitOrder(order++);
                listShop.setIsSkipped(0);
                listShopMapper.insert(listShop);
            }

            // 重新规划路线
            replanRoute(id);
        }

        log.info("清单更新成功");
        return getListById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteList(Long id) {
        log.info("删除清单：{}", id);

        ShoppingList list = shoppingListMapper.selectById(id);
        if (list == null) {
            throw new RuntimeException("清单不存在");
        }

        shoppingListMapper.deleteById(id);
        log.info("清单删除成功");
    }

    @Override
    public ShoppingListVO getListById(Long id) {
        ShoppingList list = shoppingListMapper.selectById(id);
        if (list == null) {
            return null;
        }

        ShoppingListVO vo = new ShoppingListVO();
        BeanUtils.copyProperties(list, vo);

        // 设置状态文本
        vo.setStatusText(getStatusText(list.getStatus()));

        // 查询店铺列表
        LambdaQueryWrapper<ListShop> lsWrapper = new LambdaQueryWrapper<>();
        lsWrapper.eq(ListShop::getListId, id)
                .orderByAsc(ListShop::getVisitOrder);
        List<ListShop> listShops = listShopMapper.selectList(lsWrapper);

        if (CollUtil.isNotEmpty(listShops)) {
            List<Long> shopIds = listShops.stream()
                    .map(ListShop::getShopId)
                    .collect(Collectors.toList());

            List<Shop> shops = shopMapper.selectBatchIds(shopIds);
            Map<Long, Shop> shopMap = shops.stream()
                    .collect(Collectors.toMap(Shop::getId, s -> s));

            List<ShoppingListVO.ShopItem> shopItems = listShops.stream()
                    .map(ls -> {
                        Shop shop = shopMap.get(ls.getShopId());
                        if (shop == null) return null;

                        ShoppingListVO.ShopItem item = new ShoppingListVO.ShopItem();
                        item.setId(shop.getId());
                        item.setName(shop.getName());
                        item.setAddress(shop.getAddress());
                        item.setLongitude(shop.getLongitude());
                        item.setLatitude(shop.getLatitude());
                        item.setVisitOrder(ls.getVisitOrder());
                        item.setIsSkipped(ls.getIsSkipped() == 1);
                        return item;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            vo.setShops(shopItems);
        }

        // 查询路线方案
        LambdaQueryWrapper<RoutePlan> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.eq(RoutePlan::getListId, id);
        List<RoutePlan> plans = routePlanMapper.selectList(rpWrapper);
        vo.setPlans(plans);

        return vo;
    }

    @Override
    public Page<ShoppingListVO> getListPage(Integer pageNum, Integer pageSize, Integer status) {
        log.info("分页查询清单列表，页码：{}，大小：{}，状态：{}", pageNum, pageSize, status);

        Page<ShoppingList> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ShoppingList> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(ShoppingList::getStatus, status);
        }

        wrapper.orderByDesc(ShoppingList::getCreatedAt);
        Page<ShoppingList> listPage = shoppingListMapper.selectPage(page, wrapper);

        Page<ShoppingListVO> voPage = new Page<>();
        BeanUtils.copyProperties(listPage, voPage, "records");
        voPage.setRecords(listPage.getRecords().stream()
                .map(list -> {
                    ShoppingListVO vo = new ShoppingListVO();
                    BeanUtils.copyProperties(list, vo);
                    vo.setStatusText(getStatusText(list.getStatus()));
                    return vo;
                })
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsCompleted(Long id) {
        log.info("标记清单为已完成：{}", id);

        ShoppingList list = shoppingListMapper.selectById(id);
        if (list == null) {
            throw new RuntimeException("清单不存在");
        }

        if (list.getStatus() != 0) {
            throw new RuntimeException("只有待执行状态的清单可以标记完成");
        }

        // 更新清单状态
        list.setStatus(1);
        shoppingListMapper.updateById(list);

        // 获取清单中的店铺ID
        LambdaQueryWrapper<ListShop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ListShop::getListId, id);
        List<ListShop> listShops = listShopMapper.selectList(wrapper);

        if (CollUtil.isNotEmpty(listShops)) {
            List<Long> shopIds = listShops.stream()
                    .map(ListShop::getShopId)
                    .collect(Collectors.toList());

            // 批量更新店铺状态为已探店
            for (Long shopId : shopIds) {
                Shop shop = shopMapper.selectById(shopId);
                if (shop != null) {
                    shop.setVisitStatus(1);
                    shopMapper.updateById(shop);
                }
            }
        }

        log.info("清单标记完成成功，已更新{}个店铺状态", listShops.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsCancelled(Long id) {
        log.info("标记清单为已取消：{}", id);

        ShoppingList list = shoppingListMapper.selectById(id);
        if (list == null) {
            throw new RuntimeException("清单不存在");
        }

        list.setStatus(2);
        shoppingListMapper.updateById(list);
        log.info("清单标记取消成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoutePlan> replanRoute(Long id) {
        log.info("重新规划路线，清单ID：{}", id);

        ShoppingList list = shoppingListMapper.selectById(id);
        if (list == null) {
            throw new RuntimeException("清单不存在");
        }

        // 获取清单中的店铺
        LambdaQueryWrapper<ListShop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ListShop::getListId, id)
                .eq(ListShop::getIsSkipped, 0);
        List<ListShop> listShops = listShopMapper.selectList(wrapper);

        if (CollUtil.isEmpty(listShops)) {
            log.warn("清单中没有店铺，跳过路线规划");
            return Collections.emptyList();
        }

        List<Long> shopIds = listShops.stream()
                .map(ListShop::getShopId)
                .collect(Collectors.toList());
        List<Shop> shops = shopMapper.selectBatchIds(shopIds);

        // 删除旧的路线方案
        LambdaQueryWrapper<RoutePlan> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RoutePlan::getListId, id);
        routePlanMapper.delete(deleteWrapper);

        // 生成3个方案（使用模拟算法）
        List<RoutePlan> plans = generateRoutePlans(list, shops);

        // 保存方案
        for (RoutePlan plan : plans) {
            routePlanMapper.insert(plan);
        }

        log.info("路线规划完成，生成{}个方案", plans.size());
        return plans;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectPlan(Long listId, String planType) {
        log.info("选择路线方案，清单ID：{}，方案类型：{}", listId, planType);

        ShoppingList list = shoppingListMapper.selectById(listId);
        if (list == null) {
            throw new RuntimeException("清单不存在");
        }

        list.setSelectedPlanType(planType);
        shoppingListMapper.updateById(list);
        log.info("方案选择成功");
    }

    /**
     * 生成路线方案（使用模拟算法）
     */
    private List<RoutePlan> generateRoutePlans(ShoppingList list, List<Shop> shops) {
        List<RoutePlan> plans = new ArrayList<>();

        // 时间优先方案
        plans.add(createPlan(list, shops, "time_first", 1.2, 0.8));

        // 成本优先方案
        plans.add(createPlan(list, shops, "cost_first", 0.8, 1.2));

        // 均衡方案
        plans.add(createPlan(list, shops, "balanced", 1.0, 1.0));

        return plans;
    }

    /**
     * 创建单个路线方案
     */
    private RoutePlan createPlan(ShoppingList list, List<Shop> shops, String planType, 
                                   double timeFactor, double costFactor) {
        RoutePlan plan = new RoutePlan();
        plan.setListId(list.getId());
        plan.setPlanType(planType);

        log.info("【路线规划】开始规划{}方案，店铺数量：{}", planType, shops.size());

        // 使用最近邻算法生成访问顺序
        List<Shop> orderedShops = nearestNeighborTSP(list, shops);

        // 计算总距离、总耗时、总花费
        int totalDistance = 0;
        int totalDuration = 0;
        double totalCost = 0.0;

        List<Map<String, Object>> segments = new ArrayList<>();
        BigDecimal prevLng = list.getStartLng();
        BigDecimal prevLat = list.getStartLat();

        for (int i = 0; i < orderedShops.size(); i++) {
            Shop shop = orderedShops.get(i);

            // 使用高德地图API规划路线
            Map<String, Object> routeResult = planRouteBetweenPoints(
                    prevLng.doubleValue(), prevLat.doubleValue(),
                    shop.getLongitude().doubleValue(), shop.getLatitude().doubleValue(),
                    planType, "上海" // 默认城市
            );

            if (routeResult != null) {
                int distance = (int) routeResult.get("distance");
                int duration = (int) routeResult.get("duration");
                double cost = (double) routeResult.get("cost");
                String mode = (String) routeResult.get("mode");
                String instruction = (String) routeResult.get("instruction");

                totalDistance += distance;
                totalDuration += duration;
                totalCost += cost;

                // 构建路段信息（包含路径坐标）
                Map<String, Object> segment = new HashMap<>();
                segment.put("from_type", i == 0 ? "start" : "shop");
                segment.put("to_shop_id", shop.getId());
                segment.put("mode", mode);
                segment.put("instruction", instruction);
                segment.put("distance_m", distance);
                segment.put("duration_sec", duration);
                segment.put("cost", cost);
                segment.put("steps", routeResult.get("steps"));
                
                // 保存路径坐标用于地图显示
                if (routeResult.containsKey("polyline")) {
                    segment.put("polyline", routeResult.get("polyline"));
                }
                
                // 保存换乘详情
                if (routeResult.containsKey("transitDetails")) {
                    segment.put("transitDetails", routeResult.get("transitDetails"));
                }
                
                segments.add(segment);
            }

            prevLng = shop.getLongitude();
            prevLat = shop.getLatitude();
        }

        plan.setTotalDistanceM(totalDistance);
        plan.setTotalDurationSec(totalDuration);
        plan.setTotalCost(BigDecimal.valueOf(totalCost).setScale(2, RoundingMode.HALF_UP));

        // 构建route_json
        Map<String, Object> routeJson = new HashMap<>();
        routeJson.put("sequence", orderedShops.stream().map(Shop::getId).collect(Collectors.toList()));
        routeJson.put("segments", segments);
        plan.setRouteJson(JSON.toJSONString(routeJson));

        log.info("【路线规划】{}方案规划完成：{}米，{}秒，{}元", 
                planType, totalDistance, totalDuration, totalCost);

        return plan;
    }

    /**
     * 使用高德地图API规划两点之间的路线（优先地铁）
     */
    private Map<String, Object> planRouteBetweenPoints(double originLng, double originLat,
                                                        double destLng, double destLat,
                                                        String planType, String city) {
        // 计算直线距离
        double dx = (destLng - originLng) * 111000;
        double dy = (destLat - originLat) * 111000;
        double directDistance = Math.sqrt(dx * dx + dy * dy);

        // 根据距离和策略决定交通方式
        if (directDistance <= 500) {
            // 很短距离：步行
            log.info("【路线规划】距离{}米，选择步行", (int)directDistance);
            return amapService.walkingRoute(originLng, originLat, destLng, destLat);
        } else {
            // 中长距离：优先使用公交/地铁
            // strategy: 0-最快捷，1-最经济，2-最少换乘，3-最少步行，5-地铁优先
            int strategy;
            switch (planType) {
                case "time_first":
                    strategy = 0; // 最快捷
                    break;
                case "cost_first":
                    strategy = 1; // 最经济
                    break;
                default:
                    strategy = 5; // 地铁优先
            }
            
            log.info("【路线规划】距离{}米，选择公交/地铁，策略={}", (int)directDistance, strategy);
            
            // 先尝试公交/地铁路线
            Map<String, Object> transitResult = amapService.transitRoute(
                    originLng, originLat, destLng, destLat, city, strategy);
            
            // 如果公交规划成功，返回结果
            if (transitResult != null) {
                return transitResult;
            }
            
            // 如果公交规划失败，根据距离选择步行或驾车
            if (directDistance <= 1500) {
                log.info("【路线规划】公交规划失败，距离较短，选择步行");
                return amapService.walkingRoute(originLng, originLat, destLng, destLat);
            } else {
                log.info("【路线规划】公交规划失败，选择驾车");
                return amapService.drivingRoute(originLng, originLat, destLng, destLat);
            }
        }
    }

    /**
     * 最近邻算法TSP（简化版）
     */
    private List<Shop> nearestNeighborTSP(ShoppingList list, List<Shop> shops) {
        if (shops.isEmpty()) return shops;

        List<Shop> result = new ArrayList<>();
        List<Shop> remaining = new ArrayList<>(shops);

        BigDecimal currentLng = list.getStartLng();
        BigDecimal currentLat = list.getStartLat();

        while (!remaining.isEmpty()) {
            Shop nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (Shop shop : remaining) {
                double distance = calculateDistance(currentLng, currentLat, 
                        shop.getLongitude(), shop.getLatitude());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = shop;
                }
            }

            if (nearest != null) {
                result.add(nearest);
                remaining.remove(nearest);
                currentLng = nearest.getLongitude();
                currentLat = nearest.getLatitude();
            }
        }

        return result;
    }

    /**
     * 计算两点间距离（简化版，使用欧几里得距离）
     */
    private double calculateDistance(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        double dx = lng2.subtract(lng1).doubleValue() * 111000; // 大约每度111公里
        double dy = lat2.subtract(lat1).doubleValue() * 111000;
        return Math.sqrt(dx * dx + dy * dy) / 1000.0; // 返回公里
    }

    /**
     * 决定交通方式
     */
    private String decideTransportMode(int distanceM, String planType) {
        if (distanceM <= 800) {
            return "walking";
        } else if (distanceM <= 3000) {
            return planType.equals("time_first") ? "subway" : "bus";
        } else {
            return "subway";
        }
    }

    /**
     * 计算耗时（秒）
     */
    private int calculateDuration(int distanceM, String mode, double timeFactor) {
        int baseDuration;
        switch (mode) {
            case "walking":
                baseDuration = (int) (distanceM / 80.0 * 60); // 步行速度约80米/分钟
                break;
            case "bus":
                baseDuration = (int) (distanceM / 300.0 * 60) + 300; // 公交约15km/h，加5分钟等车
                break;
            case "subway":
                baseDuration = (int) (distanceM / 500.0 * 60) + 480; // 地铁约30km/h，加8分钟换乘
                break;
            default:
                baseDuration = (int) (distanceM / 100.0 * 60);
        }
        return (int) (baseDuration * timeFactor);
    }

    /**
     * 计算花费（元）
     */
    private double calculateCost(int distanceM, String mode, double costFactor) {
        double baseCost;
        switch (mode) {
            case "walking":
                baseCost = 0.0;
                break;
            case "bus":
                baseCost = 2.0; // 普通公交
                break;
            case "subway":
                baseCost = distanceM > 6000 ? 5.0 : 3.0; // 地铁起步价
                break;
            default:
                baseCost = 0.0;
        }
        return baseCost * costFactor;
    }

    /**
     * 生成指引文本
     */
    private String generateInstruction(String mode, int distanceM, int durationSec) {
        int minutes = durationSec / 60;
        double distanceKm = distanceM / 1000.0;

        String modeText;
        switch (mode) {
            case "walking":
                modeText = "步行";
                break;
            case "bus":
                modeText = "乘公交";
                break;
            case "subway":
                modeText = "乘地铁";
                break;
            default:
                modeText = "前往";
        }

        return String.format("%s %.1f公里，预计%d分钟", modeText, distanceKm, minutes);
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        switch (status) {
            case 0:
                return "待执行";
            case 1:
                return "已完成";
            case 2:
                return "已取消";
            default:
                return "未知";
        }
    }
}
