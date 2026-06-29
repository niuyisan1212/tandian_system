package com.tandian.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 店铺实体类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
@TableName("shop")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 店铺名称 */
    private String name;

    /** 店铺类别 */
    private String category;

    /** 店铺地址 */
    private String address;

    /** 经度（GCJ-02坐标系） */
    private BigDecimal longitude;

    /** 纬度（GCJ-02坐标系） */
    private BigDecimal latitude;

    /** 坐标来源（geocode/manual/accurate） */
    private String geoSource;

    /** 联系电话 */
    private String phone;

    /** 营业时间 */
    private String businessHours;

    /** 备注信息 */
    private String remark;

    /** 探店状态（0未探店/1已探店） */
    private Integer visitStatus;

    /** 过期时间（NULL表示永不过期） */
    private LocalDate expireTime;

    /** 可用人人数（默认1人） */
    private Integer availableCount = 1;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
