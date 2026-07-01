package com.tandian.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tandian.system.entity.Explorer;
import com.tandian.system.entity.ShopExplorer;
import com.tandian.system.mapper.ExplorerMapper;
import com.tandian.system.mapper.ShopExplorerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 探店员服务
 */
@Slf4j
@Service
public class ExplorerService extends ServiceImpl<ExplorerMapper, Explorer> {

    @Resource
    private ShopExplorerMapper shopExplorerMapper;

    /** 创建探店员 */
    @Transactional(rollbackFor = Exception.class)
    public Explorer createExplorer(Explorer explorer) {
        log.info("创建探店员：{}", explorer.getName());
        this.save(explorer);
        return explorer;
    }

    /** 更新探店员 */
    @Transactional(rollbackFor = Exception.class)
    public Explorer updateExplorer(Long id, Explorer explorer) {
        log.info("更新探店员：{}", id);
        explorer.setId(id);
        this.updateById(explorer);
        return this.getById(id);
    }

    /** 删除探店员（同时删除关联） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExplorer(Long id) {
        log.info("删除探店员：{}", id);
        // 删除关联
        shopExplorerMapper.delete(new LambdaQueryWrapper<ShopExplorer>()
                .eq(ShopExplorer::getExplorerId, id));
        this.removeById(id);
    }

    /** 获取店铺关联的探店员列表 */
    public List<Explorer> getExplorersByShopId(Long shopId) {
        List<ShopExplorer> relations = shopExplorerMapper.selectList(
                new LambdaQueryWrapper<ShopExplorer>().eq(ShopExplorer::getShopId, shopId));
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> explorerIds = relations.stream().map(ShopExplorer::getExplorerId).collect(Collectors.toList());
        return this.listByIds(explorerIds);
    }

    /** 设置店铺关联的探店员（全量覆盖） */
    @Transactional(rollbackFor = Exception.class)
    public void setShopExplorers(Long shopId, List<Long> explorerIds) {
        // 先删除旧关联
        shopExplorerMapper.delete(new LambdaQueryWrapper<ShopExplorer>()
                .eq(ShopExplorer::getShopId, shopId));
        // 再插入新关联
        if (explorerIds != null) {
            for (Long explorerId : explorerIds) {
                ShopExplorer se = new ShopExplorer();
                se.setShopId(shopId);
                se.setExplorerId(explorerId);
                shopExplorerMapper.insert(se);
            }
        }
    }

    /** 根据姓名查找探店员（模糊匹配） */
    public Explorer findByName(String name) {
        return this.getOne(new LambdaQueryWrapper<Explorer>()
                .eq(Explorer::getName, name)
                .last("LIMIT 1"));
    }

    /** 根据姓名查找，不存在则自动创建 */
    @Transactional(rollbackFor = Exception.class)
    public Explorer findOrCreateByName(String name) {
        Explorer explorer = findByName(name);
        if (explorer == null) {
            explorer = new Explorer();
            explorer.setName(name);
            this.save(explorer);
            log.info("自动创建探店员：{}", name);
        }
        return explorer;
    }
}
