package com.tandian.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tandian.system.entity.ShoppingList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 探店清单Mapper接口
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Mapper
public interface ShoppingListMapper extends BaseMapper<ShoppingList> {
}
