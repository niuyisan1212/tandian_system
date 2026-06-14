package com.tandian.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 探店清单实体类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
@TableName("shopping_list")
public class ShoppingList implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
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

    /** 状态（0待执行/1已完成/2已取消） */
    private Integer status;

    /** 选择的方案类型 */
    private String selectedPlanType;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
