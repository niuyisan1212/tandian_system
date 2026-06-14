package com.tandian.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

/**
 * 清单-店铺关联实体类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Data
@TableName("list_shop")
public class ListShop implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 清单ID */
    private Long listId;

    /** 店铺ID */
    private Long shopId;

    /** 访问顺序（1开始） */
    private Integer visitOrder;

    /** 是否跳过（0否/1是） */
    private Integer isSkipped;
}
