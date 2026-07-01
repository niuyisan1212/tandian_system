package com.tandian.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;

/**
 * 店铺-探店员关联实体
 */
@Data
@TableName("shop_explorer")
public class ShopExplorer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long explorerId;
}
