package com.tandian.system.vo;

import lombok.Data;
import com.tandian.system.entity.RoutePlan;
import com.tandian.system.entity.Shop;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 清单详情VO
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
public class ShoppingListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 清单名称 */
    private String name;

    /** 任务日期 */
    private LocalDate taskDate;

    /** 起点经度 */
    private BigDecimal startLng;

    /** 起点纬度 */
    private BigDecimal startLat;

    /** 起点地址文本 */
    private String startAddress;

    /** 状态 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 选择的方案类型 */
    private String selectedPlanType;

    /** 店铺列表 */
    private List<ShopItem> shops;

    /** 路线方案列表 */
    private List<RoutePlan> plans;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 店铺项 */
    @Data
    public static class ShopItem implements Serializable {
        private Long id;
        private String name;
        private String address;
        private BigDecimal longitude;
        private BigDecimal latitude;
        private Integer visitOrder;
        private Boolean isSkipped;
    }
}
