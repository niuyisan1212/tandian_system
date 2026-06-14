package com.tandian.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tandian.system.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

/**
 * 店铺Mapper接口
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}
