package com.tandian.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tandian.system.dto.ShopDTO;
import com.tandian.system.entity.Shop;
import com.tandian.system.mapper.ShopMapper;
import com.tandian.system.service.ShopService;
import com.tandian.system.vo.ShopVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 店铺Service实现类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVO createShop(ShopDTO dto) {
        log.info("创建店铺：{}", dto.getName());
        
        Shop shop = new Shop();
        BeanUtils.copyProperties(dto, shop);
        
        // 设置默认值
        if (shop.getGeoSource() == null) {
            shop.setGeoSource("manual");
        }
        if (shop.getVisitStatus() == null) {
            shop.setVisitStatus(0);
        }
        if (shop.getAvailableCount() == null) {
            shop.setAvailableCount(1);
        }
        
        this.save(shop);
        log.info("店铺创建成功，ID：{}", shop.getId());
        
        return ShopVO.fromEntity(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVO updateShop(Long id, ShopDTO dto) {
        log.info("更新店铺：{}", id);
        
        Shop shop = this.getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在");
        }
        
        BeanUtils.copyProperties(dto, shop, "id");
        this.updateById(shop);
        log.info("店铺更新成功");
        
        return ShopVO.fromEntity(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShop(Long id) {
        log.info("删除店铺：{}", id);
        
        Shop shop = this.getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在");
        }
        
        this.removeById(id);
        log.info("店铺删除成功");
    }

    @Override
    public ShopVO getShopById(Long id) {
        Shop shop = this.getById(id);
        return ShopVO.fromEntity(shop);
    }

    @Override
    public Page<ShopVO> getShopPage(Integer pageNum, Integer pageSize, Integer visitStatus, String category, String keyword, Integer isValid, String expireTimeStart, String expireTimeEnd, Integer availableCount) {
        log.info("分页查询店铺列表，页码：{}，大小：{}，状态：{}，类别：{}，关键词：{}，生效状态：{}，过期范围：{}~{}，可用人数：{}", pageNum, pageSize, visitStatus, category, keyword, isValid, expireTimeStart, expireTimeEnd, availableCount);
        
        Page<Shop> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        
        // 状态筛选
        if (visitStatus != null) {
            wrapper.eq(Shop::getVisitStatus, visitStatus);
        }
        
        // 类别筛选
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Shop::getCategory, category);
        }
        
        // 关键词搜索
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Shop::getName, keyword)
                    .or()
                    .like(Shop::getAddress, keyword));
        }
        
        // 生效状态筛选：1=生效中（未过期且未探店），0=已失效
        if (isValid != null) {
            LocalDate today = LocalDate.now();
            if (isValid == 1) {
                // 生效中：未探店 + (未设过期时间 或 过期时间 > 今天)
                wrapper.eq(Shop::getVisitStatus, 0)
                        .and(w -> w.isNull(Shop::getExpireTime)
                                .or()
                                .gt(Shop::getExpireTime, today));
            } else if (isValid == 0) {
                // 已失效：已探店 或 (过期时间不为空 且 过期时间 <= 今天)
                wrapper.and(w -> w.eq(Shop::getVisitStatus, 1)
                        .or(w2 -> w2.isNotNull(Shop::getExpireTime)
                                .le(Shop::getExpireTime, today)));
            }
        }
        
        // 过期时间范围筛选
        if (StrUtil.isNotBlank(expireTimeStart)) {
            try {
                LocalDate startDate = LocalDate.parse(expireTimeStart);
                wrapper.ge(Shop::getExpireTime, startDate);
            } catch (Exception e) {
                log.warn("过期时间开始日期格式错误：{}", expireTimeStart);
            }
        }
        if (StrUtil.isNotBlank(expireTimeEnd)) {
            try {
                LocalDate endDate = LocalDate.parse(expireTimeEnd);
                wrapper.le(Shop::getExpireTime, endDate);
            } catch (Exception e) {
                log.warn("过期时间结束日期格式错误：{}", expireTimeEnd);
            }
        }
        
        // 可用人数筛选
        if (availableCount != null) {
            wrapper.eq(Shop::getAvailableCount, availableCount);
        }
        
        // 排序：按过期时间升序（快过期的排前面），无过期时间的排最后，然后按创建时间倒序
        wrapper.orderByAsc(Shop::getExpireTime)
                .orderByDesc(Shop::getCreatedAt);
        
        Page<Shop> shopPage = this.page(page, wrapper);
        
        // 转换为VO
        Page<ShopVO> voPage = new Page<>();
        BeanUtils.copyProperties(shopPage, voPage, "records");
        voPage.setRecords(shopPage.getRecords().stream()
                .map(ShopVO::fromEntity)
                .collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public List<ShopVO> getValidShops() {
        log.info("获取所有有效待探店店铺");
        
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getVisitStatus, 0)
                .and(w -> w.isNull(Shop::getExpireTime)
                        .or()
                        .gt(Shop::getExpireTime, today))
                .orderByAsc(Shop::getExpireTime)
                .orderByDesc(Shop::getCreatedAt);
        
        List<Shop> shops = this.list(wrapper);
        return shops.stream()
                .map(ShopVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsVisited(Long id) {
        log.info("标记店铺为已探店：{}", id);
        
        Shop shop = this.getById(id);
        if (shop == null) {
            throw new RuntimeException("店铺不存在");
        }
        
        shop.setVisitStatus(1);
        this.updateById(shop);
        log.info("店铺标记成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsVisited(List<Long> ids) {
        log.info("批量标记店铺为已探店，数量：{}", ids.size());
        
        for (Long id : ids) {
            Shop shop = this.getById(id);
            if (shop != null) {
                shop.setVisitStatus(1);
                this.updateById(shop);
            }
        }
        log.info("批量标记成功");
    }

    @Override
    public int getTotalAvailableCount() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        // 只统计生效中的店铺
        wrapper.eq(Shop::getVisitStatus, 0)
                .and(w -> w.isNull(Shop::getExpireTime)
                        .or()
                        .gt(Shop::getExpireTime, today))
                .isNotNull(Shop::getAvailableCount);
        
        return this.list(wrapper).stream()
                .mapToInt(shop -> shop.getAvailableCount() != null ? shop.getAvailableCount() : 1)
                .sum();
    }

    @Override
    public List<Integer> getAvailableCountOptions() {
        log.info("获取可用人数选项");
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Shop::getAvailableCount)
                .isNotNull(Shop::getAvailableCount)
                .groupBy(Shop::getAvailableCount)
                .orderByAsc(Shop::getAvailableCount);
        return this.listObjs(wrapper, obj -> (Integer) obj);
    }
}
