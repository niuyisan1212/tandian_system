package com.tandian.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 店铺创建/更新DTO
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
public class ShopDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 店铺名称 */
    @NotBlank(message = "店铺名称不能为空")
    private String name;

    /** 店铺类别 */
    private String category;

    /** 店铺地址 */
    @NotBlank(message = "店铺地址不能为空")
    private String address;

    /** 经度（GCJ-02坐标系） */
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    /** 纬度（GCJ-02坐标系） */
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

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
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate expireTime;

    /** 可用人人数 */
    private Integer availableCount;

    /** 关联探店员ID列表 */
    private List<Long> explorerIds;
}
