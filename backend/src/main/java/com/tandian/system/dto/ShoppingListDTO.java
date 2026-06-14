package com.tandian.system.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 清单创建/更新DTO
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
public class ShoppingListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 清单名称 */
    @NotBlank(message = "清单名称不能为空")
    private String name;

    /** 任务日期 */
    @NotNull(message = "任务日期不能为空")
    private LocalDate taskDate;

    /** 起点经度 */
    private BigDecimal startLng;

    /** 起点纬度 */
    private BigDecimal startLat;

    /** 起点地址文本 */
    private String startAddress;

    /** 店铺ID列表 */
    @NotNull(message = "店铺列表不能为空")
    private List<Long> shopIds;
}
