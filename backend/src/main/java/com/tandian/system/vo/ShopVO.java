package com.tandian.system.vo;

import lombok.Data;
import com.tandian.system.entity.Shop;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 店铺详情VO
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
public class ShopVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 店铺名称 */
    private String name;

    /** 店铺类别 */
    private String category;

    /** 店铺类别文本 */
    private String categoryText;

    /** 店铺地址 */
    private String address;

    /** 经度 */
    private Object longitude;

    /** 纬度 */
    private Object latitude;

    /** 坐标来源 */
    private String geoSource;

    /** 联系电话 */
    private String phone;

    /** 营业时间 */
    private String businessHours;

    /** 备注信息 */
    private String remark;

    /** 探店状态 */
    private Integer visitStatus;

    /** 过期时间 */
    private LocalDate expireTime;

    /** 可用人人数 */
    private Integer availableCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否有效（未过期且未探店） */
    private Boolean isValid;

    /** 探店状态文本 */
    private String visitStatusText;

    /** 有效性文本 */
    private String isValidText;

    /** 从Entity转换 */
    public static ShopVO fromEntity(Shop shop) {
        if (shop == null) return null;
        
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setCategory(shop.getCategory());
        vo.setCategoryText(getCategoryText(shop.getCategory()));
        vo.setAddress(shop.getAddress());
        vo.setLongitude(shop.getLongitude());
        vo.setLatitude(shop.getLatitude());
        vo.setGeoSource(shop.getGeoSource());
        vo.setPhone(shop.getPhone());
        vo.setBusinessHours(shop.getBusinessHours());
        vo.setRemark(shop.getRemark());
        vo.setVisitStatus(shop.getVisitStatus());
        vo.setExpireTime(shop.getExpireTime());
        vo.setAvailableCount(shop.getAvailableCount());
        vo.setCreatedAt(shop.getCreatedAt());
        vo.setUpdatedAt(shop.getUpdatedAt());
        
        // 判断是否有效
        boolean isNotVisited = shop.getVisitStatus() != null && shop.getVisitStatus() == 0;
        boolean isNotExpired = shop.getExpireTime() == null || shop.getExpireTime().isAfter(LocalDate.now());
        vo.setIsValid(isNotVisited && isNotExpired);
        
        // 设置状态文本
        vo.setVisitStatusText(shop.getVisitStatus() != null && shop.getVisitStatus() == 1 ? "已探店" : "未探店");
        vo.setIsValidText(vo.getIsValid() ? "有效" : "无效");
        
        return vo;
    }
    
    /** 获取类别文本 */
    private static String getCategoryText(String category) {
        if (category == null || category.isEmpty()) {
            return "其他";
        }
        return category;
    }
}
