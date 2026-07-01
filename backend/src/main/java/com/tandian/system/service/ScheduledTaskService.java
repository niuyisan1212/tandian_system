package com.tandian.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tandian.system.entity.ScheduledTask;
import com.tandian.system.entity.Shop;
import com.tandian.system.entity.Explorer;
import com.tandian.system.mapper.ScheduledTaskMapper;
import com.tandian.system.vo.ExplorerVO;
import com.tandian.system.vo.ShopVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * 定时任务服务
 * 支持动态调度：启动/停止/修改频率/手动触发
 */
@Slf4j
@Service
public class ScheduledTaskService extends ServiceImpl<ScheduledTaskMapper, ScheduledTask> {

    @Resource
    private EmailService emailService;

    @Resource
    private ShopService shopService;

    @Resource
    private ExplorerService explorerService;

    private ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> taskFutures = new ConcurrentHashMap<>();

    /**
     * 初始化：启动所有已启用的定时任务
     */
    public void init() {
        if (taskScheduler == null) {
            taskScheduler = new ThreadPoolTaskScheduler();
            taskScheduler.setPoolSize(4);
            taskScheduler.setThreadNamePrefix("td-scheduled-");
            taskScheduler.initialize();
        }

        List<ScheduledTask> tasks = this.list(new LambdaQueryWrapper<ScheduledTask>()
                .eq(ScheduledTask::getEnabled, 1));

        for (ScheduledTask task : tasks) {
            startTask(task);
        }

        log.info("【定时任务】初始化完成，已启动 {} 个任务", tasks.size());
    }

    /**
     * 启动任务
     */
    public void startTask(ScheduledTask task) {
        stopTask(task.getTaskKey());

        try {
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeTask(task.getTaskKey()),
                    new CronTrigger(task.getCronExpression())
            );
            taskFutures.put(task.getTaskKey(), future);
            log.info("【定时任务】启动：{} - {}", task.getTaskName(), task.getCronExpression());
        } catch (Exception e) {
            log.error("【定时任务】启动失败：{} - {}", task.getTaskName(), e.getMessage());
        }
    }

    /**
     * 停止任务
     */
    public void stopTask(String taskKey) {
        ScheduledFuture<?> future = taskFutures.remove(taskKey);
        if (future != null) {
            future.cancel(false);
            log.info("【定时任务】停止：{}", taskKey);
        }
    }

    /**
     * 执行任务
     */
    public void executeTask(String taskKey) {
        ScheduledTask task = this.getOne(new LambdaQueryWrapper<ScheduledTask>()
                .eq(ScheduledTask::getTaskKey, taskKey));

        if (task == null) {
            log.warn("【定时任务】任务不存在：{}", taskKey);
            return;
        }

        log.info("【定时任务】开始执行：{}", task.getTaskName());
        long startTime = System.currentTimeMillis();

        try {
            String message;
            switch (taskKey) {
                case "expire_reminder":
                    message = doExpireReminder(task);
                    break;
                case "weekly_recommend":
                    message = doWeeklyRecommend(task);
                    break;
                default:
                    message = "未知任务类型";
            }

            long duration = System.currentTimeMillis() - startTime;
            task.setLastExecuteTime(LocalDateTime.now());
            task.setLastExecuteStatus("success");
            task.setLastExecuteMessage(message + "（耗时" + duration + "ms）");
            this.updateById(task);

            log.info("【定时任务】执行成功：{} - {}", task.getTaskName(), message);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            task.setLastExecuteTime(LocalDateTime.now());
            task.setLastExecuteStatus("failed");
            task.setLastExecuteMessage(e.getMessage() + "（耗时" + duration + "ms）");
            this.updateById(task);

            log.error("【定时任务】执行失败：{} - {}", task.getTaskName(), e.getMessage(), e);
        }
    }

    /**
     * 探店即将过期提醒
     * 查询T+1天会过期的店铺
     */
    private String doExpireReminder(ScheduledTask task) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String tomorrowStr = tomorrow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 查询T+1天过期的生效店铺
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getVisitStatus, 0)
                .eq(Shop::getExpireTime, tomorrow);
        List<Shop> shops = shopService.list(wrapper);
        List<ShopVO> shopVOs = shops.stream().map(ShopVO::fromEntity).collect(Collectors.toList());

        // 填充探店员信息
        fillExplorers(shopVOs);

        if (shopVOs.isEmpty()) {
            log.info("【过期提醒】明天({})没有即将过期的店铺", tomorrowStr);
            return "明天(" + tomorrowStr + ")没有即将过期的店铺";
        }

        // 发送邮件
        if (task.getNotifyEnabled() != null && task.getNotifyEnabled() == 1) {
            String subject = "探店即将过期提醒 - " + tomorrowStr + " 共" + shopVOs.size() + "家店铺";
            String html = emailService.buildExpireReminderHtml(shopVOs, tomorrowStr);
            emailService.sendHtmlEmail(task, subject, html);
        }

        return "发现" + shopVOs.size() + "家店铺明天(" + tomorrowStr + ")即将过期";
    }

    /**
     * 探店每周推荐
     * 规则：排除已探店和已过期 → 过期时间最近的优先 → 可用人多的优先 → 美食/娱乐优先 → 取前5
     */
    private String doWeeklyRecommend(ScheduledTask task) {
        LocalDate today = LocalDate.now();

        // 查询所有生效店铺
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getVisitStatus, 0)
                .and(w -> w.isNull(Shop::getExpireTime)
                        .or()
                        .gt(Shop::getExpireTime, today));
        List<Shop> allShops = shopService.list(wrapper);

        if (allShops.isEmpty()) {
            log.info("【每周推荐】当前没有生效的店铺");
            return "当前没有生效的店铺";
        }

        // 排序规则：
        // 1. 有过期时间的排前面（过期时间越近越靠前）
        // 2. 可用人多的优先
        // 3. 美食/娱乐类别优先
        List<Shop> sorted = allShops.stream().sorted((a, b) -> {
            // 规则1：过期时间越近排越前（null排最后）
            boolean aHasExpire = a.getExpireTime() != null;
            boolean bHasExpire = b.getExpireTime() != null;
            if (aHasExpire && bHasExpire) {
                int cmp = a.getExpireTime().compareTo(b.getExpireTime());
                if (cmp != 0) return cmp;
            } else if (aHasExpire) {
                return -1;
            } else if (bHasExpire) {
                return 1;
            }

            // 规则2：可用人多的优先
            int aCount = a.getAvailableCount() != null ? a.getAvailableCount() : 1;
            int bCount = b.getAvailableCount() != null ? b.getAvailableCount() : 1;
            int countCmp = Integer.compare(bCount, aCount);
            if (countCmp != 0) return countCmp;

            // 规则3：美食/娱乐优先
            return Integer.compare(getCategoryPriority(a.getCategory()), getCategoryPriority(b.getCategory()));
        }).collect(Collectors.toList());

        // 取前5
        List<ShopVO> top5 = sorted.stream()
                .limit(5)
                .map(ShopVO::fromEntity)
                .collect(Collectors.toList());

        // 填充探店员信息
        fillExplorers(top5);

        // 发送邮件
        if (task.getNotifyEnabled() != null && task.getNotifyEnabled() == 1) {
            String subject = "探店每周推荐 - 精选" + top5.size() + "家店铺";
            String html = emailService.buildWeeklyRecommendHtml(top5);
            emailService.sendHtmlEmail(task, subject, html);
        }

        return "推荐了" + top5.size() + "家店铺：" + top5.stream().map(ShopVO::getName).collect(Collectors.joining("、"));
    }

    /**
     * 类别优先级：美食=1, 娱乐=2, 其他更高数字
     */
    private int getCategoryPriority(String category) {
        if (category == null) return 99;
        switch (category) {
            case "美食": return 1;
            case "娱乐": return 2;
            case "购物": return 3;
            case "服务": return 4;
            default: return 5;
        }
    }

    /**
     * 更新任务并重新调度
     */
    public void updateAndReschedule(ScheduledTask task) {
        ScheduledTask old = this.getById(task.getId());
        if (old == null) {
            throw new RuntimeException("任务不存在");
        }

        // 更新数据库
        this.updateById(task);

        // 重新从数据库加载完整对象
        ScheduledTask updated = this.getById(task.getId());

        // 如果任务启用，重新调度；否则停止
        if (updated.getEnabled() != null && updated.getEnabled() == 1) {
            startTask(updated);
        } else {
            stopTask(updated.getTaskKey());
        }
    }

    /**
     * 手动触发任务
     */
    public void manualTrigger(Long taskId) {
        ScheduledTask task = this.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        executeTask(task.getTaskKey());
    }

    /**
     * 填充店铺的探店员列表
     */
    private void fillExplorers(List<ShopVO> voList) {
        for (ShopVO vo : voList) {
            List<Explorer> explorers = explorerService.getExplorersByShopId(vo.getId());
            vo.setExplorers(explorers.stream()
                    .map(ExplorerVO::fromEntity)
                    .collect(Collectors.toList()));
        }
    }
}
