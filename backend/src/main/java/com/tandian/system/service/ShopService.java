package com.tandian.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tandian.system.dto.ShopDTO;
import com.tandian.system.entity.Shop;
import com.tandian.system.vo.ShopVO;

import java.util.List;

/**
 * 店铺Service接口
 * 
 * @author tandian-system
 * @version 1.0.0
 */
public interface ShopService extends IService<Shop> {

    /**
     * 创建店铺
     */
    ShopVO createShop(ShopDTO dto);

    /**
     * 更新店铺
     */
    ShopVO updateShop(Long id, ShopDTO dto);

    /**
     * 删除店铺
     */
    void deleteShop(Long id);

    /**
     * 获取店铺详情
     */
    ShopVO getShopById(Long id);

    /**
     * 分页查询店铺列表
     */
    Page<ShopVO> getShopPage(Integer pageNum, Integer pageSize, Integer visitStatus, String category, String keyword, Integer isValid, String expireTimeStart, String expireTimeEnd, Integer availableCount);

    /**
     * 获取所有有效待探店店铺
     */
    List<ShopVO> getValidShops();

    /**
     * 标记店铺为已探店
     */
    void markAsVisited(Long id);

    /**
     * 批量标记店铺为已探店
     */
    void batchMarkAsVisited(List<Long> ids);

    /**
     * 获取生效店铺的可用人总数
     */
    int getTotalAvailableCount();

    /**
     * 获取数据库中已有的可用人数选项（去重排序）
     */
    List<Integer> getAvailableCountOptions();
}
