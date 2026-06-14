package com.tandian.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tandian.system.dto.ShoppingListDTO;
import com.tandian.system.entity.RoutePlan;
import com.tandian.system.entity.ShoppingList;
import com.tandian.system.vo.ShoppingListVO;

import java.util.List;

/**
 * 探店清单Service接口
 * 
 * @author tandian-system
 * @version 1.0.0
 */
public interface ShoppingListService {

    /**
     * 创建清单（包含路线规划）
     */
    ShoppingListVO createList(ShoppingListDTO dto);

    /**
     * 更新清单
     */
    ShoppingListVO updateList(Long id, ShoppingListDTO dto);

    /**
     * 删除清单
     */
    void deleteList(Long id);

    /**
     * 获取清单详情
     */
    ShoppingListVO getListById(Long id);

    /**
     * 分页查询清单列表
     */
    Page<ShoppingListVO> getListPage(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 标记清单为已完成
     */
    void markAsCompleted(Long id);

    /**
     * 标记清单为已取消
     */
    void markAsCancelled(Long id);

    /**
     * 重新规划路线
     */
    List<RoutePlan> replanRoute(Long id);

    /**
     * 选择路线方案
     */
    void selectPlan(Long listId, String planType);
}
