package com.tandian.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 路线方案实体类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
@TableName("route_plan")
public class RoutePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 清单ID */
    private Long listId;

    /** 方案类型（time_first/cost_first/balanced） */
    private String planType;

    /** 总距离（米） */
    private Integer totalDistanceM;

    /** 总耗时（秒） */
    private Integer totalDurationSec;

    /** 总花费（元） */
    private BigDecimal totalCost;

    /** 路线详情JSON */
    private String routeJson;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
